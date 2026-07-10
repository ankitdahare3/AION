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

/** T-102 AC — Gmail plugin, real REST API shape, verified against a mocked HTTP engine (live smoke against the real API needs the owner's real OAuth token, 🧍HC-3/5). */
class GmailPluginTest {
    @Test
    fun `list_messages returns the ids from a successful response`() =
        runTest {
            val client = clientReturning(HttpStatusCode.OK, """{"messages":[{"id":"a"},{"id":"b"}]}""")
            val result = GmailPlugin("token", client).execute(ToolCall("list_messages", "{}", false))

            assertTrue(result.success)
            assertTrue(result.resultJson.contains("\"a\""))
            assertTrue(result.resultJson.contains("\"b\""))
        }

    @Test
    fun `read_message extracts subject and from headers plus the snippet`() =
        runTest {
            val body =
                """{"id":"m1","snippet":"hi there","payload":{"headers":[""" +
                    """{"name":"Subject","value":"Hello"},{"name":"From","value":"a@b.com"}]}}"""
            val client = clientReturning(HttpStatusCode.OK, body)
            val result = GmailPlugin("token", client).execute(ToolCall("read_message", """{"id":"m1"}""", false))

            assertTrue(result.success)
            assertTrue(result.resultJson.contains("Hello"))
            assertTrue(result.resultJson.contains("a@b.com"))
            assertTrue(result.resultJson.contains("hi there"))
        }

    @Test
    fun `send_email posts a base64url-encoded MIME message and does not require approval logic itself`() =
        runTest {
            val captured = mutableListOf<HttpRequestData>()
            val client = clientReturning(HttpStatusCode.OK, """{"id":"sent1"}""", captured)
            val args = """{"to":"a@b.com","subject":"Hi","body":"hello world"}"""
            val result = GmailPlugin("token", client).execute(ToolCall("send_email", args, true))

            assertTrue(result.success)
            assertEquals("/gmail/v1/users/me/messages/send", captured.single().url.encodedPath)
        }

    @Test
    fun `compose_draft hits the drafts endpoint, not send`() =
        runTest {
            val captured = mutableListOf<HttpRequestData>()
            val client = clientReturning(HttpStatusCode.OK, """{"id":"draft1"}""", captured)
            val args = """{"to":"a@b.com","subject":"Hi","body":"hello world"}"""
            val result = GmailPlugin("token", client).execute(ToolCall("compose_draft", args, false))

            assertTrue(result.success)
            assertEquals("/gmail/v1/users/me/drafts", captured.single().url.encodedPath)
        }

    @Test
    fun `a non-2xx response is reported as a clean failure, not a thrown exception`() =
        runTest {
            val client = clientReturning(HttpStatusCode.Unauthorized, """{"error":"invalid token"}""")
            val result = GmailPlugin("bad-token", client).execute(ToolCall("list_messages", "{}", false))

            assertFalse(result.success)
            assertTrue(result.error?.contains("401") == true)
        }

    @Test
    fun `a missing required arg is rejected without a network call`() =
        runTest {
            val captured = mutableListOf<HttpRequestData>()
            val client = clientReturning(HttpStatusCode.OK, "{}", captured)
            val result = GmailPlugin("token", client).execute(ToolCall("send_email", """{"to":"a@b.com"}""", true))

            assertFalse(result.success)
            assertTrue(result.error?.contains("subject") == true)
            assertTrue(captured.isEmpty())
        }

    @Test
    fun `an unknown tool name is rejected`() =
        runTest {
            val result = GmailPlugin("token", clientReturning(HttpStatusCode.OK, "{}")).execute(ToolCall("delete_everything", "{}", false))

            assertFalse(result.success)
            assertTrue(result.error?.contains("delete_everything") == true)
        }
}
