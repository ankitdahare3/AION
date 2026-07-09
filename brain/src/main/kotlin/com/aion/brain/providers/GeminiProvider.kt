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
data class GeminiPart(
    val text: String,
)

@Serializable
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>,
)

@Serializable
data class GeminiGenerationConfig(
    @SerialName("maxOutputTokens") val maxOutputTokens: Int? = null,
)

@Serializable
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    @SerialName("system_instruction") val systemInstruction: GeminiContent? = null,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig? = null,
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent,
)

@Serializable
data class GeminiUsage(
    @SerialName("promptTokenCount") val promptTokenCount: Int = 0,
    @SerialName("candidatesTokenCount") val candidatesTokenCount: Int = 0,
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val usageMetadata: GeminiUsage? = null,
)

/** DOC-013 §3 — Gemini adapter (google kind); distinct request/response schema from OpenAI-compat. */
class GeminiProvider(
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
                httpClient.post("$endpoint/models/$model:generateContent") {
                    contentType(ContentType.Application.Json)
                    header("x-goog-api-key", apiKey)
                    setBody(
                        GeminiGenerateRequest(
                            systemInstruction = GeminiContent(parts = listOf(GeminiPart(req.system))),
                            contents =
                                req.messages.map {
                                    GeminiContent(
                                        role = if (it.role == "assistant") "model" else "user",
                                        parts = listOf(GeminiPart(it.content)),
                                    )
                                },
                            generationConfig = GeminiGenerationConfig(maxOutputTokens = req.maxTokens),
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
                response.body<GeminiResponse>()
            } catch (e: Exception) {
                throw ProviderFailure.BadOutput("unparseable response: ${e.message}")
            }

        val text =
            parsed.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?: throw ProviderFailure.BadOutput("no candidates in response")

        return BrainResult(
            text = text,
            provider = id,
            latencyMs = System.currentTimeMillis() - t0,
            costUsd = estimateCost(parsed.usageMetadata),
        )
    }

    private fun estimateCost(usage: GeminiUsage?): Double {
        if (usage == null) return 0.0
        return usage.promptTokenCount / 1_000_000.0 * costInPerMTok +
            usage.candidatesTokenCount / 1_000_000.0 * costOutPerMTok
    }
}
