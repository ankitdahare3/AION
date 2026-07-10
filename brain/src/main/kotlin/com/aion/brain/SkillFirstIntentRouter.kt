package com.aion.brain

sealed class SkillFirstOutcome {
    data class SkilledDone(
        val skill: Skill,
        val state: AgentState,
    ) : SkillFirstOutcome()

    data class FellBackToPlanner(
        val reason: String,
        val state: AgentState,
    ) : SkillFirstOutcome()
}

/**
 * DOC-006 §4 — "IntentClassifier checks skill triggers FIRST... skill runs → planning skipped.
 * Failure at any step → fall back to Planner". [runPlanner] is the caller's full planner-based
 * [AionGraph] run, kept abstract (a lambda) so `:brain` doesn't need to know `AionGraphFactory`'s
 * concrete shape — the router only cares whether planning happened, not how.
 */
class SkillFirstIntentRouter(
    private val skillExecutor: SkillExecutor,
    private val pluginId: String,
) {
    suspend fun run(
        goal: String,
        activeSkills: List<Skill>,
        runPlanner: suspend () -> AgentState,
    ): SkillFirstOutcome {
        val match =
            SkillMatcher.bestMatch(goal, activeSkills)
                ?: return SkillFirstOutcome.FellBackToPlanner("no skill matched", runPlanner())

        return when (val result = skillExecutor.execute(match.skill, pluginId)) {
            is SkillRunResult.Success ->
                SkillFirstOutcome.SkilledDone(
                    match.skill,
                    AgentState(goal = goal, done = true, response = "Done via skill: ${match.skill.id}"),
                )
            is SkillRunResult.Failed ->
                SkillFirstOutcome.FellBackToPlanner(
                    "skill step ${result.failedStep} failed: ${result.reason}",
                    runPlanner(),
                )
        }
    }
}
