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
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
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

@Serializable
private data class TelegramSendMessageRequest(
    val chat_id: String,
    val text: String,
)

@Serializable
private data class TelegramMessage(
    val message_id: Long = 0,
)

@Serializable
private data class TelegramSendResponse(
    val ok: Boolean,
    val result: TelegramMessage? = null,
    val description: String? = null,
)

@Serializable
private data class TelegramChat(
    val id: Long,
)

@Serializable
private data class TelegramIncomingMessage(
    val text: String? = null,
    val chat: TelegramChat,
)

@Serializable
private data class TelegramUpdate(
    val update_id: Long,
    val message: TelegramIncomingMessage? = null,
)

@Serializable
private data class TelegramGetUpdatesResponse(
    val ok: Boolean,
    val result: List<TelegramUpdate> = emptyList(),
    val description: String? = null,
)

/**
 * DOC-005 §5 v1 built-in — Telegram, via the real Telegram Bot API (`api.telegram.org`). Same
 * "no ActionExecutor, structured argsJson straight to a real HTTP API" shape as [GmailPlugin]. No
 * OAuth here — a bot token is a single static secret, same shape `SecretVault` already handles for
 * LLM provider keys.
 */
class TelegramPlugin(
    private val botToken: String,
    private val httpClient: HttpClient = defaultProviderHttpClient(),
) : AionPlugin() {
    override val manifest =
        PluginManifest(
            id = ID,
            name = "Telegram",
            version = "1.0.0",
            apiLevel = DNAValidator.SUPPORTED_API_LEVEL,
            permissions = listOf("INTERNET"),
            tools =
                listOf(
                    ToolSchema(
                        "send_message",
                        sideEffect = true,
                        inputSchema = """{"chat_id":"string","text":"string"}""",
                        description = "Sends a Telegram message immediately",
                    ),
                    ToolSchema(
                        "get_updates",
                        sideEffect = false,
                        inputSchema = """{"offset":"number?"}""",
                        description = "Reads incoming Telegram messages since an optional update offset",
                    ),
                ),
            dna = DnaConfig(benchmark = "bench/telegram.yaml"),
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
                "send_message" -> sendMessage(args)
                "get_updates" -> getUpdates(args["offset"]?.jsonPrimitive?.content?.toLongOrNull())
                else -> ToolResult(false, "{}", "unknown tool: ${call.name}")
            }
        } catch (e: IllegalArgumentException) {
            ToolResult(false, "{}", e.message)
        } catch (e: Exception) {
            ToolResult(false, "{}", "telegram request failed: ${e.message}")
        }
    }

    private suspend fun sendMessage(args: JsonObject): ToolResult {
        val chatId = args["chat_id"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("missing required arg: chat_id")
        val text = args["text"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("missing required arg: text")
        val response =
            httpClient.post("$BASE_URL$botToken/sendMessage") {
                contentType(ContentType.Application.Json)
                setBody(TelegramSendMessageRequest(chatId, text))
            }
        val parsed = response.body<TelegramSendResponse>()
        if (!response.status.isSuccess() || !parsed.ok) return response.toFailure(parsed.description)
        return ToolResult(true, observationJson("message sent, id=${parsed.result?.message_id}"))
    }

    private suspend fun getUpdates(offset: Long?): ToolResult {
        val response =
            httpClient.get("$BASE_URL$botToken/getUpdates") {
                if (offset != null) parameter("offset", offset)
            }
        val parsed = response.body<TelegramGetUpdatesResponse>()
        if (!response.status.isSuccess() || !parsed.ok) return response.toFailure(parsed.description)
        val messages =
            parsed.result.mapNotNull { update ->
                val msg = update.message ?: return@mapNotNull null
                JsonObject(
                    mapOf(
                        "update_id" to JsonPrimitive(update.update_id),
                        "chat_id" to JsonPrimitive(msg.chat.id),
                        "text" to JsonPrimitive(msg.text.orEmpty()),
                    ),
                )
            }
        return ToolResult(true, JsonObject(mapOf("messages" to JsonArray(messages))).toString())
    }

    private fun HttpResponse.toFailure(description: String?): ToolResult =
        ToolResult(false, "{}", description ?: "telegram api error (${status.value})")

    companion object {
        const val ID = "com.aion.plugin.telegram"
        private const val BASE_URL = "https://api.telegram.org/bot"
    }
}
