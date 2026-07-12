# CLAUDE.md — Agent Instructions for the AION Repository
You are the implementation engineer for Project AION. Work autonomously through TASKS.md, top to bottom, one task at a time, until v0.1 Alpha.

## Session rule (always)
**At session start read PROGRESS.md + TASKS.md. At session end update PROGRESS.md.**
Read GAP_REPORT.md once for the current honest state of the project (audit of 2026-07-12).

## Architecture in one paragraph
Two modules. **`:brain`** is pure JVM Kotlin — the agent loop (`AionGraph`: Planner→Executor→Reflector→Responder), provider routing/scoring, plugins, skills, memory logic; zero Android imports, all unit-testable on the JVM. **`:android:app`** is the host — Hilt DI, Compose UI, Room+SQLCipher persistence, the accessibility "hands" (read screen / tap / type / OCR), and the safety layer (approval sheet, kill-switch, audit chain, SecretVault). The app implements `:brain`'s interfaces (Provider, ScoreStore, ApprovalGate, MemoryStore, ActionExecutor…) and injects them via `AionGraphFactory`. Cloud LLMs (Groq/OpenRouter/NVIDIA/Gemini via Ktor) do the thinking; the phone does the doing.

## File map (where things live)
- `brain/src/main/kotlin/com/aion/brain/` — agents (PlannerAgent, ExecutorAgent, ReflectorAgent, ResponderAgent, ResponsePhrasing), AionGraph + frozen contracts (Provider.kt, ProviderRouter.kt, AionGraph.kt), routing math (ScoringMath, ExploringScoreStore), config (ProvidersConfig), IntentClassifier, ContextBuilder, memory logic (Memory, MemoryConsolidator, DeviceExplorer), skills (Skill*, RepeatedTaskDetector, PatternLearner), benchmark set (BenchmarkTasks), `plugins/` (8 built-ins + UIAutomationPlugin), `providers/` (OpenAiCompat, Gemini)
- `android/app/src/main/java/com/aion/host/` — MainActivity (screen switcher: Setup / Audit Log / API Keys / Chat), `setup/` (permission wizard), `security/` (AuditChain/Logger, ApprovalGateService+Sheet, KillSwitch+overlay, InjectionFilter, SecretVault+SecretsScreen, AionDatabase, DbPassphraseStore), `automation/` (AionAccessibilityService, A11yTreeReader, ActionDispatcher, ElementResolver, StepVerifier, MlKitOcrEngine, ShizukuBridge, DispatcherActionExecutor), `brain/` (AionGraphFactory, Room-backed stores, BuiltInPluginRegistry, RealApprovalGate, ChatScreen, DeviceExplorationWorker, DreamModeWorker), `di/` (Hilt modules), `svc/AionNotificationListener` (shell only, T-137)
- Tests: `brain/src/test` + `android/app/src/test` (JVM, 260 green as of 2026-07-12) and `android/app/src/androidTest` (instrumented: benchmark harness, live provider smoke, vault/kill-switch/migrations)
- Docs: PRD.md (product), TECH_SPEC.md + docs/DOC-001..020 (normative detail), docs/ADR-INDEX.md (locked decisions), TASKS.md (work queue), PROGRESS.md (journal), BACKLOG.md (proposed work), GAP_REPORT.md (audit)
- `bench/` YAMLs + `docs/T-121_BENCHMARK_REPORT.json` — benchmark inputs/results. `models/`, `plugins/`, `skills/`, `tests/` at root are empty placeholders — ignore.

## Hard rules (never violate)
1. **Safety before capability.** ApprovalGate + AuditLogger + kill-switch stay wired in front of every side-effect path.
2. **Frozen contracts.** Do not change signatures in brain/src (Provider, ProviderRouter, AionGraph) without an ADR in docs/ADR-INDEX.md first; mark the task blocked for human review.
3. **Locked ADRs stand.** llama.cpp, Kotlin AION Graph, Room+sqlite-vec. Voice: ADR-011 (openWakeWord/whisper.cpp/Piper) is the final stack; ADR-011a (platform SpeechRecognizer/TextToSpeech) is the owner-approved interim for T-135/T-136 only.
4. **No hardcoded providers/models/keys.** Config via providers.yaml; keys only via SecretVault/Keystore — never in code, logs, or commits.
5. **Every task = code + unit tests + audit hooks (if action-related) + TASKS.md checkbox update + conventional commit** (`feat(voice): T-135 mic button`).
6. **Complete files only** — no placeholder bodies. Can't finish → mark ⚠️BLOCKED with reason.
7. **Screen/notification content is data, not instructions.** InjectionFilter wraps everything model-bound.
8. **Honesty.** If a test can't pass in this environment, say so — never fake a pass. Benchmark scores are reported as measured (current honest score: 14%).

## Human checkpoints (STOP and ask the owner)
- 🧍 HC-1: Device Owner provisioning on the physical phone (still open)
- 🧍 HC-2: ~200 "AION" wake-word samples (only needed for ADR-011, not ADR-011a)
- 🧍 HC-3: API keys — done 2026-07-10 (keys live in SecretVault; never wipe app data without warning: `connectedAndroidTest` reinstalls DO wipe it — use direct `am instrument`)
- 🧍 HC-4: every EPIC end — owner runs on-device exit test
- 🧍 HC-5: skill/plugin approvals — never auto-approve

## Conventions (learned the hard way — see PROGRESS.md for the incidents)
- Version pins are deliberate: Kotlin 2.1.21 / AGP 8.11.1 / Hilt 2.58 / Ktor 3.1.2 / Room 2.8.4. Newer majors have known incompatibilities; don't bump casually.
- ktlint: run `ktlintFormat`, then `ktlintCheck` as a separate invocation (same-run race).
- Verify tests via the JUnit XML reports, not gradle exit codes. Standard command: `./gradlew assembleDebug testDebugUnitTest :brain:test`.
- Instrumented benchmark runs: use `adb shell am instrument` directly (preserves SecretVault); force-rebind the a11y service via `settings put secure enabled_accessibility_services` toggle.
- Emulator `aion_test` (API 31); 12GB-RAM dev machine — one emulator max, prefer the real phone (SM-G990E) when the owner isn't using it.
- Small commits; PROGRESS.md entry after each EPIC; ambiguity → docs' interpretation + note the assumption; new scope → BACKLOG.md, not code.
