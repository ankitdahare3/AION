# BACKLOG.md — Proposed work not yet in TASKS.md

- **AionNotificationListener real ingestion logic** (android/app/src/main/java/com/aion/host/svc/AionNotificationListener.kt).
  Added 2026-07-09 as an empty shell purely so the "Notification access" PR-02 permission has a
  real component to grant (T-004). No task in TASKS.md currently owns building the actual
  `onNotificationPosted`/`onNotificationRemoved` content-ingestion + InjectionFilter wrapping
  (DOC-004 §6, DOC-009) — propose a task for this alongside or after T-040 (AionAccessibilityService),
  since both feed the same automation-context pipeline.

- **Replace `IntentClassifier` (T-033) with the real local-LLM classifier DOC-004 §3 specifies**
  ("Local 4B model classifies..."). Current v1 is a bilingual keyword/pattern heuristic (90% on a
  hand-labeled 100-utterance set) built because T-032 (llama.cpp JNI) was deliberately deferred —
  a native-build wall on this dev machine, owner's explicit call on 2026-07-09. Once T-032 lands,
  swap the implementation behind the same `classify(utterance: String): Intent` shape.

- **Expand `ContextBuilder` (T-034) to the full DOC-004 §4 spec**: user-profile summary (≤300 tok),
  vector-recalled memories (top-k=5), compressed a11y tree (≤2000 tok), and tool schemas. v1 only
  covers the persona+safety immutable prefix + last N=6 turns, because Memory (T-06x), the a11y
  reader (T-040), and PluginManager (T-070+) don't exist yet. Add each as its own budgeted section
  once its source system ships — `BuiltContext`'s shape shouldn't need to change, just the budget
  split.

- **`DispatcherActionExecutor` (T-051) can't execute `type`/`swipe`/`scrollTo` plan steps.** The
  frozen `PlanStep` (`action, target, expected, sideEffect`) has no field for typed text or a swipe
  direction/distance, so there's no honest way to derive them from a plan step alone — currently
  returns a clear "not yet supported" `ExecutionOutcome` instead of guessing. Needs either an ADR
  to add a field to `PlanStep`, or a convention for encoding it into `target` (e.g.
  `"message box::running late"`) that `PlannerAgent`'s prompt and `DispatcherActionExecutor`'s
  parsing both agree on. Revisit once a real goal actually needs typed input (e.g. "send a message
  saying...") to drive the design instead of guessing ahead of need.

- **`PlannerAgent` (T-050) doesn't use `ContextBuilder`'s persona+safety prefix** — it has its own
  minimal, self-contained planning instructions. Wire it once the real AION persona/safety-rules
  text exists as an actual constant somewhere (currently only test-literal placeholders exist);
  likely lands alongside T-053's full graph wiring.

- **T-062 (MemoryAgent write policy) must confidence-gate writes and tag screen/OCR-sourced facts
  as `UNVERIFIED`, never auto-promoted** (DOC-017 T5, found in the T-120 security audit,
  docs/SECURITY_AUDIT.md). `Memory.provenance` (T-111) is currently a free-form string with no
  `UNVERIFIED` concept anywhere in code; `MemoryConsolidator`'s promote-on-merge logic boosts
  confidence on any duplicate cluster with no source-awareness at all. Not fixed speculatively now
  because nothing writes a real memory row in production yet (`MemoryWriterAgent` is still an
  intentional no-op stub) — but T-062 must not ship without this gate, and `MemoryConsolidator`
  needs a matching update the same day.

- **A biometric app-lock for T6 (physical device access), DOC-017 §5's "AION app-lock (biometric)"**
  — found genuinely absent in the T-120 security audit (zero `BiometricPrompt` usage anywhere).
  Device Owner scaffolding + FBE already cover the encryption half; this is the missing
  "lock the app itself" half. Sized like its own task (BiometricPrompt integration, a lock screen,
  session-timeout logic), not a line-level audit fix — propose as a new task (suggested T-124)
  rather than folding into T-120.

- **PII redaction pre-egress (DOC-017 T7)** — no regex/NER redaction pass exists anywhere near a
  network call. Not built speculatively in T-120 because no live code path currently sends
  screen/vision content to any cloud endpoint (OCR is on-device-only, T-100; Gmail/Telegram, T-102,
  only ever send what the user explicitly composes). Must land alongside whichever task first wires
  a real cloud vision/multimodal path (DOC-012 §2), not after.

- **`AionAccessibilityService.captureScreenshot()` fails on every real call with "Services don't
  have the capability of taking the screenshot."** — found in T-121's real 50-task benchmark run
  (4/50 tasks hit this, all via `DispatcherActionExecutor.captureVision()`'s a11y-empty fallback:
  MAPS "current location check karo", "distance to office check karo", "nearby petrol pump dhundo",
  MEDIA "video pause karo"). `accessibility_service_config.xml` declares no screenshot capability
  at all (`android:canRetrieveWindowContent` only) — `takeScreenshot()` (API 30+) needs the service
  to actually hold `AccessibilityServiceInfo.CAPABILITY_CAN_TAKE_SCREENSHOT`, which isn't requested
  anywhere. Not fixed inline while writing up T-121 because the exact declaration mechanism
  (manifest XML attribute vs. a runtime `AccessibilityServiceInfo.setCapabilities()` call, and its
  real minSdk gate) needs verifying against the actual framework source/decompile first — same
  "confirm before touching a security-relevant service config" discipline as T-110's
  `Constraints.Builder` decompile — rather than guessing an attribute name into a shipped XML file.
  Whoever picks this up next: fix this FIRST, then re-run T-121's benchmark — it's the cheapest,
  most isolated fix among everything the run surfaced (one config gap, one clear error message
  naming the exact missing capability) and should lift MAPS/MEDIA's real pass rate directly.

- **T-121's real 50-task benchmark honest result: 7/50 (14%), well below the ≥60% target** — see
  `docs/T-121_BENCHMARK_REPORT.json` (full per-task results) and TASKS.md's T-121 entry for the
  full breakdown. Two other real gaps besides the screenshot one above, in order of how many tasks
  they cost: (1) 19/50 failures are `no launch intent for <pkg>` — this bare `aion_test` AVD simply
  doesn't have WhatsApp/Calendar/Paytm/GPay/banking apps/Play Music/Gallery installed, an ENVIRONMENT
  gap, not an AION bug; re-running against a Play Store-enabled AVD (or the eventual real dedicated
  phone) with those apps actually installed should resolve most of this bucket on its own, no code
  change needed. (2) 16/50 are `could not resolve element: <X>` — the planner (real Groq/NVIDIA/
  OpenRouter output, not scripted) guesses plausible-sounding on-screen label text ("Wi-Fi toggle",
  "Turn on", "Missed calls", "Save") that doesn't match the real app's actual visible strings closely
  enough for `ElementResolver`'s fuzzy match; this is a genuine planner-grounding gap — the planner
  has no real knowledge of what's actually on any given app's screen before guessing a target string.
  T-114's new device-profile memories (`DeviceExplorer`/`DeviceExplorationWorker`) are aimed exactly
  at this problem but aren't wired into `PlannerAgent`'s prompt/context yet — that wiring is the
  natural next step and should directly improve this bucket once built. (3) 4/50 are the
  already-known `gesture callback never fired within 5000ms` (`ActionDispatcher`'s own documented
  emulator-reliability gap, T-041).

- **`AionGraphFactory`'s route closure has a stale `lastFailureCount` bug** — found while tracing
  the graph's actual flow for T-115 (ResponderAgent natural-language replies). `lastFailureCount`
  is a `var` captured once per `create()` call and only ever updated inside the
  `"executor" -> s.failures.size > lastFailureCount` branch; `ReflectorAgent`'s own recoverable-cause
  branch resets `s.failures` to `emptyList()` on every retry, but nothing resets `lastFailureCount`
  back down to match. Effect: the FIRST failure for a goal is correctly detected as new (`failures.size`
  1 > `lastFailureCount` 0) and routes to `ReflectorAgent` for a possible retry; if the SAME failure
  happens again after the retry, `failures.size` is back to 1 (list was cleared, then one new entry
  added) — which is NOT greater than the already-recorded `lastFailureCount` (1) — so the "new
  failure" check silently fails and the run falls through to whatever `currentStep >= plan.size`
  says instead, which for a short plan is often `"responder"` directly, skipping `ReflectorAgent`'s
  classification entirely on the SECOND occurrence of the exact same recoverable failure. This is
  real, reproducible in the T-121 benchmark data (e.g. "wifi on karo" ending at `stepCount: 16`,
  well under the 30-step circuit breaker, with the raw `"could not resolve element: Wi-Fi toggle"`
  text landing straight in `ResponderAgent`'s failures-branch) — not fixed as part of T-115 because
  it's a retry-count/recovery-correctness bug, not a response-tone bug, and T-115's `ResponsePhrasing`
  fix already makes BOTH the reflector-routed and responder-routed outcomes equally natural/safe to
  show a user. Whoever picks this up: `lastFailureCount` needs to track total failures ever seen
  (e.g. increment a separate counter in `ReflectorAgent`'s branches, or compare against a running
  total instead of the post-clear list size) so every recoverable failure gets its intended retry
  attempt, not just the first one per goal.

- **`ExecutorAgent`'s empty-plan "plan complete" branch is dead code today, but would leak an
  unnaturalized string if ever reached** — found alongside T-115. `s.plan.getOrNull(s.currentStep)
  ?: return s.copy(done = true, response = s.response ?: "plan complete")` only fires when
  `ExecutorAgent` is entered with `currentStep >= plan.size` from the very start, which requires an
  empty plan — `PlannerAgent`'s own successful-parse path always returns a non-empty plan
  (`.takeIf { it.isNotEmpty() }`), so this is unreachable via any real call path today. Not fixed in
  T-115 (out of scope, and touching genuinely-dead code isn't worth the risk) — but if `PlannerAgent`
  or `AgentState`'s construction ever changes to allow an empty plan, this branch needs the same
  `ResponsePhrasing` treatment the other three call sites got, or a raw "plan complete" string would
  reach the user unnaturalized.

- **Planner guesses AOSP-style package names, which don't exist on Samsung's (or any OEM-skinned)
  device** — found via T-116's real-device benchmark re-run (real Samsung SM-G990E, One UI). Stock
  Android package names the planner (and `BenchmarkTasks`' own launch-goal phrasing) assume —
  `com.android.camera2`, `com.android.gallery3d`, `com.android.mms`, `com.android.calendar` — simply
  don't exist on this device; Samsung ships its own Camera/Gallery/Messages/Calendar/Contacts apps
  under entirely different package names. Distinct failure mode from "the app genuinely isn't
  installed" (T-121's finding on the bare AOSP emulator) — here the equivalent app IS installed,
  just under a name the planner never considers, so every OEM device this ships to will hit its own
  version of this gap. Not a hardcoded Samsung package list (a losing game across OEMs/versions) —
  the real fix is feeding the planner real, device-specific package names at prompt time. T-114's
  `DeviceExplorer`/`DeviceExplorationWorker` already scans every installed launchable app's package
  name into `Memory(kind = PROFILE)` rows; the natural next step (already noted as an open gap in
  T-114's own scope note) is reading those PROFILE memories back into `PlannerAgent`'s context so it
  can pick a real installed package instead of guessing an AOSP one.

- **A plain greeting can get planned as a device-automation task instead of a chat reply** — found
  during T-130's live chat-screen verification: the goal "Namaste AION" (a pure US1 greeting, no
  action implied) produced a real multi-step plan that opened Settings and asked to tap
  "Auto-rotate". `IntentClassifier` (T-033, still the v1 keyword/pattern heuristic) is the likely
  culprit — a short greeting with no action verb has few tokens to classify on and may be
  defaulting to SIMPLE_ACTION/MULTI_STEP rather than CHAT. Not fixed inline here (T-130's own scope
  was verifying the chat screen and the denial-response bug it exposed, not planner-classification
  accuracy) — worth a dedicated look once IntentClassifier's real-LLM upgrade (already tracked
  above, blocked on T-032) lands, or sooner if it recurs during T-131's benchmark re-run: check
  whether any of the 50 benchmark goals are pure-chat phrasing being misclassified the same way.

- **`IntentClassifier` (T-033) is built but never wired into the live `AionGraphFactory` graph** —
  found 2026-07-12 while trying to verify T-137's AC ("a chat question about the latest
  notification" should surface it). The graph always routes every goal straight through
  `PlannerAgent`, which only ever produces a tap/launchApp/globalAction plan — there is no
  conversational/CHAT path at all today, regardless of what IntentClassifier would say. This is the
  exact same root cause as the "Namaste AION" greeting getting planned as a Settings-automation
  task (found during T-130's live verification, noted above). It also means no goal has any path to
  answer a factual question from memory: `PlannerAgent.withKnownApps` (T-117) only ever folds
  `PROFILE` memories into the ACTION-planning prompt, never a conversational response. Wiring
  IntentClassifier into the graph — routing CHAT-classified goals to a real conversational
  node/path that can read recent memories (including T-137's new notification-sourced FACT
  memories) and answer in natural language — is real, scoped work: touches `AionGraphFactory`'s
  node map and routing closure, likely needs a new `Agent` (a "chat responder" distinct from
  `ResponderAgent`'s post-execution phrasing role) and a decision on how ContextBuilder folds
  recent FACT memories into that path's prompt. Sized bigger than a 2-3 file task — propose as its
  own task once picked up, not built speculatively alongside T-137.
