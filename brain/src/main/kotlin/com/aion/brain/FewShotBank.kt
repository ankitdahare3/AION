package com.aion.brain

/** DOC-007 §3 patch target — a labeled example of a plan that failed, so the Planner can avoid repeating it. */
data class CounterExample(
    val goal: String,
    val badPlanJson: String,
    val reason: String,
)

/**
 * DOC-007 §3 — "Planner few-shot bank (max 50, LRU)". A flat, insertion-ordered list bounded two
 * ways: [maxSize] globally (oldest entry anywhere evicted first, real LRU across every goal), and
 * [maxPerGoal] per goal (oldest entry FOR THAT GOAL evicted once it has too many) — so a single
 * goal retried many times can't crowd out every other goal's memory, and its own remembered
 * mistakes stay a bounded, prompt-sized list rather than growing without limit.
 *
 * T-163 (BACKLOG.md) — originally a `Map<goal, CounterExample>`, which meant re-adding a mistake
 * for the same goal silently REPLACED the previous one rather than accumulating it: a model
 * cycling between 2-3 different wrong choices for the same goal could "forget" an earlier mistake
 * the instant a different one overwrote it. Changed to accumulate (up to [maxPerGoal]) once
 * `ReflectorAgent`'s own retry ceiling (T-163, default 5) made this safe to do without risking an
 * unbounded prompt — a single stuck run can now add at most a handful of counter-examples for its
 * own goal before that ceiling ends the run anyway.
 */
class FewShotBank(
    private val maxSize: Int = 50,
    private val maxPerGoal: Int = 5,
) {
    private val entries = mutableListOf<CounterExample>()

    @Synchronized
    fun add(example: CounterExample) {
        entries.add(example)
        while (entries.size > maxSize) {
            entries.removeAt(0)
        }
        val sameGoalIndices = entries.indices.filter { entries[it].goal.equals(example.goal, ignoreCase = true) }
        if (sameGoalIndices.size > maxPerGoal) {
            entries.removeAt(sameGoalIndices.first())
        }
    }

    @Synchronized
    fun examplesFor(goal: String): List<CounterExample> = entries.filter { it.goal.equals(goal, ignoreCase = true) }

    @Synchronized
    fun size(): Int = entries.size
}
