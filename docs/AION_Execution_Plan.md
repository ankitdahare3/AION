# AION EXECUTION PLAN — Kaise Karna Hai, Kya Karna Hai
**v1.0 | 07 July 2026 | Companion to DOC-020**

Ye document DOC-020 (Implementation Plan) ka practical version hai: tumhare actions vs AI ke actions, week-by-week.

---

## PHASE A — SETUP (Week 1, one-time, ~4-6 hours total)

### A1. Hardware (Day 1)
| Item | Requirement | Action |
|---|---|---|
| Dedicated phone | Android 12+ (API 31+), 8GB+ RAM, Snapdragon 7-series ya better | Purana phone hai to use karo (factory reset hoga). Nahi to used/refurb ~₹10-15k (e.g., Snapdragon 7+ Gen 2/3 device). 8GB RAM non-negotiable (local LLM ke liye) |
| Dev machine | Lenovo LOQ (Ryzen 7, RTX 4050, 16GB) | ✅ Already have — perfect. RTX 4050 se local model testing bhi laptop par hogi |
| Cable | USB data cable | ADB ke liye |

### A2. Laptop setup (Day 1-2, ~2 hrs)
1. Install **Android Studio** (latest) + JDK 17 (Studio bundle me aata hai)
2. Install **Git** + GitHub account me private repo banao: `AION`
3. Zip jo maine diya hai (AION-v0.1-docs-and-scaffold) → extract → repo me push:
   ```
   git init && git add . && git commit -m "S1: docs + scaffold" && git push
   ```
4. Install **Python 3.11+** (bench harness ke liye)
5. `adb devices` working confirm karo (phone connect karke)

### A3. API keys (Day 2, ~30 min, sab FREE)
| Provider | Kahan se | Cost |
|---|---|---|
| Groq | console.groq.com | Free quota |
| OpenRouter | openrouter.ai | Free models available |
| Google AI Studio | aistudio.google.com | Gemini free tier |
| (Optional later) OpenAI/Anthropic | jab paid chahiye | Skip for now |
Keys ek local file me rakho (repo me kabhi commit NAHI — .gitignore me hai).

### A4. Phone provisioning (Day 3, ~1 hr) — DOC-016 §3
1. Factory reset → setup me **Google account SKIP** karo (Device Owner tabhi banega)
2. Developer options + USB debugging on
3. Jab app ka pehla APK banega: `scripts/provision-device-owner.sh` run karo
4. Shizuku install + adb se start
⚠️ Ye phone ab sirf AION ka hai — personal use nahi.

---

## PHASE B — WORKING MODEL (Tum + AI, permanent workflow)

### Roles
- **AI (Claude):** architecture decisions, complete code files likhna, errors debug karna, research, benchmarks design, docs update
- **Tum:** code ko Android Studio me daalna, build, phone par test, errors/logs/screenshots wapas paste karna, approvals, git commits

### Per-session loop (har coding session, ~1-2 hrs)
```
1. Tum: "S2 start" / "ye error aaya" + logs paste
2. AI: complete file(s) deta hai (kabhi partial snippets nahi)
3. Tum: file paste → Build → Run on phone
4. Pass → git commit → next task | Fail → logcat error paste → AI fix
5. Session end: AI progress note deta hai (kal continue karne ke liye)
```

### Strong recommendation: Claude Code
Ye chat code likh sakti hai lekin tumhare laptop par build/test nahi kar sakti. **Claude Code** (desktop) tumhare repo me directly kaam karega — files khud edit, build errors khud padh ke fix. Loop 5x fast ho jayega. Is chat ko architecture/research/review ke liye rakho, Claude Code ko implementation ke liye.

### Discipline rules (non-negotiable)
1. Ek sprint ek waqt — agla sprint tabhi jab exit test pass
2. SafetyCore pehle, automation baad me (order kabhi ulta nahi)
3. Har feature = code + test + commit (DOC-020 §3 DoD)
4. Har Friday: 15-min review — kya pass hua, kya blocked, docs update

---

## PHASE C — SPRINT EXECUTION (kya karna hai, kab)

Realistic pace: job ke saath 10-15 hrs/week → 1 sprint ≈ 2-3 weeks. Total alpha ≈ 7-9 months. (Full-time hota to 6.)

| Sprint | Tumhara kaam | AI ka kaam | Exit test |
|---|---|---|---|
| **S1** (ab) | A1-A4 setup complete karo; repo push; empty app build karke phone par install | App skeleton ka complete code (Manifest, Application, MainActivity, FGS shells, AdminReceiver) | App boots as Device Owner |
| **S2** | openWakeWord "AION" model train (AI guide karega, ~200 samples record), APK test | WakeWordService, whisper.cpp integration, Piper TTS, VoiceSession code | Bolo "AION" → wo transcript wapas bole |
| **S3** | Groq/OpenRouter keys settings me daalo; Hindi/English chat test | Router wiring, 3 provider adapters, llama.cpp JNI + Qwen3-4B download flow, chat UI | Voice chat works, offline bhi |
| **S4** | Settings app me scripted taps observe karo, failures report | A11yTreeReader, ActionDispatcher, 5 primitives, ElementResolver v0 | Scripted navigation reliable |
| **S5** | Approval prompts test, kill-switch test | Planner/Executor/Verifier agents, ApprovalGate UI, AuditLogger | **M1: "Open YouTube and play Arijit Singh" by voice** 🏆 |
| **S6** | Memory browser use karo, facts feed karo | Room schema, sqlite-vec, MemoryAgent, ContextBuilder | Cross-session recall |
| **S7** | 6 plugins ko real tasks do | Plugin SDK + System/Contacts/SMS/Calendar/Files/Browser plugins | Tool-calls via PluginManager |
| **S8** | Failures induce karo (app update, UI change) | Reflector, failure taxonomy, ElementMap cache | Recovery >50% |
| **S9** | Ek repeated task 3x karo, skill approve karo | RepeatedTaskDetector, SkillGenerator, sandbox | Learned skill daily use me |
| **S10** | Gmail/Telegram API setup (AI guide karega) | Vision fallback (OCR+grounding), Gmail/Telegram plugins | **M2: 3-app multi-step task** 🏆 |
| **S11** | Nightly reports padho | Dream Mode, Learning Engine, dashboards | Nightly report generates |
| **S12** | 50-task benchmark run karo | Security audit vs DOC-017, hardening, release notes | **v0.1 ALPHA** 🏆 |

---

## PHASE D — WEEKLY ROUTINE (suggested)

- **Mon-Thu (1-2 hr/evening):** coding loop (Phase B)
- **Sat (3-4 hrs):** bada block — integration + phone testing
- **Fri (15 min):** review + docs/ADR update
- **Sun:** off (burnout = project death; ye 7-9 month ka marathon hai)

## PHASE E — BUDGET

| Item | Cost |
|---|---|
| Phone (agar kharidna pada) | ₹10-15k one-time |
| APIs (free tiers) | ₹0 start; paid optional baad me (~₹500-1000/month max cap) |
| Software (Studio, Git, sab) | ₹0 |
| **Total to M1** | **Phone ke alawa ~₹0** |

## PHASE F — DECISION LOG DISCIPLINE
Koi bhi naya decision → docs/ADR-INDEX.md me entry pehle, code baad me. Android ka har naya release → 30-min impact review (AAPM/FGS changes, DOC-001 risk #1).

---

## ABHI KA IMMEDIATE TO-DO (is hafte)
1. ☐ Zip download → GitHub private repo push
2. ☐ Android Studio + Git + Python install
3. ☐ Groq + OpenRouter + AI Studio keys le lo
4. ☐ Dedicated phone decide (purana ya kharidna)
5. ☐ Wapas aao → bolo **"S1 code do"** → main app skeleton ke complete files dunga
