package com.aion.host.security

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** DOC-017 §4 — every action, approval, cloud call, and memory write must go through this. */
@Singleton
class AuditLogger
    @Inject
    constructor(
        private val dao: AuditDao,
    ) {
        // Audit-review finding, 2026-07-13: getLast->computeHash->insert must be atomic across
        // concurrent callers (ActionDispatcher on IO, ApprovalGateService/KillSwitch on their own
        // scopes) — without this lock, two concurrent record() calls read the same prevHash and
        // both insert, breaking the hash chain (AuditChain.verify would report tampering that
        // never happened).
        private val mutex = Mutex()

        suspend fun record(
            actor: String,
            action: String,
            payloadJson: String,
            ts: Long = System.currentTimeMillis(),
        ): AuditLogEntry =
            mutex.withLock {
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
                entry.copy(seq = seq)
            }

        suspend fun verifyChain(): Boolean = AuditChain.verify(dao.getAllOrdered())

        fun observeEntries(): Flow<List<AuditLogEntry>> = dao.observeAll()
    }
