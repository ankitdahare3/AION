package com.aion.brain

import kotlinx.serialization.Serializable

/** DOC-005 §2 — one entry in a manifest's `tools` array. */
@Serializable
data class ToolSchema(
    val name: String,
    val sideEffect: Boolean,
    val inputSchema: String,
    val description: String,
)

/** DOC-005 §2 — a plugin's self-declared capabilities. `benchmark` is a path to a bench yaml file. */
@Serializable
data class DnaConfig(
    val learn: Boolean = false,
    val reflect: Boolean = false,
    val benchmark: String? = null,
    val update: Boolean = false,
)

/** DOC-005 §2 — `aion-plugin.json`. */
@Serializable
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val apiLevel: Int,
    val permissions: List<String> = emptyList(),
    val tools: List<ToolSchema>,
    val dna: DnaConfig,
)

/** Result of a routed [ToolCall] (DOC-005 §3's `execute` return type — not specified beyond "ToolResult"). */
data class ToolResult(
    val success: Boolean,
    val resultJson: String,
    val error: String? = null,
)
