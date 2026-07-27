package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Bm25RankerTest {
    @Test
    fun `a document containing the query term ranks above one that doesn't`() {
        val docs = listOf("owner likes cold coffee", "the weather today is sunny")

        val ranked = Bm25Ranker.rank("coffee", docs) { it }

        assertEquals("owner likes cold coffee", ranked.first())
    }

    @Test
    fun `a rarer query term across the corpus scores higher than a common one`() {
        // "flight" appears in only one document (rare, high IDF); "the" appears in both (common,
        // low/negative IDF) — the flight-specific document should win even though "the" also matches.
        val docs =
            listOf(
                "the flight leaves at 9am",
                "the weather is nice today",
                "the meeting is at noon",
            )

        val ranked = Bm25Ranker.rank("flight", docs) { it }

        assertEquals("the flight leaves at 9am", ranked.first())
    }

    @Test
    fun `an empty query returns documents unranked, not crashed or emptied`() {
        val docs = listOf("a", "b", "c")

        val ranked = Bm25Ranker.rank("", docs) { it }

        assertEquals(docs, ranked)
    }

    @Test
    fun `an empty document list returns empty, not a crash`() {
        val ranked = Bm25Ranker.rank("anything", emptyList<String>()) { it }

        assertTrue(ranked.isEmpty())
    }

    @Test
    fun `matching is case-insensitive and punctuation-tolerant`() {
        val docs = listOf("Owner's FLIGHT is delayed.", "nothing relevant here")

        val ranked = Bm25Ranker.rank("flight", docs) { it }

        assertEquals("Owner's FLIGHT is delayed.", ranked.first())
    }

    @Test
    fun `a document matching none of the query terms still sorts last, not thrown out`() {
        val docs = listOf("no overlap at all", "owner's flight is at 9am")

        val ranked = Bm25Ranker.rank("flight time", docs) { it }

        assertEquals(2, ranked.size)
        assertEquals("owner's flight is at 9am", ranked.first())
    }
}
