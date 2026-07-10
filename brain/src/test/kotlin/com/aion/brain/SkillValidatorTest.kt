package com.aion.brain

import org.junit.Assert.assertTrue
import org.junit.Test

private fun validSkill(
    id: String = "skill.test/v1",
    examples: List<String> = listOf("do the thing"),
    steps: List<SkillStep> = listOf(SkillStep(tool = "app.action")),
    generatedBy: String = "SkillGenerator",
    approvedBy: String? = "user",
) = Skill(
    id = id,
    trigger = SkillTrigger(examples = examples),
    steps = steps,
    provenance = Provenance(generatedBy = generatedBy, approvedBy = approvedBy),
)

class SkillValidatorTest {
    @Test
    fun `a well-formed skill passes validation`() {
        assertTrue(SkillValidator.validate(validSkill()) is SkillValidationResult.Valid)
    }

    @Test
    fun `a blank id fails validation`() {
        val result = SkillValidator.validate(validSkill(id = ""))
        assertInvalidContaining(result, "id")
    }

    @Test
    fun `no trigger examples fails validation`() {
        val result = SkillValidator.validate(validSkill(examples = emptyList()))
        assertInvalidContaining(result, "trigger.examples")
    }

    @Test
    fun `no steps fails validation`() {
        val result = SkillValidator.validate(validSkill(steps = emptyList()))
        assertInvalidContaining(result, "steps must not be empty")
    }

    @Test
    fun `a step with neither tool nor approval fails validation`() {
        val result = SkillValidator.validate(validSkill(steps = listOf(SkillStep())))
        assertInvalidContaining(result, "tool or an approval marker")
    }

    @Test
    fun `a step with both tool and approval fails validation`() {
        val result = SkillValidator.validate(validSkill(steps = listOf(SkillStep(tool = "x", approval = "required"))))
        assertInvalidContaining(result, "must not set both")
    }

    @Test
    fun `blank provenance generated_by fails validation`() {
        val result = SkillValidator.validate(validSkill(generatedBy = ""))
        assertInvalidContaining(result, "generated_by")
    }

    @Test
    fun `blank but present approved_by fails validation`() {
        val result = SkillValidator.validate(validSkill(approvedBy = ""))
        assertInvalidContaining(result, "approved_by")
    }

    @Test
    fun `null approved_by (not yet approved) is fine`() {
        assertTrue(SkillValidator.validate(validSkill(approvedBy = null)) is SkillValidationResult.Valid)
    }

    @Test
    fun `multiple violations are all reported together, not just the first`() {
        val result = SkillValidator.validate(validSkill(id = "", examples = emptyList(), steps = emptyList()))
        check(result is SkillValidationResult.Invalid)
        assertTrue("expected at least 3 errors, got ${result.errors}", result.errors.size >= 3)
    }

    private fun assertInvalidContaining(
        result: SkillValidationResult,
        substring: String,
    ) {
        check(result is SkillValidationResult.Invalid) { "expected Invalid, got $result" }
        assertTrue(
            "expected an error containing \"$substring\", got ${result.errors}",
            result.errors.any {
                it.contains(substring)
            },
        )
    }
}
