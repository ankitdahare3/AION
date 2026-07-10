package com.aion.brain

/** A cluster of ≥3 similar episodes — DOC-006 §3's signal that a skill draft is worth generating. */
data class RepeatedTaskCandidate(
    val representativeGoal: String,
    val episodes: List<ReflectionRecord>,
)

/**
 * DOC-006 §3 — "RepeatedTaskDetector (≥3 similar episodic memories, cosine >0.85)". Real cosine
 * similarity needs embeddings (T-061), blocked on T-032's llama.cpp — same wall as EPIC 2, already
 * deferred once this session. v1 uses [TextSimilarity] on each episode's `goal` as a stand-in,
 * at the same 0.85 threshold DOC-006 states, so swapping in real embeddings later only needs a new
 * scoring function, not a new clustering algorithm or a different [RepeatedTaskCandidate] shape.
 *
 * Greedy single-pass clustering: pick the first not-yet-clustered episode as an anchor, gather
 * everything similar enough to it, repeat with what's left. Not globally optimal (a different
 * anchor order could group things differently), but deterministic and good enough for "does a
 * repeated pattern exist at all" — DOC-006 §3's actual question, not "find the best clustering".
 */
object RepeatedTaskDetector {
    private const val SIMILARITY_THRESHOLD = 0.85
    private const val MIN_EPISODES = 3

    fun detect(episodes: List<ReflectionRecord>): List<RepeatedTaskCandidate> {
        val remaining = episodes.toMutableList()
        val candidates = mutableListOf<RepeatedTaskCandidate>()

        while (remaining.isNotEmpty()) {
            val anchor = remaining.removeAt(0)
            val cluster = mutableListOf(anchor)
            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val episode = iterator.next()
                if (TextSimilarity.similarity(anchor.goal, episode.goal) >= SIMILARITY_THRESHOLD) {
                    cluster += episode
                    iterator.remove()
                }
            }
            if (cluster.size >= MIN_EPISODES) {
                candidates += RepeatedTaskCandidate(anchor.goal, cluster)
            }
        }
        return candidates
    }
}
