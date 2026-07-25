package com.aion.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val noopScoreStore =
    object : ScoreStore {
        override fun taskScore(
            id: String,
            t: TaskType,
        ) = 0.5

        override fun latencyNorm(id: String) = 0.5

        override fun notInCooldown(id: String) = true

        override fun recordSuccess(
            id: String,
            t: TaskType,
            latencyMs: Long,
            cost: Double,
        ) {}

        override fun recordFailure(
            id: String,
            t: TaskType,
            e: ProviderFailure,
        ) {}
    }

private val alwaysCanSpend =
    object : BudgetGuard {
        override fun canSpend(req: BrainRequest) = true

        override fun record(cost: Double) {}
    }

private fun scriptedChatProvider(reply: String) =
    object : Provider {
        override val id = "scripted-chat"
        override val tier = Tier.LOCAL
        override val caps = ProviderCaps()

        override suspend fun complete(req: BrainRequest): BrainResult =
            BrainResult(text = reply, provider = id, latencyMs = 1, costUsd = 0.0)
    }

private fun throwingProvider(message: String) =
    object : Provider {
        override val id = "throwing"
        override val tier = Tier.LOCAL
        override val caps = ProviderCaps()

        override suspend fun complete(req: BrainRequest): BrainResult = throw IllegalStateException(message)
    }

private fun routerFor(provider: Provider) =
    ProviderRouter(registry = listOf(provider), scores = noopScoreStore, budget = alwaysCanSpend)

class ChatAgentTest {
    @Test
    fun `a real reply ends the run with that exact text, no failures`() =
        runTest {
            val agent = ChatAgent(routerFor(scriptedChatProvider("Main theek hoon, aap batao?")))

            val result = agent.step(AgentState(goal = "kaise ho"))

            assertTrue(result.done)
            assertEquals("Main theek hoon, aap batao?", result.response)
            assertTrue(result.failures.isEmpty())
        }

    @Test
    fun `a blank reply is an honest UNKNOWN failure, not a fabricated success`() =
        runTest {
            val agent = ChatAgent(routerFor(scriptedChatProvider("   ")))

            val result = agent.step(AgentState(goal = "namaste"))

            assertTrue(result.done)
            assertTrue(result.failures.any { it.contains("blank") })
            assertEquals(
                ResponsePhrasing.forFailure(FailureCause.UNKNOWN, hinglish = false),
                result.response,
            )
        }

    @Test
    fun `a routing exception surfaces its own message and still ends with a real response`() =
        runTest {
            val agent = ChatAgent(routerFor(throwingProvider("all providers exhausted")))

            val result = agent.step(AgentState(goal = "hello"))

            assertTrue(result.done)
            assertTrue(result.failures.any { it.contains("all providers exhausted") })
            assertEquals(
                ResponsePhrasing.forFailure(FailureCause.UNKNOWN, hinglish = false),
                result.response,
            )
        }
}

private class RecordingAgent(
    private val label: String,
) : Agent {
    var called = false
        private set

    override suspend fun step(s: AgentState): AgentState {
        called = true
        return s.copy(done = true, response = label)
    }
}

class IntentRoutingAgentTest {
    @Test
    fun `a plain greeting is diverted to the chat agent, not the planner`() =
        runTest {
            val chat = RecordingAgent("chat handled it")
            val planner = RecordingAgent("planner handled it")
            val router = IntentRoutingAgent(chat, planner)

            val result = router.step(AgentState(goal = "namaste AION"))

            assertTrue(chat.called)
            assertFalse(planner.called)
            assertEquals("chat handled it", result.response)
        }

    @Test
    fun `a real device-action goal still goes to the planner`() =
        runTest {
            val chat = RecordingAgent("chat handled it")
            val planner = RecordingAgent("planner handled it")
            val router = IntentRoutingAgent(chat, planner)

            val result = router.step(AgentState(goal = "wifi on karo"))

            assertFalse(chat.called)
            assertTrue(planner.called)
            assertEquals("planner handled it", result.response)
        }

    @Test
    fun `a real llmClassifier result is used instead of the keyword classifier`() =
        runTest {
            val chat = RecordingAgent("chat handled it")
            val planner = RecordingAgent("planner handled it")
            // The keyword classifier alone would send "battery batao" to the planner (SIMPLE_ACTION-
            // shaped) — a real llmClassifier result overriding that to CHAT proves it's genuinely
            // consulted first, not just present-but-ignored.
            val router = IntentRoutingAgent(chat, planner, llmClassifier = LlmIntentClassifier { Intent.CHAT })

            val result = router.step(AgentState(goal = "battery batao"))

            assertTrue(chat.called)
            assertFalse(planner.called)
            assertEquals("chat handled it", result.response)
        }

    @Test
    fun `a null llmClassifier result falls back to the keyword classifier`() =
        runTest {
            val chat = RecordingAgent("chat handled it")
            val planner = RecordingAgent("planner handled it")
            val router = IntentRoutingAgent(chat, planner, llmClassifier = LlmIntentClassifier { null })

            val result = router.step(AgentState(goal = "namaste AION"))

            assertTrue(chat.called)
            assertFalse(planner.called)
            assertEquals("chat handled it", result.response)
        }

    @Test
    fun `a throwing llmClassifier falls back to the keyword classifier instead of crashing the run`() =
        runTest {
            val chat = RecordingAgent("chat handled it")
            val planner = RecordingAgent("planner handled it")
            val throwingClassifier = LlmIntentClassifier { throw IllegalStateException("model not loaded") }
            val router = IntentRoutingAgent(chat, planner, llmClassifier = throwingClassifier)

            val result = router.step(AgentState(goal = "namaste AION"))

            assertTrue(chat.called)
            assertFalse(planner.called)
            assertEquals("chat handled it", result.response)
        }
}
