# AION — PRODUCT REQUIREMENTS DOCUMENT (PRD)
**v1.1 | 12 July 2026 | Owner: Ankit Pawar | Full detail: docs/DOC-001, DOC-002 | v1.1 = v1.0 + audit findings (GAP_REPORT.md)**

## 1. Product
AION: personal, open-source AI Operating Layer that turns a dedicated Android phone into a voice-first, self-evolving agent the user owns. Tagline: "You own the operator."

## 2. Problem
Big-tech assistants are vendor-locked, closed, non-extensible, privacy-hostile, and ignore Hindi/Indic users. Open automation frameworks (droidrun etc.) are developer infra, not products.

## 3. Target user (v1)
Builder-owner: technical, privacy-conscious, Hindi+English bilingual, dedicates a spare Android phone (8GB+ RAM, Android 12+). First user = the owner (dogfooding).

## 4. Core user stories (must-have, v0.1 alpha)
- US1: Bolke baat karu (Hindi/English/Hinglish), AION naturally jawab de — offline bhi basic chat chale
- US2: "YouTube kholo aur Arijit Singh bajao" — AION khud app khol ke kaam kare
- US3: Koi bhi side-effect (send/pay/post/install/delete) se pehle AION approval maange; "AION stop" se sab turant ruke
- US4: AION mujhe yaad rakhe (naam, preferences, kaam) sessions ke paar; main memory dekh/edit/delete kar saku
- US5: Jo kaam main baar-baar karta hu, AION skill bana ke propose kare; approve karu to agli baar direct kare
- US6: Fail hone par AION khud samjhe kyun fail hua aur agli baar better kare
- US7: Har action ka audit log dikhe

## 5. What the FINISHED v0.1 alpha contains (audit-derived checklist)
Status as of the 2026-07-12 audit in brackets.

**Safety & trust (all DONE, device-verified)**
- Approval sheet before every side-effect; deny = hard stop [done]
- Kill-switch overlay + "aion stop" phrase [done]
- Hash-chained, tamper-evident audit log + viewer screen [done]
- InjectionFilter: screen/notification text is data, never instructions [done]
- SecretVault (Keystore-encrypted API keys) + masked keys screen with FLAG_SECURE [done]
- SQLCipher-encrypted database [done]

**Using AION**
- Setup wizard walking through all PR-02 permissions [done]
- Text chat screen "Talk to AION" — type a goal, watch it run, read the reply [coded, needs live verify + commit → T-130]
- Mic button: speak the goal (platform SpeechRecognizer, hi+en — ADR-011a interim) [missing → T-135]
- Spoken replies (platform TextToSpeech, matches reply language, mutable) [missing → T-136]
- Full voice pipeline: "AION" wake word, streaming STT (whisper.cpp), Piper TTS, barge-in [missing → T-010..T-015, after ADR-011a proves the loop; needs 🧍HC-2]

**Brain**
- Planner→Executor→Reflector→Responder graph over real cloud providers (Groq/OpenRouter/NVIDIA/Gemini), scored routing, budget guard, checkpoints [done; Gemini blocked by account quota, not code]
- Natural bilingual replies (ResponsePhrasing) [done]
- Local offline model (llama.cpp, Qwen3-4B) so US1's "offline bhi chale" is real [deferred → T-032]
- Intent classifier + context builder [done, v1 heuristics; local-LLM upgrade tracked in BACKLOG]

**Hands (device control)**
- Accessibility service: read screen, tap/type/scroll/launch, rate-limited, audited [done]
- OCR vision fallback (ML Kit) + element resolver + step verifier [done; screenshot capability config gap → T-132]
- Shizuku privileged-ops bridge with graceful degrade [done]
- **Accuracy: ≥60% on the 50-task Hindi+English benchmark — the alpha's pass/fail line. Honest score today: 7/50 (14%); diagnosed fixes queued → T-131..T-134**

**Memory & learning**
- Room schema + consolidation (dedupe/decay/promote) + Dream Mode nightly job [done]
- Device explorer feeding real installed-app names to the planner [done]
- MemoryAgent write policy (confidence-gated, UNVERIFIED tagging) [missing → T-062]
- Semantic recall (sqlite-vec + local embedder) [missing → T-061]
- Memory browser UI: search/view/edit/delete, "bhool jao" [missing → T-063]
- Skill engine: detect repeated tasks → draft skill → owner approval → skill-first execution [done, needs real-usage proof]

**Plugins**
- 8 built-ins registered + enabled: System, Contacts, Phone/SMS, Calendar, Files, Browser, Gmail, Telegram [done; Gmail/Telegram need owner account setup to be live]
- Notification ingestion via AionNotificationListener [shell only → T-137]

**Platform**
- Device Owner provisioning on the dedicated phone [script ready; 🧍HC-1 owner step pending]
- Biometric app-lock [missing → T-138]
- 24h battery / 7d uptime soak [pending → T-122, meaningful once a voice service runs continuously]

## 6. Success metrics (alpha exit — DOC-001 §12 Stage 1)
≥60% on 50-task internal Hindi+English benchmark (today: 14% — the #1 gap) · zero unauthorized irreversible actions · wake→listening <2s · median automation step <15s · cloud cost <$0.15/task, ≥50% requests free/local · crash-free ≥99%

## 7. Non-goals (v1)
Custom ROM · root required · daily-driver phone · real-time games · autonomous irreversible actions · any single-vendor dependency

## 8. Release
v0.1 Alpha = Sprints S1-S12 complete (docs/DOC-020) + EPIC 13 audit follow-ups, milestones M1 (S5: YouTube task) and M2 (S10: 3-app multi-step task) passed. Build order: GAP_REPORT.md "MY RECOMMENDED BUILD ORDER".
