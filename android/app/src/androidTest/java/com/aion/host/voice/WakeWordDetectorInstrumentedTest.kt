package com.aion.host.voice

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aion.host.voice.wakeword.WakeWordDetector
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * T-011 AC ("test WAVs trigger/reject correctly") — real ONNX inference (no fakes/mocks) against
 * two genuine speech clips from openWakeWord's own test suite (Apache-2.0, tests/data/): a real
 * "alexa" utterance for the bundled `alexa_v0.1.onnx` placeholder wake model (positive/trigger
 * case) and a real "hey mycroft" utterance — genuine speech, just a different wake phrase, a much
 * more meaningful negative than silence (reject case). Runs on-device/emulator only: ONNX
 * Runtime's native libs are Android-ABI .so files, not loadable in a host-JVM unit test.
 */
@RunWith(AndroidJUnit4::class)
class WakeWordDetectorInstrumentedTest {
    private fun scoreClip(assetName: String): Float {
        // The fixture WAVs live in this test APK's own assets/, not the target app's.
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val samples = testContext.assets.open("wakeword/$assetName").use { readWavPcm16(it) }

        // 1s silence padding front/back, same as openWakeWord's own `predict_clip` default —
        // short clips need it so the streaming buffers have enough context to warm up on.
        val padding = ShortArray(16000)
        val padded = padding + samples + padding

        // The target app's assets/ hold the real .onnx model files (T-011).
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        WakeWordDetector(appContext).use { detector ->
            var maxScore = 0f
            var offset = 0
            while (offset < padded.size - CHUNK) {
                val chunk = padded.copyOfRange(offset, offset + CHUNK)
                maxScore = maxOf(maxScore, detector.accept(chunk))
                offset += CHUNK
            }
            android.util.Log.i("WakeWordDetectorTest", "$assetName maxScore=$maxScore")
            return maxScore
        }
    }

    @Test
    fun alexaClipTriggers() {
        val score = scoreClip("alexa_test.wav")
        assertTrue("expected alexa_test.wav to trigger the alexa model (score=$score)", score >= 0.5f)
    }

    @Test
    fun heyMycroftClipIsRejected() {
        val score = scoreClip("hey_mycroft_test.wav")
        assertTrue("expected hey_mycroft_test.wav NOT to trigger the alexa model (score=$score)", score < 0.5f)
    }

    private companion object {
        const val CHUNK = 1280

        /** Minimal canonical PCM16 WAV reader: walks RIFF chunks to find `data` rather than
         * assuming a fixed 44-byte header. */
        fun readWavPcm16(input: InputStream): ShortArray {
            val bytes = input.readBytes()
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            buf.position(12) // skip "RIFF"<size>"WAVE"
            while (buf.remaining() >= 8) {
                val chunkId = ByteArray(4).also { buf.get(it) }
                val chunkSize = buf.int
                val id = String(chunkId, Charsets.US_ASCII)
                if (id == "data") {
                    val samples = ShortArray(chunkSize / 2)
                    for (i in samples.indices) samples[i] = buf.short
                    return samples
                }
                buf.position(buf.position() + chunkSize + (chunkSize and 1))
            }
            error("no data chunk found in WAV file")
        }
    }
}
