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
}
