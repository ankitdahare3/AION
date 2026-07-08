# DOC-007 — SELF REFLECTION ENGINE
**Project AION | v1.0 | 07 July 2026**

## 1. Purpose
Convert every task outcome (success AND failure) into structural improvement: patched skills, better plans, updated element maps, provider re-scoring.

## 2. Reflection Loop (runs after every task, async, local-model-first)
```
TaskOutcome {goal, plan, steps[], observations[], result, latency, cost}
→ Classify: SUCCESS | PARTIAL | FAILURE
→ if FAILURE: root cause via taxonomy:
   E1 wrong element resolved     → update ElementMap for app+screen
   E2 UI changed (version drift) → invalidate cached selectors, re-explore
   E3 OCR/vision misread         → flag region, prefer a11y over vision here
   E4 model/plan error           → store counter-example for planner few-shots
   E5 permission/platform block  → surface to user, update capability matrix
   E6 timing/race                → insert wait/verify step in skill
→ Emit ReflectionRecord → Memory(episodic) + patch proposals
```

## 3. Patch targets
- Skill patch (YAML diff) → re-approval if side-effect steps changed
- Planner few-shot bank (max 50, LRU)
- ElementMap per app-version (selector cache with confidence decay)
- Provider scorecard (E4 attributed to provider lowers task-type score)

## 4. Metrics (fed to DOC-018 Performance)
First-attempt success rate, recovery success rate (after replan), mean steps vs optimal, failure taxonomy distribution weekly.

## 5. Honesty rule
If cause is UNKNOWN → record UNKNOWN. Never fabricate a cause (Rule: AI never pretends).
