package com.aion.brain

data class SkillDryRunResult(
    val passed: Boolean,
    val issues: List<String>,
)

/**
 * DOC-006 §3 "Sandbox dry-run (mock tool layer)" — no real tool layer exists to mock against yet
 * (that's PluginManager routing to real plugins, which this deliberately does NOT invoke — a dry
 * run must never cause a real side effect). What's checkable without touching a real tool: every
 * `{placeholder}` a step's args reference must resolve to either a declared [SkillParam] or a known
 * system-provided value (currently just `date`, matching DOC-006 §2's own example) — an undeclared
 * placeholder means the skill would fail at real runtime with no way to fill it in.
 */
object SkillSandbox {
    private val PLACEHOLDER = Regex("\\{(\\w+)\\}")
    private val BUILT_IN_PLACEHOLDERS = setOf("date")

    fun dryRun(skill: Skill): SkillDryRunResult {
        val declared = skill.params.map { it.name }.toSet()
        val issues = mutableListOf<String>()

        skill.steps.forEachIndexed { i, step ->
            step.args.forEach { (argName, value) ->
                PLACEHOLDER.findAll(value).forEach { match ->
                    val name = match.groupValues[1]
                    if (name !in declared && name !in BUILT_IN_PLACEHOLDERS) {
                        issues +=
                            "step $i arg \"$argName\" references undeclared placeholder {$name} — not in params and not a built-in"
                    }
                }
            }
        }

        return SkillDryRunResult(passed = issues.isEmpty(), issues = issues)
    }
}
