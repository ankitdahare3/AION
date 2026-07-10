package com.aion.host.brain

import com.aion.brain.Memory
import com.aion.brain.MemoryKind
import com.aion.brain.MemoryStore
import com.aion.host.memory.MemoryDao
import com.aion.host.memory.MemoryEntity
import javax.inject.Inject
import javax.inject.Singleton

/** T-111 — Room-backed [MemoryStore], reusing the `memories` table (DOC-019 §1) T-060 already scoped. */
@Singleton
class RoomMemoryStore
    @Inject
    constructor(
        private val dao: MemoryDao,
    ) : MemoryStore {
        override suspend fun insert(memory: Memory): Long = dao.insert(memory.toEntity())

        override suspend fun getAllActive(): List<Memory> = dao.getAllActive().map { it.toMemory() }

        override suspend fun update(memory: Memory) = dao.update(memory.toEntity())

        override suspend fun softDelete(id: Long) = dao.softDelete(id)

        private fun Memory.toEntity() =
            MemoryEntity(
                id = id,
                kind = kind.name,
                text = text,
                confidence = confidence,
                provenance = provenance,
                piiTags = piiTags.joinToString(","),
                created = created,
                accessed = accessed,
                decayScore = decayScore,
                deletedSoft = deletedSoft,
            )

        private fun MemoryEntity.toMemory() =
            Memory(
                id = id,
                kind = MemoryKind.valueOf(kind),
                text = text,
                confidence = confidence,
                provenance = provenance,
                piiTags = piiTags.split(",").filter { it.isNotBlank() },
                created = created,
                accessed = accessed,
                decayScore = decayScore,
                deletedSoft = deletedSoft,
            )
    }
