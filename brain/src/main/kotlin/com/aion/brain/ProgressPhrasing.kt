package com.aion.brain

/**
 * Streaming execution progress (2026 backend upgrade) — turns an in-flight [AgentState] into a
 * short, human phrase for the chat UI, same "never show a raw internal string" rule
 * [ResponsePhrasing] already follows for the terminal response. Pure and Android-free so it's
 * testable without a device, same "algorithm here, platform mechanics in :android:app" split this
 * codebase already uses (e.g. [DeviceExplorer] vs. its `:android:app` worker).
 *
 * Reads only fields [AionGraph.run] already sets on every step ([AgentState.plan]/`currentStep`/
 * `needsApproval`/`failures`/`done`) — no new state, no change to the frozen graph's signature.
 */
object ProgressPhrasing {
    fun describe(
        state: AgentState,
        hinglish: Boolean,
    ): String =
        when {
            state.done -> if (hinglish) DONE_HI else DONE_EN
            state.needsApproval -> if (hinglish) WAITING_APPROVAL_HI else WAITING_APPROVAL_EN
            state.plan.isEmpty() -> if (hinglish) PLANNING_HI else PLANNING_EN
            state.failures.isNotEmpty() -> if (hinglish) RETRYING_HI else RETRYING_EN
            state.currentStep < state.plan.size -> describeStep(state, hinglish)
            else -> if (hinglish) FINISHING_HI else FINISHING_EN
        }

    private fun describeStep(
        state: AgentState,
        hinglish: Boolean,
    ): String {
        val step = state.plan[state.currentStep]
        val position = "${state.currentStep + 1}/${state.plan.size}"
        return if (hinglish) {
            "Step $position: ${step.action} — ${step.target}"
        } else {
            "Step $position: ${step.action} ${step.target}"
        }
    }

    private const val PLANNING_EN = "Planning..."
    private const val PLANNING_HI = "Plan bana raha hoon..."

    private const val WAITING_APPROVAL_EN = "Waiting for your approval..."
    private const val WAITING_APPROVAL_HI = "Aapke approval ka intezaar kar raha hoon..."

    private const val RETRYING_EN = "That didn't work, trying a different way..."
    private const val RETRYING_HI = "Wo nahi hua, doosre tarike se try kar raha hoon..."

    private const val FINISHING_EN = "Finishing up..."
    private const val FINISHING_HI = "Bas ho hi raha hai..."

    private const val DONE_EN = "Done."
    private const val DONE_HI = "Ho gaya."
}
