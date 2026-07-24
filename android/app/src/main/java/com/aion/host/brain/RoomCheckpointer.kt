package com.aion.host.brain

import com.aion.brain.AgentState
import com.aion.brain.Checkpointer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DOC-019 §1 — [Checkpointer.save] is synchronous (called inline by [com.aion.brain.AionGraph]
 * after every node), so persistence is fire-and-forget on a background scope, same pattern as
 * [RoomScoreStore]/[RoomBudgetGuard].
 *
 * [liveState] (streaming execution progress, added alongside the Anthropic provider / real
 * MemoryWriterAgent gaps) is the one addition here: [AionGraph.run][com.aion.brain.AionGraph.run]
 * is frozen (no ADR to change its signature), but it already calls [save] synchronously after
 * every single node — updating an in-memory [StateFlow] here, before the async Room write, gives
 * [ChatScreen] a live view of the current step without touching the graph itself at all. One
 * shared flow across the whole app is fine: this UI only ever has one goal running at a time.
 */
@Singleton
class RoomCheckpointer
    @Inject
    constructor(
        private val dao: GraphCheckpointDao,
    ) : Checkpointer {
        // internal + var so tests can substitute a TestDispatcher-backed scope, same reasoning as RoomScoreStore.
        internal var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private val _liveState = MutableStateFlow<AgentState?>(null)
        val liveState: StateFlow<AgentState?> = _liveState.asStateFlow()

        /** T-168 (Voice Command History screen) — real past runs, terminal state only. */
        suspend fun recentCompletedRuns(limit: Int = 50): List<GraphCheckpointEntity> = dao.getRecentCompleted(limit)

        /** Called once per goal, before [AionGraph.run][com.aion.brain.AionGraph.run] starts, so a stale state from the previous goal never leaks into the next one's progress display. */
        fun resetLiveState() {
            _liveState.value = null
        }

        override fun save(s: AgentState) {
            _liveState.value = s
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
