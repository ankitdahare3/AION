package com.aion.host.brain

import com.aion.host.memory.ConversationDao
import com.aion.host.memory.ConversationEntity
import com.aion.host.memory.TurnDao
import com.aion.host.memory.TurnEntity

/** In-memory ProviderStatsDao/BudgetDao for plain-JVM unit tests — no Room/Android runtime needed. */
class FakeProviderStatsDao : ProviderStatsDao {
    val stats = mutableMapOf<Pair<String, String>, ProviderTaskStatEntity>()
    val cooldowns = mutableMapOf<String, ProviderCooldownEntity>()

    override suspend fun upsertStat(stat: ProviderTaskStatEntity) {
        stats[stat.provider to stat.taskType] = stat
    }

    override suspend fun getAllStats(): List<ProviderTaskStatEntity> = stats.values.toList()

    override suspend fun upsertCooldown(cooldown: ProviderCooldownEntity) {
        cooldowns[cooldown.provider] = cooldown
    }

    override suspend fun getAllCooldowns(): List<ProviderCooldownEntity> = cooldowns.values.toList()
}

class FakeBudgetDao : BudgetDao {
    val days = mutableMapOf<Long, BudgetDayEntity>()

    override suspend fun upsert(day: BudgetDayEntity) {
        days[day.dayEpoch] = day
    }

    override suspend fun getForDay(dayEpoch: Long): BudgetDayEntity? = days[dayEpoch]
}

class FakeGraphCheckpointDao : GraphCheckpointDao {
    val saved = mutableListOf<GraphCheckpointEntity>()

    override suspend fun insert(checkpoint: GraphCheckpointEntity) {
        saved += checkpoint
    }

    override suspend fun getForGoal(goal: String): List<GraphCheckpointEntity> = saved.filter { it.goal == goal }

    override suspend fun getRecentCompleted(limit: Int): List<GraphCheckpointEntity> =
        saved.filter { it.done }.sortedByDescending { it.id }.take(limit)
}

class FakeConversationDao : ConversationDao {
    val rows = mutableListOf<ConversationEntity>()
    private var nextId = 1L

    override suspend fun insert(conversation: ConversationEntity): Long {
        val withId = conversation.copy(id = nextId++)
        rows += withId
        return withId.id
    }

    override suspend fun getAll(): List<ConversationEntity> = rows.sortedByDescending { it.startedAt }
}

class FakeTurnDao : TurnDao {
    val rows = mutableListOf<TurnEntity>()
    private var nextId = 1L

    override suspend fun insert(turn: TurnEntity) {
        rows += turn.copy(id = nextId++)
    }

    override suspend fun getForConversation(convId: Long): List<TurnEntity> =
        rows.filter { it.convId == convId }.sortedBy { it.ts }
}
