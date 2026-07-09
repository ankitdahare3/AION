package com.aion.host.brain

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProviderStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStat(stat: ProviderTaskStatEntity)

    @Query("SELECT * FROM provider_task_stats")
    suspend fun getAllStats(): List<ProviderTaskStatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCooldown(cooldown: ProviderCooldownEntity)

    @Query("SELECT * FROM provider_cooldown")
    suspend fun getAllCooldowns(): List<ProviderCooldownEntity>
}

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: BudgetDayEntity)

    @Query("SELECT * FROM budget_day WHERE dayEpoch = :dayEpoch")
    suspend fun getForDay(dayEpoch: Long): BudgetDayEntity?
}
