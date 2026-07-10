package com.aion.host.brain

import com.aion.brain.Provenance
import com.aion.brain.Skill
import com.aion.brain.SkillStatus
import com.aion.brain.SkillStep
import com.aion.brain.SkillTrigger
import com.aion.host.memory.SkillDao
import com.aion.host.memory.SkillEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSkillDao : SkillDao {
    val rows = mutableListOf<SkillEntity>()

    override suspend fun insert(skill: SkillEntity): Long {
        rows += skill
        return rows.size.toLong()
    }

    override suspend fun getByStatus(status: String): List<SkillEntity> = rows.filter { it.status == status }
}

private fun testSkill(id: String) =
    Skill(
        id = id,
        trigger = SkillTrigger(examples = listOf("do the thing")),
        steps = listOf(SkillStep(tool = "app.action")),
        provenance = Provenance(generatedBy = "SkillGenerator", approvedBy = "user"),
    )

class RoomSkillStoreTest {
    @Test
    fun `an installed active skill round-trips through activeSkills`() =
        runTest {
            val store = RoomSkillStore(FakeSkillDao())

            store.install(testSkill("skill.a"), SkillStatus.ACTIVE)

            assertEquals("skill.a", store.activeSkills().single().id)
        }

    @Test
    fun `a proposed skill does not show up as active`() =
        runTest {
            val store = RoomSkillStore(FakeSkillDao())

            store.install(testSkill("skill.a"), SkillStatus.PROPOSED)

            assertTrue(store.activeSkills().isEmpty())
        }

    @Test
    fun `multiple active skills all round-trip`() =
        runTest {
            val store = RoomSkillStore(FakeSkillDao())

            store.install(testSkill("skill.a"), SkillStatus.ACTIVE)
            store.install(testSkill("skill.b"), SkillStatus.ACTIVE)
            store.install(testSkill("skill.c"), SkillStatus.RETIRED)

            val ids = store.activeSkills().map { it.id }.toSet()
            assertEquals(setOf("skill.a", "skill.b"), ids)
        }
}
