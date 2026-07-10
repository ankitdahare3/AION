package com.aion.brain

sealed class SkillSafetyResult {
    object Safe : SkillSafetyResult()

    data class Unsafe(
        val reasons: List<String>,
    ) : SkillSafetyResult()
}

/**
 * DOC-006 §3 "Static validation (schema, tool existence, param safety)" — the tool-existence and
 * param-safety parts (schema shape itself is [SkillValidator]). Grounded directly in DOC-017's SR
 * rules rather than invented policy: SR-01 (no side-effect action without approval) and SR-06 (no
 * credential entry into any field, ever).
 */
object SkillSafetyChecker {
    private val SIDE_EFFECT_KEYWORDS =
        listOf("send", "delete", "pay", "purchase", "install", "remove", "transfer", "buy", "post")
    private val CREDENTIAL_KEYWORDS = listOf("password", "otp", "pin", "secret", "token", "credential")

    fun check(
        skill: Skill,
        knownTools: Set<String>,
    ): SkillSafetyResult {
        val reasons = mutableListOf<String>()

        skill.steps.forEachIndexed { i, step ->
            val tool = step.tool
            if (tool != null) {
                if (tool !in knownTools) {
                    reasons += "step $i: unknown tool \"$tool\""
                }
                if (SIDE_EFFECT_KEYWORDS.any { tool.lowercase().contains(it) }) {
                    val precededByApproval = i > 0 && skill.steps[i - 1].isApprovalGate
                    if (!precededByApproval) {
                        reasons +=
                            "step $i ($tool) looks side-effecting but has no \"approval: required\" step immediately before it (SR-01)"
                    }
                }
                step.args.keys.forEach { argName ->
                    if (CREDENTIAL_KEYWORDS.any { argName.lowercase().contains(it) }) {
                        reasons +=
                            "step $i ($tool) arg \"$argName\" looks like a credential field — skills must never handle credentials (SR-06)"
                    }
                }
            }
        }

        return if (reasons.isEmpty()) SkillSafetyResult.Safe else SkillSafetyResult.Unsafe(reasons)
    }
}
