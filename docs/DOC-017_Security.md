# DOC-017 — SECURITY
**Project AION | v1.0 | 07 July 2026**

## 1. Threat model (top 8)
T1 Prompt injection via screen/notification/web content → hijacked actions
T2 Malicious/buggy plugin exfiltrates data
T3 Generated skill contains unsafe step
T4 API keys theft
T5 Memory poisoning (false facts steering behavior)
T6 Physical access to dedicated device
T7 Cloud provider sees excessive personal data
T8 Voice spoofing (non-owner commands)

## 2. Controls
- T1: InjectionFilter — screen/notification text wrapped `<screen_data>`, imperative-pattern strip, Brain hard rule: data≠instructions (DOC-004 §6); side-effect actions ALWAYS ApprovalGate regardless of source
- T2: plugin sandbox + permission facade + egress allowlist per plugin + DNA/QA gates + audit (DOC-005 §4)
- T3: skill pipeline static checks + sandbox dry-run + mandatory human approval (DOC-006 §3); side-effect steps re-approved on any patch
- T4: Android Keystore, StrongBox where available; no logs; screenshot redaction of key screens
- T5: memory writes confidence-gated + provenance-tagged; facts from screen content marked UNVERIFIED, never auto-promoted; user memory browser (FR-M08)
- T6: device encryption, AION app-lock (biometric), auto-lock kiosk P2
- T7: data minimization + PII redaction pre-egress (regex+NER local pass: numbers, addresses, IDs) + per-app vision consent (DOC-015 §4)
- T8: speaker verification for side-effect approvals (P1); until then approvals require screen tap too

## 3. Sensitive-app policy (SR-07)
Default observe-only list: banking, UPI/payment, password managers, WhatsApp (ToS risk, DOC-001 §9.3). Override = explicit user setting per app, logged.

## 4. Audit & transparency
Every action, approval, cloud call, memory write → tamper-evident local log (hash-chained), viewer UI, 90d retention, exportable.

## 5. Update integrity
Plugins/skills/models: checksum manifests; repo-signed releases (sigstore P1); downgrade protection.
