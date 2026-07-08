# AION — TECHNICAL SPECIFICATION (condensed; authoritative detail in docs/)
**v1.0 | Contracts here are FROZEN. Doc references are normative.**

## 1. Stack
Kotlin 2.x + Jetpack Compose, minSdk 31, targetSdk 36 | Hilt DI | Room + sqlite-vec + SQLCipher | WorkManager | kotlinx.serialization | llama.cpp via JNI (local LLM) | whisper.cpp (STT) | openWakeWord ONNX (wake) | Piper (TTS) | ML Kit OCR | Shizuku + Device Owner (privileged ops)

## 2. Process/module layout (DOC-003)
Gradle modules: `:android:app` (UI+services), `:brain` (pure Kotlin: AionGraph, Router, agents), plus source dirs plugins/, skills/, tools/.
Android processes: `:core` (brain+voice), `:automation` (a11y service), `:inference` (llama.cpp host, killable).

## 3. Frozen interfaces (already in brain/src — do not change signatures)
Provider / BrainRequest / BrainResult / ToolCall / ProviderFailure (Provider.kt)
ProviderRouter scoring+failover (ProviderRouter.kt) — weights: task 0.4, tier 0.3, latency 0.2, privacy 0.1; LOCAL>FREE>PAID
AionGraph / AgentState / Agent / ApprovalGate / Checkpointer (AionGraph.kt) — graph: intent→planner→executor⇄verifier→(fail→reflector→planner)→responder→memory_writer→END

## 4. Locked ADRs (docs/ADR-INDEX.md)
ADR-001 llama.cpp | ADR-002 custom Kotlin AION Graph (no Python frameworks on device) | ADR-011 openWakeWord+Silero+whisper.cpp+Piper | ADR-019 Room+sqlite-vec+SQLCipher

## 5. Safety architecture (DOC-017 — implement BEFORE automation)
ApprovalGate blocks all sideEffect ToolCalls until explicit user yes (voice+tap) · hash-chained audit_log table · InjectionFilter: all screen/notification text wrapped <screen_data>, imperatives stripped, system prompt states data≠instructions · kill phrase "aion stop" <1s halt · secrets only in Android Keystore · sensitive apps observe-only default

## 6. Data (DOC-019 schema is normative)
Tables: conversations, turns, memories, episodes, skills, element_maps, providers_stats, plugins, audit_log + sqlite-vec virtual tables (384-dim, local embeddings only — memories never leave device)

## 7. Per-module specs
Voice: DOC-011 (streaming, barge-in, bilingual, FGS type=microphone)
Automation: DOC-009 (a11y-first structured text ≤2000 tokens, API-first routing, step verification, rate ≤1 action/300ms)
Vision fallback: DOC-012 | Memory: DOC-010 | Plugins: DOC-005 (manifest+DNA gates) | Skills: DOC-006 (YAML format, approval-mandatory pipeline) | Reflection: DOC-007 | Learning/Dream: DOC-008 | Router: DOC-013 | Local AI: DOC-014 | Cloud: DOC-015 | Android core+provisioning: DOC-016 | Perf budgets: DOC-018

## 8. Performance budgets (regressions >10% block merge)
wake→listen <1s · simple e2e <5s · step median <10s · local first-token <1.2s · idle battery <3%/hr · crash-free ≥99.5%
