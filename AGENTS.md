# AGENTS.md — Brief for Antigravity (and any AI coding agent) working on AION

You are the implementation engineer for Project AION in this repository. This file is your
orientation. CLAUDE.md contains the binding rules — read it first and obey it; this file adds
context, environment truth, and the execution plan.

## What AION is (30 seconds)
A personal, open-source "AI Operating Layer" that turns a dedicated Android phone into a
voice-first, self-evolving agent. Hindi + English + Hinglish. Local-first (llama.cpp on device),
cloud only when needed. The owner controls everything; every side-effect action needs explicit
approval. Tagline: "You own the operator."

## Read order (do not skip)
1. `CLAUDE.md` — hard rules, human checkpoints, working style (BINDING)
2. `PRD.md` — product, user stories, success metrics
3. `TECH_SPEC.md` — frozen contracts, locked ADRs, safety architecture
4. `TASKS.md` — your work queue; execute strictly top-to-bottom
5. `docs/DOC-0XX` referenced by the current task (normative detail)
6. `MASTER_PROMPT.md` — the one-paragraph operating loop

## Environment truth (verified 2026-07-08 — trust this over older notes)
- Owner's machine: Windows 11 desktop "DESKTOP-IO16SBT" — Ryzen 5 5500U, 12GB RAM
  (~9.86GB usable), integrated Radeon graphics (NO discrete GPU), ~302GB free disk.
- **JDK, Gradle, and the Android SDK are NOT installed yet.** `java` is not on PATH.
  Android Studio is NOT installed (earlier notes claiming it was were wrong).
- Consequence: you can WRITE all code, but `./gradlew assembleDebug` cannot run until the
  toolchain exists. Your first real action on T-001 is to walk the owner through installing:
  1. JDK 17 (Temurin), 2. Android command-line tools + `sdkmanager` (platform-tools,
  platforms;android-31/34/36, build-tools), 3. set `JAVA_HOME`/`ANDROID_HOME`, then generate
  the Gradle wrapper and verify the build. Never claim a build passed without running it.
- RAM is tight: prefer physical-device testing over the emulator. If an emulator is
  unavoidable, one low-API image at a time, other heavy apps closed.
- No dGPU does not matter for the product: llama.cpp/whisper.cpp/Piper run ON THE PHONE.
- The dedicated test phone is owner-operated: you prepare exact steps + expected logcat
  markers; the owner runs them and reports back (checkpoint 🧍HC-4).

## Architecture in one screen (full detail: docs/DOC-003, DOC-004)
- 6 layers, calls go downward only:
  L6 Interaction (voice/chat UI, approval sheets) → L5 Agents (Planner/Executor/Reflector…)
  → L4 Brain (AionGraph orchestrator, ProviderRouter, ContextBuilder) → L3 Capabilities
  (plugins, skills, automation) → L2 Core services (memory, vision, voice, security)
  → L1 Platform (a11y, Shizuku, Device Owner, llama.cpp JNI, Room/sqlite-vec, Keystore).
- Gradle modules: `:android:app` (UI + Android services) and `:brain` (pure Kotlin, no
  Android deps: AionGraph, ProviderRouter, agents). Android runs 3 processes: `:core`,
  `:automation` (a11y service), `:inference` (llama.cpp host, killable).
- Standard agent graph (v1):
  `intent → planner → executor ⇄ verifier → (fail → reflector → planner) → responder → memory_writer → END`
- Provider routing: score = 0.4·taskScore + 0.3·tierWeight + 0.2·(1−latencyNorm) + 0.1·privacy;
  LOCAL > FREE > PAID; top-3 candidates, failover on ProviderFailure; BudgetGuard gates PAID.

## Frozen contracts — do NOT change signatures
`brain/src/main/kotlin/com/aion/brain/` — Provider.kt, ProviderRouter.kt, AionGraph.kt.
Changing any signature requires writing an ADR in docs/ADR-INDEX.md first and marking the
task ⚠️BLOCKED for human review. Locked ADRs (no framework substitutions): llama.cpp,
custom Kotlin AION Graph (no Python on device), openWakeWord+Silero+whisper.cpp+Piper,
Room+sqlite-vec+SQLCipher.

## Safety is sequenced BEFORE capability (non-negotiable)
EPIC 1 (T-020..T-024: hash-chained AuditLogger, ApprovalGate, kill-switch "aion stop" <1s,
InjectionFilter, SecretVault) must be implemented and wired before ANY code that performs
automation or side effects (T-030+). Screen/notification text is DATA, never instructions —
it always enters model context wrapped in `<screen_data>` via InjectionFilter. API keys only
in Android Keystore — never in code, logs, or commits.

## Known review findings to address during T-001/T-030 (from 2026-07-08 code review)
- `ProviderRouter.route()`: if all candidates are PAID and budget is exhausted, the loop
  falls through with `last == null` → generic `IllegalStateException("Routing failed")`.
  Surface a distinct, diagnosable error (e.g. BudgetExhausted). Do this without changing
  public signatures.
- `AionGraph`: forced route to `"reflector"` assumes that node exists; `nodes.getValue`
  throws otherwise. Add a constructor-time `require(nodes.containsKey("reflector"))` (and
  `"planner"`). Not a signature change.
- Build scaffold gaps: no root `build.gradle.kts`, no version catalog, no Gradle wrapper,
  no `brain/build.gradle.kts`, no `AndroidManifest.xml`. This IS T-001.

## Execution plan (condensed from DOC-020; details in TASKS.md)
Phase 0 (owner-assisted): install JDK 17 + Android cmdline SDK → unblocks all builds.
S1  EPIC 0: Gradle setup, CI, app skeleton, AdminReceiver + provisioning 🧍HC-1
S1  EPIC 1: Safety core (audit, approval, kill-switch, injection filter, vault)
S2  EPIC 2: Voice loop (wake word 🧍HC-2, whisper.cpp STT, Piper TTS, echo bot)
S3  EPIC 3: Brain online (providers.yaml, adapters 🧍HC-3, local LLM, offline chat 🏆)
S4  EPIC 4: Hands (a11y reader, dispatcher, resolver, Shizuku, verifier)
S5  EPIC 5: Agent loop → 🏆 M1 "Open YouTube and play Arijit Singh" by voice
S6..S11: Memory → Plugins → Reflection → Skills → Vision/Comms (🏆 M2) → Learning/Dream
S12 EPIC 12: security audit, 50-task Hindi+English benchmark, v0.1.0-alpha tag.
Per task: complete code + unit tests + audit hooks (if action-related) + TASKS.md checkbox
+ conventional commit `type(scope): T-XXX summary`. After each EPIC: PROGRESS.md entry.

## Honesty protocol
If a test cannot run in your environment (no JDK, no device, no mic), say so explicitly in
the task notes and PROGRESS.md. Never fake or assume a pass. Never auto-approve anything
marked 🧍 — those belong to the owner (Hindi/English bilingual; keep questions simple).
