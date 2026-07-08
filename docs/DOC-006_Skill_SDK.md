# DOC-006 — SKILL SDK
**Project AION | v1.0 | 07 July 2026**

## 1. Skill vs Plugin
Plugin = capability (code). Skill = learned/authored PROCEDURE composed of tool calls (data, mostly). Skills execute without planning → fast, cheap, deterministic.

## 2. Skill Format (aion-skill.yaml)
```yaml
id: skill.email_hr_report/v3
trigger: {examples: ["HR ko report mail karo", "send report to HR"], embedding: auto}
params: [{name: attachment, ask_if_missing: true}]
steps:
  - tool: gmail.compose {to: "hr@rapidorganic.in", subject: "Daily Report {date}"}
  - tool: gmail.attach {file: "{attachment}"}
  - approval: required          # side-effect step
  - tool: gmail.send
success_check: {tool: gmail.sent_exists, within: 60s}
provenance: {generated_by: SkillGenerator, approved_by: user, date: ...}
```

## 3. Generation Pipeline (locked)
RepeatedTaskDetector (≥3 similar episodic memories, cosine >0.85)
→ SkillGenerator (LLM drafts YAML from episode traces)
→ Static validation (schema, tool existence, param safety)
→ Sandbox dry-run (mock tool layer)
→ QAAgent scenario tests
→ **User approval (mandatory, shows human-readable summary)**
→ Install to SkillStore → trigger-embedding indexed

## 4. Runtime
IntentClassifier checks skill triggers FIRST (vector match >0.9) → skill runs → planning skipped. Failure at any step → fall back to Planner + log for Reflector → skill auto-patch proposal (needs re-approval).

## 5. Self-Coding Boundary (restated, absolute)
Generated skills/plugins NEVER auto-install. Pipeline always ends at human approval.
