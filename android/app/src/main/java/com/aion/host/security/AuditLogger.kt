package com.aion.host.security

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** DOC-017 §4 — every action, approval, cloud call, and memory write must go through this. */
@Singleton
class AuditLogger
    @Inject
    constructor(
        private val dao: AuditDao,
    ) {
        suspend fun record(
            actor: String,
            action: String,
            payloadJson: String,
            ts: Long = System.currentTimeMillis(),
        ): AuditLogEntry {
            val prevHash = dao.getLast()?.hash ?: AuditChain.GENESIS_HASH
            val hash = AuditChain.computeHash(prevHash, actor, action, payloadJson, ts)
            val entry =
                AuditLogEntry(
                    prevHash = prevHash,
                    hash = hash,
                    actor = actor,
                    action = action,
                    payloadJson = payloadJson,
                    ts = ts,
                )
            val seq = dao.insert(entry)
            return entry.copy(seq = seq)
        }

        suspend fun verifyChain(): Boolean = AuditChain.verify(dao.getAllOrdered())

        fun observeEntries(): Flow<List<AuditLogEntry>> = dao.observeAll()
    }
