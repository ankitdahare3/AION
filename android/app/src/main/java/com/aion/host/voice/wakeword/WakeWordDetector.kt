package com.aion.host.voice.wakeword

import ai.onnxruntime.OrtEnvironment
import android.content.Context

/**
 * T-011 (DOC-011 §1) — combines [AudioFeatures] + [WakeWordModel] + [SileroVad] into the single
 * call [VoiceForegroundService][com.aion.host.voice.VoiceForegroundService]'s capture loop needs
 * per audio buffer. `hey_jarvis`-class pretrained models zero their first 5 predictions during
 * warm-up (openWakeWord's own rule, ported here) since the feature buffer briefly still holds
 * warm-up noise embeddings.
 *
 * VAD gating mirrors openWakeWord's own optional `vad_threshold` feature in `Model.predict()`
 * (not DOC-011 §2's separate STT-endpointing use of VAD, which is T-015's job once a real session
 * exists): a wake-word score only counts if Silero VAD also saw real speech in the last ~0.4-0.56s,
 * suppressing false positives on non-speech audio (music, noise) that happens to score high on
 * the wake classifier alone.
 */
class WakeWordDetector(
    context: Context,
    wakeModelAsset: String = "models/alexa_v0.1.onnx",
    private val threshold: Float = 0.5f,
    private val vadThreshold: Float = 0.5f,
) : AutoCloseable {
    private val env = OrtEnvironment.getEnvironment()
    private val audioFeatures = AudioFeatures(env, context)
    private val wakeModel = WakeWordModel(env, context.assets.open(wakeModelAsset).readBytes())
    private val vad = SileroVad(env, context.assets.open("models/silero_vad.onnx").readBytes())
    private val vadHistory = ArrayDeque<Float>()
    private var framesSeen = 0

    /** Feeds one chunk of 16kHz mono PCM16 audio; returns this chunk's (VAD-gated) wake-word score (0..1). */
    fun accept(samples: ShortArray): Float {
        audioFeatures.push(FloatArray(samples.size) { samples[it].toFloat() })
        framesSeen++

        vadHistory.addLast(vad.score(samples))
        if (vadHistory.size > VAD_HISTORY_LEN) vadHistory.removeFirst()

        if (framesSeen <= 5) return 0f
        val wakeScore = wakeModel.score(audioFeatures.features(wakeModel.expectedFrames))

        // Same lookback window as openWakeWord's own VAD gate: the 3 frames ending ~0.4s ago
        // (Python's `list(prediction_buffer)[-7:-4]`, ported as explicit clamped indices).
        val n = vadHistory.size
        val start = maxOf(0, n - 7)
        val end = maxOf(0, n - 4)
        val vadMax = if (end > start) vadHistory.toList().subList(start, end).max() else 0f
        return if (vadMax < vadThreshold) 0f else wakeScore
    }

    fun triggered(samples: ShortArray): Boolean = accept(samples) >= threshold

    override fun close() {
        audioFeatures.close()
        wakeModel.close()
        vad.close()
    }

    private companion object {
        const val VAD_HISTORY_LEN = 125
    }
}
