package com.aion.host.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.aion.host.security.AuditLogger
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/** Where a tap/longPress lands: a resolved element's bounds-center, or a raw screen point. */
sealed class TapTarget {
    data class Element(
        val ref: ElementRef,
    ) : TapTarget()

    data class Point(
        val x: Int,
        val y: Int,
    ) : TapTarget()
}

/** A raw [TapTarget.Point] is trusted as-is; an [TapTarget.Element] must have valid on-screen bounds. */
private fun TapTarget.isOnScreen(): Boolean =
    when (this) {
        is TapTarget.Point -> true
        is TapTarget.Element -> ref.bounds.isOnScreen()
    }

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

enum class GlobalAction { BACK, HOME, RECENTS }

sealed class ActionResult {
    object Success : ActionResult()

    data class Failure(
        val reason: String,
    ) : ActionResult()
}

/**
 * DOC-009 §3 — dispatches UI actions against the live [AionAccessibilityService]: tap/longPress/swipe
 * via `dispatchGesture`, scrollTo as a bounded swipe, type via `ACTION_SET_TEXT` on a re-resolved live
 * node, global nav via `performGlobalAction`, launchApp via `PackageManager`. `notificationAction` is
 * not a distinct mechanism — DOC-009 lists it as a primitive, but mechanically it's a tap on an element
 * already visible in the notification shade (itself reachable via [swipe]), so it's a thin alias.
 *
 * §6 — every call rate-limits to <=1/300ms via [RateLimiter] and is written to [AuditLogger] before
 * the actual dispatch, in that order (limiter can suspend and the audit entry should reflect the
 * action actually about to happen, not one that's still queued).
 */
@Singleton
class ActionDispatcher
    @Inject
    constructor(
        private val auditLogger: AuditLogger,
    ) {
        private val rateLimiter = RateLimiter()

        suspend fun tap(target: TapTarget): ActionResult =
            withService("tap", payloadFor(target)) { service ->
                if (!target.isOnScreen()) {
                    ActionResult.Failure("element is off-screen or has stale bounds: $target")
                } else {
                    val (x, y) = pointOf(target)
                    dispatchGesture(service, tapGesture(x, y, durationMs = 80))
                }
            }

        suspend fun longPress(target: TapTarget): ActionResult =
            withService("longPress", payloadFor(target)) { service ->
                if (!target.isOnScreen()) {
                    ActionResult.Failure("element is off-screen or has stale bounds: $target")
                } else {
                    val (x, y) = pointOf(target)
                    dispatchGesture(service, tapGesture(x, y, durationMs = 600))
                }
            }

        suspend fun swipe(
            direction: SwipeDirection,
            bounds: Bounds,
        ): ActionResult =
            withService("swipe", """{"direction":"$direction","bounds":"$bounds"}""") { service ->
                if (!bounds.isOnScreen()) {
                    ActionResult.Failure("swipe bounds are off-screen or stale: $bounds")
                } else {
                    val (x1, y1, x2, y2) = swipePathFor(direction, bounds)
                    dispatchGesture(service, swipeGesture(x1, y1, x2, y2, durationMs = 300))
                }
            }

        /** A single scroll step within [container]'s bounds; callers/StepVerifier decide whether to repeat. */
        suspend fun scrollTo(
            container: ElementRef,
            direction: SwipeDirection,
        ): ActionResult = swipe(direction, container.bounds)

        suspend fun type(
            text: String,
            field: ElementRef,
        ): ActionResult =
            withService("type", """{"field":"${field.id}","length":${text.length}}""") { service ->
                val node =
                    service.findNodeById(field.id)
                        ?: return@withService ActionResult.Failure("element ${field.id} not found on current screen")
                try {
                    val args =
                        Bundle().apply {
                            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                        }
                    val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    if (ok) ActionResult.Success else ActionResult.Failure("ACTION_SET_TEXT returned false")
                } finally {
                    @Suppress("DEPRECATION")
                    node.recycle()
                }
            }

        suspend fun globalAction(action: GlobalAction): ActionResult =
            withService("globalAction:${action.name}", "{}") { service ->
                val code =
                    when (action) {
                        GlobalAction.BACK -> AccessibilityService.GLOBAL_ACTION_BACK
                        GlobalAction.HOME -> AccessibilityService.GLOBAL_ACTION_HOME
                        GlobalAction.RECENTS -> AccessibilityService.GLOBAL_ACTION_RECENTS
                    }
                if (service.performGlobalAction(code)) {
                    ActionResult.Success
                } else {
                    ActionResult.Failure("performGlobalAction returned false")
                }
            }

        suspend fun launchApp(packageName: String): ActionResult =
            withService("launchApp", """{"package":"$packageName"}""") { service ->
                val intent =
                    service.packageManager.getLaunchIntentForPackage(packageName)
                        ?: return@withService ActionResult.Failure("no launch intent for $packageName")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    service.startActivity(intent)
                    ActionResult.Success
                } catch (e: android.content.ActivityNotFoundException) {
                    ActionResult.Failure("startActivity failed: ${e.message}")
                }
            }

        /** DOC-009 §3's `notification(action)` primitive — see class doc: it's just a tap. */
        suspend fun notificationAction(ref: ElementRef): ActionResult = tap(TapTarget.Element(ref))

        private suspend fun withService(
            actionName: String,
            payloadJson: String,
            perform: suspend (AionAccessibilityService) -> ActionResult,
        ): ActionResult {
            val service =
                AionAccessibilityService.instance
                    ?: return ActionResult.Failure("accessibility service not connected")
            rateLimiter.acquire()
            auditLogger.record(actor = "ActionDispatcher", action = actionName, payloadJson = payloadJson)
            return perform(service)
        }

        private fun payloadFor(target: TapTarget): String =
            when (target) {
                is TapTarget.Point -> """{"x":${target.x},"y":${target.y}}"""
                is TapTarget.Element -> """{"elementId":"${target.ref.id}"}"""
            }

        private fun pointOf(target: TapTarget): Pair<Int, Int> =
            when (target) {
                is TapTarget.Point -> target.x to target.y
                is TapTarget.Element -> {
                    val b = target.ref.bounds
                    (b.left + b.right) / 2 to (b.top + b.bottom) / 2
                }
            }

        private data class SwipePath(
            val x1: Int,
            val y1: Int,
            val x2: Int,
            val y2: Int,
        )

        private fun swipePathFor(
            direction: SwipeDirection,
            bounds: Bounds,
        ): SwipePath {
            val cx = (bounds.left + bounds.right) / 2
            val cy = (bounds.top + bounds.bottom) / 2
            val marginX = ((bounds.right - bounds.left) * 0.2).toInt()
            val marginY = ((bounds.bottom - bounds.top) * 0.2).toInt()
            return when (direction) {
                SwipeDirection.UP -> SwipePath(cx, bounds.bottom - marginY, cx, bounds.top + marginY)
                SwipeDirection.DOWN -> SwipePath(cx, bounds.top + marginY, cx, bounds.bottom - marginY)
                SwipeDirection.LEFT -> SwipePath(bounds.right - marginX, cy, bounds.left + marginX, cy)
                SwipeDirection.RIGHT -> SwipePath(bounds.left + marginX, cy, bounds.right - marginX, cy)
            }
        }

        private fun tapGesture(
            x: Int,
            y: Int,
            durationMs: Long,
        ): GestureDescription {
            val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
            return GestureDescription
                .Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                .build()
        }

        private fun swipeGesture(
            x1: Int,
            y1: Int,
            x2: Int,
            y2: Int,
            durationMs: Long,
        ): GestureDescription {
            val path =
                Path().apply {
                    moveTo(x1.toFloat(), y1.toFloat())
                    lineTo(x2.toFloat(), y2.toFloat())
                }
            return GestureDescription
                .Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                .build()
        }

        private suspend fun dispatchGesture(
            service: AccessibilityService,
            gesture: GestureDescription,
        ): ActionResult =
            try {
                withTimeout(GESTURE_TIMEOUT_MS) {
                    suspendCancellableCoroutine { cont ->
                        val callback =
                            object : AccessibilityService.GestureResultCallback() {
                                override fun onCompleted(gestureDescription: GestureDescription?) {
                                    if (cont.isActive) cont.resumeWith(Result.success(ActionResult.Success))
                                }

                                override fun onCancelled(gestureDescription: GestureDescription?) {
                                    if (cont.isActive) {
                                        cont.resumeWith(Result.success(ActionResult.Failure("gesture cancelled")))
                                    }
                                }
                            }
                        // Explicit main-thread Handler: relying on the docs' "null = service's own
                        // main thread" default left the callback undelivered in practice on-device.
                        val dispatched = service.dispatchGesture(gesture, callback, mainHandler)
                        if (!dispatched && cont.isActive) {
                            cont.resumeWith(Result.success(ActionResult.Failure("dispatchGesture returned false")))
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                ActionResult.Failure("gesture callback never fired within ${GESTURE_TIMEOUT_MS}ms")
            }

        private companion object {
            const val GESTURE_TIMEOUT_MS = 5_000L
        }

        private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    }
