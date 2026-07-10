package com.aion.brain

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
private data class PlanStepDto(
    val action: String,
    val target: String,
    val expected: String,
    val sideEffect: Boolean = false,
)

/**
 * DOC-004 §5 — goal -> ordered, JSON-schema-constrained plan steps. One repair-retry: if the
 * model's output doesn't parse, we ask once more with an explicit "invalid JSON" hint before
 * giving up. Full persona/safety-prefix wiring (ContextBuilder) is deferred to whichever task
 * finalizes the real AION persona text — out of scope for T-050's own AC.
 *
 * [fewShotBank] (T-081, DOC-007 §3) is optional: when present, any counter-examples recorded for
 * this exact goal are folded into the system prompt as an explicit "don't repeat this" warning.
 */
class PlannerAgent(
    private val router: ProviderRouter,
    private val fewShotBank: FewShotBank? = null,
) : Agent {
    override suspend fun step(s: AgentState): AgentState {
        val steps = callAndParse(s.goal, repairHint = false) ?: callAndParse(s.goal, repairHint = true)
        return if (steps != null) {
            s.copy(plan = steps)
        } else {
            s.copy(done = true, failures = s.failures + "planner: failed to produce a valid JSON plan after retry")
        }
    }

    private suspend fun callAndParse(
        goal: String,
        repairHint: Boolean,
    ): List<PlanStep>? {
        val req =
            BrainRequest(
                taskType = TaskType.PLAN,
                system = buildSystem(goal, repairHint),
                messages = listOf(Msg("user", goal)),
                jsonSchema = PLAN_SCHEMA,
            )
        val result =
            try {
                router.route(req)
            } catch (e: Exception) {
                return null
            }
        return parsePlan(result.text)
    }

    private fun buildSystem(
        goal: String,
        repairHint: Boolean,
    ): String {
        val base = if (repairHint) "$PERSONA\n$REPAIR_HINT" else PERSONA
        val counterExamples = fewShotBank?.examplesFor(goal).orEmpty()
        if (counterExamples.isEmpty()) return base
        val warnings =
            counterExamples.joinToString("\n") { "- Plan ${it.badPlanJson} was WRONG for this goal: ${it.reason}" }
        return "$base\n\nPrevious mistakes for this exact goal — do NOT repeat them:\n$warnings"
    }

    private fun parsePlan(text: String): List<PlanStep>? =
        try {
            Json
                .decodeFromString<List<PlanStepDto>>(text.trim())
                .map { PlanStep(it.action, it.target, it.expected, it.sideEffect) }
                .takeIf { it.isNotEmpty() }
        } catch (e: SerializationException) {
            null
        }

    private companion object {
        const val PERSONA =
            "You are AION's planner. Given a goal, output ONLY a JSON array of steps, matching this " +
                "schema exactly. No prose, no markdown fences, no explanation — the JSON array is the entire response."
        const val REPAIR_HINT =
            "Your previous response was not valid JSON matching the schema. Output ONLY the JSON array this time."
        const val PLAN_SCHEMA =
            """{"type":"array","items":{"type":"object","properties":{"action":{"type":"string"},""" +
                """"target":{"type":"string"},"expected":{"type":"string"},"sideEffect":{"type":"boolean"}},""" +
                """"required":["action","target","expected"]}}"""
    }
}
