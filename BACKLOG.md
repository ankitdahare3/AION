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

- ~~**A plain greeting can get planned as a device-automation task instead of a chat reply**~~ —
  **RESOLVED 2026-07-13 (T-150)**: `IntentRoutingAgent` now sits in the graph's "planner" slot,
  classifying via `IntentClassifier` and diverting CHAT-intent goals to a new `ChatAgent` (real
  `ProviderRouter` CHAT call) instead of planning a device action for them. Live-verified: "Namaste
  AION" now gets a real conversational reply ("Namaste, kaise ho aap?"), while "wifi on karo" still
  correctly reaches the real automation path. See TASKS.md EPIC 17.
  Still open, deliberately not part of T-150's scope: `PlannerAgent.withKnownApps` (T-117) still
  only folds `PROFILE` memories into the ACTION-planning prompt — `ChatAgent` doesn't read any
  memories yet either, so a factual question ("what was that HR mail about?") still won't get a
  memory-grounded answer, just a generic LLM reply. That's real follow-up work, not built
  speculatively here — needs a decision on how ContextBuilder folds recent FACT/notification
  memories into ChatAgent's prompt.

- **Many goals now loop to `AionGraph`'s 30-step circuit breaker instead of resolving or failing
  fast** — found 2026-07-13 while verifying T-139's real-screen-grounding fix. E1_WRONG_ELEMENT
  genuinely dropped to zero (T-139's whole point), but ~18 of the 50-task benchmark's failures now
  have `stepCount` at 32-33 (right at/above the ceiling) and land in `ReflectorAgent`'s "no failures
  to reflect on" branch — i.e., the graph keeps retrying without ever reaching a stable pass OR a
  classifiable failure, burning 30+ real LLM round-trips per goal (60-90s latency each) before
  giving up. Plausible cause: with real screen text now folded into EVERY planning call (T-139),
  a retry after a partial success might see a *slightly different* screen state each time (e.g.
  after a toggle flips, or a partial scroll), causing the planner to keep generating a fresh
  plausible-looking plan that doesn't quite match `StepVerifier`'s exact-text `expected` check,
  looping instead of converging or giving up. Not fixed here — deliberately out of T-139's own
  scope (its own AC was "does E1 improve," not "why do other goals loop instead"). Two candidate
  angles for whoever picks this up: (1) `StepVerifier`'s exact-substring `expected` match may be
  too strict now that the planner sees real, exact on-screen text and could write `expected` values
  that are technically-correct-but-differently-worded from what actually appears after the action —
  worth checking whether fuzzy-matching `expected` the same way `ElementResolver` fuzzy-matches
  `target` would help; (2) a lower step ceiling *specifically for the same goal retried N times*
  (distinct from the existing flat 30-step ceiling) might convert a slow loop into an honest,
  faster UNKNOWN failure without needing to diagnose the root loop cause first.

- **`SetupPermission.ACCESSIBILITY.isGranted()` is hardcoded to `false` forever** — found 2026-07-13
  while touching `SetupPermission.kt` for T-010. The comment says "No AccessibilityService is
  declared yet (ships in T-040)", but T-040 shipped `AionAccessibilityService` long ago — the setup
  wizard's Accessibility row permanently shows "Grant" even after the owner actually enables it in
  Settings. The real check should mirror `NOTIFICATION_ACCESS`'s own pattern: read
  `Settings.Secure.ACCESSIBILITY_ENABLED` + `enabled_accessibility_services` and check for
  `AionAccessibilityService`'s component name, same as `enabled_notification_listeners` already
  does for `AionNotificationListener`. Not fixed inline — out of scope for the task being worked on
  when found, and touching the setup wizard's own permission-status logic deserves its own
  isolated verification pass rather than a drive-by edit.

- **No formal NOTICE/THIRD_PARTY_LICENSES file for bundled model assets** — found 2026-07-13
  during T-011. `assets/models/*.onnx` (melspectrogram, embedding, alexa_v0.1, silero_vad) are
  openWakeWord v0.5.1 release assets (Apache-2.0) and Silero VAD (MIT); the Kotlin port in
  `voice/wakeword/` is also structurally derived from openWakeWord's own `model.py`/`utils.py`
  (Apache-2.0). Attribution is documented in code comments/PROGRESS.md today but there's no
  repo-root NOTICE file — the project currently has no LICENSE file at all, so this is really "add
  one before any public release," not urgent for solo/private use.

- **Wake-word/VAD thresholds are fixed constants, not owner-tunable** — found 2026-07-13 during
  T-011. `WakeWordDetector`'s `threshold`/`vadThreshold` (both 0.5) and `VoiceForegroundService`'s
  `WAKE_THRESHOLD`/`WAKE_DEBOUNCE_MS` are hardcoded. Fine for now (no UI surface asks for this yet,
  and the real "AION" model from T-012 will need its own calibrated threshold anyway once trained
  on real false-accept data) — revisit once T-012's `<1 false-accept/hr` AC needs a knob to tune.
