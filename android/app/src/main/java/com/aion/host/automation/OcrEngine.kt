package com.aion.host.automation

import android.graphics.Bitmap

/** DOC-012 §2 — on-device OCR. Kept as an interface so a future Devanagari-model or multimodal-LLM path can slot in without changing callers. */
interface OcrEngine {
    suspend fun recognize(bitmap: Bitmap): VisionObservation
}
