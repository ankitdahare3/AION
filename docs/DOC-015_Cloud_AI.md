# DOC-015 — CLOUD AI
**Project AION | v1.0 | 07 July 2026**

## 1. Role
Frontier reasoning for MULTI_STEP planning, complex generation, hard vision. Everything else stays local (DOC-004 §3 target: ≥70% turns local).

## 2. Key management
User-supplied keys only; stored in Android Keystore (SR-08); per-provider enable toggle; keys never logged/exported. No AION-hosted proxy in v1 (self-hosted principle).

## 3. Free-tier strategy (Free First rule)
Priority: Groq free quota → OpenRouter free models → provider trial credits → paid (explicit opt-in). Router tier weights encode this (DOC-013 §2). Rate-limit calendars per provider tracked to avoid burn.

## 4. Data minimization (NFR-09)
Cloud context = task-necessary only: compressed a11y text (no full dumps), memory SUMMARIES (never raw store), no contact lists, no files unless task requires + user approves. PII redaction pass (DOC-017) before egress. Vision images to cloud: per-app consent flags.

## 5. Streaming & tools
SSE streaming mandatory for TTS pipelining; tool-calling via normalized schema (DOC-013 §3); JSON-schema constrained outputs for Planner.

## 6. Offline behavior
No network → Router returns local-only candidates; if task needs cloud → honest response: "Ye kaam ke liye internet chahiye" + queue option (retry on connectivity).
