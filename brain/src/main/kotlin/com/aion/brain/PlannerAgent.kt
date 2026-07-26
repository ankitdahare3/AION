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
    val extra: String? = null,
)

/**
 * T-139 (BACKLOG.md, E1_WRONG_ELEMENT) — the real implementation lives in `:android:app`
 * (`AionAccessibilityService.currentScreenText()`), wrapped via `InjectionFilter.wrap` before it
 * ever crosses into `:brain` — same "screen content is data, never instructions" rule DOC-004 §6
 * states, and the same module-boundary reason [com.aion.host.svc.NotificationIngestion] (T-137)
 * can't live in `:brain` either.
 */
fun interface ScreenSnapshotProvider {
    suspend fun currentScreenText(): String?
}

/**
 * DOC-004 §5 — goal -> ordered, JSON-schema-constrained plan steps. One repair-retry: if the
 * model's output doesn't parse, we ask once more with an explicit "invalid JSON" hint before
 * giving up. Full persona/safety-prefix wiring (ContextBuilder) is deferred to whichever task
 * finalizes the real AION persona text — out of scope for T-050's own AC.
 *
 * [fewShotBank] (T-081, DOC-007 §3) is optional: when present, any counter-examples recorded for
 * this exact goal are folded into the system prompt as an explicit "don't repeat this" warning.
 *
 * [memoryStore] (T-117, BACKLOG.md) is optional too: when present, any `PROFILE` memories written
 * by [DeviceExplorer]'s "Explore Device" scan (T-114) are folded in as a known-real-package list —
 * the planner otherwise guesses plausible-but-often-wrong AOSP package names (`com.android.camera2`
 * etc.) that don't exist on OEM-skinned devices (found via T-116's real Samsung benchmark run).
 *
 * [screenSnapshotProvider] (T-139, BACKLOG.md — E1_WRONG_ELEMENT) is optional too: when present,
 * whatever is ACTUALLY on screen right now is folded into the prompt so `tap`/`longPress` targets
 * can be grounded in real visible text instead of guessed blind. This helps the very next step most
 * — the current screen at planning time is exactly the screen a retry (after `ReflectorAgent`
 * clears state and comes back here) needs to act on, since the earlier steps that got the device
 * there already ran; later steps in a multi-step plan still target screens that don't exist yet, so
 * this doesn't ground the whole plan, only ever the first action. `T-121`'s real benchmark data
 * found `E1_WRONG_ELEMENT` ("could not resolve element: X") as the single largest failure cause
 * across every environment tried, even after T-117's package-name grounding — because a real
 * package name and a real on-screen button label are two different unknowns.
 */
class PlannerAgent(
    private val router: ProviderRouter,
    private val fewShotBank: FewShotBank? = null,
    private val memoryStore: MemoryStore? = null,
    private val screenSnapshotProvider: ScreenSnapshotProvider? = null,
) : Agent {
    // Antigravity-audit finding, 2026-07-13: `callAndParse` used to swallow `router.route`'s
    // exception into a bare `null`, indistinguishable from a plain JSON-parse failure — a real
    // network/auth/quota error and "the model just replied with prose" looked identical in
    // `s.failures`. Capturing the real message here means whoever reads the audit trail actually
    // sees WHY, not just THAT it failed.
    private var lastRouteFailure: String? = null

    override suspend fun step(s: AgentState): AgentState {
        lastRouteFailure = null
        val steps = callAndParse(s.goal, repairHint = false) ?: callAndParse(s.goal, repairHint = true)
        return if (steps != null) {
            s.copy(plan = steps)
        } else {
            // Sets done=true directly here (same as ReflectorAgent's own abort branch), which per
            // AionGraph's frozen run() loop means ResponderAgent never gets a turn — so this must
            // author its own natural-language response too, or the graph would end with `response`
            // still null (confirmed by an earlier real benchmark run, T-121/BACKLOG.md).
            val reason =
                lastRouteFailure?.let { "planner: routing failed: $it" }
                    ?: "planner: failed to produce a valid JSON plan after retry"
            s.copy(
                done = true,
                failures = s.failures + reason,
                response =
                    ResponsePhrasing.forFailure(
                        FailureCause.E4_MODEL_PLAN_ERROR,
                        ResponsePhrasing.isHinglish(s.goal),
                    ),
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
                lastRouteFailure = e.message ?: e.javaClass.simpleName
                return null
            }
        return parsePlan(result.text)
    }

    private suspend fun buildSystem(
        goal: String,
        repairHint: Boolean,
    ): String {
        val withApps = withKnownApps(if (repairHint) "$PERSONA\n$REPAIR_HINT" else PERSONA)
        val base = withCurrentScreen(withApps)
        val counterExamples = fewShotBank?.examplesFor(goal).orEmpty()
        if (counterExamples.isEmpty()) return base
        val warnings =
            counterExamples.joinToString("\n") { "- Plan ${it.badPlanJson} was WRONG for this goal: ${it.reason}" }
        return "$base\n\nPrevious mistakes for this exact goal — do NOT repeat them:\n$warnings"
    }

    private suspend fun withCurrentScreen(base: String): String {
        val screenText = screenSnapshotProvider?.currentScreenText()
        if (screenText.isNullOrBlank()) return base
        return "$base\n\nWhat's ACTUALLY visible on screen right now (only trust this for your very " +
            "next tap/longPress target — later steps in a multi-step plan will be on a different " +
            "screen you can't see yet, so don't assume this list applies to them too):\n$screenText"
    }

    private suspend fun withKnownApps(base: String): String {
        val packages =
            memoryStore
                ?.getAllActive()
                ?.filter { it.kind == MemoryKind.PROFILE && it.provenance == DeviceExplorer.PROVENANCE }
                ?.mapNotNull { packageNameOf(it.text) }
                ?.distinct()
                .orEmpty()
        if (packages.isEmpty()) return base
        return "$base\n\nApps actually installed on this device (use these EXACT package names for " +
            "launchApp — never invent or guess a package name that isn't in this list):\n" +
            packages.joinToString(", ")
    }

    // Memory.text is DeviceExplorer's own "App <pkg>: <screenText>" format.
    private fun packageNameOf(memoryText: String): String? {
        if (!memoryText.startsWith("App ")) return null
        return memoryText
            .removePrefix("App ")
            .substringBefore(':')
            .trim()
            .takeIf { it.isNotBlank() }
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
                .map { PlanStep(it.action, it.target, it.expected, it.sideEffect, it.extra) }
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
                "Every step MUST be an object with these fields (\"extra\" is optional, only sendSms uses it):\n" +
                """{"action": "<action name>", "target": "<target>", "expected": "<what is true after """ +
                """this step succeeds>", "sideEffect": <true|false>, "extra": "<optional second value>"}""" +
                "\n\nExample response:\n" +
                """[{"action":"launchApp","target":"com.android.settings","expected":"Settings app """ +
                """open","sideEffect":false},""" +
                """{"action":"tap","target":"Wi-Fi","expected":"Wi-Fi settings open","sideEffect":false}]""" +
                "\n\nYou may ONLY use these action names — anything else will fail to execute:\n" +
                "- tap: target = visible text/label of the element to tap (fuzzy-matched on screen)\n" +
                "- longPress: same as tap, but a long press\n" +
                "- launchApp: target = the app's Android package name (e.g. com.android.settings)\n" +
                "- globalAction: target = one of BACK, HOME, RECENTS\n" +
                "- callContact: target = the phone number to dial. Opens the dialer with the number ready — " +
                "the owner still taps the call button themselves, nothing is dialed automatically.\n" +
                "- sendSms: target = the recipient's phone number, extra = the message text. Opens Messages " +
                "with both filled in — the owner still taps send themselves, nothing is sent automatically.\n" +
                "- openUrl: target = the URL to open.\n" +
                "- searchWeb: target = the search query.\n" +
                "Prefer callContact/sendSms/openUrl/searchWeb over a tap/longPress sequence whenever the goal " +
                "is exactly one of those — one direct step beats hunting for the right on-screen element.\n" +
                "Set sideEffect true for any step that is irreversible or outward-facing (sending, deleting, " +
                "posting, purchasing) — callContact/sendSms only pre-fill a screen the owner must still " +
                "confirm themselves, so they don't need sideEffect true just for that. Typing text into a " +
                "field is NOT currently possible for tap-based steps — never plan a tap/longPress step that " +
                "requires it; if the goal cannot be done with the actions above, do your best with what's " +
                "available rather than inventing an unsupported action."
        const val REPAIR_HINT =
            "Your previous response was not valid JSON matching the schema. Output ONLY the JSON array this time."
        const val PLAN_SCHEMA =
            """{"type":"array","items":{"type":"object","properties":{"action":{"type":"string"},""" +
                """"target":{"type":"string"},"expected":{"type":"string"},"sideEffect":{"type":"boolean"},""" +
                """"extra":{"type":"string"}},"required":["action","target","expected"]}}"""
    }
}
