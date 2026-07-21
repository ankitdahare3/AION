package com.aion.host.security

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

            // voiceLine embeds the plan step's target text (RealApprovalGate), which ultimately
            // comes from LLM output — hand-rolled interpolation would break the hash-chained audit
            // log's JSON the moment a target contains a literal quote (JsonObject escapes it).
            auditLogger.record(
                actor = "user",
                action = "approval.decision",
                payloadJson =
                    JsonObject(
                        mapOf(
                            "voiceLine" to JsonPrimitive(voiceLine),
                            "approved" to JsonPrimitive(approved),
                        ),
                    ).toString(),
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
