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
