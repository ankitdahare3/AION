# DOC-020 — IMPLEMENTATION PLAN
**Project AION | v1.0 | 07 July 2026**

## 1. Monorepo (created today)
```
AION/
  docs/            DOC-001..020, ADRs
  android/         app (:core :automation :inference modules)
  brain/           shared Kotlin brain lib (AION Graph, Router)
  plugins/         built-in plugin sources
  skills/          skill YAML library + schema
  models/          registry.json, checksums (weights not committed)
  tools/bench/     python bench harness
  tools/harness/   laptop dev-harness (LangGraph prototyping, log analysis)
  tests/           unit, integration, scenario (QAAgent suites)
  scripts/         provisioning (device-owner setup), release
```

## 2. Sprint plan (2-week sprints; solo-dev + AI pair realistic pace)
| Sprint | Deliverable | Exit test |
|---|---|---|
| S1 | Repo, CI, app skeleton, FGS scaffolding, provisioning script | app boots as Device Owner on test phone |
| S2 | Voice loop v0: wake word + STT + TTS (no brain) | "AION" → echo transcript spoken back |
| S3 | Router + 3 providers (local llama.cpp, Groq, OpenRouter) + chat | bilingual chat by voice, offline chat works |
| S4 | A11y reader + ActionDispatcher + 5 primitives | scripted taps navigate Settings reliably |
| S5 | AION Graph v1 (planner/executor/verifier) + ApprovalGate + audit | **MILESTONE M1: "Open YouTube and play Arijit Singh" by voice** |
| S6 | Memory v1 (stores+vectors+ContextBuilder) + Memory browser UI | AION recalls facts across sessions |
| S7 | Plugin SDK + 6 built-ins (System, Contacts, SMS, Calendar, Files, Browser) | tool-calls routed through PluginManager |
| S8 | Reflector + failure taxonomy + ElementMaps | recovery success >50% on induced failures |
| S9 | Skill engine: detect→generate→approve→run | one real learned skill in daily use |
| S10 | Vision fallback (OCR+grounding) + Gmail/Telegram API plugins | M2: 3-app multi-step task e2e |
| S11 | Dream Mode + Learning Engine + dashboards | nightly report generated |
| S12 | Hardening: security audit vs DOC-017, 50-task benchmark, docs | **v0.1 ALPHA release — Stage-1 criteria (DOC-001 §12) evaluated** |

## 3. Definition of Done (every feature)
code + unit tests + bench entry + audit hooks + doc section + QAAgent scenario + no budget regression (DOC-018 §2).

## 4. Tooling
GitHub (issues = requirements FR/SR/NFR ids), Actions CI (build, unit, lint, bench-sim), device farm = the dedicated phone + emulator matrix (API 31/34/36).

## 5. Risk-driven order rationale
Voice before brain (hardest UX), a11y before vision (primary path), approval+audit before ANY side-effect capability (safety first), skills after reflection (need failure data).
