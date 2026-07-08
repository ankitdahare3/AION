# AION — PRODUCT REQUIREMENTS DOCUMENT (PRD)
**v1.0 | 07 July 2026 | Owner: Ankit Pawar | Full detail: docs/DOC-001, DOC-002**

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

## 5. Success metrics (alpha exit — DOC-001 §12 Stage 1)
≥60% on 50-task internal Hindi+English benchmark · zero unauthorized irreversible actions · wake→listening <2s · median automation step <15s · cloud cost <$0.15/task, ≥50% requests free/local · crash-free ≥99%

## 6. Non-goals (v1)
Custom ROM · root required · daily-driver phone · real-time games · autonomous irreversible actions · any single-vendor dependency

## 7. Release
v0.1 Alpha = Sprints S1-S12 complete (docs/DOC-020), milestones M1 (S5: YouTube task) and M2 (S10: 3-app multi-step task) passed.
