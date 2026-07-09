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
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiChatMessage>,
    @SerialName("max_tokens") val maxTokens: Int? = null,
)

@Serializable
data class OpenAiChoice(
    val message: OpenAiChatMessage,
)

@Serializable
data class OpenAiUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
)

@Serializable
data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList(),
    val usage: OpenAiUsage? = null,
)

/**
 * DOC-013 §3 — OpenAI-compatible chat-completions adapter. Covers Groq, OpenRouter, DeepSeek,
 * Ollama, vLLM, LM Studio — any server implementing the `/chat/completions` contract.
 */
class OpenAiCompatProvider(
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
                httpClient.post("$endpoint/chat/completions") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                    setBody(
                        OpenAiChatRequest(
                            model = model,
                            messages =
                                listOf(OpenAiChatMessage("system", req.system)) +
                                    req.messages.map { OpenAiChatMessage(it.role, it.content) },
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
                response.body<OpenAiChatResponse>()
            } catch (e: Exception) {
                throw ProviderFailure.BadOutput("unparseable response: ${e.message}")
            }

        val text =
            parsed.choices
                .firstOrNull()
                ?.message
                ?.content
                ?: throw ProviderFailure.BadOutput("no choices in response")

        return BrainResult(
            text = text,
            provider = id,
            latencyMs = System.currentTimeMillis() - t0,
            costUsd = estimateCost(parsed.usage),
        )
    }

    private fun estimateCost(usage: OpenAiUsage?): Double {
        if (usage == null) return 0.0
        return usage.promptTokens / 1_000_000.0 * costInPerMTok +
            usage.completionTokens / 1_000_000.0 * costOutPerMTok
    }
}
