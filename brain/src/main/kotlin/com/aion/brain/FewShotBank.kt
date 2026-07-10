package com.aion.brain

/** DOC-007 §3 patch target — a labeled example of a plan that failed, so the Planner can avoid repeating it. */
data class CounterExample(
    val goal: String,
    val badPlanJson: String,
    val reason: String,
)

/**
 * DOC-007 §3 — "Planner few-shot bank (max 50, LRU)". Bounded by insertion order: once full, the
 * oldest entry is evicted to make room for a new one. Keyed by goal so [examplesFor] can pull
 * exactly the counter-examples relevant to the goal being planned right now.
 */
class FewShotBank(
    private val maxSize: Int = 50,
) {
    private val entries = LinkedHashMap<String, CounterExample>()

    @Synchronized
    fun add(example: CounterExample) {
        entries[example.goal] = example
        if (entries.size > maxSize) {
            entries.remove(entries.keys.first())
        }
    }

    @Synchronized
    fun examplesFor(goal: String): List<CounterExample> =
        entries.values.filter { it.goal.equals(goal, ignoreCase = true) }

    @Synchronized
    fun size(): Int = entries.size
}
