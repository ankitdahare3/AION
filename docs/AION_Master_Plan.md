# PROJECT AION
## The Self-Evolving AI Operating Platform — Master Plan Document

**Version:** 0.1 Alpha Architecture
**Date:** 05 July 2026
**Owner:** Ankit Pawar
**Status:** Phase 0 (Research + Architecture) — In Progress
**Estimated Timeline:** 6–12 months
**Estimated Scale:** 100,000+ LOC

---

## 1. Vision & Mission

AION (AI Operating Network) is not an AI assistant app. It is an **AI Operating Layer** for Android — the AI becomes the primary operator of a dedicated, factory-reset Android phone. The user only talks; the AI does everything else.

**Mission:** Build an Android AI Host that:
- Is the phone's primary operator
- Talks naturally by voice (Hindi + English)
- Operates apps by itself (APIs where available, UI automation where not)
- Uses Local + Cloud AI (hybrid brain)
- Understands the screen like a human
- Can add new skills and tools continuously
- Runs on a maximum free/open-source stack

**Philosophy:** *"Build once. Scale forever."*

**End-state example:**
> User: "Good Morning AION."
> AION: "Good morning Ankit. Aaj 2 meetings hain, Rapid Organic ke 3 mails aaye hain, ek TC pending hai, aur tumhara audit document kal complete nahi hua tha. Kya pehle usse continue karna hai?"

**5-year dream:** New phone → install AION → skills, memory, plugins, workflows, preferences restore → in 5 minutes the new phone behaves like the old one. Long-term, the same Brain should run on Android, Windows, Linux, macOS, Raspberry Pi, smart glasses, robots.

---

## 2. The Three Levels of AION

| Level | Name | Capabilities |
|---|---|---|
| 🟢 Level 1 | Assistant | Voice, Chat, Phone Control |
| 🟡 Level 2 | Operator | Multi-step planning, UI automation, Memory, Skills, Plugins |
| 🔴 Level 3 | Digital Employee | Goal-based end-to-end work (e.g., "AION, mera organic certification ka kaam complete karo" → mails, PDFs, Excel, documents, TC processing, reports, reminders, status — all under one goal) |

---

## 3. Golden Rules (Locked)

### Product Rules
1. **Free First** — paid APIs are optional, never forced
2. **Local First** — if the phone can do it, cloud is not used
3. **Modular Architecture** — every feature is a plugin
4. **Provider Independent** — no dependency on one company
5. **Model Independent** — swap Gemma → Qwen → anything via config only
6. **Offline Degrade** — basic AI works without internet
7. **Natural Language Only** — no fixed command syntax

### Engineering Rules (Rule 001–010)
1. Core never contains business logic — Core is only an orchestrator
2. Every capability is a plugin
3. Every plugin is replaceable
4. Every AI model is replaceable
5. Every provider is optional
6. Offline first
7. Privacy first
8. Self-learning
9. Everything measurable — every feature gets a benchmark
10. AI never pretends — if it doesn't know, it says "Mujhe nahi pata"

### AI Development Rules
NO HARDCODE · NO COMPANY LOCK · NO SINGLE MODEL · NO SINGLE PROVIDER · EVERYTHING REPLACEABLE · EVERYTHING MODULAR · FREE FIRST · LOCAL FIRST · SECURITY FIRST · SELF-EVOLVING

### Feature Gate — "Evolution over Features"
Every new feature must pass one test:
> **"Kya ye AI ko aur intelligent banata hai?"** → YES = Add · NO = Reject

### Trust & Safety Rules (Non-negotiable)
AION will NEVER without explicit user permission:
- Send mail
- Make payments
- Post on social media
- Install plugins
- Deploy code

It always says: *"Maine ye prepare kar diya hai. Kya approve karte ho?"*

---

## 4. Platform Approach (Decided)

**✅ AI Host App** — NOT a custom ROM, NOT replacing Android.

A single privileged app becomes the phone's "primary user":

```
Android
├── AI Host App (Always Running)
│   ├── Voice Assistant
│   ├── AI Agent
│   ├── Memory
│   ├── Vision
│   ├── Automation
│   ├── MCP Tools
│   └── Local Models
├── Accessibility Service
├── Notification Listener
├── Device Owner
├── Shizuku
├── Foreground Service
└── Other Apps
```

### Permission / Access Stack (Dedicated Phone)
- Accessibility Service
- Notification Access
- Usage Access
- Overlay (draw over other apps)
- Battery optimization ignore
- Auto-start
- **Device Owner** (set after factory reset)
- **Shizuku** (ADB-level shell permissions)
- All runtime permissions granted once
- Default Assistant App + optionally Default Home Launcher

### Known Limits (Accepted)
- No app can bypass all Android restrictions; signature-level permissions unavailable
- Banking/secure apps may block automation
- Some system settings need root/system app — root deferred, considered only if a specific feature needs it

### Key Research Findings (Android Platform — Deep Research #1)
- `AccessibilityService.takeScreenshot()` available API ≥ 31 (Android 12); **MediaProjection** fallback for older APIs
- Android 14+ foreground service **type** requirements for microphone and mediaProjection must be handled
- Binder/AIDL IPC, app sandboxing, Intents/ContentProviders define integration boundaries
- Android 2026 direction: AppFunctions API (Android MCP), Gemini Nano/AICore/ML Kit GenAI for on-device AI
- Best open mobile-agent stacks: **droidrun/mobilerun** — 91.4% on AndroidWorld benchmark (accessibility API driven, not screenshots)

### Honest Framing (from research — accepted constraints)
1. **The planning/tapping "brain" must be a cloud frontier model** — on-device 1B–4B models cannot drive reliable multi-step UI automation. They handle wake word, STT, intent classification, embeddings, simple offline fallback. "Fully offline autonomy" = limited degraded mode, not the target.
2. **UI automation violates many apps' ToS** (Instagram, WhatsApp, ride-hailing) — detectable, can get accounts banned. Controlled risk on burner accounts; prefer official APIs (Gmail, Calendar, Telegram, YouTube Data) wherever they exist.
3. **Thermal + battery are the real ceiling** — sustained on-device inference drops GPU clocks ~40% in minutes, ~10% battery per ~20 inferences. Winning pattern: **bursty on-device + escalate-to-cloud** (phone does the fast/private/cheap 80%, cloud does the hard 20%).

---

## 5. Brain Architecture (Decided: Hybrid)

```
                    AION Brain
                         │
                Brain Router Engine
                         │
 ┌──────────────┬──────────────┬──────────────┬──────────────┐
 │              │              │              │
Local LLM   Cloud APIs   Installed AI Apps   Future Providers
 │              │              │              │
Gemma       GPT API       ChatGPT App      Anything
Qwen        Gemini API    Gemini App       MCP
Phi         Claude API    Claude App       Custom
```

### Brain Abstraction Layer (BAL)
AION doesn't care what the backend is. Supported providers (target 10–15):
OpenAI · Anthropic · Google · xAI · DeepSeek · OpenRouter · Groq · NVIDIA NIM · Ollama (LAN) · llama.cpp server · vLLM · LM Studio · On-phone local model · Installed AI apps (fallback only)

### Provider Router Logic
```
Request → Local LLM can do it? → Yes → Local
        → No → Cloud API available? → Yes → API
        → No → Installed AI App enabled? → Yes → UI automation
        → No → Tell user
```
Failure handling: auth error / quota / rate limit / timeout / server down → auto-switch to next provider. (Providers don't expose live token balance — detect via error types.)

### Provider Scoring
Each provider scored on: Quality · Speed · Cost · Privacy · Offline · Context size · Vision · Coding.
**Brain Learning Engine** learns preferences over time (e.g., GPT for coding, Gemini for images, local for reminders) and auto-selects.

### Cost Priority Ladder
1. 🥇 Free & Open Source (default)
2. 🥈 Local first
3. 🥉 Free cloud tiers (OpenRouter free models, Groq free quota, provider free credits)
4. 💰 Paid (only if user configures; never hardcoded keys; app never forces paid)

Target: **90–95% of features run on free/open-source components.**

### Task-Type Routing
- Coding → Claude/GPT
- Deep reasoning → GPT
- Fast reply → Groq/OpenRouter
- Offline → Local LLM
- Vision → best available vision model
- Long documents → largest-context provider

Installed ChatGPT/Gemini apps = **emergency fallback only** (slow, UI-fragile, unofficial).

---

## 6. Multi-Agent System (Decided)

Core agents:
- **Brain** (orchestrator)
- **Planner**
- **Executor**
- **Memory Agent**
- **Vision Agent**
- **Voice Agent**
- **Tool Manager**
- **Learning Agent**

Meta agents (never talk to the user; only improve AION):
- **Architect Agent** — analyzes performance/logs, suggests improvements, optimizes plugins, detects bugs, compares new models
- **Research Agent** — watches internet/GitHub/docs for new models/libraries → benchmark → report → user approves upgrades (never self-upgrades)
- **QA Agent** — runs test suites on every plugin; production only if tests pass (e.g., 98/100 pass with critical failures = Reject)

*Final framework decision pending — see Section 13 (ADR-002).* Candidates: LangGraph, AutoGen, CrewAI, OpenAI Agents SDK, Google ADK, Semantic Kernel, Haystack, MCP-native, custom actor-based.

---

## 7. Core Modules (18)

1. AI Brain
2. Voice Engine
3. Vision Engine
4. Memory
5. Task Planner
6. Skill Manager
7. UI Automation
8. Provider Router
9. Local LLM Manager
10. Cloud AI Manager
11. Plugin Manager
12. Settings
13. Security
14. Analytics
15. Logs
16. Updates
17. Background Services
18. Developer Console

---

## 8. Plugin & Skill System

### Plugin SDK
- Every capability = plugin: Camera, Gallery, WhatsApp, Browser, Files, Contacts, Calendar, Music, Vision, OCR, Maps, Automation, MCP, APIs
- Humans AND AI can build plugins
- Every plugin is versioned (Mail v3.2, Chrome v1.4, Camera v2.0)
- Local Skill Marketplace on-device

### AION DNA (Plugin Compatibility Standard)
Every plugin must pass:
```
Can Learn? → Can Reflect? → Can Benchmark? → Can Update? → Compatible
```
If a plugin fails these standards → it does not install.

### Automatic Skill Generation (Self-Evolving Layer)
```
Task repeats → Detect → Auto Skill Generator → Skill Test →
Skill Optimize → User Approval → Skill Install → Direct use next time
```
Example: user asks "HR ko mail bhejo" 20 times → AI proposes "Email Skill v1" → tested → approved → next time zero planning, direct skill execution.

### Plugin Generator Agent
New app installed → AI reads UI via Accessibility → identifies buttons/navigation/common actions → proposes plugin → generates → sandbox test → user approval → install.

### Self-Coding Boundary (Locked)
AI never generates code and installs to production by itself. Pipeline:
Identify need → Draft plugin/code (via Codex or coding model) → Automated tests → Sandbox → **User approval** → Install.

---

## 9. Memory, Reflection & Learning

### Memory Types
Short-term · Long-term · Semantic · Episodic · **Skill Memory** · **Experience Memory** (e.g., Instagram → Upload → Caption → Success → Store, like human experience) · RAG retrieval

### AION OS Memory (behavioral)
Remembers how the user writes mails, tone per contact, app shortcuts, time routines, repeated tasks → proactively offers: *"Ankit, tum ye kaam roz karte ho. Kya main ise permanent skill bana du?"*

### Self-Reflection Engine
After every task:
```
Success? → No → Why? (Wrong button? UI changed? OCR issue? Model issue?)
→ Fix → Update Skill
```

### Learning Engine
Observes patterns (e.g., daily 7 AM weather check) → suggests "Morning Routine Skill".

### Dream Mode 🌙
When phone is charging + idle: analyze logs, optimize skills, merge duplicate memories, check plugin updates, run local benchmarks, suggest new workflows — like human sleep memory consolidation.

---

## 10. Voice & Vision

### Voice Stack
- Wake word (Porcupine / openWakeWord — benchmark)
- Streaming STT (Whisper-class / Moonshine / Indic wav2vec — Hindi support mandatory)
- Streaming TTS (Piper / Kokoro / latest maintained)
- Interruptible, context-aware, low latency, Hindi + English

### Vision Stack
- Accessibility tree scrape (primary)
- Screenshot capture (takeScreenshot API 31+ / MediaProjection fallback)
- OCR + icon/UI detection
- Screen understanding via multimodal LLM
- Gesture planning + self-correction

### Automation Routing
```
Task → API/MCP available? → Yes → API/MCP (Gmail, Calendar, GitHub, Drive, Telegram, YouTube Data)
     → No → UI Automation (Accessibility + Vision) — games, gallery, settings, third-party apps
```
Fast-paced games (BGMI/COD) = impractical real-time; turn-based games = feasible.

---

## 11. Tech Stack (2026)

| Layer | Choice |
|---|---|
| Android | Kotlin + Jetpack Compose |
| Database | Room/SQLite + ObjectBox or sqlite-vec (vector) |
| Local LLM runtime | llama.cpp / ExecuTorch / LiteRT-LM / MLC-LLM (benchmark → ADR) |
| Local models | Gemma, Qwen, Phi (open-weight, hot-swappable) |
| Voice | Whisper-class STT + Piper TTS (or latest maintained) |
| Vision | ML Kit + multimodal LLM |
| Cloud providers | OpenAI, Anthropic, Google, xAI, OpenRouter, Groq, NVIDIA NIM |
| Automation | Accessibility + Shizuku + Device Owner |
| Tooling standard | MCP-ready + Plugin SDK |
| Dev/ops | GitHub, Docker, CI/CD, laptop-side dev harness |

---

## 12. Non-Goals & Success Criteria

### Non-Goals
❌ Custom ROM · ❌ Replacing Android · ❌ Mandatory root · ❌ Dependence on any single AI company · ❌ Science fiction ("AI hacks internet", "AI does everything without permission", "magic control of every app")

### Success Criteria (v1)
✅ Voice conversation · ✅ Screen understanding · ✅ App control · ✅ Camera/photos · ✅ File management · ✅ Browser use · ✅ Read notifications · ✅ Multi-step task completion · ✅ Memory in use · ✅ Local AI working · ✅ Cloud AI switching

---

## 13. Documentation & Decision System

### Architecture Decision Records (ADR)
Every decision gets an ID, decision, reason, alternatives, rejected options. Decisions freeze once recorded.

- **ADR-001** — Local LLM Runtime → candidate: ExecuTorch (pending benchmark)
- **ADR-002** — Brain / Agent Architecture → **PENDING** (Deep Research #2 was triggered: LangGraph vs AutoGen vs CrewAI vs OpenAI Agents SDK vs Google ADK vs Semantic Kernel vs Haystack vs MCP vs custom actor-based; focus on memory, planning, self-reflection, tool calling, multi-agent comms, plugin systems, skill creation, event bus, orchestration, scalability)

### Document Plan (DOC-001 → DOC-020)
| ID | Document | ID | Document |
|---|---|---|---|
| DOC-001 | Project Vision | DOC-011 | Voice Engine |
| DOC-002 | Requirements (400+) | DOC-012 | Vision Engine |
| DOC-003 | System Architecture (200+ components) | DOC-013 | Provider Router |
| DOC-004 | AI Brain (internal design) | DOC-014 | Local AI |
| DOC-005 | Plugin SDK | DOC-015 | Cloud AI |
| DOC-006 | Skill SDK | DOC-016 | Android Core |
| DOC-007 | Self Reflection Engine | DOC-017 | Security |
| DOC-008 | Learning Engine | DOC-018 | Performance |
| DOC-009 | Automation Engine | DOC-019 | Database |
| DOC-010 | Memory Engine | DOC-020 | Implementation Plan |

Every document must contain: Research · Comparison · Benchmarks · GitHub alternatives · Latest papers · Official docs · Final decision · Why not other options · Future upgrades.

Also planned: **AION Manifesto** (what we build, what we don't, engineering principles, AI ethics, privacy, architecture philosophy, long-term vision) + SRS, API Spec, DB Design, Security Design, Test Strategy, Release Roadmap.

### Monorepo Structure
```
AION/
  docs/  research/  architecture/  brain/  android/  plugins/  skills/
  sdk/  models/  runtime/  tools/  backend/  tests/  benchmark/
  examples/  assets/  scripts/
```

### AION Research Lab
Every technology: Research → 10 candidate libraries → Benchmark → Winner → Production. Nothing enters production without a benchmark.

---

## 14. Roadmap

### Phase 0 — Research + Architecture (current; 50% of the project; zero code until architecture freeze)

**12 Research Phases:**
1. Project Vision & Requirements
2. Overall System Architecture
3. Android Architecture ✅ (Deep Research #1 complete)
4. AI Brain (multi-provider router, local+cloud, failover, context) 🔄 (Deep Research #2 triggered)
5. Local AI (models, quantization, runtimes, NPU/GPU)
6. Voice System (wake word, STT, TTS, Hindi+English, latency)
7. Vision & UI Automation
8. Memory (types + RAG)
9. Plugin & Tool Framework (MCP, extensibility)
10. Learning & Autonomy (workflow recording, reflection, recovery)
11. Performance & Security (battery, thermal, prompt-injection protection, logging)
12. Implementation Blueprint (monorepo, standards, milestones, CI/CD, releases)

Rule: **next phase starts only after current phase is complete and approved.**

### Implementation Phases (after architecture freeze; Codex implements, AION architecture reviews)
- **Phase 1 — AI Host App MVP:** Kotlin+Compose app, Accessibility, voice assistant, AI Brain, multi-provider, local memory, tap/swipe/type automation, settings, API key manager. Milestone: *"Open YouTube and play Arijit Singh"* works end-to-end.
- **Phase 2:** Vision, OCR, screen understanding, planning, self-correction
- **Phase 3:** Local LLM, offline mode, RAG memory, plugin system
- **Phase 4:** Skill learning, workflow recording, multi-agent, self-improving macros

### Sprint 1 / Phase 1.1 — AION Core Research (active)
15 research topics: Android Architecture · AI Brain · Multi-Agent · Plugin SDK · Skill Engine · Self Reflection · Learning Engine · Memory · Voice Stack · Vision Stack · UI Automation · Provider Router · Security · Performance · Background Execution.

### Engineering Discipline (committed)
Git, documentation, ADRs, testing, code review, benchmarks — professional engineering workflow throughout. No "jaldi bana do" approach.

---

## 15. Current Status & Immediate Next Steps

| Item | Status |
|---|---|
| Vision, rules, philosophy | ✅ Locked |
| Platform approach (AI Host App) | ✅ Locked |
| Hybrid Brain + Provider Router design | ✅ Locked |
| Plugin/Skill architecture + AION DNA | ✅ Locked |
| Safety boundaries (approval-gated actions) | ✅ Locked |
| Deep Research #1 — Android Platform | ✅ Complete |
| Deep Research #2 — Agent Architectures | 🔄 Triggered / report pending review |
| ADR-002 (Brain framework decision) | ⏳ Pending |
| DOC-001 (Vision document) | ⏳ Next after ADR-002 |

**Immediate next step:** Review Agent Architecture research → lock ADR-002 → write DOC-001 → continue research phases in order → architecture freeze → implementation via Codex.

---

*This document consolidates all decisions, rules, research findings, and roadmap from the AION planning conversation. It is the single source of truth until superseded by DOC-001 through DOC-020.*
