package com.aion.host.brain

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/** T-053 AC (DOC-019 §1) — a snapshot of AgentState after every graph step, for debugging/audit. */
@Entity(tableName = "graph_checkpoint")
data class GraphCheckpointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goal: String,
    val currentStep: Int,
    val stepCount: Int,
    val needsApproval: Boolean,
    val done: Boolean,
    val response: String?,
    val planSummary: String,
    val toolResultsCount: Int,
    val failuresSummary: String,
    val timestamp: Long,
)

@Dao
interface GraphCheckpointDao {
    @Insert
    suspend fun insert(checkpoint: GraphCheckpointEntity)

    @Query("SELECT * FROM graph_checkpoint WHERE goal = :goal ORDER BY id ASC")
    suspend fun getForGoal(goal: String): List<GraphCheckpointEntity>
}
