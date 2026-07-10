package com.aion.host.brain

import com.aion.brain.Skill
import com.aion.host.security.ApprovalGateService

/**
 * T-092 "approval UI" — DOC-006 §3's mandatory human-approval step, reusing the existing
 * [ApprovalGateService]/`ApprovalSheetHost` (T-021) wholesale rather than a parallel approval
 * screen: a skill-install decision is still just "does the owner say yes to something," which is
 * exactly what that mechanism already does — suspends until resolved, shows a human-readable
 * detail, audits the decision. [skillSummary] supplies the human-readable part DOC-006 §3 asks for.
 */
suspend fun requestSkillApproval(
    approvalGateService: ApprovalGateService,
    skill: Skill,
): Boolean =
    approvalGateService.requestApproval(voiceLine = "Install skill \"${skill.id}\"?", detail = skillSummary(skill))

fun skillSummary(skill: Skill): String =
    buildString {
        appendLine("Triggers: ${skill.trigger.examples.joinToString(", ")}")
        append("Steps:")
        skill.steps.forEach { step ->
            append("\n  - ")
            if (step.isApprovalGate) {
                append("[will ask for your approval again during each run]")
            } else {
                append(step.tool)
                if (step.args.isNotEmpty()) {
                    append(" (${step.args.entries.joinToString { "${it.key}=${it.value}" }})")
                }
            }
        }
    }
