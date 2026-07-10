package com.aion.brain

data class SkillMatch(
    val skill: Skill,
    val confidence: Double,
)

/**
 * DOC-006 §4 — "IntentClassifier checks skill triggers FIRST (vector match >0.9)". A real vector
 * embedding index needs a local embedder (T-061), blocked on T-032's llama.cpp JNI (deliberately
 * deferred, same wall as EPIC 2). v1 stand-in: match the utterance against each skill's
 * `trigger.examples` via [TextSimilarity] (same algorithm [com.aion.host.automation.ElementResolver]
 * uses), same "v1 rule-based, real thing later" pattern as T-033's IntentClassifier. Swap the body
 * of [bestMatch] for a real cosine-similarity lookup once T-061 lands — the 0.9 threshold and
 * SkillMatch shape shouldn't need to change.
 */
object SkillMatcher {
    private const val MATCH_THRESHOLD = 0.9

    fun bestMatch(
        utterance: String,
        skills: List<Skill>,
    ): SkillMatch? =
        skills
            .map { skill ->
                skill to
                    (skill.trigger.examples.maxOfOrNull { TextSimilarity.similarity(utterance, it) } ?: 0.0)
            }.filter { it.second >= MATCH_THRESHOLD }
            .maxByOrNull { it.second }
            ?.let { (skill, score) -> SkillMatch(skill, score) }
}
