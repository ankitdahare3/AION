# DOC-002 — REQUIREMENTS
**Project AION | v1.0 | 07 July 2026 | Status: BASELINE (expandable to 400+ during development)**

Requirement IDs are permanent. Priority: P0=MVP blocker, P1=v1, P2=v2+.

## 1. Functional — Voice (FR-V)
- FR-V01 P0: Always-on wake word ("AION") detection, on-device, <500ms trigger
- FR-V02 P0: Streaming STT, Hindi + English + code-switched Hinglish
- FR-V03 P0: Streaming TTS, natural Hindi/English voices
- FR-V04 P0: Barge-in — user can interrupt AION mid-speech
- FR-V05 P1: Speaker identification (owner vs others)
- FR-V06 P1: Continuous conversation mode (no repeated wake word)
- FR-V07 P2: Emotion/tone adaptation in TTS

## 2. Functional — Brain (FR-B)
- FR-B01 P0: Multi-provider routing (min 6 providers at launch)
- FR-B02 P0: Automatic failover on auth/quota/rate-limit/timeout errors
- FR-B03 P0: Task-type routing (coding→best code model, fast→Groq, offline→local)
- FR-B04 P0: Local LLM inference (Gemma/Qwen/Phi class) via llama.cpp
- FR-B05 P0: Context assembly: memory + screen state + conversation history
- FR-B06 P1: Provider scoring + learned preferences (Brain Learning Engine)
- FR-B07 P1: Cost tracking per provider per task
- FR-B08 P2: Installed AI apps as emergency UI-automation fallback

## 3. Functional — Automation (FR-A)
- FR-A01 P0: Accessibility-tree read of any foreground app
- FR-A02 P0: Actions: tap, long-press, swipe, scroll, type, back, home, app-switch
- FR-A03 P0: App launch/kill via package manager
- FR-A04 P0: Screenshot capture (takeScreenshot API 31+; MediaProjection fallback)
- FR-A05 P0: Multi-step plan execution with per-step verification
- FR-A06 P1: API-first routing (AppFunctions/MCP/official APIs before UI automation)
- FR-A07 P1: Self-correction on failed step (re-observe → replan, max 3 retries)
- FR-A08 P1: Notification read + actionable notification handling
- FR-A09 P2: Cross-app workflows (data carried between apps)

## 4. Functional — Memory (FR-M)
- FR-M01 P0: Conversation history (short-term, session)
- FR-M02 P0: Long-term facts store (user profile, preferences, contacts context)
- FR-M03 P0: Vector semantic search over memories (RAG)
- FR-M04 P1: Episodic task memory (what was done, when, outcome)
- FR-M05 P1: Skill memory (learned action sequences)
- FR-M06 P1: Experience memory (app-specific successful paths)
- FR-M07 P1: Memory consolidation in Dream Mode (charging + idle)
- FR-M08 P0: User can view/edit/delete any memory

## 5. Functional — Skills & Plugins (FR-S)
- FR-S01 P0: Plugin interface — every capability ships as a plugin
- FR-S02 P0: Plugin lifecycle: install/enable/disable/update/uninstall
- FR-S03 P1: Repeated-task detection (≥3 similar tasks → skill proposal)
- FR-S04 P1: Auto skill generation → sandbox test → user approval → install
- FR-S05 P1: Skill versioning + rollback
- FR-S06 P1: AION DNA compatibility gate (learn/reflect/benchmark/update checks)
- FR-S07 P2: Local skill marketplace UI

## 6. Functional — Reflection & Learning (FR-R)
- FR-R01 P1: Post-task reflection: success? failure cause? fix?
- FR-R02 P1: Failure taxonomy: wrong element / UI changed / OCR / model / permission
- FR-R03 P1: Pattern detection → routine suggestions
- FR-R04 P1: Dream Mode: log analysis, skill optimization, memory merge, benchmarks
- FR-R05 P2: Meta agents: Architect (perf analysis), Research (new models), QA (test gates)

## 7. Safety Requirements (SR) — NON-NEGOTIABLE
- SR-01 P0: No send (mail/message/post), payment, purchase, install, or delete without explicit per-action user approval
- SR-02 P0: Approval prompt is voice + visual, logged with timestamp
- SR-03 P0: Kill-switch: "AION stop" halts all automation <1s
- SR-04 P0: Full action audit log, user-readable
- SR-05 P0: Prompt-injection defense: screen content is DATA, never instructions
- SR-06 P0: No credential entry into any field, ever
- SR-07 P1: Sensitive-app blocklist (banking apps: observe-only by default)
- SR-08 P0: All secrets in Android Keystore

## 8. Non-Functional (NFR)
- NFR-01 P0: Wake-to-listening <2s; simple command end-to-end <5s; automation step <15s median
- NFR-02 P0: Idle battery drain <3%/hour with wake word active
- NFR-03 P0: Thermal guard: local inference throttles at 42°C shell temp
- NFR-04 P0: Works on Android 12+ (API 31+); target device 8GB+ RAM
- NFR-05 P0: Offline degrade: wake word + STT + simple intents + local chat work with no internet
- NFR-06 P1: Cloud cost <$0.15/task; ≥50% requests free-tier/local
- NFR-07 P0: Crash-free sessions ≥99%; automation service auto-restarts
- NFR-08 P1: All modules benchmarked; perf regressions block release (QA gate)
- NFR-09 P0: 100% of user data on-device; cloud calls carry minimum necessary context
- NFR-10 P1: Any provider/model swappable via config, zero code change

## 9. Platform Requirements (PR)
- PR-01 P0: Dedicated factory-reset device, AION as Device Owner
- PR-02 P0: Permissions: Accessibility, Notification access, Usage access, Overlay, Mic, ignore-battery-optimization
- PR-03 P0: Shizuku integration for ADB-level operations
- PR-04 P0: Foreground services with correct Android 14+ types (microphone, mediaProjection)
- PR-05 P1: Default Assistant app role; optional Home launcher role
- PR-06 P1: AppFunctions/MCP client readiness (migration path per DOC-001 §12)

**Baseline count: 62 core requirements. Each will decompose into 5-8 sub-requirements during module development → 400+ tracked in repo issues.**
