package com.aion.brain.plugins

import com.aion.brain.AionPlugin
import com.aion.brain.DNAValidator
import com.aion.brain.DnaConfig
import com.aion.brain.PluginManifest
import com.aion.brain.ToolCall
import com.aion.brain.ToolResult
import com.aion.brain.ToolSchema
import com.aion.brain.providers.defaultProviderHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

@Serializable
private data class GmailMessageRef(
    val id: String,
)

@Serializable
private data class GmailListResponse(
    val messages: List<GmailMessageRef> = emptyList(),
)

@Serializable
private data class GmailHeader(
    val name: String,
    val value: String,
)

@Serializable
private data class GmailPayload(
    val headers: List<GmailHeader> = emptyList(),
)

@Serializable
private data class GmailMessageDetail(
    val id: String,
    val snippet: String = "",
    val payload: GmailPayload? = null,
)

@Serializable
private data class GmailRawMessage(
    val raw: String,
)

@Serializable
private data class GmailDraftRequest(
    val message: GmailRawMessage,
)

/**
 * DOC-005 §5 v1 built-in — Gmail, via the real Gmail REST API (no ActionExecutor/PlanStep
 * involved; unlike the a11y-driven built-ins, this plugin's `execute(call)` reads structured
 * `argsJson` directly and calls Google's API over HTTPS, same pattern as `:brain/providers`'
 * `GeminiProvider`/`OpenAiCompatProvider`).
 *
 * [accessToken] is a bare OAuth2 access token, not a client id/secret pair — this project doesn't
 * build a full Google OAuth consent/refresh flow (a separate, much larger feature); v1 expects the
 * owner to paste a valid access token into `SecretVault` themselves (`SecretsScreen`, T-024, already
 * generalizes over every `ProviderKey` for free). Access tokens expire in ~1h with no refresh here —
 * an honest, named gap for BACKLOG.md, not hidden behind a comment nobody reads.
 */
class GmailPlugin(
    private val accessToken: String,
    private val httpClient: HttpClient = defaultProviderHttpClient(),
) : AionPlugin() {
    override val manifest =
        PluginManifest(
            id = ID,
            name = "Gmail",
            version = "1.0.0",
            apiLevel = DNAValidator.SUPPORTED_API_LEVEL,
            permissions = listOf("INTERNET"),
            tools =
                listOf(
                    ToolSchema(
                        "list_messages",
                        sideEffect = false,
                        inputSchema = """{"query":"string?","maxResults":"number?"}""",
                        description = "Lists recent Gmail message ids matching an optional search query",
                    ),
                    ToolSchema(
                        "read_message",
                        sideEffect = false,
                        inputSchema = """{"id":"string"}""",
                        description = "Reads a Gmail message's subject, sender, and snippet by id",
                    ),
                    ToolSchema(
                        "compose_draft",
                        sideEffect = false,
                        inputSchema = """{"to":"string","subject":"string","body":"string"}""",
                        description = "Creates a Gmail draft — never sent, reversible",
                    ),
                    ToolSchema(
                        "send_email",
                        sideEffect = true,
                        inputSchema = """{"to":"string","subject":"string","body":"string"}""",
                        description = "Sends a Gmail email immediately",
                    ),
                ),
            dna = DnaConfig(benchmark = "bench/gmail.yaml"),
        )

    override suspend fun execute(call: ToolCall): ToolResult {
        val args =
            try {
                Json.parseToJsonElement(call.argsJson).jsonObject
            } catch (e: Exception) {
                return ToolResult(false, "{}", "malformed argsJson for ${call.name}: ${e.message}")
            }

        return try {
            when (call.name) {
                "list_messages" -> listMessages(args["query"]?.jsonPrimitive?.content)
                "read_message" -> readMessage(args.require("id"))
                "compose_draft" -> composeOrSend(args, draft = true)
                "send_email" -> composeOrSend(args, draft = false)
                else -> ToolResult(false, "{}", "unknown tool: ${call.name}")
            }
        } catch (e: IllegalArgumentException) {
            ToolResult(false, "{}", e.message)
        } catch (e: Exception) {
            ToolResult(false, "{}", "gmail request failed: ${e.message}")
        }
    }

    private suspend fun listMessages(query: String?): ToolResult {
        val response =
            httpClient.get("$BASE_URL/messages") {
                bearer()
                if (query != null) parameter("q", query)
            }
        if (!response.status.isSuccess()) return response.toFailure()
        val ids = response.body<GmailListResponse>().messages.map { it.id }
        return ToolResult(true, JsonObject(mapOf("ids" to JsonArray(ids.map { JsonPrimitive(it) }))).toString())
    }

    private suspend fun readMessage(id: String): ToolResult {
        val response = httpClient.get("$BASE_URL/messages/$id") { bearer() }
        if (!response.status.isSuccess()) return response.toFailure()
        val detail = response.body<GmailMessageDetail>()
        val subject = detail.payload?.headers?.find { it.name.equals("Subject", ignoreCase = true) }?.value.orEmpty()
        val from = detail.payload?.headers?.find { it.name.equals("From", ignoreCase = true) }?.value.orEmpty()
        return ToolResult(
            true,
            JsonObject(
                mapOf(
                    "subject" to JsonPrimitive(subject),
                    "from" to JsonPrimitive(from),
                    "snippet" to JsonPrimitive(detail.snippet),
                ),
            ).toString(),
        )
    }

    private suspend fun composeOrSend(
        args: JsonObject,
        draft: Boolean,
    ): ToolResult {
        val to = args.require("to")
        val subject = args.require("subject")
        val body = args.require("body")
        val mime = "To: $to\r\nSubject: $subject\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n$body"
        val raw = Base64.getUrlEncoder().withoutPadding().encodeToString(mime.toByteArray(Charsets.UTF_8))
        val response =
            if (draft) {
                httpClient.post("$BASE_URL/drafts") {
                    bearer()
                    contentType(ContentType.Application.Json)
                    setBody(GmailDraftRequest(GmailRawMessage(raw)))
                }
            } else {
                httpClient.post("$BASE_URL/messages/send") {
                    bearer()
                    contentType(ContentType.Application.Json)
                    setBody(GmailRawMessage(raw))
                }
            }
        if (!response.status.isSuccess()) return response.toFailure()
        return ToolResult(true, observationJson(if (draft) "draft created" else "email sent to $to"))
    }

    private fun HttpRequestBuilder.bearer() = header("Authorization", "Bearer $accessToken")

    private suspend fun HttpResponse.toFailure(): ToolResult =
        ToolResult(false, "{}", "gmail api error (${status.value}): ${bodyAsText()}")

    private fun JsonObject.require(key: String): String =
        this[key]?.jsonPrimitive?.content ?: throw IllegalArgumentException("missing required arg: $key")

    companion object {
        const val ID = "com.aion.plugin.gmail"
        private const val BASE_URL = "https://gmail.googleapis.com/gmail/v1/users/me"
    }
}
