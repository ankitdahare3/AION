package com.aion.brain

data class ConsolidationReport(
    val totalScanned: Int,
    val mergedGroups: Int,
    val mergedAway: Int,
    val decayedCount: Int,
    val promotedCount: Int,
    val summary: String,
)

data class ConsolidationResult(
    val report: ConsolidationReport,
    val updates: List<Memory>,
    val softDeletes: List<Long>,
)

/**
 * DOC-008 §5 step 2 — dedupe (cosine>0.95 merge in the doc; this project has no real embeddings yet,
 * T-032/T-061 both deliberately deferred, so — same honest stand-in [SkillMatcher]/
 * [RepeatedTaskDetector] already use — [TextSimilarity] fills in at the same threshold), decay
 * stale, promote repeated → long-term. This schema has one `memories` table, not a separate
 * short/long-term split (DOC-019 §1) — "promote" here means merging duplicates raises the
 * survivor's confidence and resets its decay clock, since repeated evidence for the same fact IS
 * the "worth keeping" signal, not a row migration to a table that doesn't exist.
 */
object MemoryConsolidator {
    const val DEFAULT_DEDUPE_THRESHOLD = 0.95
    const val DEFAULT_STALE_MS = 30L * 24 * 60 * 60 * 1000
    const val DEFAULT_DECAY_STEP = 0.1
    const val PROMOTE_BOOST_PER_DUPLICATE = 0.1

    fun consolidate(
        memories: List<Memory>,
        nowMs: Long,
        dedupeThreshold: Double = DEFAULT_DEDUPE_THRESHOLD,
        staleMs: Long = DEFAULT_STALE_MS,
        decayStep: Double = DEFAULT_DECAY_STEP,
    ): ConsolidationResult {
        val active = memories.filterNot { it.deletedSoft }
        val clusters = clusterByText(active, dedupeThreshold)

        val updates = mutableListOf<Memory>()
        val softDeletes = mutableListOf<Long>()
        var mergedGroups = 0
        var mergedAway = 0
        var promotedCount = 0
        var decayedCount = 0

        for (cluster in clusters) {
            if (cluster.size > 1) {
                mergedGroups++
                val survivor = cluster.minBy { it.created }
                val duplicates = cluster - survivor
                mergedAway += duplicates.size
                softDeletes += duplicates.map { it.id }

                // Trust the most-confident version of this fact seen across the cluster, then boost
                // further for repetition — not just the survivor's own (possibly stale/low) value.
                val baseConfidence = cluster.maxOf { it.confidence }
                val boosted = (baseConfidence + PROMOTE_BOOST_PER_DUPLICATE * duplicates.size).coerceAtMost(1.0)
                updates += survivor.copy(confidence = boosted, decayScore = 1.0, accessed = nowMs)
                promotedCount++
            } else {
                val m = cluster.single()
                if (nowMs - m.accessed > staleMs && m.decayScore > 0.0) {
                    updates += m.copy(decayScore = (m.decayScore - decayStep).coerceAtLeast(0.0))
                    decayedCount++
                }
            }
        }

        val summary =
            "Consolidated ${active.size} memories: $mergedGroups group(s) merged " +
                "($mergedAway duplicate(s) removed, $promotedCount promoted), $decayedCount decayed."

        return ConsolidationResult(
            report = ConsolidationReport(active.size, mergedGroups, mergedAway, decayedCount, promotedCount, summary),
            updates = updates,
            softDeletes = softDeletes,
        )
    }

    private fun clusterByText(
        memories: List<Memory>,
        threshold: Double,
    ): List<List<Memory>> {
        val remaining = memories.toMutableList()
        val clusters = mutableListOf<List<Memory>>()
        while (remaining.isNotEmpty()) {
            val anchor = remaining.removeAt(0)
            val matched =
                remaining.filter {
                    it.kind == anchor.kind && TextSimilarity.similarity(anchor.text, it.text) >= threshold
                }
            clusters += listOf(anchor) + matched
            remaining -= matched.toSet()
        }
        return clusters
    }
}
