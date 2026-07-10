package com.aion.brain

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

sealed class ManifestLoadResult {
    data class Loaded(
        val manifest: PluginManifest,
    ) : ManifestLoadResult()

    data class ParseError(
        val reason: String,
    ) : ManifestLoadResult()
}

/** DOC-005 §2 — parses `aion-plugin.json`. Parsing and DNA validation are separate steps on purpose: a malformed file is a different failure than a well-formed-but-untrustworthy one. */
object PluginManifestLoader {
    fun load(json: String): ManifestLoadResult =
        try {
            ManifestLoadResult.Loaded(Json.decodeFromString<PluginManifest>(json))
        } catch (e: SerializationException) {
            ManifestLoadResult.ParseError(e.message ?: "malformed plugin manifest JSON")
        }
}
