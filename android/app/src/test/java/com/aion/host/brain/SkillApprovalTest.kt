package com.aion.host.brain

import com.aion.brain.Provenance
import com.aion.brain.Skill
import com.aion.brain.SkillStep
import com.aion.brain.SkillTrigger
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillApprovalTest {
    @Test
    fun `skillSummary lists triggers and each step in human-readable form`() {
        val skill =
            Skill(
                id = "skill.email_hr_report/v1",
                trigger = SkillTrigger(examples = listOf("HR ko report mail karo", "send report to HR")),
                steps =
                    listOf(
                        SkillStep(tool = "gmail.compose", args = mapOf("to" to "hr@example.com")),
                        SkillStep(approval = "required"),
                        SkillStep(tool = "gmail.send"),
                    ),
                provenance = Provenance(generatedBy = "SkillGenerator"),
            )

        val summary = skillSummary(skill)

        assertTrue(summary.contains("HR ko report mail karo"))
        assertTrue(summary.contains("gmail.compose"))
        assertTrue(summary.contains("to=hr@example.com"))
        assertTrue(summary.contains("will ask for your approval again"))
        assertTrue(summary.contains("gmail.send"))
    }
}
