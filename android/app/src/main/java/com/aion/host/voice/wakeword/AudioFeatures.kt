package com.aion.host.voice.wakeword

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.nio.FloatBuffer
import kotlin.random.Random

/**
 * T-011 (DOC-011 §1) — a faithful Kotlin port of openWakeWord's `AudioFeatures` streaming pipeline
 * (openwakeword/utils.py, Apache-2.0): raw 16kHz mono PCM16 -> melspectrogram.onnx -> embedding_model.onnx,
 * buffered into a rolling window of embeddings that [WakeWordModel] scores. The exact buffer sizes/strides
 * (1280-sample chunks, 76-frame embedding windows, 8-frame stride, 970-row melspec cap, 120-row feature
 * cap) are copied from the reference implementation, not guessed — getting these wrong would silently
 * produce plausible-looking but incorrect scores.
 */
internal class AudioFeatures(
    env: OrtEnvironment,
    context: Context,
) : AutoCloseable {
    private val melspecModel = OnnxModel(env, context.assets.open("models/melspectrogram.onnx").readBytes())
    private val embeddingModel = OnnxModel(env, context.assets.open("models/embedding_model.onnx").readBytes())

    private val rawDataBuffer = ArrayDeque<Float>()
    private var rawDataRemainder = FloatArray(0)
    private var accumulatedSamples = 0
    private val melspectrogramBuffer = MutableList(76) { FloatArray(32) { 1f } }
    private val featureBuffer = mutableListOf<FloatArray>()

    init {
        featureBuffer.addAll(warmUpEmbeddings())
    }

    /** Feeds one chunk of raw 16kHz mono PCM16 samples (as floats, NOT normalized) into the pipeline. */
    fun push(x: FloatArray) {
        var input = x
        if (rawDataRemainder.isNotEmpty()) {
            input = rawDataRemainder + x
            rawDataRemainder = FloatArray(0)
        }
        if (accumulatedSamples + input.size >= CHUNK_SAMPLES) {
            val remainder = (accumulatedSamples + input.size) % CHUNK_SAMPLES
            if (remainder != 0) {
                bufferRawData(input.copyOfRange(0, input.size - remainder))
                accumulatedSamples += input.size - remainder
                rawDataRemainder = input.copyOfRange(input.size - remainder, input.size)
            } else {
                bufferRawData(input)
                accumulatedSamples += input.size
            }
        } else {
            accumulatedSamples += input.size
            bufferRawData(input)
        }

        if (accumulatedSamples >= CHUNK_SAMPLES && accumulatedSamples % CHUNK_SAMPLES == 0) {
            streamMelspectrogram(accumulatedSamples)
            val steps = accumulatedSamples / CHUNK_SAMPLES
            val total = melspectrogramBuffer.size
            for (i in steps - 1 downTo 0) {
                val end = total - EMBED_STEP * i
                val start = end - EMBED_WINDOW
                if (start >= 0 && end <= total) {
                    featureBuffer.add(embeddingModel.predictEmbedding(melspectrogramBuffer.subList(start, end)))
                }
            }
            accumulatedSamples = 0
        }
        if (featureBuffer.size > FEATURE_BUFFER_MAX_LEN) {
            val drop = featureBuffer.size - FEATURE_BUFFER_MAX_LEN
            repeat(drop) { featureBuffer.removeAt(0) }
        }
    }

    /** Returns the most recent [n] embedding frames, flattened row-major as `[1, n, 96]`. */
    fun features(n: Int): FloatArray {
        val start = maxOf(0, featureBuffer.size - n)
        val slice = featureBuffer.subList(start, featureBuffer.size)
        val flat = FloatArray(n * EMBED_DIM)
        val offsetRows = n - slice.size
        for ((idx, row) in slice.withIndex()) {
            System.arraycopy(row, 0, flat, (offsetRows + idx) * EMBED_DIM, EMBED_DIM)
        }
        return flat
    }

    private fun bufferRawData(x: FloatArray) {
        for (s in x) {
            rawDataBuffer.addLast(s)
            if (rawDataBuffer.size > RAW_BUFFER_MAX_LEN) rawDataBuffer.removeFirst()
        }
    }

    private fun streamMelspectrogram(nSamples: Int) {
        val take = nSamples + 160 * 3
        val recent = rawDataBuffer.toFloatArray().let { it.copyOfRange(maxOf(0, it.size - take), it.size) }
        melspectrogramBuffer.addAll(melspecModel.predictMelspec(recent))
        if (melspectrogramBuffer.size > MELSPEC_MAX_LEN) {
            val drop = melspectrogramBuffer.size - MELSPEC_MAX_LEN
            repeat(drop) { melspectrogramBuffer.removeAt(0) }
        }
    }

    /** Mirrors the reference's own warm-up: embeds a few seconds of random noise so the feature
     * buffer isn't empty before any real audio has streamed in (the "zero first 5 predictions"
     * rule in [WakeWordDetector] already mutes the very first frames regardless). */
    private fun warmUpEmbeddings(): List<FloatArray> {
        val noise = FloatArray(16000 * 4) { (Random.nextInt(2001) - 1000).toFloat() }
        val melFrames = melspecModel.predictMelspec(noise)
        val rows = mutableListOf<FloatArray>()
        var i = 0
        while (i + EMBED_WINDOW <= melFrames.size) {
            rows.add(embeddingModel.predictEmbedding(melFrames.subList(i, i + EMBED_WINDOW)))
            i += EMBED_STEP
        }
        return rows
    }

    private fun ArrayDeque<Float>.toFloatArray(): FloatArray = FloatArray(size) { this[it] }

    override fun close() {
        melspecModel.close()
        embeddingModel.close()
    }

    private companion object {
        const val CHUNK_SAMPLES = 1280
        const val EMBED_WINDOW = 76
        const val EMBED_STEP = 8
        const val EMBED_DIM = 96
        const val MELSPEC_MAX_LEN = 970
        const val FEATURE_BUFFER_MAX_LEN = 120
        const val RAW_BUFFER_MAX_LEN = 16000 * 10
    }
}

/** Thin wrapper resolving input/output tensor names dynamically (never hardcoded) so a future
 * custom-trained model (T-012) with different auto-generated ONNX names still loads correctly. */
internal class OnnxModel(
    private val env: OrtEnvironment,
    modelBytes: ByteArray,
) : AutoCloseable {
    private val session = env.createSession(modelBytes, OrtSession.SessionOptions())
    private val inputName = session.inputNames.iterator().next()
    private val outputName = session.outputNames.iterator().next()

    /** melspectrogram.onnx: raw PCM16-as-float samples -> mel frames (transform x/10+2 applied). */
    fun predictMelspec(samples: FloatArray): List<FloatArray> {
        OnnxTensor.createTensor(env, FloatBuffer.wrap(samples), longArrayOf(1, samples.size.toLong())).use { input ->
            session.run(mapOf(inputName to input)).use { result ->
                val flat = (result.get(outputName).get() as OnnxTensor).floatBuffer
                val total = flat.remaining()
                val frames = total / 32
                return List(frames) { r ->
                    FloatArray(32) { c -> flat.get(r * 32 + c) / 10f + 2f }
                }
            }
        }
    }

    /** embedding_model.onnx: one 76x32 mel window -> a single 96-dim embedding. */
    fun predictEmbedding(window: List<FloatArray>): FloatArray {
        val flat = FloatArray(76 * 32)
        for (r in 0 until 76) System.arraycopy(window[r], 0, flat, r * 32, 32)
        OnnxTensor.createTensor(env, FloatBuffer.wrap(flat), longArrayOf(1, 76, 32, 1)).use { input ->
            session.run(mapOf(inputName to input)).use { result ->
                val out = (result.get(outputName).get() as OnnxTensor).floatBuffer
                return FloatArray(96) { out.get(it) }
            }
        }
    }

    override fun close() = session.close()
}
