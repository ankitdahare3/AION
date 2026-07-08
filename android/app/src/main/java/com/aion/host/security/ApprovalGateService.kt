package com.aion.host.security

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** A side-effect action awaiting explicit owner approval (SR-01). */
data class ApprovalRequest(
    val id: String,
    /** Short natural-language sentence — this exact string is what T-014 (Piper TTS) will speak. */
    val voiceLine: String,
    val detail: String,
)

/**
 * SR-01/02 (DOC-017 §2) — no side-effect action executes without explicit owner approval, voice
 * or tap. [requestApproval] suspends the calling coroutine (e.g. ExecutorAgent, wired at T-051)
 * until [resolve] is called from the approval sheet UI. Every decision is audited.
 */
@Singleton
class ApprovalGateService
    @Inject
    constructor(
        private val auditLogger: AuditLogger,
    ) {
        private val _pending = MutableStateFlow<ApprovalRequest?>(null)
        val pending: StateFlow<ApprovalRequest?> = _pending.asStateFlow()

        private val pendingDecisions = mutableMapOf<String, CompletableDeferred<Boolean>>()

        suspend fun requestApproval(
            voiceLine: String,
            detail: String,
        ): Boolean {
            val id = UUID.randomUUID().toString()
            val deferred = CompletableDeferred<Boolean>()
            pendingDecisions[id] = deferred
            _pending.value = ApprovalRequest(id, voiceLine, detail)

            val approved =
                try {
                    deferred.await()
                } finally {
                    pendingDecisions.remove(id)
                    if (_pending.value?.id == id) _pending.value = null
                }

            auditLogger.record(
                actor = "user",
                action = "approval.decision",
                payloadJson = """{"voiceLine":"$voiceLine","approved":$approved}""",
            )
            return approved
        }

        /** Called by the approval sheet UI when the owner taps Approve/Deny, or dismisses (= deny, SR-01). */
        fun resolve(
            id: String,
            approved: Boolean,
        ) {
            pendingDecisions[id]?.complete(approved)
        }
    }
