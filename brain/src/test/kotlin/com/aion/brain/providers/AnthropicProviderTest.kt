package com.aion.brain.providers

import com.aion.brain.BrainRequest
import com.aion.brain.ProviderCaps
import com.aion.brain.ProviderFailure
import com.aion.brain.TaskType
import com.aion.brain.Tier
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

private val req =
    BrainRequest(
        taskType = TaskType.CHAT,
        system = "you are AION",
        messages = listOf(com.aion.brain.Msg("user", "hello")),
    )

private fun clientReturning(
    status: HttpStatusCode,
    body: String,
): HttpClient {
    val engine =
        MockEngine {
            respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
    return HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
}

private fun provider(client: HttpClient) =
    AnthropicProvider(
        id = "anthropic",
        tier = Tier.PAID,
        caps = ProviderCaps(vision = true),
        endpoint = "https://api.anthropic.com/v1",
        model = "claude-haiku-4-5-20251001",
        apiKey = "test-key",
        costInPerMTok = 1.00,
        costOutPerMTok = 5.00,
        httpClient = client,
    )

class AnthropicProviderTest {
    @Test
    fun `parses a successful completion and estimates cost from usage`() =
        runTest {
            val body =
                """{"content":[{"type":"text","text":"hi there"}],""" +
                    """"usage":{"input_tokens":1000000,"output_tokens":1000000}}"""
            val result = provider(clientReturning(HttpStatusCode.OK, body)).complete(req)

            assertEquals("hi there", result.text)
            assertEquals("anthropic", result.provider)
            // 1M input tok @ $1.00/M + 1M output tok @ $5.00/M = $6.00
            assertEquals(6.00, result.costUsd, 1e-9)
        }

    @Test
    fun `401 maps to Auth failure (shared error mapping works for Anthropic too)`() =
        runTest {
            try {
                provider(
                    clientReturning(HttpStatusCode.Unauthorized, """{"error":{"message":"invalid x-api-key"}}"""),
                ).complete(req)
                fail("expected ProviderFailure.Auth")
            } catch (e: ProviderFailure.Auth) {
                assertTrue(e.message!!.contains("invalid x-api-key"))
            }
        }

    @Test
    fun `429 maps to RateLimit failure`() =
        runTest {
            try {
                provider(
                    clientReturning(HttpStatusCode.TooManyRequests, """{"error":{"message":"rate limited"}}"""),
                ).complete(req)
                fail("expected ProviderFailure.RateLimit")
            } catch (e: ProviderFailure.RateLimit) {
                assertTrue(e.message!!.contains("rate limited"))
            }
        }

    @Test
    fun `a non-text content block, like only tool_use, maps to BadOutput not a blank success`() =
        runTest {
            try {
                provider(
                    clientReturning(HttpStatusCode.OK, """{"content":[{"type":"tool_use"}]}"""),
                ).complete(req)
                fail("expected ProviderFailure.BadOutput")
            } catch (e: ProviderFailure.BadOutput) {
                assertTrue(e.message!!.contains("no text content block"))
            }
        }
}
