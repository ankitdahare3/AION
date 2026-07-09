package com.aion.host.brain

import com.aion.brain.BrainRequest
import com.aion.brain.BudgetGuard
import com.aion.brain.ScoringMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.Volatile

/**
 * DOC-013 §4 BudgetGuard — [BudgetGuard]'s methods are synchronous, so today's spend lives in a
 * volatile field; Room persistence happens on a background scope. Call [load] once at startup.
 */
@Singleton
class RoomBudgetGuard
    @Inject
    constructor(
        private val dao: BudgetDao,
    ) : BudgetGuard {
        // internal + var so tests can substitute a TestDispatcher-backed scope, same reasoning as RoomScoreStore.
        internal var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        @Volatile private var spentTodayUsd: Double = 0.0
        private val dayEpoch = AtomicLong(currentDayEpoch())

        suspend fun load() {
            val today = currentDayEpoch()
            dayEpoch.set(today)
            spentTodayUsd = dao.getForDay(today)?.spentUsd ?: 0.0
        }

        override fun canSpend(req: BrainRequest): Boolean {
            resetIfNewDay()
            return ScoringMath.canSpend(spentTodayUsd)
        }

        override fun record(cost: Double) {
            resetIfNewDay()
            synchronized(this) {
                spentTodayUsd += cost
                val day = dayEpoch.get()
                val spent = spentTodayUsd
                scope.launch { dao.upsert(BudgetDayEntity(day, spent)) }
            }
        }

        private fun resetIfNewDay() {
            val today = currentDayEpoch()
            if (today != dayEpoch.get()) {
                dayEpoch.set(today)
                spentTodayUsd = 0.0
            }
        }

        companion object {
            private const val MS_PER_DAY = 86_400_000L

            private fun currentDayEpoch(): Long = System.currentTimeMillis() / MS_PER_DAY
        }
    }
