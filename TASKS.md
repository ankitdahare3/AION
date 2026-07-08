# TASKS.md — AION Build Queue (v0.1 Alpha)
Format: `[ ] T-XXX (doc-ref) description — AC: acceptance criteria`
Work top-to-bottom. 🧍 = human checkpoint. Milestones: 🏆

## EPIC 0 — Repo & CI
[x] T-001 (DOC-020) Complete Gradle setup: root build.gradle.kts, version catalog, wrapper; :android:app + :brain compile — AC: `./gradlew assembleDebug` succeeds ✅ verified 2026-07-08, BUILD SUCCESSFUL in 2m16s, apk at android/app/build/outputs/apk/debug/app-debug.apk
[x] T-002 Add Hilt, Room, kotlinx.serialization, Compose BOM wiring — AC: clean build, sample @HiltAndroidApp boots in emulator ✅ clean build verified 2026-07-08 (BUILD SUCCESSFUL, kspDebugKotlin/hiltAggregateDepsDebug/hiltJavaCompileDebug all ran); ⚠️ "boots in emulator" NOT verified — no AVD/physical device on this machine, see PROGRESS.md
[x] T-003 GitHub Actions CI: build + unit tests + ktlint — AC: green pipeline on push ✅ verified 2026-07-08: pushed to private repo github.com/ankitdahare3/AION (main), CI run succeeded in 5m37s — https://github.com/ankitdahare3/AION/actions/runs/28962379819
[ ] T-004 (DOC-016§3) AdminReceiver + provisioning flow + setup wizard screens (permission walkthrough) — AC: emulator: app requests all PR-02 permissions; script updated 🧍HC-1

## EPIC 1 — Safety Core (BEFORE anything acts)
[ ] T-020 (DOC-017§4, DOC-019) audit_log table, hash-chained AuditLogger + viewer screen — AC: unit test verifies chain tamper detection
[ ] T-021 (SR-01/02) ApprovalGate service + Compose approval sheet (voice line + tap confirm) — AC: suspend fun blocks until decision; decision audited
[ ] T-022 (SR-03) Kill-switch: overlay button + "aion stop" hook halts dispatcher <1s — AC: instrumented test
[ ] T-023 (DOC-004§6) InjectionFilter: <screen_data> wrapper + imperative stripper — AC: unit tests with 20 injection strings, 0 pass-through
[ ] T-024 (SR-08) SecretVault over Android Keystore; settings screen for API keys — AC: keys survive restart, absent from logs/backups

## EPIC 2 — Voice Loop (S2)
[ ] T-010 (DOC-011) VoiceFgs (type=microphone) + AudioFocus + privacy indicator overlay — AC: service survives 24h emulator soak
[ ] T-011 openWakeWord ONNX runtime integration + Silero VAD — AC: test WAVs trigger/reject correctly
[ ] T-012 🧍HC-2 Wake model training pipeline (tools/wakeword: record+train scripts) — AC: custom "AION" model <1 false-accept/hr on 8h noise sample
[ ] T-013 whisper.cpp JNI, streaming STT, hi+en+code-switch — AC: 20 test clips WER measured & logged (target <12%)
[ ] T-014 Piper TTS (hi + en voices), streaming playback, barge-in ducking — AC: TTS start <300ms; barge-in stops <200ms
[ ] T-015 VoiceSessionManager: wake→listen→endpoint→respond + 8s follow-up window — AC: echo bot works end-to-end 🧍HC-4

## EPIC 3 — Brain Online (S3)
[ ] T-030 (DOC-013) providers.yaml loader + ScoreStore(Room-backed) + BudgetGuard impl — AC: unit tests for cooldowns/failover/budget
[ ] T-031 🧍HC-3 Adapters: OpenAI-compat (covers Groq/OpenRouter/Ollama), Gemini — AC: live smoke test each (owner keys)
[ ] T-032 (DOC-014) llama.cpp JNI in :inference + model manifest download UI (Qwen3-4B int4) — AC: decode ≥12 tok/s on device / measured on emulator host
[ ] T-033 IntentClassifier (local): CHAT/SIMPLE_ACTION/MULTI_STEP/INFO/SYSTEM — AC: ≥90% on 100-utterance labeled set (create set)
[ ] T-034 ContextBuilder v1 (persona+safety prefix, history N=6, budget 8K) — AC: token budget never exceeded (unit tests)
[ ] T-035 Wire voice→router→voice: bilingual chat, offline falls to local — AC: airplane-mode chat works 🏆 S3 exit 🧍HC-4

## EPIC 4 — Hands (S4)
[ ] T-040 (DOC-009§2) AionAccessibilityService + A11yTreeReader → structured text ≤2000 tokens — AC: golden-file tests on 5 recorded trees
[ ] T-041 ActionDispatcher: tap/longPress/swipe/scrollTo/type/global; rate limiter 300ms — AC: instrumented tests on emulator Settings app
[ ] T-042 ElementResolver: id→fuzzy-text chain + confidence — AC: 95% resolution on golden trees
[ ] T-043 ShizukuBridge (input/am/pm ops) with permission gating — AC: graceful degrade when Shizuku absent
[ ] T-044 StepVerifier: expected vs post-action diff, 500ms debounce — AC: unit tests pass/fail/ambiguous cases 🧍HC-4

## EPIC 5 — Agent Loop + M1 (S5)
[ ] T-050 (DOC-004) PlannerAgent: goal→JSON plan (schema-constrained, repair-retry once) — AC: 20 canned goals produce valid plans
[ ] T-051 ExecutorAgent + wiring: plan step→ToolCall→dispatcher; sideEffect→needsApproval — AC: approval fires on every side-effect in tests
[ ] T-052 ReflectorAgent v0: failure classify (E1-E6) → patch plan or abort honestly — AC: induced failures classified correctly ≥80%
[ ] T-053 Responder + memory_writer stubs; full AionGraph run wired to voice — AC: graph checkpoints persisted
[ ] T-054 🏆 M1: "Open YouTube and play Arijit Singh" by voice, 5/5 runs 🧍HC-4

## EPIC 6 — Memory (S6)
[ ] T-060 (DOC-019) Full Room schema + migrations + SQLCipher — AC: schema tests
[ ] T-061 sqlite-vec + local embedder (bge-small int8 via llama.cpp) — AC: <15ms/item emulator-host
[ ] T-062 (DOC-010) MemoryAgent write policy (confidence gate, PII tags) + read policy in ContextBuilder — AC: recall scenario tests
[ ] T-063 Memory browser UI: search/view/edit/delete + "bhool jao" soft-delete — AC: FR-M08 flows 🧍HC-4 (cross-session recall demo)

## EPIC 7 — Plugins (S7)
[ ] T-070 (DOC-005) PluginManager + manifest loader + DNAValidator + sandbox facade — AC: contract tests
[ ] T-071..076 Built-ins: System, Contacts, Phone/SMS, Calendar, Files, Browser — AC: each ships bench/gmail-style yaml + QA scenario 🧍HC-5 for enable
[ ] T-077 Route Planner ToolCalls through PluginManager exclusively — AC: no direct dispatcher calls remain 🧍HC-4

## EPIC 8 — Reflection Deep (S8)
[ ] T-080 (DOC-007) ReflectionRecord store + ElementMap cache w/ confidence decay — AC: E2 (UI change) auto-invalidates selectors
[ ] T-081 Planner few-shot bank (max 50 LRU) from counter-examples — AC: repeat-failure rate drops in test harness
[ ] T-082 Recovery drill: induced failures suite — AC: recovery success >50% 🧍HC-4

## EPIC 9 — Skills (S9)
[ ] T-090 (DOC-006) Skill YAML schema + SkillStore + trigger embedding index — AC: schema validation tests
[ ] T-091 RepeatedTaskDetector (≥3 episodes, cosine>0.85) — AC: synthetic episodes trigger detection
[ ] T-092 SkillGenerator (LLM draft) + static checks + sandbox dry-run + approval UI 🧍HC-5 — AC: unsafe drafts rejected in tests
[ ] T-093 Skill-first execution path in IntentClassifier — AC: matched skill skips planner; failure falls back 🧍HC-4 (one real skill in daily use)

## EPIC 10 — Vision + Comms + M2 (S10)
[ ] T-100 (DOC-012) Screenshot capture (API31 path + MediaProjection fallback) + ML Kit OCR — AC: OCR on 10 golden screenshots
[ ] T-101 Vision grounding: OCR blocks→targets merge into ElementResolver (a11y wins) — AC: canvas-app tap demo
[ ] T-102 Gmail API plugin (read/compose/send w/ approval) + Telegram Bot API plugin — AC: live smoke 🧍HC-3/5
[ ] T-103 🏆 M2: 3-app multi-step goal e2e ("kal ki meeting ka time nikal ke HR ko mail karo aur reminder laga do") 🧍HC-4

## EPIC 11 — Learning & Dream (S11)
[ ] T-110 (DOC-008) Dream Mode WorkManager job (charge+idle constraints, budget guards) — AC: constraint tests
[ ] T-111 Consolidation: dedupe/decay/promote + nightly report generator — AC: report artifact produced
[ ] T-112 Brain Learning scorecards feeding Router (ε=0.05 exploration) — AC: score shifts measurable in sim
[ ] T-113 Pattern Learner → routine proposals UI 🧍HC-5 — AC: synthetic routine detected 🧍HC-4

## EPIC 12 — Hardening & Alpha (S12)
[ ] T-120 Security audit vs DOC-017 T1-T8 checklist; fix gaps — AC: written audit doc, all P0 closed
[ ] T-121 (DOC-018) 50-task Hindi+English benchmark harness + run — AC: report; ≥60% target evaluated honestly
[ ] T-122 Perf soak: 24h battery, 7d uptime sim — AC: budgets met or deviations documented
[ ] T-123 Docs sync (all DOC deltas), PROGRESS.md final, release notes — AC: repo tagged v0.1.0-alpha 🏆 🧍HC-4
