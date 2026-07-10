package com.aion.brain

import java.time.Instant
import java.time.ZoneId

enum class RoutineKind { TIME_BASED, SEQUENCE }

/** DOC-008 §2 — a detected routine, ready to feed [SkillGenerationPipeline] (same shape [RepeatedTaskDetector] already produces) once the owner approves it ("Ye roz karte ho, skill bana du?"). */
data class RoutineProposal(
    val kind: RoutineKind,
    val description: String,
    val candidate: RepeatedTaskCandidate,
)

/**
 * DOC-008 §2 — "Mines episodic memory nightly: time-based routines (7AM weather), sequence
 * patterns (mail→calendar→notes), context patterns (location/charging state)."
 *
 * Context patterns are deliberately NOT built: DOC-019 §1's `episodes` table has no location or
 * charging-state column to mine at all — building this would mean inventing a schema change beyond
 * this task's scope, not a detection-algorithm gap. Named honestly, tracked in BACKLOG.md, rather
 * than faking a detector that has no real signal to work from.
 *
 * Both detectors reuse [RepeatedTaskCandidate] — the exact shape [SkillGenerationPipeline] already
 * consumes (T-092) — so a detected routine needs zero new plumbing to become a skill draft once
 * approved; a routine proposal IS a specialized way of finding what [RepeatedTaskDetector] finds,
 * just clustered by a different signal (time-of-day / adjacency) instead of pure text similarity.
 */
object PatternLearner {
    private const val MIN_OCCURRENCES = 3
    private const val TEXT_SIMILARITY_THRESHOLD = 0.85
    private const val DEFAULT_TIME_WINDOW_MINUTES = 60
    private const val DEFAULT_MAX_GAP_MS = 30L * 60 * 1000

    /** "7AM weather" — the same goal recurring at roughly the same time of day, ≥3 times. */
    fun detectTimeBasedRoutines(
        episodes: List<ReflectionRecord>,
        windowMinutes: Int = DEFAULT_TIME_WINDOW_MINUTES,
    ): List<RoutineProposal> {
        val remaining = episodes.toMutableList()
        val proposals = mutableListOf<RoutineProposal>()

        while (remaining.isNotEmpty()) {
            val anchor = remaining.removeAt(0)
            val anchorMinute = minuteOfDay(anchor.ts)
            val cluster = mutableListOf(anchor)
            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val e = iterator.next()
                val minuteGap = Math.abs(minuteOfDay(e.ts) - anchorMinute)
                if (TextSimilarity.similarity(anchor.goal, e.goal) >= TEXT_SIMILARITY_THRESHOLD && minuteGap <= windowMinutes) {
                    cluster += e
                    iterator.remove()
                }
            }
            if (cluster.size >= MIN_OCCURRENCES) {
                val desc = "Every day around ${formatMinuteOfDay(anchorMinute)}, you: \"${anchor.goal}\" — make this a routine?"
                proposals += RoutineProposal(RoutineKind.TIME_BASED, desc, RepeatedTaskCandidate(anchor.goal, cluster))
            }
        }
        return proposals
    }

    /** "mail→calendar→notes" — the same pair of goals happening back-to-back, ≥3 separate times. */
    fun detectSequencePatterns(
        episodes: List<ReflectionRecord>,
        maxGapMs: Long = DEFAULT_MAX_GAP_MS,
    ): List<RoutineProposal> {
        val sorted = episodes.sortedBy { it.ts }
        val occurrences = mutableMapOf<Pair<String, String>, MutableList<ReflectionRecord>>()

        for (i in 0 until sorted.size - 1) {
            val a = sorted[i]
            val b = sorted[i + 1]
            val gap = b.ts - a.ts
            if (gap in 0..maxGapMs) {
                occurrences.getOrPut(a.goal to b.goal) { mutableListOf() }.addAll(listOf(a, b))
            }
        }

        return occurrences
            .filter { (_, eps) -> eps.size >= MIN_OCCURRENCES * 2 } // each occurrence contributes 2 episodes (a, b)
            .map { (key, eps) ->
                val (goalA, goalB) = key
                val desc = "You often do \"$goalA\" then \"$goalB\" together — make this a routine?"
                RoutineProposal(RoutineKind.SEQUENCE, desc, RepeatedTaskCandidate("$goalA → $goalB", eps.distinct()))
            }
    }

    private fun minuteOfDay(ts: Long): Int {
        val time = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalTime()
        return time.hour * 60 + time.minute
    }

    private fun formatMinuteOfDay(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)
}
