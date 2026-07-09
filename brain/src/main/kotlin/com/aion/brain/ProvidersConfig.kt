package com.aion.brain

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** DOC-013 §1 — providers.yaml schema: config-driven registry, zero-code swap (NFR-10). */
@Serializable
data class ProviderConfig(
    val id: String,
    val kind: String,
    val endpoint: String? = null,
    val models: List<String> = emptyList(),
    val caps: ProviderCapsConfig = ProviderCapsConfig(),
    val cost: ProviderCostConfig = ProviderCostConfig(),
    val tier: String,
    val privacy: String,
)

@Serializable
data class ProviderCapsConfig(
    val vision: Boolean = false,
    val tools: Boolean = true,
    val context: Int = 8192,
    val stream: Boolean = true,
)

@Serializable
data class ProviderCostConfig(
    @SerialName("in") val costInPerMTok: Double = 0.0,
    @SerialName("out") val costOutPerMTok: Double = 0.0,
)

@Serializable
data class ProvidersRegistry(
    val providers: List<ProviderConfig>,
)

/** Parses providers.yaml (DOC-013 §1). Throws on malformed YAML — callers decide fallback behavior. */
object ProvidersConfigLoader {
    fun load(yamlText: String): ProvidersRegistry =
        Yaml.default.decodeFromString(ProvidersRegistry.serializer(), yamlText)
}
