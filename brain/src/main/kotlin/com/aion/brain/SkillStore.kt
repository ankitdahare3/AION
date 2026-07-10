package com.aion.brain

/** status: proposed|approved|active|retired (DOC-019 §1 `skills` table). */
enum class SkillStatus { PROPOSED, APPROVED, ACTIVE, RETIRED }

/** DOC-006 §3's final pipeline step, "Install to SkillStore". Room-backed impl lives in `:android:app` (T-090). */
interface SkillStore {
    suspend fun install(
        skill: Skill,
        status: SkillStatus = SkillStatus.ACTIVE,
    )

    suspend fun activeSkills(): List<Skill>
}
