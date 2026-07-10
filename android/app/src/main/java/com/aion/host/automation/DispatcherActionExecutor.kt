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
 */
@Singleton
class DispatcherActionExecutor
    @Inject
    constructor(
        private val dispatcher: ActionDispatcher,
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
                    "globalaction" -> {
                        val action =
                            GlobalAction.entries.find { it.name.equals(step.target, ignoreCase = true) }
                                ?: return ExecutionOutcome(false, before, "unknown global action: ${step.target}")
                        dispatcher.globalAction(action)
                    }
                    "tap", "longpress" -> {
                        val resolved =
                            ElementResolver.resolve(service.currentElements(), ResolveQuery.ByText(step.target))
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
            val success = result is ActionResult.Success || verification.outcome == VerificationOutcome.PASS
            return ExecutionOutcome(
                success = success,
                observation = after,
                error = if (success) null else (result as? ActionResult.Failure)?.reason ?: verification.reason,
            )
        }

        private companion object {
            const val DEBOUNCE_MS = 500L
        }
    }
