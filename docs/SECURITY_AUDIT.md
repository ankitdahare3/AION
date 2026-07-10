# SECURITY AUDIT — T-120
**Project AION | 2026-07-10 | vs docs/DOC-017_Security.md §2**

Audit of the 8 named threats (T1–T8) and their named controls, against the actual codebase as of this date. Each entry: verdict, evidence, and what (if anything) was fixed under this task vs. deferred with a reason.

## T1 — Prompt injection via screen/notification/web content
**Verdict: control exists, unwired. Not currently exploitable.**

`android/app/src/main/java/com/aion/host/security/InjectionFilter.kt` fully implements the doc's design — imperative-pattern stripping, `<screen_data>` wrapping, with tests. But nothing calls it in production: `AionNotificationListener.kt` explicitly notes wrapping is unscoped future work, and `PlannerAgent` (`:brain`) only ever receives a plain `goal: String` — no screen/notification content reaches an LLM prompt anywhere in the app today (`ContextBuilder` isn't wired to the Planner either). What *is* real: side-effect actions always route through `ApprovalGateService`/`PluginApprovalGate` regardless of source (T-021/T-070), which is the doc's own stated backstop even if injection succeeded.

**Not fixed here.** There's nothing to wire it TO yet — the vulnerable data path (screen content → LLM prompt) doesn't exist in this app's current wiring. Flagged as a hard requirement for whichever task first wires `ContextBuilder`/screen text into a real prompt: that wiring must not land without `InjectionFilter.wrap()` applied at the boundary.

## T2 — Malicious/buggy plugin exfiltrates data
**Verdict: schema-level gates real, runtime sandbox absent. Not currently exploitable.**

`DNAValidator` does real static manifest validation (known-permission whitelist, benchmark requirement). Side-effect tools route through `PluginApprovalGate` (`PluginManager.kt`). `AuditLogger` (hash-chained, T-020) is wired. Missing: true classloader sandboxing, a permission-enforcement facade, and a per-plugin network egress allowlist — `PluginManager.kt`'s own doc comment already says sandboxing is out of scope for now.

**Not fixed here.** The exploitable precondition — a user installing arbitrary third-party plugin code — doesn't exist yet either; only 8 hardcoded, source-controlled first-party plugins exist, and the 2 that touch the network (Gmail, Telegram) are registered-but-disabled by default (owner's explicit 🧍HC-5 enable required, per T-102). Real sandboxing/egress-allowlisting is a substantial feature in its own right, appropriately scoped as a future task once a third-party plugin installation path exists, not a line-level audit fix.

## T3 — Generated skill contains an unsafe step
**Verdict: built and working. Re-approval-on-patch not applicable — no patch mechanism exists.**

`SkillValidator` (schema) → `SkillSafetyChecker` (SR-01/SR-06: unknown tools, side-effect steps without a preceding approval marker, credential-looking arg names) → `SkillSandbox` (placeholder dry-run) → mandatory human approval via the same `ApprovalGateService` used everywhere else (T-090/091/092). No gap found in the built pipeline itself. "Side-effect steps re-approved on any patch" doesn't apply today because no skill-edit/patch capability exists anywhere in the codebase — approval is a one-time install-time event by construction. Worth remembering as a requirement if/when a skill-editing feature is ever built.

## T4 — API key theft
**Verdict: 2 real gaps found and fixed; core mechanism (Keystore, no logging) was already solid.**

`SecretVault` already used Android Keystore-backed `EncryptedSharedPreferences` (AES256_GCM/SIV) correctly, `allowBackup="false"` was already set, and no code anywhere logs a secret value. Two concrete gaps found and fixed in this task:
- **StrongBox not requested.** `SecretVault.kt` now tries `setRequestStrongBoxBacked(true)` first and falls back to the plain TEE-backed key on a caught exception (StrongBox hardware isn't universal — most emulators and many real devices lack it, and the androidx API throws rather than silently degrading). Verified for real: `SecretVaultInstrumentedTest` (3/3) passes on the `aion_test` emulator, which has no StrongBox hardware, confirming the fallback path works.
- **No screenshot/recording protection on the API Keys screen.** `MainActivity.kt` now applies `WindowManager.LayoutParams.FLAG_SECURE` for exactly the duration the API Keys screen (`Screen.API_KEYS`) is shown, clearing it on navigating away. Verified for real on-device, not just by inspection: `adb shell screencap` against the API Keys screen produces a genuine 0-byte capture, while the same command against the adjacent Setup screen produces a normal 221KB image — confirmed via the accessibility tree (not just a resumed-activity check) that the API Keys screen was actually rendered and populated in both cases, so the empty capture is FLAG_SECURE doing its job, not a crash or blank state.

## T5 — Memory poisoning (false facts steering behavior)
**Verdict: only passive data-model fields exist; the actual write path is an intentional no-op. Not currently exploitable.**

`Memory.confidence`/`Memory.provenance` fields exist (T-111), but no `UNVERIFIED` concept exists anywhere in code — only in this doc. The real write node, `MemoryWriterAgent`, is a deliberate no-op stub (its own comment: "Real episodic-memory writing is EPIC 6 (T-060+), which doesn't exist yet"). `RoomMemoryStore.insert()` has zero gating, and `MemoryConsolidator`'s promote-on-merge logic (T-111) boosts confidence on any duplicate cluster with no source-awareness at all — the literal opposite of "never auto-promote screen-sourced facts."

**Not fixed here.** Nothing writes a real memory row in production today (confirmed: `MemoryWriterAgent` never calls `MemoryStore.insert()`), so there's no live poisoning vector to close yet. Recorded as a hard requirement for T-062 (MemoryAgent write policy, still unbuilt): writes must be confidence-gated, screen/OCR-sourced facts must be tagged in a way `MemoryConsolidator`'s promote logic can recognize and refuse to auto-boost, and that gate must be added to `MemoryConsolidator` at the same time T-062 introduces the concept — not bolted on speculatively now with nothing real to gate.

## T6 — Physical access to the dedicated device
**Verdict: baseline present, the named app-lock control is a real unbuilt feature, not an audit-fixable gap.**

Device Owner scaffolding (`AdminReceiver.kt`) and `allowBackup="false"` exist. Device encryption relies on stock Android FBE (default since API 23) with no AION-specific check. No biometric app-lock, no auto-lock, no kiosk mode exist anywhere — zero matches for `BiometricPrompt` or equivalent in the codebase.

**Not fixed here.** DOC-017 §5 itself marks "auto-lock kiosk" as P2. A real biometric app-lock is a full feature (BiometricPrompt integration, a lock-screen UI, session-timeout logic) — sized like its own task, not a line-level fix inside an audit. Recommend a follow-up task (suggested: T-124) rather than force-building it here.

## T7 — Cloud provider sees excessive personal data
**Verdict: not built, and not currently exploitable either.**

No PII redaction, regex, or NER pass exists anywhere near a network/egress code path. No per-app vision-consent gating exists.

**Not fixed here.** No live code path currently sends screen/vision content to any cloud endpoint at all — OCR (T-100) is on-device-only (ML Kit, never leaves the device), and Gmail/Telegram (T-102) send exactly what the user explicitly composes, not automatically-scraped screen content. The doc's actual concern (a future cloud multimodal-vision path, DOC-012 §2) doesn't exist yet — T-032's local LLM is deferred and no cloud vision provider is wired. Recorded as a hard requirement for whichever task first builds that path: PII redaction must land with it, not after.

## T8 — Voice spoofing (non-owner commands)
**Verdict: built correctly for this phase. No gap.**

`ApprovalGateService.resolve()` is only ever invoked from `ApprovalSheet.kt`'s real Compose tap handlers — no voice-only approval-resolution path exists anywhere. This matches the doc's own stated interim control ("until then approvals require screen tap too") exactly. Speaker verification is correctly absent, matching its own P1/deferred status in the doc.

## Summary

| # | Threat | Verdict | Action |
|---|--------|---------|--------|
| T1 | Prompt injection | Filter built, unwired | Deferred — no live data path exists to wire it to yet |
| T2 | Malicious plugin | Gates real, no sandbox | Deferred — no 3rd-party install path exists yet |
| T3 | Unsafe skill | Built, working | No gap found |
| T4 | API key theft | 2 real gaps | **Fixed**: StrongBox + fallback, FLAG_SECURE on API Keys screen |
| T5 | Memory poisoning | Fields only, no-op writer | Deferred — requirement recorded for T-062 |
| T6 | Physical access | Baseline only | Deferred — recommend new task for biometric app-lock |
| T7 | Excessive cloud data | Not built | Deferred — requirement recorded for future cloud-vision task |
| T8 | Voice spoofing | Built correctly | No gap |

No P0 (immediately exploitable in the app's current, real wiring) gaps were found beyond the two fixed under T4. Every other gap traces back to a control guarding a data path or capability that doesn't exist in this app's live wiring yet — fixing them now would mean building speculative security infrastructure ahead of the feature it protects, which this project's own established practice (see PROGRESS.md throughout EPIC 6–11) has consistently avoided in favor of naming the requirement for whoever builds that feature next.
