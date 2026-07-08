package com.aion.brain

interface ScoreStore {
    fun taskScore(
        id: String,
        t: TaskType,
    ): Double

    fun latencyNorm(id: String): Double

    fun notInCooldown(id: String): Boolean

    fun recordSuccess(
        id: String,
        t: TaskType,
        latencyMs: Long,
        cost: Double,
    )

    fun recordFailure(
        id: String,
        t: TaskType,
        e: ProviderFailure,
    )
}

interface BudgetGuard {
    fun canSpend(req: BrainRequest): Boolean

    fun record(cost: Double)
}

/** DOC-013 Provider Router — scoring + failover. */
class ProviderRouter(
    private val registry: List<Provider>,
    private val scores: ScoreStore,
    private val budget: BudgetGuard,
) {
    suspend fun route(req: BrainRequest): BrainResult {
        val candidates =
            registry
                .filter { it.caps.tools || req.toolSchemas.isEmpty() }
                .filter { !req.needsVision || it.caps.vision }
                .filter { scores.notInCooldown(it.id) }
                .sortedByDescending { score(it, req) }
                .take(3)
        require(candidates.isNotEmpty()) { "No provider available for ${req.taskType}" }

        var last: Exception? = null
        var budgetBlocked = 0
        for (p in candidates) {
            if (p.tier == Tier.PAID && !budget.canSpend(req)) {
                budgetBlocked++
                continue
            }
            try {
                val t0 = System.currentTimeMillis()
                val res = p.complete(req)
                scores.recordSuccess(p.id, req.taskType, System.currentTimeMillis() - t0, res.costUsd)
                budget.record(res.costUsd)
                return res
            } catch (e: ProviderFailure) {
                scores.recordFailure(p.id, req.taskType, e)
                last = e
            }
        }
        throw last ?: IllegalStateException(
            if (budgetBlocked == candidates.size) {
                "Routing failed for ${req.taskType}: all ${candidates.size} candidate(s) are PAID-tier and budget-exhausted"
            } else {
                "Routing failed for ${req.taskType}: no candidate could be reached"
            },
        )
    }

    private fun score(
        p: Provider,
        req: BrainRequest,
    ): Double {
        val tierW =
            when (p.tier) {
                Tier.LOCAL -> 1.0
                Tier.FREE -> 0.7
                Tier.PAID -> 0.3
            }
        return 0.4 * scores.taskScore(p.id, req.taskType) +
            0.3 * tierW +
            0.2 * (1.0 - scores.latencyNorm(p.id)) +
            0.1 * (if (p.tier == Tier.LOCAL) 1.0 else 0.0)
    }
}
