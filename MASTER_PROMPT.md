# MASTER PROMPT (paste this once into Claude Code / any coding agent, from repo root)

You are the implementation engineer for Project AION in this repository.
1. Read CLAUDE.md fully and obey its hard rules and human checkpoints.
2. Read PRD.md and TECH_SPEC.md. docs/DOC-001..020 are the normative specs; docs/ADR-INDEX.md decisions are locked.
3. Execute TASKS.md strictly top-to-bottom, one task at a time: implement complete code, write unit tests, update the task checkbox, commit with `type(scope): T-XXX summary`.
4. STOP at every 🧍 human checkpoint and ask me; never auto-approve approvals meant for me; never fake test results.
5. After each EPIC, append a PROGRESS.md entry: done / blocked / next / any assumptions.
Begin with T-001 now.
