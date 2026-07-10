package com.aion.host.brain

import com.aion.brain.Skill
import com.aion.brain.SkillLoader
import com.aion.brain.SkillParseResult
import com.aion.brain.SkillStatus
import com.aion.brain.SkillStore
import com.aion.host.memory.SkillDao
import com.aion.host.memory.SkillEntity
import javax.inject.Inject
import javax.inject.Singleton

/** T-090 — Room-backed [SkillStore], storing each skill as YAML text in the existing `skills` table (T-060). */
@Singleton
class RoomSkillStore
    @Inject
    constructor(
        private val dao: SkillDao,
    ) : SkillStore {
        override suspend fun install(
            skill: Skill,
            status: SkillStatus,
        ) {
            val yaml = SkillLoader.serialize(skill)
            dao.insert(
                SkillEntity(
                    yaml = yaml,
                    version = 1,
                    status = status.name.lowercase(),
                    successCount = 0,
                    failCount = 0,
                    approvedAt = if (status == SkillStatus.PROPOSED) null else System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun activeSkills(): List<Skill> =
            dao.getByStatus(SkillStatus.ACTIVE.name.lowercase()).mapNotNull { entity ->
                (SkillLoader.parse(entity.yaml) as? SkillParseResult.Parsed)?.skill
            }
    }
