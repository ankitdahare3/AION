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
