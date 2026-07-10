package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DOC-006 §2's own inline example uses shorthand like `tool: gmail.compose {to: "...", ...}` that
 * isn't actually valid YAML as written (a scalar directly followed by a flow-map on the same line,
 * with no key for it) — illustrative pseudo-syntax, not something a real YAML parser accepts. This
 * fixture captures the same semantic content (same skill, same steps, same approval gate) in valid
 * YAML matching [Skill]'s schema, since T-070's "parse the doc's example verbatim" approach only
 * works when the doc's example is actually parseable.
 */
private val VALID_SKILL_YAML =
    """
    id: skill.email_hr_report/v3
    trigger:
      examples: ["HR ko report mail karo", "send report to HR"]
      embedding: auto
    params:
      - name: attachment
        ask_if_missing: true
    steps:
      - tool: gmail.compose
        args: {to: "hr@rapidorganic.in", subject: "Daily Report"}
      - tool: gmail.attach
        args: {file: "{attachment}"}
      - approval: required
      - tool: gmail.send
    success_check:
      tool: gmail.sent_exists
      within: 60s
    provenance:
      generated_by: SkillGenerator
      approved_by: user
      date: "2026-07-10"
    """.trimIndent()

class SkillLoaderTest {
    @Test
    fun `a well-formed skill yaml parses into the full Skill shape`() {
        val result = SkillLoader.parse(VALID_SKILL_YAML)

        check(result is SkillParseResult.Parsed) { "expected Parsed, got $result" }
        val skill = result.skill
        assertEquals("skill.email_hr_report/v3", skill.id)
        assertEquals(2, skill.trigger.examples.size)
        assertEquals("attachment", skill.params.single().name)
        assertTrue(skill.params.single().askIfMissing)
        assertEquals(4, skill.steps.size)
        assertTrue(skill.steps[2].isApprovalGate)
        assertEquals("gmail.sent_exists", skill.successCheck?.tool)
        assertEquals("SkillGenerator", skill.provenance.generatedBy)
        assertEquals("user", skill.provenance.approvedBy)
    }

    @Test
    fun `malformed yaml is a clean ParseError, not a crash`() {
        val result = SkillLoader.parse("id: [unclosed")

        assertTrue(result is SkillParseResult.ParseError)
    }

    @Test
    fun `a missing required field is caught as a ParseError`() {
        val result = SkillLoader.parse("trigger:\n  examples: [\"hi\"]\nsteps: []\nprovenance: {generated_by: x}")

        assertTrue(result is SkillParseResult.ParseError)
    }
}
