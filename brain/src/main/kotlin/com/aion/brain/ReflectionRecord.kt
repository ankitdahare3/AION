package com.aion.brain

enum class TaskOutcome { SUCCESS, PARTIAL, FAILURE }

/** DOC-007 §2's `TaskOutcome` shape, emitted after every task (success and failure alike). */
data class ReflectionRecord(
    val goal: String,
    val planJson: String,
    val outcome: TaskOutcome,
    val failureCause: FailureCause?,
    val latencyMs: Long,
    val costUsd: Double,
    val appPkg: String?,
)

/** DOC-019 §1 `episodes` table. Room-backed impl lives in `:android:app` (T-080). */
interface ReflectionRecordStore {
    suspend fun record(r: ReflectionRecord)

    suspend fun recent(limit: Int): List<ReflectionRecord>
}
