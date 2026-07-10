package com.aion.host.brain

import com.aion.brain.FailureCause
import com.aion.brain.ReflectionRecord
import com.aion.brain.ReflectionRecordStore
import com.aion.brain.TaskOutcome
import com.aion.host.memory.EpisodeDao
import com.aion.host.memory.EpisodeEntity
import javax.inject.Inject
import javax.inject.Singleton

/** T-080 — Room-backed [ReflectionRecordStore], reusing the `episodes` table (DOC-019 §1) rather than a new one. */
@Singleton
class RoomReflectionRecordStore
    @Inject
    constructor(
        private val dao: EpisodeDao,
    ) : ReflectionRecordStore {
        override suspend fun record(r: ReflectionRecord) {
            dao.insert(
                EpisodeEntity(
                    goal = r.goal,
                    planJson = r.planJson,
                    outcome = r.outcome.name,
                    failureClass = r.failureCause?.name,
                    latencyMs = r.latencyMs,
                    costUsd = r.costUsd,
                    appPkg = r.appPkg,
                    ts = System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun recent(limit: Int): List<ReflectionRecord> =
            dao.getAll().take(limit).map { e ->
                ReflectionRecord(
                    goal = e.goal,
                    planJson = e.planJson,
                    outcome = TaskOutcome.valueOf(e.outcome),
                    failureCause = e.failureClass?.let { FailureCause.valueOf(it) },
                    latencyMs = e.latencyMs,
                    costUsd = e.costUsd,
                    appPkg = e.appPkg,
                )
            }
    }
