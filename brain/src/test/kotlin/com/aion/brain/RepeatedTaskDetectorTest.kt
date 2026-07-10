package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun episode(goal: String) = ReflectionRecord(goal, "[]", TaskOutcome.SUCCESS, null, 100, 0.0, null)

class RepeatedTaskDetectorTest {
    @Test
    fun `3 similar synthetic episodes trigger a repeated-task candidate`() {
        val episodes =
            listOf(
                episode("wifi on karo"),
                episode("WIFI ON KARO"),
                episode("wifi on karo please"),
            )

        val candidates = RepeatedTaskDetector.detect(episodes)

        assertEquals(1, candidates.size)
        assertEquals(3, candidates.single().episodes.size)
    }

    @Test
    fun `only 2 similar episodes does not trigger detection`() {
        val episodes = listOf(episode("wifi on karo"), episode("WIFI ON KARO"))

        assertTrue(RepeatedTaskDetector.detect(episodes).isEmpty())
    }

    @Test
    fun `two separate repeated patterns produce two independent candidates`() {
        val episodes =
            listOf(
                episode("wifi on karo"),
                episode("WIFI ON KARO"),
                episode("wifi on karo please"),
                episode("open settings"),
                episode("OPEN SETTINGS"),
                episode("open settings karo"),
                episode("play music"), // unrelated, one-off — should not join either cluster
            )

        val candidates = RepeatedTaskDetector.detect(episodes)

        assertEquals(2, candidates.size)
        assertTrue(candidates.all { it.episodes.size == 3 })
        val allClusteredGoals = candidates.flatMap { it.episodes }.map { it.goal }
        assertTrue("the unrelated episode must not be swept into a cluster", "play music" !in allClusteredGoals)
    }

    @Test
    fun `no episodes means no candidates`() {
        assertTrue(RepeatedTaskDetector.detect(emptyList()).isEmpty())
    }

    @Test
    fun `entirely unrelated episodes never cluster`() {
        val episodes = listOf(episode("wifi on karo"), episode("open camera"), episode("send an email"))

        assertTrue(RepeatedTaskDetector.detect(episodes).isEmpty())
    }
}
