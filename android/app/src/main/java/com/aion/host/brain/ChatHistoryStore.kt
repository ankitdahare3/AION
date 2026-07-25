package com.aion.host.brain

import com.aion.host.memory.ConversationDao
import com.aion.host.memory.ConversationEntity
import com.aion.host.memory.TurnDao
import com.aion.host.memory.TurnEntity
import javax.inject.Inject
import javax.inject.Singleton

private const val SUMMARY_MAX_CHARS = 60

/**
 * T-060's `conversations`/`turns` tables (DOC-019 §1) had a schema and DAOs but no caller — every
 * chat exchange was ephemeral, discarded the moment the next goal was submitted. This is the first
 * real writer/reader, used by both [ChatScreen] (the live transcript) and [ChatHistoryScreen] (the
 * conversation list + "New Chat").
 */
@Singleton
class ChatHistoryStore
    @Inject
    constructor(
        private val conversationDao: ConversationDao,
        private val turnDao: TurnDao,
    ) {
        suspend fun startConversation(
            firstMessage: String,
            nowMs: Long,
        ): Long =
            conversationDao.insert(
                ConversationEntity(startedAt = nowMs, summary = firstMessage.take(SUMMARY_MAX_CHARS)),
            )

        suspend fun appendTurn(
            convId: Long,
            role: String,
            text: String,
            lang: String,
            nowMs: Long,
        ) = turnDao.insert(TurnEntity(convId = convId, role = role, text = text, lang = lang, ts = nowMs))

        suspend fun turnsFor(convId: Long): List<TurnEntity> = turnDao.getForConversation(convId)

        suspend fun conversations(): List<ConversationEntity> = conversationDao.getAll()
    }
