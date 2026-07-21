package com.aion.host.voice.wakeword

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * T-011 (DOC-011 §1) — Silero VAD via the ONNX file openWakeWord itself bundles (an older
 * explicit-LSTM-state `input`/`sr`/`h`/`c` -> `output`/`hn`/`cn` API, not the newer combined-state
 * v5 interface DOC-011 names; this is honestly what ships in the openWakeWord v0.5.1 release
 * assets today, not a fabricated "v5" claim). Recurrent state (`h`/`c`) carries across calls, so a
 * single instance must be fed the whole session's audio in order — [reset] starts a fresh session.
 */
internal class SileroVad(
    private val env: OrtEnvironment,
    modelBytes: ByteArray,
) : AutoCloseable {
    private val session = env.createSession(modelBytes, OrtSession.SessionOptions())
    private var h = FloatArray(2 * 64)
    private var c = FloatArray(2 * 64)

    fun reset() {
        h = FloatArray(2 * 64)
        c = FloatArray(2 * 64)
    }

    /** Mean voice-activity score (0..1) over [samples], split into [frameSize]-sample chunks (default 640 = 40ms @16kHz, matching openWakeWord's own streaming call). */
    fun score(
        samples: ShortArray,
        frameSize: Int = 640,
    ): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0f
        var count = 0
        var offset = 0
        while (offset < samples.size) {
            val end = minOf(offset + frameSize, samples.size)
            val chunk = FloatArray(end - offset) { samples[offset + it] / 32767f }
            sum += runFrame(chunk)
            count++
            offset = end
        }
        return if (count > 0) sum / count else 0f
    }

    private fun runFrame(chunk: FloatArray): Float {
        OnnxTensor.createTensor(env, FloatBuffer.wrap(chunk), longArrayOf(1, chunk.size.toLong())).use { input ->
            OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(16000L)), longArrayOf()).use { sr ->
                OnnxTensor.createTensor(env, FloatBuffer.wrap(h), longArrayOf(2, 1, 64)).use { hIn ->
                    OnnxTensor.createTensor(env, FloatBuffer.wrap(c), longArrayOf(2, 1, 64)).use { cIn ->
                        session.run(mapOf("input" to input, "sr" to sr, "h" to hIn, "c" to cIn)).use { result ->
                            h = toFloatArray(result.get("hn").get() as OnnxTensor)
                            c = toFloatArray(result.get("cn").get() as OnnxTensor)
                            return (result.get("output").get() as OnnxTensor).floatBuffer.get(0)
                        }
                    }
                }
            }
        }
    }

    private fun toFloatArray(tensor: OnnxTensor): FloatArray {
        val buf = tensor.floatBuffer
        return FloatArray(buf.remaining()) { buf.get(it) }
    }

    override fun close() = session.close()
}
