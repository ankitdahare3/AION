package com.aion.host.brain

import com.aion.brain.AgentState
import com.aion.brain.Checkpointer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DOC-019 §1 — [Checkpointer.save] is synchronous (called inline by [com.aion.brain.AionGraph]
 * after every node), so persistence is fire-and-forget on a background scope, same pattern as
 * [RoomScoreStore]/[RoomBudgetGuard].
 */
@Singleton
class RoomCheckpointer
    @Inject
    constructor(
        private val dao: GraphCheckpointDao,
    ) : Checkpointer {
        // internal + var so tests can substitute a TestDispatcher-backed scope, same reasoning as RoomScoreStore.
        internal var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** T-168 (Voice Command History screen) — real past runs, terminal state only. */
        suspend fun recentCompletedRuns(limit: Int = 50): List<GraphCheckpointEntity> = dao.getRecentCompleted(limit)

        override fun save(s: AgentState) {
            val entity =
                GraphCheckpointEntity(
                    goal = s.goal,
                    currentStep = s.currentStep,
                    stepCount = s.stepCount,
                    needsApproval = s.needsApproval,
                    done = s.done,
                    response = s.response,
                    planSummary = s.plan.joinToString("|") { "${it.action}:${it.target}" },
                    toolResultsCount = s.toolResults.size,
                    failuresSummary = s.failures.joinToString("\n"),
                    timestamp = System.currentTimeMillis(),
                )
            scope.launch { dao.insert(entity) }
        }
    }
