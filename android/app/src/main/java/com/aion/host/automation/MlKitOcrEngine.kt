package com.aion.host.automation

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** T-100 — real ML Kit Text Recognition v2, on-device, no network (DOC-012 §2/§5). Latin-script model; see BACKLOG.md for Devanagari. */
@Singleton
class MlKitOcrEngine
    @Inject
    constructor() : OcrEngine {
        private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        override suspend fun recognize(bitmap: Bitmap): VisionObservation {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = awaitTask(recognizer.process(image))
            val blocks =
                result.textBlocks.flatMap { block ->
                    block.lines.map { line ->
                        val box = line.boundingBox
                        OcrBlock(
                            text = line.text,
                            bounds =
                                if (box !=
                                    null
                                ) {
                                    Bounds(box.left, box.top, box.right, box.bottom)
                                } else {
                                    Bounds(0, 0, 0, 0)
                                },
                            // ML Kit's Line/Block don't expose a per-line confidence score in this API version.
                            confidence = 1.0f,
                        )
                    }
                }
            return VisionObservation(ocrBlocks = blocks, screenSummary = result.text)
        }

        private suspend fun awaitTask(task: com.google.android.gms.tasks.Task<Text>): Text =
            suspendCancellableCoroutine { cont ->
                task
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
    }
