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
            // Sets done=true directly here (same as ReflectorAgent's own abort branch), which per
            // AionGraph's frozen run() loop means ResponderAgent never gets a turn — so this must
            // author its own natural-language response too, or the graph would end with `response`
            // still null (confirmed by an earlier real benchmark run, T-121/BACKLOG.md).
            s.copy(
                done = true,
                failures = s.failures + "planner: failed to produce a valid JSON plan after retry",
                response = ResponsePhrasing.forFailure(FailureCause.E4_MODEL_PLAN_ERROR, ResponsePhrasing.isHinglish(s.goal)),
            )
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

    // T-121 finding — real models routinely ignore "no markdown fences" and wrap the array in
    // ```json ... ``` anyway, or add a stray sentence before/after it. Rather than a stricter
    // prompt (already tried, didn't help) or a second repair round-trip for something this
    // mechanical, extract from the first '[' to the last ']' before parsing — the same defensive
    // technique most real-world LLM-JSON integrations end up needing.
    private fun parsePlan(text: String): List<PlanStep>? {
        val jsonArray = extractJsonArray(text) ?: return null
        return try {
            Json
                .decodeFromString<List<PlanStepDto>>(jsonArray)
                .map { PlanStep(it.action, it.target, it.expected, it.sideEffect) }
                .takeIf { it.isNotEmpty() }
        } catch (e: SerializationException) {
            null
        }
    }

    private fun extractJsonArray(text: String): String? {
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start == -1 || end == -1 || end < start) return null
        return text.substring(start, end + 1)
    }

    private companion object {
        // T-121 finding — ExecutorAgent routes every step through UIAutomationPlugin exclusively
        // (T-077), and DispatcherActionExecutor only actually implements these 4 actions today;
        // "type"/"swipe"/"scrollTo" are declared in UIAutomationPlugin's manifest but return
        // "not yet supported" at execution time (PlanStep has no field for typed text, BACKLOG.md).
        // The planner had no idea any of this before — it was guessing action names blind.
        // T-121's second real finding: the original prompt said "matching this schema exactly" but
        // the schema itself only ever traveled in BrainRequest.jsonSchema — which NO provider
        // adapter actually transmits to any API (OpenAiCompatProvider/GeminiProvider send
        // model/messages/maxTokens only). Real models had never seen the schema and consistently
        // omitted the required "expected" field, failing every real parse; scripted-provider tests
        // never caught it because they echo canned JSON. The step shape now lives IN the prompt.
        const val PERSONA =
            "You are AION's planner. Given a goal, output ONLY a JSON array of steps. No prose, no " +
                "markdown fences, no explanation — the JSON array is the entire response.\n\n" +
                "Every step MUST be an object with ALL of these fields:\n" +
                """{"action": "<action name>", "target": "<target>", "expected": "<what is true after this step succeeds>", "sideEffect": <true|false>}""" +
                "\n\nExample response:\n" +
                """[{"action":"launchApp","target":"com.android.settings","expected":"Settings app open","sideEffect":false},""" +
                """{"action":"tap","target":"Wi-Fi","expected":"Wi-Fi settings open","sideEffect":false}]""" +
                "\n\nYou may ONLY use these action names — anything else will fail to execute:\n" +
                "- tap: target = visible text/label of the element to tap (fuzzy-matched on screen)\n" +
                "- longPress: same as tap, but a long press\n" +
                "- launchApp: target = the app's Android package name (e.g. com.android.settings)\n" +
                "- globalAction: target = one of BACK, HOME, RECENTS\n" +
                "Set sideEffect true for any step that is irreversible or outward-facing (sending, deleting, " +
                "posting, purchasing). Typing text into a field is NOT currently possible — never plan a step " +
                "that requires it; if the goal cannot be done with only tap/longPress/launchApp/globalAction, " +
                "do your best with what's available rather than inventing an unsupported action."
        const val REPAIR_HINT =
            "Your previous response was not valid JSON matching the schema. Output ONLY the JSON array this time."
        const val PLAN_SCHEMA =
            """{"type":"array","items":{"type":"object","properties":{"action":{"type":"string"},""" +
                """"target":{"type":"string"},"expected":{"type":"string"},"sideEffect":{"type":"boolean"}},""" +
                """"required":["action","target","expected"]}}"""
    }
}
