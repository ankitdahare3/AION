package com.aion.brain

import kotlin.math.ln

/**
 * FR-M03 (DOC-002, P0) / DOC-010 §2/§4 — real relevance ranking for memory retrieval, not just
 * recency. DOC-010's own spec calls for neural embeddings + vector search, but that needs a real
 * on-device embedding model this project doesn't have — the same wall T-032/T-061 already hit for
 * [MemoryConsolidator]/`SkillMatcher`/`RepeatedTaskDetector`, all three currently standing in with
 * [TextSimilarity]'s Jaccard-based heuristic instead (see their own KDocs). BM25 is a real,
 * well-established lexical relevance-ranking algorithm — the standard pre-neural-embedding
 * information-retrieval baseline, genuinely effective at this corpus size — and a real upgrade
 * over Jaccard's presence/absence-only matching (this weighs how RARE and how OFTEN a term
 * appears, not just whether it appears), with zero new dependencies, zero model download, zero
 * privacy question (DOC-010 §2 requires embeddings stay fully on-device — this is on-device by
 * construction, it's just arithmetic). A real neural-embedding upgrade remains a legitimate,
 * larger follow-up once T-032/T-061's on-device-embedder wall clears — tracked in BACKLOG.md, not
 * silently declared equivalent to what DOC-010 actually specifies.
 */
object Bm25Ranker {
    private const val K1 = 1.5
    private const val B = 0.75

    /** Ranks [documents] by BM25 relevance to [query], highest first. Empty query/documents pass through unranked. */
    fun <T> rank(
        query: String,
        documents: List<T>,
        textOf: (T) -> String,
    ): List<T> = rankScored(query, documents, textOf).map { it.first }

    /**
     * Same ranking as [rank], but keeps each document's raw score alongside it. A caller that
     * wants "only documents that actually share content with the query" (not just "whatever's
     * left after truncating to the pool") should filter on `score > 0.0` here — [rank] alone can't
     * express that distinction since it only returns the reordered list.
     */
    fun <T> rankScored(
        query: String,
        documents: List<T>,
        textOf: (T) -> String,
    ): List<Pair<T, Double>> {
        if (documents.isEmpty()) return emptyList()
        val queryTerms = tokenize(query)
        if (queryTerms.isEmpty()) return documents.map { it to 0.0 }

        val docTokens = documents.map { tokenize(textOf(it)) }
        val avgDocLen = docTokens.map { it.size }.average().takeIf { it > 0.0 } ?: 1.0
        val docCount = documents.size
        val distinctQueryTerms = queryTerms.distinct()

        // Standard BM25 IDF: rarer terms across the corpus score higher.
        val idf =
            distinctQueryTerms.associateWith { term ->
                val containing = docTokens.count { term in it }
                ln((docCount - containing + 0.5) / (containing + 0.5) + 1.0)
            }

        val scores =
            docTokens.map { tokens ->
                if (tokens.isEmpty()) {
                    0.0
                } else {
                    val termFreq = tokens.groupingBy { it }.eachCount()
                    distinctQueryTerms.sumOf { term ->
                        val tf = termFreq[term] ?: 0
                        if (tf == 0) {
                            0.0
                        } else {
                            val termIdf = idf.getValue(term)
                            termIdf * (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * tokens.size / avgDocLen))
                        }
                    }
                }
            }

        return documents.indices.sortedByDescending { scores[it] }.map { documents[it] to scores[it] }
    }

    private fun tokenize(text: String): List<String> = text.lowercase().split(TOKEN_SPLIT).filter { it.isNotBlank() }

    private val TOKEN_SPLIT = Regex("[^\\p{L}\\p{N}]+")
}
