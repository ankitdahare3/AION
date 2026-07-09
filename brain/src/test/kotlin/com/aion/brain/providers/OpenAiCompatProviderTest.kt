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

private val req = BrainRequest(taskType = TaskType.CHAT, system = "you are AION", messages = emptyList())

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
    OpenAiCompatProvider(
        id = "groq",
        tier = Tier.FREE,
        caps = ProviderCaps(),
        endpoint = "https://api.groq.com/openai/v1",
        model = "llama-3.3-70b-versatile",
        apiKey = "test-key",
        costInPerMTok = 0.05,
        costOutPerMTok = 0.08,
        httpClient = client,
    )

class OpenAiCompatProviderTest {
    @Test
    fun `parses a successful completion and estimates cost from usage`() =
        runTest {
            val body =
                """{"choices":[{"message":{"role":"assistant","content":"hi there"}}],""" +
                    """"usage":{"prompt_tokens":1000000,"completion_tokens":1000000}}"""
            val result = provider(clientReturning(HttpStatusCode.OK, body)).complete(req)

            assertEquals("hi there", result.text)
            assertEquals("groq", result.provider)
            // 1M prompt tok @ $0.05/M + 1M completion tok @ $0.08/M = $0.13
            assertEquals(0.13, result.costUsd, 1e-9)
        }

    @Test
    fun `401 maps to Auth failure`() =
        runTest {
            try {
                provider(
                    clientReturning(HttpStatusCode.Unauthorized, """{"error":{"message":"invalid api key"}}"""),
                ).complete(req)
                fail("expected ProviderFailure.Auth")
            } catch (e: ProviderFailure.Auth) {
                assertTrue(e.message!!.contains("invalid api key"))
            }
        }

    @Test
    fun `429 with quota in the body maps to Quota, not RateLimit`() =
        runTest {
            try {
                provider(
                    clientReturning(
                        HttpStatusCode.TooManyRequests,
                        """{"error":{"message":"monthly quota exceeded"}}""",
                    ),
                ).complete(req)
                fail("expected ProviderFailure.Quota")
            } catch (e: ProviderFailure.Quota) {
                assertTrue(e.message!!.contains("quota"))
            }
        }

    @Test
    fun `429 without quota wording maps to RateLimit`() =
        runTest {
            try {
                provider(
                    clientReturning(HttpStatusCode.TooManyRequests, """{"error":{"message":"slow down"}}"""),
                ).complete(req)
                fail("expected ProviderFailure.RateLimit")
            } catch (e: ProviderFailure.RateLimit) {
                assertTrue(e.message!!.contains("slow down"))
            }
        }

    @Test
    fun `500 maps to Server failure`() =
        runTest {
            try {
                provider(
                    clientReturning(HttpStatusCode.InternalServerError, """{"error":{"message":"oops"}}"""),
                ).complete(req)
                fail("expected ProviderFailure.Server")
            } catch (e: ProviderFailure.Server) {
                assertTrue(e.message!!.contains("oops"))
            }
        }

    @Test
    fun `malformed JSON body maps to BadOutput`() =
        runTest {
            try {
                provider(clientReturning(HttpStatusCode.OK, "not json at all")).complete(req)
                fail("expected ProviderFailure.BadOutput")
            } catch (e: ProviderFailure.BadOutput) {
                assertTrue(e.message!!.contains("unparseable"))
            }
        }

    @Test
    fun `empty choices array maps to BadOutput`() =
        runTest {
            try {
                provider(clientReturning(HttpStatusCode.OK, """{"choices":[]}""")).complete(req)
                fail("expected ProviderFailure.BadOutput")
            } catch (e: ProviderFailure.BadOutput) {
                assertTrue(e.message!!.contains("no choices"))
            }
        }
}
