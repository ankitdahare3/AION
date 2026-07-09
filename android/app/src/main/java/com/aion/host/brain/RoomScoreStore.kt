package com.aion.host.brain

import com.aion.brain.ProviderFailure
import com.aion.brain.ScoreStore
import com.aion.brain.ScoringMath
import com.aion.brain.TaskType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private data class StatKey(
    val provider: String,
    val taskType: TaskType,
)

private data class Stat(
    val successEma: Double,
    val latencyEmaMs: Double,
    val costEma: Double,
)

/**
 * DOC-013 §2 ScoreStore, DOC-019 providers_stats — [ScoreStore]'s methods are synchronous (the
 * router calls them inline while sorting candidates), so reads/writes go through an in-memory
 * cache; persistence to Room happens on a background scope and never blocks the caller. Call
 * [load] once at startup to warm the cache from disk before the first [ScoreStore] call.
 */
@Singleton
class RoomScoreStore
    @Inject
    constructor(
        private val dao: ProviderStatsDao,
    ) : ScoreStore {
        private val stats = ConcurrentHashMap<StatKey, Stat>()
        private val cooldowns = ConcurrentHashMap<String, Long>()

        // internal + var so tests can substitute a TestDispatcher-backed scope and deterministically
        // await the fire-and-forget persist calls instead of racing real Dispatchers.IO.
        internal var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun load() {
            dao.getAllStats().forEach { e ->
                stats[StatKey(e.provider, TaskType.valueOf(e.taskType))] =
                    Stat(e.successEma, e.latencyEmaMs, e.costEma)
            }
            dao.getAllCooldowns().forEach { c -> cooldowns[c.provider] = c.cooldownUntilMs }
        }

        override fun taskScore(
            id: String,
            t: TaskType,
        ): Double = stats[StatKey(id, t)]?.successEma ?: ScoringMath.DEFAULT_SUCCESS_SCORE

        override fun latencyNorm(id: String): Double {
            val relevant = stats.filterKeys { it.provider == id }.values
            val avgLatencyMs =
                if (relevant.isEmpty()) {
                    ScoringMath.DEFAULT_LATENCY_MS
                } else {
                    relevant
                        .map {
                            it.latencyEmaMs
                        }.average()
                }
            return ScoringMath.latencyNorm(avgLatencyMs)
        }

        override fun notInCooldown(id: String): Boolean {
            val until = cooldowns[id] ?: return true
            return System.currentTimeMillis() >= until
        }

        override fun recordSuccess(
            id: String,
            t: TaskType,
            latencyMs: Long,
            cost: Double,
        ) {
            val key = StatKey(id, t)
            val prev = stats[key] ?: Stat(ScoringMath.DEFAULT_SUCCESS_SCORE, ScoringMath.DEFAULT_LATENCY_MS, 0.0)
            val updated =
                Stat(
                    successEma = ScoringMath.ema(prev.successEma, 1.0),
                    latencyEmaMs = ScoringMath.ema(prev.latencyEmaMs, latencyMs.toDouble()),
                    costEma = ScoringMath.ema(prev.costEma, cost),
                )
            stats[key] = updated
            persist(id, t, updated)
        }

        override fun recordFailure(
            id: String,
            t: TaskType,
            e: ProviderFailure,
        ) {
            val key = StatKey(id, t)
            val prev = stats[key] ?: Stat(ScoringMath.DEFAULT_SUCCESS_SCORE, ScoringMath.DEFAULT_LATENCY_MS, 0.0)
            val updated = prev.copy(successEma = ScoringMath.ema(prev.successEma, 0.0))
            stats[key] = updated
            persist(id, t, updated)

            val cooldownMs = ScoringMath.cooldownDurationMs(e)
            if (cooldownMs > 0) {
                val until = System.currentTimeMillis() + cooldownMs
                cooldowns[id] = until
                scope.launch { dao.upsertCooldown(ProviderCooldownEntity(id, until)) }
            }
        }

        private fun persist(
            id: String,
            t: TaskType,
            stat: Stat,
        ) {
            scope.launch {
                dao.upsertStat(
                    ProviderTaskStatEntity(
                        provider = id,
                        taskType = t.name,
                        successEma = stat.successEma,
                        latencyEmaMs = stat.latencyEmaMs,
                        costEma = stat.costEma,
                        updated = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }
