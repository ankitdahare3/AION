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
    GeminiProvider(
        id = "gemini",
        tier = Tier.PAID,
        caps = ProviderCaps(vision = true),
        endpoint = "https://generativelanguage.googleapis.com/v1beta",
        model = "gemini-2.5-flash",
        apiKey = "test-key",
        costInPerMTok = 0.075,
        costOutPerMTok = 0.30,
        httpClient = client,
    )

class GeminiProviderTest {
    @Test
    fun `parses a successful completion and estimates cost from usageMetadata`() =
        runTest {
            val body =
                """{"candidates":[{"content":{"parts":[{"text":"hi there"}]}}],""" +
                    """"usageMetadata":{"promptTokenCount":1000000,"candidatesTokenCount":1000000}}"""
            val result = provider(clientReturning(HttpStatusCode.OK, body)).complete(req)

            assertEquals("hi there", result.text)
            assertEquals("gemini", result.provider)
            // 1M prompt tok @ $0.075/M + 1M output tok @ $0.30/M = $0.375
            assertEquals(0.375, result.costUsd, 1e-9)
        }

    @Test
    fun `403 maps to Auth failure (shared error mapping works for Gemini too)`() =
        runTest {
            try {
                provider(
                    clientReturning(HttpStatusCode.Forbidden, """{"error":{"message":"API key not valid"}}"""),
                ).complete(req)
                fail("expected ProviderFailure.Auth")
            } catch (e: ProviderFailure.Auth) {
                assertTrue(e.message!!.contains("API key not valid"))
            }
        }

    @Test
    fun `no candidates maps to BadOutput`() =
        runTest {
            try {
                provider(clientReturning(HttpStatusCode.OK, """{"candidates":[]}""")).complete(req)
                fail("expected ProviderFailure.BadOutput")
            } catch (e: ProviderFailure.BadOutput) {
                assertTrue(e.message!!.contains("no candidates"))
            }
        }
}
