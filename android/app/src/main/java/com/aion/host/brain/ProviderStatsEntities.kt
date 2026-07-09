package com.aion.host.brain

import androidx.room.Entity
import androidx.room.PrimaryKey

/** DOC-019 §1 providers_stats — per (provider, taskType) EMA scorecards feeding ScoreStore. */
@Entity(tableName = "provider_task_stats", primaryKeys = ["provider", "taskType"])
data class ProviderTaskStatEntity(
    val provider: String,
    val taskType: String,
    val successEma: Double,
    val latencyEmaMs: Double,
    val costEma: Double,
    val updated: Long,
)

/** Provider-wide cooldown window (DOC-013 §2: QUOTA/AUTH 6h, RATE_LIMIT short backoff). */
@Entity(tableName = "provider_cooldown")
data class ProviderCooldownEntity(
    @PrimaryKey val provider: String,
    val cooldownUntilMs: Long,
)

/** Daily cloud spend, keyed by day-epoch (DOC-013 §4: default $1/day). */
@Entity(tableName = "budget_day")
data class BudgetDayEntity(
    @PrimaryKey val dayEpoch: Long,
    val spentUsd: Double,
)
