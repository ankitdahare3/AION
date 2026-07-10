package com.aion.brain

sealed class SkillValidationResult {
    object Valid : SkillValidationResult()

    data class Invalid(
        val errors: List<String>,
    ) : SkillValidationResult()
}

/**
 * DOC-006 §3 "Static validation (schema, tool existence, param safety)" — the schema/shape part of
 * that pipeline stage (tool-existence checking needs a real PluginManager registry to check against,
 * out of scope here; param safety is DOC-017-level content filtering, also separate). Collects every
 * violation rather than stopping at the first, same diagnosable-failure preference as DNAValidator.
 */
object SkillValidator {
    fun validate(skill: Skill): SkillValidationResult {
        val errors = mutableListOf<String>()

        if (skill.id.isBlank()) errors += "id must not be blank"
        if (skill.trigger.examples.isEmpty()) errors += "trigger.examples must have at least one example"
        if (skill.trigger.examples.any { it.isBlank() }) errors += "trigger.examples must not contain blank entries"
        if (skill.steps.isEmpty()) errors += "steps must not be empty"

        skill.steps.forEachIndexed { i, step ->
            when {
                step.isApprovalGate && step.tool != null -> errors += "step $i must not set both tool and approval"
                step.isApprovalGate && step.approval != "required" ->
                    errors +=
                        "step $i approval must be \"required\" if set"
                !step.isApprovalGate && step.tool.isNullOrBlank() ->
                    errors +=
                        "step $i must have either a tool or an approval marker"
            }
        }

        if (skill.provenance.generatedBy.isBlank()) errors += "provenance.generated_by must not be blank"
        // DOC-006 §5 self-coding boundary: a skill can never claim to already be approved by nobody.
        if (skill.provenance.approvedBy != null && skill.provenance.approvedBy.isBlank()) {
            errors += "provenance.approved_by, if present, must not be blank"
        }

        return if (errors.isEmpty()) SkillValidationResult.Valid else SkillValidationResult.Invalid(errors)
    }
}
