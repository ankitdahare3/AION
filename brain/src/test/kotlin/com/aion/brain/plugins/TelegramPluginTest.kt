package com.aion.brain.plugins

import com.aion.brain.ToolCall
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun clientReturning(
    status: HttpStatusCode,
    body: String,
    capture: MutableList<HttpRequestData> = mutableListOf(),
): HttpClient {
    val engine =
        MockEngine { request ->
            capture += request
            respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
    return HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
}

/** T-102 AC — Telegram Bot API plugin, verified against a mocked HTTP engine (live smoke against the real API needs the owner's real bot token, 🧍HC-3/5). */
class TelegramPluginTest {
    @Test
    fun `send_message posts chat_id and text to the sendMessage endpoint`() =
        runTest {
            val captured = mutableListOf<HttpRequestData>()
            val client = clientReturning(HttpStatusCode.OK, """{"ok":true,"result":{"message_id":42}}""", captured)
            val result =
                TelegramPlugin(
                    "TOKEN123",
                    client,
                ).execute(ToolCall("send_message", """{"chat_id":"123","text":"hi"}""", true))

            assertTrue(result.success)
            assertTrue(result.resultJson.contains("42"))
            assertEquals("/botTOKEN123/sendMessage", captured.single().url.encodedPath)
        }

    @Test
    fun `get_updates returns only updates that have a message`() =
        runTest {
            val body =
                """{"ok":true,"result":[""" +
                    """{"update_id":1,"message":{"text":"hello","chat":{"id":9}}},""" +
                    """{"update_id":2}]}"""
            val client = clientReturning(HttpStatusCode.OK, body)
            val result = TelegramPlugin("TOKEN123", client).execute(ToolCall("get_updates", "{}", false))

            assertTrue(result.success)
            assertTrue(result.resultJson.contains("hello"))
            assertTrue(result.resultJson.contains("\"update_id\":1"))
            assertFalse(result.resultJson.contains("\"update_id\":2"))
        }

    @Test
    fun `telegram ok=false is a clean failure carrying the description`() =
        runTest {
            val client = clientReturning(HttpStatusCode.OK, """{"ok":false,"description":"chat not found"}""")
            val result =
                TelegramPlugin(
                    "TOKEN123",
                    client,
                ).execute(ToolCall("send_message", """{"chat_id":"999","text":"hi"}""", true))

            assertFalse(result.success)
            assertTrue(result.error?.contains("chat not found") == true)
        }

    @Test
    fun `a missing required arg is rejected without a network call`() =
        runTest {
            val captured = mutableListOf<HttpRequestData>()
            val client = clientReturning(HttpStatusCode.OK, "{}", captured)
            val result =
                TelegramPlugin(
                    "TOKEN123",
                    client,
                ).execute(ToolCall("send_message", """{"chat_id":"1"}""", true))

            assertFalse(result.success)
            assertTrue(result.error?.contains("text") == true)
            assertTrue(captured.isEmpty())
        }

    @Test
    fun `an unknown tool name is rejected`() =
        runTest {
            val result =
                TelegramPlugin(
                    "TOKEN123",
                    clientReturning(HttpStatusCode.OK, "{}"),
                ).execute(ToolCall("delete_everything", "{}", false))

            assertFalse(result.success)
            assertTrue(result.error?.contains("delete_everything") == true)
        }
}
