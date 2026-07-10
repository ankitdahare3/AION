package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun memory(
    id: Long,
    text: String,
    kind: MemoryKind = MemoryKind.FACT,
    confidence: Double = 0.6,
    created: Long = 0,
    accessed: Long = 0,
    decayScore: Double = 1.0,
) = Memory(id, kind, text, confidence, "test", emptyList(), created, accessed, decayScore)

/** T-111 AC — dedupe/decay/promote, plus the "report artifact" itself. */
class MemoryConsolidatorTest {
    @Test
    fun `near-duplicate memories merge into the oldest survivor, promoted and duplicates removed`() {
        val memories =
            listOf(
                memory(1, "user's name is Ankit", created = 100, confidence = 0.5),
                memory(2, "user's name is ankit", created = 200, confidence = 0.7),
            )

        val result = MemoryConsolidator.consolidate(memories, nowMs = 1_000)

        assertEquals(1, result.report.mergedGroups)
        assertEquals(1, result.report.mergedAway)
        assertEquals(1, result.report.promotedCount)
        assertEquals(listOf(2L), result.softDeletes)
        val survivor = result.updates.single { it.id == 1L }
        assertTrue("expected confidence to increase past the higher original value", survivor.confidence > 0.7)
        assertEquals(1.0, survivor.decayScore, 1e-9)
    }

    @Test
    fun `unrelated memories never merge`() {
        val memories =
            listOf(
                memory(1, "user's name is Ankit"),
                memory(2, "wifi password is hunter2"),
            )

        val result = MemoryConsolidator.consolidate(memories, nowMs = 1_000)

        assertEquals(0, result.report.mergedGroups)
        assertTrue(result.softDeletes.isEmpty())
    }

    @Test
    fun `same text but different kind does not merge`() {
        val memories =
            listOf(
                memory(1, "formal tone", kind = MemoryKind.FACT),
                memory(2, "formal tone", kind = MemoryKind.PREF),
            )

        val result = MemoryConsolidator.consolidate(memories, nowMs = 1_000)

        assertEquals(0, result.report.mergedGroups)
    }

    @Test
    fun `a memory untouched for longer than the stale window decays`() {
        val memories = listOf(memory(1, "user works at Acme", accessed = 0, decayScore = 1.0))

        val result = MemoryConsolidator.consolidate(memories, nowMs = MemoryConsolidator.DEFAULT_STALE_MS + 1)

        assertEquals(1, result.report.decayedCount)
        assertEquals(1.0 - MemoryConsolidator.DEFAULT_DECAY_STEP, result.updates.single().decayScore, 1e-9)
    }

    @Test
    fun `a recently-accessed memory does not decay`() {
        val memories = listOf(memory(1, "user works at Acme", accessed = 900))

        val result = MemoryConsolidator.consolidate(memories, nowMs = 1_000)

        assertEquals(0, result.report.decayedCount)
        assertTrue(result.updates.isEmpty())
    }

    @Test
    fun `decay score never drops below zero`() {
        val memories = listOf(memory(1, "user works at Acme", accessed = 0, decayScore = 0.0))

        val result = MemoryConsolidator.consolidate(memories, nowMs = MemoryConsolidator.DEFAULT_STALE_MS + 1)

        assertTrue(result.updates.isEmpty()) // already at floor — nothing left to decay or update
        assertEquals(0, result.report.decayedCount)
    }

    @Test
    fun `soft-deleted memories are excluded from consolidation entirely`() {
        val memories =
            listOf(
                memory(1, "user's name is Ankit").copy(deletedSoft = true),
                memory(2, "user's name is Ankit"),
            )

        val result = MemoryConsolidator.consolidate(memories, nowMs = 1_000)

        assertEquals(0, result.report.mergedGroups)
        assertEquals(1, result.report.totalScanned)
    }

    @Test
    fun `an empty memory set produces a real zeroed report, not a crash`() {
        val result = MemoryConsolidator.consolidate(emptyList(), nowMs = 1_000)

        assertEquals(0, result.report.totalScanned)
        assertEquals(0, result.report.mergedGroups)
        assertEquals(0, result.report.decayedCount)
        assertTrue(result.report.summary.isNotBlank())
    }
}
