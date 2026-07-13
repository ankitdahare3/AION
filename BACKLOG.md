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

  **Update 2026-07-13 (T-156)**: candidate angle (1) is now implemented — `StepVerifier.verify`
  fuzzy-matches `expected` against post-action text via `TextSimilarity`, not just an exact
  substring. Unit-tested, but whether it actually reduces the real 30-step-loop rate on the 50-task
  benchmark is NOT yet confirmed — needs a real benchmark re-run, not assumed from this fix alone.
  Angle (2) (a per-goal retry ceiling separate from the flat 30-step one) is still open regardless.

  **Update 2026-07-13 (T-158), this bug directly hit live, not just inferred from benchmark stats**:
  investigating flight-status-via-screen-reading, asked AION (via real chat, real accessibility
  service enabled and confirmed bound) to open Settings and report Wi-Fi status — a simple,
  single-screen-navigation goal. It opened the real Settings app correctly, then sat on the
  Settings home screen for 15+ minutes without ever tapping into "Network & internet" or resolving
  to a pass/fail. T-156's fuzzy-match fix did NOT prevent this — this run never even reached a
  `StepVerifier` check with meaningfully different wording; it just never navigated forward at all.
  Suggests the loop isn't purely a `StepVerifier` wording-mismatch problem (angle 1) — something in
  the planner's own step-to-step progression can stall with no forward motion and no failure exit.
  Raises the priority of angle (2) (a per-goal retry ceiling) since a wording fix alone clearly
  isn't sufficient. Any future roadmap item needing multi-step in-app navigation (flight status,
  and anything past a single screen) should be expected to hit this until it's fixed.

  **Update 2026-07-13 (T-162)**: traced this further and found `FewShotBank` (T-081's "don't
  repeat this mistake" mechanism) was fully built and unit-tested but never actually instantiated
  anywhere in real app code — every real replan was structurally blind to the plan that just
  failed. Wired it into `AionGraphFactory`/`ReflectorAgent`/`PlannerAgent` and proved with a real
  graph-level test that a goal which would previously be incapable of self-correcting now does.
  Re-ran T-158's exact original scenario live with the fix installed — it STILL didn't resolve
  within ~10 more minutes, so this is a real, partial improvement, not a full fix. Root-caused why:
  `FewShotBank` deliberately (and already-testedly, see `FewShotBankTest`) keeps only the single
  MOST RECENT mistake per goal — a `Map` keyed by goal, not an accumulating list, a genuine T-081
  design tradeoff, not a bug, so not changed. On a screen with several plausible-but-wrong targets
  (Network & internet, Connected devices, Apps, Notifications, Battery, Storage, Sound & vibration…),
  a model cycling between 2-3 different wrong choices can "forget" an earlier mistake the instant a
  different one overwrites it in the bank. Whoever picks this up next has two real options, now
  precisely scoped rather than guessed at: (a) let `FewShotBank` accumulate multiple mistakes per
  goal (would need `FewShotBankTest`'s existing "replaces rather than duplicates" test rewritten,
  a deliberate behavior change, not a bugfix) — bounded by the same `maxSize`/LRU eviction it
  already has, just flat across all entries instead of one-per-goal; or (b) angle (2) from the
  original entry above, a per-goal retry ceiling that converts a stall into an honest, faster
  UNKNOWN failure without needing the planner to actually learn anything. Also still worth checking
  directly, not yet done: whether `ExecutorAgent`/`StepVerifier` are even detecting a failure at
  all on this specific stuck scenario (if a tap "succeeds" per `StepVerifier`'s confidence check
  without real forward progress, `ReflectorAgent`/`FewShotBank` never get invoked at all, and no
  amount of few-shot learning would help) — would need real instrumentation/logging to confirm,
  not assumed here.

  **Update 2026-07-13 (T-163) — option (b) above, implemented and live-confirmed**: added a
  per-goal recoverable-retry ceiling to `ReflectorAgent` (5 retries, instance-scoped — one
  `ReflectorAgent` per real graph run). Re-ran T-158's exact original stuck scenario a THIRD time,
  live, on a freshly restarted emulator: this time it resolved with an honest "I don't know why,
  try again" response in under 75 seconds — the two prior live attempts (T-158, T-162) never
  resolved at all after 10-15+ minutes each. For this specific, three-times-reproduced scenario,
  the loop is genuinely closed. Not claiming the whole phenomenon is closed, though: option (a)
  (accumulating multiple mistakes per goal in `FewShotBank`) is still undone and may matter for
  goals that fail in more varied ways than this one did, and the `StepVerifier`-false-positive
  question two paragraphs up is still open and unconfirmed either way. Both remain real, scoped
  follow-ups for whoever hits a goal this fix doesn't fully resolve.

  **Update 2026-07-13 (T-164) — option (a) above, now also done**: `FewShotBank` accumulates
  multiple mistakes per goal now (bounded per-goal by a new `maxPerGoal=5`, matching the retry
  ceiling that's what made this safe to do), instead of a `Map` silently overwriting the previous
  mistake with each new one. Pure data-structure change, unit-tested (`FewShotBankTest`), no live
  re-test needed (no UI-visible behavior to verify). The ONE remaining open thread on this whole
  investigation is the `StepVerifier`-false-positive question: whether a tap that "succeeds" per
  `StepVerifier`'s confidence check without real forward progress can let `ReflectorAgent`/
  `FewShotBank` get skipped entirely. Needs real instrumentation/logging to confirm either way —
  not done here, left honestly open rather than assumed.

  **Update 2026-07-13 (T-165) — that last open thread, chased down and confirmed real**:
  `DispatcherActionExecutor`'s `success = result is ActionResult.Success || verification.outcome
  == PASS` only implemented half its own documented rule — a dispatcher `Success` (which just means
  "no system exception," confirmed by reading `ActionDispatcher` directly, never "hit the intended
  element") could silently override a `StepVerifier` outcome that wasn't `PASS`, letting a
  wrong-element tap count as success and skip `ReflectorAgent`/`FewShotBank` entirely. Fixed:
  `success` is now exactly `verification.outcome == PASS`. Live re-test of the same stuck scenario
  a 4th time was inconclusive (didn't resolve in ~10 minutes, unlike T-163's clean <75s result) —
  genuinely unclear if that's real LLM-latency variance or something else; not chased further after
  4 live attempts on one scenario. This closes all four scoped follow-ups from T-158's original
  finding (T-162/T-163/T-164/T-165). **Not claiming the loop phenomenon is eliminated** — it's
  meaningfully better-understood and every real gap found along the way is genuinely fixed and
  tested, but confirming the aggregate real-world improvement needs a proper benchmark re-run
  (T-121-style), not more one-off manual live pokes at a single scenario. Whoever picks this up
  next should start there.

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

- **Proactive Suggestions doesn't cover "download completed" or "mail needs a reply"** — found
  2026-07-13 during T-155. The mockup's other two suggestion types aren't buildable honestly today:
  `DownloadManager.query()` without `ACCESS_DOWNLOAD_MANAGER` (a system-signature permission we
  can't hold) only returns downloads this app itself initiated, not other apps' — no path to see a
  real cross-app download event. "Does this mail need a reply" needs actual content classification,
  not a threshold check like the other three signals. Revisit if a real signal for either becomes
  available (e.g. a notification-listener-based download heuristic instead of `DownloadManager`).

- **`ProactiveSuggestionEngine` thresholds are fixed constants, not owner-tunable** — found
  2026-07-13 during T-155. 20% battery / 15min lookahead / 2h screen time are hardcoded. Same
  category as the wake-word thresholds above — fine until someone's actual routine shows the
  defaults are wrong, add a settings surface then, not preemptively.

- **`aion_test` AVD's `content://sms` provider accepts `adb emu sms send` but refuses shell-side
  deletes/clears** — found 2026-07-13 during T-157's live-verification. Unlike T-152's finding
  (raw `content insert` silently no-ops), `adb emu sms send <sender> <body>` genuinely delivers a
  real SMS through the emulator's simulated modem — the mechanism to use for any future SMS live-
  verification on this AVD. But cleaning up afterward failed every way tried: `content delete`
  (both a WHERE clause and per-row `content://sms/<id>` URIs) and `pm clear` on both
  `com.android.providers.telephony` and `com.google.android.apps.messaging` (even after
  `am force-stop`) all silently no-op'd. 3 fake test SMS (Amazon/Swiggy/Salary, harmless amounts)
  are left sitting on this AVD as a result — expect them present in any future SMS query on
  `emulator-5554` until someone finds the right cleanup incantation or the AVD gets recreated.
