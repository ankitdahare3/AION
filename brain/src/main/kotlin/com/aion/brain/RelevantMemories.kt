package com.aion.brain

/**
 * DOC-010 §4's "Read policy (ContextBuilder)" — hybrid retrieval, reranked, capped before it ever
 * reaches a prompt — implemented here with [Bm25Ranker] instead of the doc's specified vector
 * search (see that object's own KDoc for why). Shared by [ChatAgent] and [PlannerAgent] so a
 * relevant past fact/preference grounds BOTH a casual chat reply and a device-automation plan, not
 * just one of them — [ChatAgent] never read [MemoryStore] at all before this.
 *
 * [MemoryKind.PROFILE] is deliberately excluded: [PlannerAgent.withKnownApps] already folds those
 * in separately for a different purpose (known installed-app package names) — mixing them into a
 * generic "relevant memories" section would just duplicate that under a different heading.
 */
object RelevantMemories {
    // Bounded candidate pool per BACKLOG.md's own T-173 finding (getAllActive() already caused a
    // real ANR from unbounded growth) — BM25-rank only the most recent slice, not the whole table.
    private const val CANDIDATE_POOL = 200
    const val MAX_RESULTS = 5

    suspend fun find(
        memoryStore: MemoryStore?,
        query: String,
    ): List<Memory> {
        val candidates =
            memoryStore
                ?.getRecentActive(CANDIDATE_POOL)
                ?.filter { it.kind != MemoryKind.PROFILE }
                .orEmpty()
        if (candidates.isEmpty()) return emptyList()
        // Only genuinely relevant memories (real term overlap with the query) — a memory that
        // merely survived truncation to the candidate pool, with zero actual overlap, is noise in
        // a prompt, not retrieval; Bm25Ranker.rank alone can't express that distinction.
        return Bm25Ranker
            .rankScored(query, candidates) { it.text }
            .filter { (_, score) -> score > 0.0 }
            .take(MAX_RESULTS)
            .map { (memory, _) -> memory }
    }
}
