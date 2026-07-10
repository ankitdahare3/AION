package com.aion.brain

sealed class SkillGenerationResult {
    data class Drafted(
        val skill: Skill,
    ) : SkillGenerationResult()

    data class Failed(
        val reason: String,
    ) : SkillGenerationResult()
}

/** DOC-006 §3 — "SkillGenerator (LLM drafts YAML from episode traces)". Drafting alone, no validation — that's SkillValidator/SkillSafetyChecker/SkillSandbox, kept as separate stages so each failure mode is independently testable and diagnosable. */
class SkillGenerator(
    private val router: ProviderRouter,
) {
    suspend fun draft(candidate: RepeatedTaskCandidate): SkillGenerationResult {
        val traces =
            candidate.episodes.joinToString("\n") {
                "- goal: \"${it.goal}\", outcome: ${it.outcome}, plan: ${it.planJson}"
            }
        val req =
            BrainRequest(
                taskType = TaskType.PLAN,
                system = PERSONA,
                messages = listOf(Msg("user", "Repeated task, ${candidate.episodes.size} occurrences:\n$traces")),
            )
        val result =
            try {
                router.route(req)
            } catch (e: Exception) {
                return SkillGenerationResult.Failed("routing failed: ${e.message}")
            }
        return when (val parsed = SkillLoader.parse(result.text)) {
            is SkillParseResult.Parsed -> SkillGenerationResult.Drafted(parsed.skill)
            is SkillParseResult.ParseError -> SkillGenerationResult.Failed("invalid skill YAML: ${parsed.message}")
        }
    }

    private companion object {
        const val PERSONA =
            "You are AION's SkillGenerator. Given a repeated task's episode traces, draft a reusable " +
                "skill as YAML matching the aion-skill.yaml schema (id, trigger, params, steps, provenance). " +
                "Side-effecting steps MUST be preceded by an \"approval: required\" step. Output ONLY the YAML."
    }
}
