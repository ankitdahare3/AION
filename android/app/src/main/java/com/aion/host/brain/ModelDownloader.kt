package com.aion.host.brain

import android.content.Context
import com.aion.brain.providers.defaultProviderHttpClient
import com.aion.host.security.ProviderKey
import com.aion.host.security.SecretVault
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-172 — the in-app counterpart to the owner's manual `adb push` model setup: downloads the same
 * gated Gemma3-1B-IT `.litertlm` file [LiteRtIntentClassifier] looks for, using the owner's own
 * HuggingFace personal access token ([ProviderKey.HUGGINGFACE_TOKEN] — their own account, their
 * own accepted license, generated at huggingface.co/settings/tokens; still no token embedded in
 * this repo). Streams to a `.part` file and only renames to the real path on full success, so an
 * interrupted download is never mistaken for a real, loadable model.
 *
 * `defaultProviderHttpClient()` sets a global 30s `requestTimeoutMillis` sized for chat-completion
 * calls; a 584MB model download takes far longer than that, so every real download was aborting
 * partway through with a timeout exception. Overridden per-request below to actually finish.
 */
@Singleton
class ModelDownloader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val secretVault: SecretVault,
    ) {
        sealed class State {
            data object Idle : State()

            data class Downloading(
                val bytesDownloaded: Long,
                val totalBytes: Long?,
            ) : State()

            data object Done : State()

            data class Failed(
                val message: String,
            ) : State()
        }

        private val client by lazy { defaultProviderHttpClient() }
        private val _state = MutableStateFlow<State>(if (modelFile().exists()) State.Done else State.Idle)
        val state: StateFlow<State> = _state.asStateFlow()

        fun modelFile(): File = File(context.getExternalFilesDir(null), LiteRtIntentClassifier.MODEL_RELATIVE_PATH)

        suspend fun download() {
            val token = secretVault.get(ProviderKey.HUGGINGFACE_TOKEN)
            if (token.isNullOrBlank()) {
                _state.value = State.Failed("Add a HuggingFace access token in API Keys first")
                return
            }
            val dest = modelFile()
            dest.parentFile?.mkdirs()
            val tmp = File(dest.parentFile, "${dest.name}.part")
            _state.value = State.Downloading(0, null)
            try {
                client
                    .prepareGet(MODEL_URL) {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        timeout { requestTimeoutMillis = DOWNLOAD_TIMEOUT_MS }
                    }.execute { response ->
                        if (!response.status.isSuccess()) {
                            error(
                                "HTTP ${response.status.value} — check your HuggingFace token " +
                                    "has accepted the Gemma license",
                            )
                        }
                        val total = response.contentLength()
                        val channel = response.bodyAsChannel()
                        tmp.outputStream().use { out ->
                            var downloaded = 0L
                            val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                            while (!channel.isClosedForRead) {
                                val read = channel.readAvailable(buffer)
                                if (read > 0) {
                                    out.write(buffer, 0, read)
                                    downloaded += read
                                    _state.value = State.Downloading(downloaded, total)
                                }
                            }
                        }
                    }
                if (!tmp.renameTo(dest)) error("could not finalize the downloaded file")
                _state.value = State.Done
            } catch (e: Exception) {
                tmp.delete()
                _state.value = State.Failed(e.message ?: e.javaClass.simpleName)
            }
        }

        companion object {
            private const val DOWNLOAD_BUFFER_BYTES = 64 * 1024

            // A 584MB model over a real mobile/wifi connection can take a while; the global
            // 30s requestTimeoutMillis (sized for chat-completion calls) was aborting every
            // real download partway through, which is what this overrides.
            private const val DOWNLOAD_TIMEOUT_MS = 30 * 60 * 1000L

            // The one non-chip-specific variant on litert-community/Gemma3-1B-IT — matches
            // LiteRtIntentClassifier's Backend.CPU() choice, unlike the sm8xxx/mt69xx/Tensor-G5
            // variants which target specific SoC NPU delegates this app doesn't select.
            const val MODEL_URL =
                "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.litertlm"
        }
    }
