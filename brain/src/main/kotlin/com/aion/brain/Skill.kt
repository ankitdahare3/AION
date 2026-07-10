package com.aion.brain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** DOC-006 §2 — aion-skill.yaml schema. "embedding: auto" is a placeholder for the real vector index (T-061, blocked on T-032) — v1 matches trigger examples via string similarity instead (see SkillMatcher). */
@Serializable
data class SkillTrigger(
    val examples: List<String>,
    val embedding: String = "auto",
)

@Serializable
data class SkillParam(
    val name: String,
    @SerialName("ask_if_missing") val askIfMissing: Boolean = false,
)

/** One step: either a tool call (`tool` set) or an approval gate (`approval` set) — never both. */
@Serializable
data class SkillStep(
    val tool: String? = null,
    val args: Map<String, String> = emptyMap(),
    val approval: String? = null,
) {
    val isApprovalGate: Boolean get() = approval != null
}

@Serializable
data class SuccessCheck(
    val tool: String,
    val within: String = "60s",
)

@Serializable
data class Provenance(
    @SerialName("generated_by") val generatedBy: String,
    @SerialName("approved_by") val approvedBy: String? = null,
    val date: String? = null,
)

@Serializable
data class Skill(
    val id: String,
    val trigger: SkillTrigger,
    val params: List<SkillParam> = emptyList(),
    val steps: List<SkillStep>,
    @SerialName("success_check") val successCheck: SuccessCheck? = null,
    val provenance: Provenance,
)
