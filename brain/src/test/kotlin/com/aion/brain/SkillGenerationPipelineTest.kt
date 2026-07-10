package com.aion.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val noopScoreStore =
    object : ScoreStore {
        override fun taskScore(
            id: String,
            t: TaskType,
        ) = 0.5

        override fun latencyNorm(id: String) = 0.5

        override fun notInCooldown(id: String) = true

        override fun recordSuccess(
            id: String,
            t: TaskType,
            latencyMs: Long,
            cost: Double,
        ) {}

        override fun recordFailure(
            id: String,
            t: TaskType,
            e: ProviderFailure,
        ) {}
    }

private val alwaysCanSpend =
    object : BudgetGuard {
        override fun canSpend(req: BrainRequest) = true

        override fun record(cost: Double) {}
    }

private fun scriptedProvider(text: String) =
    object : Provider {
        override val id = "scripted"
        override val tier = Tier.LOCAL
        override val caps = ProviderCaps()

        override suspend fun complete(req: BrainRequest) =
            BrainResult(text = text, provider = id, latencyMs = 1, costUsd = 0.0)
    }

private fun candidate() =
    RepeatedTaskCandidate(
        representativeGoal = "HR ko report mail karo",
        episodes =
            List(
                3,
            ) { ReflectionRecord("HR ko report mail karo", "[]", TaskOutcome.SUCCESS, null, 100, 0.0, null) },
    )

private fun pipelineFor(
    draftYaml: String,
    knownTools: Set<String> = setOf("gmail.compose", "gmail.attach", "gmail.send"),
): SkillGenerationPipeline {
    val router = ProviderRouter(listOf(scriptedProvider(draftYaml)), noopScoreStore, alwaysCanSpend)
    return SkillGenerationPipeline(SkillGenerator(router), knownTools)
}

private val SAFE_SKILL =
    """
    id: skill.email_hr_report/v1
    trigger:
      examples: ["HR ko report mail karo"]
    steps:
      - tool: gmail.compose
        args: {to: "hr@example.com", subject: "Report"}
      - tool: gmail.attach
        args: {file: "{attachment}"}
      - approval: required
      - tool: gmail.send
    params:
      - name: attachment
        ask_if_missing: true
    provenance:
      generated_by: SkillGenerator
    """.trimIndent()

/** T-092 AC — unsafe drafts rejected in tests: each stage's rejection is proven independently. */
class SkillGenerationPipelineTest {
    @Test
    fun `a safe well-formed draft is ready for approval`() =
        runTest {
            val result = pipelineFor(SAFE_SKILL).run(candidate())

            assertTrue("expected ReadyForApproval, got $result", result is SkillPipelineResult.ReadyForApproval)
        }

    @Test
    fun `a draft referencing an unknown tool is rejected at the safety stage`() =
        runTest {
            val yaml =
                SAFE_SKILL.replace("gmail.compose", "unknown_service.do_thing")
            val result = pipelineFor(yaml).run(candidate())

            assertRejectedAt(result, "safety", "unknown tool")
        }

    @Test
    fun `a side-effecting step with no approval gate before it is rejected at the safety stage`() =
        runTest {
            val yaml =
                """
                id: skill.dangerous/v1
                trigger:
                  examples: ["delete my files"]
                steps:
                  - tool: files.delete
                    args: {path: "/sdcard/important"}
                provenance:
                  generated_by: SkillGenerator
                """.trimIndent()
            val result = pipelineFor(yaml, knownTools = setOf("files.delete")).run(candidate())

            assertRejectedAt(result, "safety", "approval")
        }

    @Test
    fun `a step arg that looks like a credential field is rejected at the safety stage`() =
        runTest {
            val yaml =
                """
                id: skill.login/v1
                trigger:
                  examples: ["log me in"]
                steps:
                  - tool: app.login
                    args: {username: "me", password: "hunter2"}
                provenance:
                  generated_by: SkillGenerator
                """.trimIndent()
            val result = pipelineFor(yaml, knownTools = setOf("app.login")).run(candidate())

            assertRejectedAt(result, "safety", "credential")
        }

    @Test
    fun `a draft referencing an undeclared param placeholder is rejected at the sandbox stage`() =
        runTest {
            val yaml = SAFE_SKILL.replace("params:\n  - name: attachment\n    ask_if_missing: true\n", "params: []\n")
            val result = pipelineFor(yaml).run(candidate())

            assertRejectedAt(result, "sandbox", "undeclared placeholder")
        }

    @Test
    fun `malformed yaml from the generator is rejected at the generation stage`() =
        runTest {
            val result = pipelineFor("not: valid: skill: yaml: [[[").run(candidate())

            assertTrue(result is SkillPipelineResult.Rejected)
            assertEquals("generation", (result as SkillPipelineResult.Rejected).stage)
        }

    @Test
    fun `a schema-invalid draft (blank id) is rejected at the schema stage`() =
        runTest {
            val yaml = SAFE_SKILL.replace("id: skill.email_hr_report/v1", "id: \"\"")
            val result = pipelineFor(yaml).run(candidate())

            assertRejectedAt(result, "schema", "id")
        }

    private fun assertRejectedAt(
        result: SkillPipelineResult,
        expectedStage: String,
        reasonSubstring: String,
    ) {
        check(result is SkillPipelineResult.Rejected) { "expected Rejected, got $result" }
        assertEquals(expectedStage, result.stage)
        assertTrue(
            "expected a reason containing \"$reasonSubstring\", got ${result.reasons}",
            result.reasons.any { it.contains(reasonSubstring, ignoreCase = true) },
        )
    }
}
