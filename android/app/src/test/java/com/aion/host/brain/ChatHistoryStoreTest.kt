package com.aion.host.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatHistoryStoreTest {
    @Test
    fun `starting a conversation stores a summary truncated from the first message`() =
        runTest {
            val store = ChatHistoryStore(FakeConversationDao(), FakeTurnDao())

            val id = store.startConversation("a".repeat(100), nowMs = 1000)

            val saved = store.conversations().single()
            assertEquals(id, saved.id)
            assertEquals(60, saved.summary?.length)
            assertEquals(1000, saved.startedAt)
        }

    @Test
    fun `appended turns round-trip in timestamp order for their own conversation`() =
        runTest {
            val store = ChatHistoryStore(FakeConversationDao(), FakeTurnDao())
            val convId = store.startConversation("hi", nowMs = 0)
            val otherConvId = store.startConversation("other chat", nowMs = 1)

            store.appendTurn(convId, role = "user", text = "hi", lang = "en", nowMs = 10)
            store.appendTurn(otherConvId, role = "user", text = "unrelated", lang = "en", nowMs = 20)
            store.appendTurn(convId, role = "assistant", text = "hello!", lang = "en", nowMs = 30)

            val turns = store.turnsFor(convId)

            assertEquals(listOf("hi", "hello!"), turns.map { it.text })
        }

    @Test
    fun `conversations list is newest-first`() =
        runTest {
            val store = ChatHistoryStore(FakeConversationDao(), FakeTurnDao())
            store.startConversation("first", nowMs = 100)
            store.startConversation("second", nowMs = 200)

            val summaries = store.conversations().map { it.summary }

            assertEquals(listOf("second", "first"), summaries)
        }

    @Test
    fun `a conversation with no turns yet has an empty turn list, not an error`() =
        runTest {
            val store = ChatHistoryStore(FakeConversationDao(), FakeTurnDao())
            val convId = store.startConversation("hi", nowMs = 0)

            assertTrue(store.turnsFor(convId).isEmpty())
        }
}
