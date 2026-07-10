package com.aion.brain

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerializationException

sealed class SkillParseResult {
    data class Parsed(
        val skill: Skill,
    ) : SkillParseResult()

    data class ParseError(
        val message: String,
    ) : SkillParseResult()
}

/**
 * DOC-006 §2 — parses aion-skill.yaml. Malformed YAML (syntax) and missing-required-field errors
 * both surface as [SerializationException] through kaml's kotlinx.serialization integration —
 * caught as one "ParseError" outcome, distinct from schema *validation* (SkillValidator), same
 * split T-070's PluginManifestLoader already established.
 */
object SkillLoader {
    fun parse(yamlText: String): SkillParseResult =
        try {
            SkillParseResult.Parsed(Yaml.default.decodeFromString(Skill.serializer(), yamlText))
        } catch (e: SerializationException) {
            SkillParseResult.ParseError(e.message ?: "malformed skill YAML")
        }

    /** kaml isn't a dependency of `:android:app`, so serialization lives here alongside [parse] rather than at the Room-backed store call site. */
    fun serialize(skill: Skill): String = Yaml.default.encodeToString(Skill.serializer(), skill)
}
