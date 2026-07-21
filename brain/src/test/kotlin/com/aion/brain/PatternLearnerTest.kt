package com.aion.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private fun tsAt(
    day: Int,
    hour: Int,
    minute: Int = 0,
): Long =
    LocalDate
        .of(2026, 7, day)
        .atTime(LocalTime.of(hour, minute))
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

private fun episode(
    goal: String,
    ts: Long,
) = ReflectionRecord(goal, "[]", TaskOutcome.SUCCESS, null, 100, 0.0, null, ts)

/** T-113 AC — "synthetic routine detected" for both time-based and sequence patterns. */
class PatternLearnerTest {
    @Test
    fun `a goal recurring at the same time of day on 3+ days is detected as a time-based routine`() {
        val episodes =
            listOf(
                episode("weather check karo", tsAt(1, 7, 0)),
                episode("Weather Check Karo", tsAt(2, 7, 5)), // case variant, still text-similar
                episode("weather check karo", tsAt(3, 6, 50)),
            )

        val proposals = PatternLearner.detectTimeBasedRoutines(episodes)

        assertEquals(1, proposals.size)
        assertEquals(RoutineKind.TIME_BASED, proposals.single().kind)
        assertEquals(
            3,
            proposals
                .single()
                .candidate.episodes.size,
        )
        assertTrue(proposals.single().description.contains("07:0"))
    }

    @Test
    fun `only 2 occurrences of a time-based pattern does not trigger detection`() {
        val episodes =
            listOf(episode("weather check karo", tsAt(1, 7, 0)), episode("weather check karo", tsAt(2, 7, 5)))

        assertTrue(PatternLearner.detectTimeBasedRoutines(episodes).isEmpty())
    }

    @Test
    fun `the same goal at wildly different times of day does not cluster`() {
        val episodes =
            listOf(
                episode("wifi on karo", tsAt(1, 7, 0)),
                episode("wifi on karo", tsAt(2, 14, 0)),
                episode("wifi on karo", tsAt(3, 21, 0)),
            )

        assertTrue(PatternLearner.detectTimeBasedRoutines(episodes).isEmpty())
    }

    @Test
    fun `a goal-pair repeating back-to-back 3+ times is detected as a sequence pattern`() {
        val episodes =
            listOf(
                episode("mail padho", tsAt(1, 9, 0)),
                episode("calendar check karo", tsAt(1, 9, 5)),
                episode("mail padho", tsAt(2, 9, 0)),
                episode("calendar check karo", tsAt(2, 9, 5)),
                episode("mail padho", tsAt(3, 9, 0)),
                episode("calendar check karo", tsAt(3, 9, 5)),
            )

        val proposals = PatternLearner.detectSequencePatterns(episodes)

        assertEquals(1, proposals.size)
        assertEquals(RoutineKind.SEQUENCE, proposals.single().kind)
        assertEquals(
            6,
            proposals
                .single()
                .candidate.episodes.size,
        )
        assertTrue(proposals.single().description.contains("mail padho"))
        assertTrue(proposals.single().description.contains("calendar check karo"))
    }

    @Test
    fun `a pair separated by too large a gap does not count as a sequence occurrence`() {
        val episodes =
            listOf(
                episode("mail padho", tsAt(1, 9, 0)),
                episode("calendar check karo", tsAt(1, 15, 0)), // 6h later — not "back-to-back"
                episode("mail padho", tsAt(2, 9, 0)),
                episode("calendar check karo", tsAt(2, 15, 0)),
                episode("mail padho", tsAt(3, 9, 0)),
                episode("calendar check karo", tsAt(3, 15, 0)),
            )

        assertTrue(PatternLearner.detectSequencePatterns(episodes).isEmpty())
    }

    @Test
    fun `unrelated interleaved episodes never produce a false sequence`() {
        val episodes =
            listOf(
                episode("mail padho", tsAt(1, 9, 0)),
                episode("wifi on karo", tsAt(1, 9, 2)),
                episode("calendar check karo", tsAt(1, 9, 5)),
            )

        assertTrue(PatternLearner.detectSequencePatterns(episodes).isEmpty())
    }

    @Test
    fun `an empty episode list produces no proposals from either detector`() {
        assertTrue(PatternLearner.detectTimeBasedRoutines(emptyList()).isEmpty())
        assertTrue(PatternLearner.detectSequencePatterns(emptyList()).isEmpty())
    }
}
