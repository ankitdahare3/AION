package com.aion.brain

enum class MemoryKind { FACT, PREF, PROFILE }

/** DOC-019 §1 `memories` table shape (DOC-010 §3's write target — T-062, not built yet). */
data class Memory(
    val id: Long = 0,
    val kind: MemoryKind,
    val text: String,
    val confidence: Double,
    val provenance: String,
    val piiTags: List<String> = emptyList(),
    val created: Long,
    val accessed: Long,
    val decayScore: Double,
    val deletedSoft: Boolean = false,
)

/** Room-backed impl lives in `:android:app` (T-111), reusing the `memories` table T-060 already scoped. */
interface MemoryStore {
    suspend fun insert(memory: Memory): Long

    suspend fun getAllActive(): List<Memory>

    suspend fun update(memory: Memory)

    suspend fun softDelete(id: Long)

    /**
     * T-178 (BACKLOG.md's own T-173 finding) — `getAllActive()` loads the WHOLE table, already
     * implicated in a real production ANR from unbounded notification-ingestion growth. Any new
     * bulk-read caller (like [Bm25Ranker]-based retrieval) uses this bounded query instead, so it
     * doesn't compound that same risk. Default impl (fakes/tests) falls back to sorting the full
     * list — fine at test scale; the real `RoomMemoryStore` overrides this with an actual `LIMIT`
     * SQL query so the bound is real at the database layer, not just in application code.
     */
    suspend fun getRecentActive(limit: Int): List<Memory> = getAllActive().sortedByDescending { it.created }.take(limit)
}
