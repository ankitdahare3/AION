package com.aion.host.security

import com.aion.host.SafetyRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SR-03 (DOC-017 §2) — "aion stop" or the overlay button must halt in-progress automation <1s.
 * [halted] is the global signal every dispatcher/agent loop (T-041+) must poll before continuing.
 */
@Singleton
class KillSwitch
    @Inject
    constructor(
        private val auditLogger: AuditLogger,
    ) {
        private val _halted = MutableStateFlow(false)
        val halted: StateFlow<Boolean> = _halted.asStateFlow()

        suspend fun trigger(source: String) {
            _halted.value = true
            auditLogger.record("user", "killswitch.trigger", """{"source":"$source"}""")
        }

        suspend fun reset() {
            _halted.value = false
            auditLogger.record("user", "killswitch.reset", "{}")
        }

        /** Hook for the voice pipeline (T-015, once real STT exists) — case-insensitive substring match. */
        suspend fun checkPhrase(transcript: String): Boolean {
            if (transcript.trim().lowercase().contains(SafetyRules.KILL_PHRASE)) {
                trigger("voice")
                return true
            }
            return false
        }
    }
