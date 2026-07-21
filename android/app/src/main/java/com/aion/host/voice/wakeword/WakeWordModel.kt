package com.aion.host.voice.wakeword

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.nio.FloatBuffer

/**
 * T-011 — one wake-word classifier (e.g. `alexa_v0.1.onnx` today, swapped for the trained "AION"
 * model once T-012 lands). Input/output tensor names AND the embedding-window length are all
 * resolved from the model itself rather than hardcoded, so a differently-shaped custom model still
 * loads correctly without a code change.
 */
internal class WakeWordModel(
    private val env: OrtEnvironment,
    modelBytes: ByteArray,
) : AutoCloseable {
    private val session = env.createSession(modelBytes, OrtSession.SessionOptions())
    private val inputName = session.inputNames.iterator().next()
    private val outputName = session.outputNames.iterator().next()

    /** The number of trailing embedding frames (columns) this model expects, e.g. 16. */
    val expectedFrames: Int =
        ((session.inputInfo[inputName]?.info as? TensorInfo)?.shape?.getOrNull(1)?.takeIf { it > 0 })?.toInt() ?: 16

    /** [features] must be a flat `expectedFrames * 96` array, oldest-first (see [AudioFeatures.features]). */
    fun score(features: FloatArray): Float {
        val shape = longArrayOf(1, expectedFrames.toLong(), 96)
        OnnxTensor.createTensor(env, FloatBuffer.wrap(features), shape).use { input ->
            session.run(mapOf(inputName to input)).use { result ->
                return (result.get(outputName).get() as OnnxTensor).floatBuffer.get(0)
            }
        }
    }

    override fun close() = session.close()
}
