package com.aion.brain

sealed class SkillPipelineResult {
    /** Passed every automated stage — still needs the mandatory human approval (DOC-006 §5) before install. Never auto-installed. */
    data class ReadyForApproval(
        val skill: Skill,
    ) : SkillPipelineResult()

    data class Rejected(
        val stage: String,
        val reasons: List<String>,
    ) : SkillPipelineResult()
}

/**
 * DOC-006 §3's generation pipeline, minus RepeatedTaskDetector (T-091, runs upstream to produce the
 * [RepeatedTaskCandidate]) and QAAgent scenario tests (no such infrastructure exists yet, same
 * "nothing real to test against" reasoning T-070 used to skip it for plugins) and the actual human
 * approval step, which is UI + a real person, not something this pipeline can do (T-092 AC:
 * "unsafe drafts rejected in tests" — the whole point is these rejections happen automatically,
 * BEFORE a human ever needs to look at the draft).
 */
class SkillGenerationPipeline(
    private val generator: SkillGenerator,
    private val knownTools: Set<String>,
) {
    suspend fun run(candidate: RepeatedTaskCandidate): SkillPipelineResult {
        val drafted =
            when (val result = generator.draft(candidate)) {
                is SkillGenerationResult.Failed -> return SkillPipelineResult.Rejected(
                    "generation",
                    listOf(result.reason),
                )
                is SkillGenerationResult.Drafted -> result.skill
            }

        val validation = SkillValidator.validate(drafted)
        if (validation is SkillValidationResult.Invalid) {
            return SkillPipelineResult.Rejected("schema", validation.errors)
        }

        val safety = SkillSafetyChecker.check(drafted, knownTools)
        if (safety is SkillSafetyResult.Unsafe) {
            return SkillPipelineResult.Rejected("safety", safety.reasons)
        }

        val dryRun = SkillSandbox.dryRun(drafted)
        if (!dryRun.passed) {
            return SkillPipelineResult.Rejected("sandbox", dryRun.issues)
        }

        return SkillPipelineResult.ReadyForApproval(drafted)
    }
}
