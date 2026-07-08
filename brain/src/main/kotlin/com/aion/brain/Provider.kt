package com.aion.brain

import kotlinx.serialization.Serializable

@Serializable data class ProviderCaps(
    val vision: Boolean = false, val tools: Boolean = true,
    val contextTokens: Int = 8192, val stream: Boolean = true,
)
@Serializable data class BrainRequest(
    val taskType: TaskType, val system: String, val messages: List<Msg>,
    val toolSchemas: List<String> = emptyList(), val maxTokens: Int = 1024,
    val needsVision: Boolean = false, val jsonSchema: String? = null,
)
@Serializable data class Msg(val role: String, val content: String)
@Serializable data class BrainResult(
    val text: String, val toolCalls: List<ToolCall> = emptyList(),
    val provider: String, val latencyMs: Long, val costUsd: Double,
)
@Serializable data class ToolCall(val name: String, val argsJson: String, val sideEffect: Boolean)
enum class TaskType { CHAT, INTENT, PLAN, CODE, VISION, EMBED, FAST }
enum class Tier { LOCAL, FREE, PAID }

sealed class ProviderFailure(msg: String) : Exception(msg) {
    class Auth(m: String) : ProviderFailure(m)
    class Quota(m: String) : ProviderFailure(m)
    class RateLimit(m: String) : ProviderFailure(m)
    class Timeout(m: String) : ProviderFailure(m)
    class Server(m: String) : ProviderFailure(m)
    class BadOutput(m: String) : ProviderFailure(m)
}

interface Provider {
    val id: String
    val tier: Tier
    val caps: ProviderCaps
    suspend fun complete(req: BrainRequest): BrainResult
}
