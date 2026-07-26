package com.aion.host.automation

import com.aion.brain.ActionExecutor
import com.aion.brain.ExecutionOutcome
import com.aion.brain.PlanStep
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-051 wiring — the real platform-side [ActionExecutor] (`:brain` is Android-independent, so it
 * only knows the interface; this is the concrete implementation). Resolves [PlanStep.target] via
 * [ElementResolver] against the live screen, then dispatches through [ActionDispatcher] (T-041).
 *
 * `type`/`swipe`/`scrollTo` aren't wired yet — [PlanStep] (frozen) has no field for typed text or
 * swipe direction, so there's no honest way to execute them from a plan step alone. They return a
 * clear "not yet supported" failure rather than a wrong guess; tracked in BACKLOG.md pending either
 * a PlanStep ADR or a richer target-encoding convention.
 *
 * T-053: success is decided by [StepVerifier]'s before/after a11y diff, not by [ActionResult] alone
 * — T-041's live verification found `dispatchGesture`'s completion callback unreliable (never fires
 * within 5s on multiple real devices/emulators even when the tap visibly worked), so a `Failure`
 * from the dispatcher is treated as inconclusive rather than a hard failure whenever the screen
 * diff independently confirms the expected outcome happened anyway.
 *
 * T-165 (BACKLOG.md, the last open thread from T-158/T-162/T-163/T-164's 30-step-loop
 * investigation) — `success` used to be `result is ActionResult.Success || verification.outcome ==
 * PASS`, which only implements HALF of the rule this doc already states: it correctly rescues a
 * dispatcher `Failure` when the screen diff confirms success anyway, but it ALSO let a dispatcher
 * `Success` silently override a `StepVerifier` outcome that was NOT `PASS` — the reverse direction,
 * never documented or intended. `ActionResult.Success` for every action type (`tap`/`longPress`/
 * `launchApp`, confirmed by reading `ActionDispatcher` directly) means only "no system-level
 * exception was thrown" — a raw gesture at the wrong coordinates (a mis-resolved element) or a
 * `launchApp` whose `startActivity` didn't throw still reports `Success` regardless of whether the
 * intended outcome actually happened. That let a wrong-element tap silently count as a real success
 * — `currentStep` would advance and `ReflectorAgent`/`FewShotBank` would never even see it — instead
 * of the `E1_WRONG_ELEMENT`/`E2_UI_CHANGED` failure it actually was. Now `success` is exactly
 * `verification.outcome == PASS`; a dispatcher `Failure` no longer needs special-casing to be
 * "rescued" by verification since that's just the default case already.
 */
@Singleton
class DispatcherActionExecutor
    @Inject
    constructor(
        private val dispatcher: ActionDispatcher,
        // Concrete type, not the OcrEngine interface — no second implementation exists yet to
        // justify a Hilt @Binds module for it (YAGNI); swap this if/when one does (see OcrEngine).
        private val ocrEngine: MlKitOcrEngine,
    ) : ActionExecutor {
        override suspend fun execute(step: PlanStep): ExecutionOutcome {
            val service =
                AionAccessibilityService.instance
                    ?: return ExecutionOutcome(
                        success = false,
                        observation = "",
                        error = "accessibility service not connected",
                    )
            val before = service.currentScreenText().orEmpty()

            val result =
                when (step.action.lowercase()) {
                    "launchapp" -> dispatcher.launchApp(step.target)
                    "callcontact" -> dispatcher.callNumber(step.target)
                    "sendsms" -> dispatcher.sendSms(step.target, step.extra.orEmpty())
                    "openurl" -> dispatcher.openUrl(step.target)
                    "searchweb" -> dispatcher.searchWeb(step.target)
                    "globalaction" -> {
                        val action =
                            GlobalAction.entries.find { it.name.equals(step.target, ignoreCase = true) }
                                ?: return ExecutionOutcome(false, before, "unknown global action: ${step.target}")
                        dispatcher.globalAction(action)
                    }
                    "tap", "longpress" -> {
                        val a11yElements = service.currentElements()
                        // DOC-012 §1 — vision is the fallback, only when a11y has nothing (canvas
                        // apps/games); paying the screenshot+OCR cost on every step would blow the
                        // ≤2.5s/step budget (DOC-012 §5) for no benefit when a11y already works.
                        val vision = if (a11yElements.isEmpty()) captureVision(service) else null
                        val resolved =
                            ElementResolver.resolve(a11yElements, ResolveQuery.ByText(step.target), vision)
                                ?: return ExecutionOutcome(false, before, "could not resolve element: ${step.target}")
                        if (step.action.equals("longpress", ignoreCase = true)) {
                            dispatcher.longPress(TapTarget.Element(resolved.element))
                        } else {
                            dispatcher.tap(TapTarget.Element(resolved.element))
                        }
                    }
                    else -> return ExecutionOutcome(
                        false,
                        before,
                        "action not yet supported by DispatcherActionExecutor: ${step.action}",
                    )
                }

            delay(DEBOUNCE_MS) // DOC-009 §4 — let the UI settle before capturing the post-action diff
            val after = service.currentScreenText().orEmpty()
            val verification = StepVerifier.verify(step.expected, before, after)
            val success = verification.outcome == VerificationOutcome.PASS
            return ExecutionOutcome(
                success = success,
                observation = after,
                error = if (success) null else (result as? ActionResult.Failure)?.reason ?: verification.reason,
            )
        }

        private suspend fun captureVision(service: AionAccessibilityService): VisionObservation? {
            val bitmap = service.captureScreenshot() ?: return null
            return ocrEngine.recognize(bitmap)
        }

        private companion object {
            const val DEBOUNCE_MS = 500L
        }
    }
