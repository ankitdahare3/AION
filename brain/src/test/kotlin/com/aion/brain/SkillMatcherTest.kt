package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun skill(
    id: String,
    vararg examples: String,
) = Skill(
    id = id,
    trigger = SkillTrigger(examples = examples.toList()),
    steps = listOf(SkillStep(tool = "app.action")),
    provenance = Provenance(generatedBy = "SkillGenerator"),
)

class SkillMatcherTest {
    @Test
    fun `an exact match on a trigger example matches with full confidence`() {
        val skills = listOf(skill("skill.hr_report", "HR ko report mail karo", "send report to HR"))

        val match = SkillMatcher.bestMatch("HR ko report mail karo", skills)

        assertEquals("skill.hr_report", match?.skill?.id)
        assertEquals(1.0, match!!.confidence, 0.0001)
    }

    @Test
    fun `an unrelated utterance matches nothing`() {
        val skills = listOf(skill("skill.hr_report", "HR ko report mail karo"))

        assertNull(SkillMatcher.bestMatch("play some music", skills))
    }

    @Test
    fun `the closest of several skills wins`() {
        val skills =
            listOf(
                skill("skill.hr_report", "HR ko report mail karo"),
                skill("skill.wifi_toggle", "wifi on karo"),
            )

        val match = SkillMatcher.bestMatch("WIFI ON KARO", skills)

        assertEquals("skill.wifi_toggle", match?.skill?.id)
    }

    @Test
    fun `no skills registered means no match`() {
        assertNull(SkillMatcher.bestMatch("anything", emptyList()))
    }
}
