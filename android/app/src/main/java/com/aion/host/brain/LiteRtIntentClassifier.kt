package com.aion.host.brain

import android.content.Context
import com.aion.brain.Intent
import com.aion.brain.LlmIntentClassification
import com.aion.brain.LlmIntentClassifier
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 2026 backend upgrade — real on-device LLM intent classification via LiteRT-LM (DOC-004 §3's
 * original spec, deferred at T-032 for the same "native-build wall" reason LiteRT-LM's own docs
 * warn about too: "optimized for high-end devices... does not reliably support emulators" — this
 * genuinely may not run on the `emulator-5554` AVD this whole project has tested against; the
 * owner made this call knowingly, see PROGRESS.md).
 *
 * No automated model download: Gemma3-1B-IT's `.litertlm` file is gated behind HuggingFace's Gemma
 * license — an app can't anonymously fetch a gated file, and embedding a personal HF access token
 * in an open-source repo is both a real secret leak and likely a license violation either way. The
 * owner accepts the license once on their own HuggingFace account, downloads the file, and pushes
 * it to [MODEL_RELATIVE_PATH] under the app's external files dir — matching this project's own
 * existing "during development, push the model via adb" convention (T-032's own scoping) and
 * needing no WRITE_EXTERNAL_STORAGE permission, since that path is app-scoped.
 *
 * [classify] never throws: missing model file, a device that can't run the backend, or a reply
 * that doesn't parse to a real [Intent] all just return null, and [IntentRoutingAgent] falls back
 * to the existing keyword classifier in every one of those cases.
 */
@Singleton
class LiteRtIntentClassifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : LlmIntentClassifier {
        private val initLock = Mutex()
        private var engine: Engine? = null
        private var initFailed = false

        override suspend fun classify(utterance: String): Intent? {
            val active = engineOrNull() ?: return null
            return try {
                withContext(Dispatchers.Default) {
                    val config =
                        ConversationConfig(systemInstruction = Contents.of(LlmIntentClassification.SYSTEM_INSTRUCTION))
                    active.createConversation(config).use { conversation ->
                        val raw = conversation.sendMessage(utterance).toString()
                        LlmIntentClassification.parseLabel(raw)
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

        // Sticky initFailed deliberately doesn't retry within one app process: a model pushed via
        // adb mid-session needs a restart to be picked up, same as any other Hilt @Singleton's
        // construction-time state in this app — not worth a re-check-every-call cost for what's an
        // owner/developer-only workflow, not an end-user one.
        private suspend fun engineOrNull(): Engine? {
            engine?.let { return it }
            if (initFailed) return null
            return initLock.withLock {
                engine?.let { return@withLock it }
                if (initFailed) return@withLock null
                val file = modelFile()
                if (!file.exists()) {
                    initFailed = true
                    return@withLock null
                }
                try {
                    withContext(Dispatchers.Default) {
                        val config = EngineConfig(modelPath = file.absolutePath, backend = Backend.CPU())
                        Engine(config).also { it.initialize() }
                    }.also { engine = it }
                } catch (e: Exception) {
                    initFailed = true
                    null
                }
            }
        }

        private fun modelFile(): File = File(context.getExternalFilesDir(null), MODEL_RELATIVE_PATH)

        companion object {
            const val MODEL_RELATIVE_PATH = "models/gemma3-1b-it.litertlm"
        }
    }
