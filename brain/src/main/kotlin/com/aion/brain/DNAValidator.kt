package com.aion.brain

sealed class ValidationResult {
    object Valid : ValidationResult()

    data class Invalid(
        val reasons: List<String>,
    ) : ValidationResult()
}

/**
 * DOC-005 §4 — "discover → DNAValidator (all 4 gates pass?)". The doc doesn't enumerate the 4
 * gates explicitly; this is the concrete, testable interpretation: (1) the manifest is
 * well-formed (non-blank id, at least one tool, each tool has a name+description), (2) `apiLevel`
 * matches what this SDK supports, (3) every declared permission is a known one — not an arbitrary
 * string a plugin author invents, (4) claiming `dna.learn`/`dna.reflect` requires a real
 * `dna.benchmark` path, since there'd otherwise be nothing to actually learn/reflect against.
 */
object DNAValidator {
    const val SUPPORTED_API_LEVEL = 1

    // DOC-005 §5's 12 v1 built-ins each need roughly one of these; kept as a closed set on purpose —
    // an unrecognized permission string is exactly the kind of thing that should fail validation,
    // not be silently accepted.
    val KNOWN_PERMISSIONS =
        setOf(
            "INTERNET",
            "READ_MAIL_SCOPE",
            "SEND_MAIL_SCOPE",
            "CONTACTS",
            "SMS",
            "PHONE",
            "CALENDAR",
            "FILES",
            "CAMERA",
            "GALLERY",
            "BROWSER",
            "CLOCK",
        )

    fun validate(manifest: PluginManifest): ValidationResult {
        val reasons = mutableListOf<String>()

        if (manifest.id.isBlank()) reasons += "id must not be blank"
        if (manifest.tools.isEmpty()) reasons += "must declare at least one tool"
        manifest.tools.forEach { tool ->
            if (tool.name.isBlank()) reasons += "a tool has a blank name"
            if (tool.description.isBlank()) reasons += "tool '${tool.name}' is missing a description"
        }

        if (manifest.apiLevel != SUPPORTED_API_LEVEL) {
            reasons += "unsupported apiLevel ${manifest.apiLevel} (this SDK supports $SUPPORTED_API_LEVEL)"
        }

        val unknownPermissions = manifest.permissions.filterNot { it in KNOWN_PERMISSIONS }
        if (unknownPermissions.isNotEmpty()) {
            reasons += "unknown permission(s): ${unknownPermissions.joinToString(", ")}"
        }

        if ((manifest.dna.learn || manifest.dna.reflect) && manifest.dna.benchmark.isNullOrBlank()) {
            reasons += "dna.benchmark is required when dna.learn or dna.reflect is true"
        }

        return if (reasons.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(reasons)
    }
}
