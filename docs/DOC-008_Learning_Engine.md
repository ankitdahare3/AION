# DOC-008 — LEARNING ENGINE
**Project AION | v1.0 | 07 July 2026**

## 1. Scope
Longitudinal improvement across tasks (vs Reflection = per-task). Four learners:

## 2. Pattern Learner
Mines episodic memory nightly: time-based routines (7AM weather), sequence patterns (mail→calendar→notes), context patterns (location/charging state). Output: routine proposals → "Ye roz karte ho, skill bana du?" (user approves).

## 3. Preference Learner
Observes corrections ("nahi, formal likho"), edits to drafts, rejected approvals. Updates user profile vectors: tone per contact, app preferences, working hours. Stored as versioned profile — user-viewable/editable (FR-M08).

## 4. Brain Learning (provider preferences)
Per task-type rolling scorecard: success rate, latency, cost, user satisfaction (implicit: no retry = good). Router consumes scores (DOC-013). Exploration: 5% ε-greedy to keep testing alternatives.

## 5. Dream Mode 🌙 (charging + idle + screen off, WorkManager constrained)
1. Log analysis + ReflectionRecord batch processing
2. Memory consolidation: dedupe (cosine >0.95 merge), decay stale, promote repeated → long-term
3. Skill optimization: merge overlapping skills, prune dead ones
4. Local micro-benchmarks (token/s, wake-word FA rate) → DOC-018 dashboards
5. Update check: model/plugin registry diff → morning proposal list
Budget: ≤30 min/night, ≤15% battery, abort on unplug/thermal.

## 6. Boundaries
Learning NEVER changes safety rules, approval requirements, or installs anything. All structural changes are proposals.
