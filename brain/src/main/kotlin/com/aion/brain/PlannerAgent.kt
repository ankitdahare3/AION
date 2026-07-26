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
 * T-177 (owner-directed research: real, comparable projects — Panda AI/"blurr" specifically —
 * reliably complete multi-step device tasks with a SENSE -> THINK -> ACT loop that re-observes the
 * screen and decides exactly ONE next action every cycle, instead of committing to a whole
 * multi-step plan upfront) — this class used to generate the ENTIRE plan in one LLM call
 * (`step()` returned `s.copy(plan = <the full list>)`); its own prior KDoc already admitted why
 * that's fragile: "later steps in a multi-step plan still target screens that don't exist yet."
 * Rewritten to decide ONE step at a time: [step] APPENDS exactly one [PlanStep] to [AgentState.plan]
 * per call (or [DONE_STEP] once the goal is accomplished), grounded every single time in the
 * CURRENT real screen (via [screenSnapshotProvider]) and everything that's actually happened so far
 * this run (via [withRecentActions] — [AgentState.plan]/[AgentState.toolResults] the prior steps and
 * their real outcomes, not just the original goal). [AionGraphFactory]'s routing sends control back
 * here after every executed action instead of looping [ExecutorAgent] through a stale pre-built
 * plan — see that class's own KDoc for the graph-topology half of this change.
 *
 * An EMPTY parsed array is now a legitimate, meaningful result ("nothing more to do"), not a parse
 * failure — [parsePlan] no longer collapses `[]` into `null`. Only a genuinely malformed/unparseable
 * response counts as a failure requiring the one repair-retry.
 *
 * One repair-retry: if the model's output doesn't parse, we ask once more with an explicit "invalid
 * JSON" hint before giving up. Full persona/safety-prefix wiring (ContextBuilder) is deferred to
 * whichever task finalizes the real AION persona text — out of scope for T-050's own AC.
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
 * can be grounded in real visible text instead of guessed blind. Since T-177 this grounds EVERY
 * step now, not just the first one in a multi-step plan — the whole reason for the rewrite.
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
        val steps = callAndParse(s, repairHint = false) ?: callAndParse(s, repairHint = true)
        return when {
            steps == null -> {
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
            // T-177: an empty array means the model believes the goal is already accomplished —
            // append the DONE sentinel so AionGraphFactory's routing sends this to "responder"
            // instead of "executor" (see its own route() KDoc).
            steps.isEmpty() -> s.copy(plan = s.plan + DONE_STEP)
            else -> s.copy(plan = s.plan + steps.first())
        }
    }

    private suspend fun callAndParse(
        s: AgentState,
        repairHint: Boolean,
    ): List<PlanStep>? {
        val req =
            BrainRequest(
                taskType = TaskType.PLAN,
                system = buildSystem(s, repairHint),
                messages = listOf(Msg("user", s.goal)),
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
        s: AgentState,
        repairHint: Boolean,
    ): String {
        val withApps = withKnownApps(if (repairHint) "$PERSONA\n$REPAIR_HINT" else PERSONA)
        val withScreen = withCurrentScreen(withApps)
        val withHistory = withRecentActions(withScreen, s)
        val counterExamples = fewShotBank?.examplesFor(s.goal).orEmpty()
        if (counterExamples.isEmpty()) return withHistory
        val warnings =
            counterExamples.joinToString("\n") { "- Plan ${it.badPlanJson} was WRONG for this goal: ${it.reason}" }
        return "$withHistory\n\nPrevious mistakes for this exact goal — do NOT repeat them:\n$warnings"
    }

    private suspend fun withCurrentScreen(base: String): String {
        val screenText = screenSnapshotProvider?.currentScreenText()
        if (screenText.isNullOrBlank()) return base
        return "$base\n\nWhat's ACTUALLY visible on screen right now — ground your next action in " +
            "this real, current screen:\n$screenText"
    }

    // T-177 — the reactive loop's whole point: tell the model what it already did and whether each
    // attempt actually worked, so it can decide the honest next move instead of guessing blind or
    // repeating an action that already succeeded. `s.plan`/`s.toolResults` stay index-aligned by
    // construction: ExecutorAgent appends exactly one observation per executed step, in order.
    private fun withRecentActions(
        base: String,
        s: AgentState,
    ): String {
        if (s.plan.isEmpty()) return base
        val lines =
            s.plan.mapIndexed { i, step ->
                val outcome =
                    s.toolResults
                        .getOrNull(i)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { " -> $it" } ?: ""
                "${i + 1}. ${step.action}(${step.target})$outcome"
            }
        return "$base\n\nActions already taken this run, in order, with their real outcomes:\n" +
            lines.joinToString("\n") +
            "\n\nDecide the next single action honestly based on what actually happened above — if " +
            "the goal is already accomplished, respond with an empty array."
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

    companion object {
        /** [AionGraphFactory]'s routing checks for this exact action name to know the run is over. */
        const val DONE_ACTION = "done"
        val DONE_STEP = PlanStep(DONE_ACTION, "", "goal accomplished", false)

        // T-121 finding — ExecutorAgent routes every step through UIAutomationPlugin exclusively
        // (T-077), and DispatcherActionExecutor only actually implements these actions today;
        // "type"/"swipe"/"scrollTo" are declared in UIAutomationPlugin's manifest but return
        // "not yet supported" at execution time (PlanStep has no field for typed text, BACKLOG.md).
        // The planner had no idea any of this before — it was guessing action names blind.
        // T-121's second real finding: the original prompt said "matching this schema exactly" but
        // the schema itself only ever traveled in BrainRequest.jsonSchema — which NO provider
        // adapter actually transmits to any API (OpenAiCompatProvider/GeminiProvider send
        // model/messages/maxTokens only). Real models had never seen the schema and consistently
        // omitted the required "expected" field, failing every real parse; scripted-provider tests
        // never caught it because they echo canned JSON. The step shape now lives IN the prompt.
        private const val PERSONA =
            "You are AION's planner, operating in a sense-think-act loop: you decide ONE next " +
                "action at a time, not a whole plan upfront, because the screen can change in ways " +
                "you can't predict two steps ahead. Given the goal and what has already happened, " +
                "output ONLY a JSON array with EXACTLY ONE step describing the single next action " +
                "to take right now — or an EMPTY array [] if the goal is already fully accomplished " +
                "and nothing more needs to happen. No prose, no markdown fences, no explanation — " +
                "the JSON array is the entire response.\n\n" +
                "A step object has these fields (\"extra\" is optional, only sendSms uses it):\n" +
                """{"action": "<action name>", "target": "<target>", "expected": "<what is true after """ +
                """this step succeeds>", "sideEffect": <true|false>, "extra": "<optional second value>"}""" +
                "\n\nExample response (one more action still needed):\n" +
                """[{"action":"tap","target":"Wi-Fi","expected":"Wi-Fi settings open","sideEffect":false}]""" +
                "\n\nExample response (goal already accomplished, nothing left to do):\n[]" +
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
                "available rather than inventing an unsupported action. After you see the ACTUAL result of " +
                "your last action, decide honestly whether the goal is now accomplished — don't keep " +
                "proposing actions past that point, and don't claim done before it actually is."
        private const val REPAIR_HINT =
            "Your previous response was not valid JSON matching the schema. Output ONLY the JSON array this time."
        private const val PLAN_SCHEMA =
            """{"type":"array","items":{"type":"object","properties":{"action":{"type":"string"},""" +
                """"target":{"type":"string"},"expected":{"type":"string"},"sideEffect":{"type":"boolean"},""" +
                """"extra":{"type":"string"}},"required":["action","target","expected"]}}"""
    }
}
