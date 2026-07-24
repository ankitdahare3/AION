package com.aion.brain.providers

import com.aion.brain.BrainRequest
import com.aion.brain.BrainResult
import com.aion.brain.Provider
import com.aion.brain.ProviderCaps
import com.aion.brain.ProviderFailure
import com.aion.brain.Tier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnthropicMessage(
    val role: String,
    val content: String,
)

@Serializable
data class AnthropicRequest(
    val model: String,
    val system: String,
    val messages: List<AnthropicMessage>,
    @SerialName("max_tokens") val maxTokens: Int,
)

@Serializable
data class AnthropicContentBlock(
    val type: String,
    val text: String? = null,
)

@Serializable
data class AnthropicUsage(
    @SerialName("input_tokens") val inputTokens: Int = 0,
    @SerialName("output_tokens") val outputTokens: Int = 0,
)

@Serializable
data class AnthropicResponse(
    val content: List<AnthropicContentBlock> = emptyList(),
    val usage: AnthropicUsage? = null,
)

/**
 * Backend upgrade item 3/4 — the real gap this session's own review found: the `Provider`
 * interface already supported a Claude adapter, providers.yaml used to list an "anthropic" kind,
 * but no class ever implemented it (T-167 removed the dead yaml entry rather than leave a config
 * line with nothing behind it). Same adapter shape as [GeminiProvider] — own request/response
 * DTOs, real HTTP call via the shared Ktor client, [mapHttpError] for the shared failure taxonomy.
 *
 * Anthropic's real Messages API takes `system` as a top-level field (not folded into the messages
 * array like OpenAI-compat does) — [BrainRequest.system] maps onto it directly, no translation
 * needed. `anthropic-version` is a required header, not an SDK detail this adapter can skip.
 */
class AnthropicProvider(
    override val id: String,
    override val tier: Tier,
    override val caps: ProviderCaps,
    private val endpoint: String,
    private val model: String,
    private val apiKey: String,
    private val costInPerMTok: Double = 0.0,
    private val costOutPerMTok: Double = 0.0,
    private val httpClient: HttpClient = defaultProviderHttpClient(),
) : Provider {
    override suspend fun complete(req: BrainRequest): BrainResult {
        val t0 = System.currentTimeMillis()
        val response =
            try {
                httpClient.post("$endpoint/messages") {
                    contentType(ContentType.Application.Json)
                    header("x-api-key", apiKey)
                    header("anthropic-version", ANTHROPIC_VERSION)
                    setBody(
                        AnthropicRequest(
                            model = model,
                            system = req.system,
                            messages = req.messages.map { AnthropicMessage(role = it.role, content = it.content) },
                            maxTokens = req.maxTokens,
                        ),
                    )
                }
            } catch (e: HttpRequestTimeoutException) {
                throw ProviderFailure.Timeout(e.message ?: "timeout")
            } catch (e: ProviderFailure) {
                throw e
            } catch (e: Exception) {
                throw ProviderFailure.Server(e.message ?: "network error")
            }

        if (!response.status.isSuccess()) {
            throw mapHttpError(response.status.value, response.bodyAsText())
        }

        val parsed =
            try {
                response.body<AnthropicResponse>()
            } catch (e: Exception) {
                throw ProviderFailure.BadOutput("unparseable response: ${e.message}")
            }

        val text =
            parsed.content
                .firstOrNull { it.type == "text" }
                ?.text
                ?: throw ProviderFailure.BadOutput("no text content block in response")

        return BrainResult(
            text = text,
            provider = id,
            latencyMs = System.currentTimeMillis() - t0,
            costUsd = estimateCost(parsed.usage),
        )
    }

    private fun estimateCost(usage: AnthropicUsage?): Double {
        if (usage == null) return 0.0
        return usage.inputTokens / 1_000_000.0 * costInPerMTok + usage.outputTokens / 1_000_000.0 * costOutPerMTok
    }

    private companion object {
        const val ANTHROPIC_VERSION = "2023-06-01"
    }
}
