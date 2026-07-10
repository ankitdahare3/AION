package com.aion.host.automation

/** DOC-012 §3 — one recognized line of text from OCR, with its screen position and confidence. */
data class OcrBlock(
    val text: String,
    val bounds: Bounds,
    val confidence: Float,
)

/**
 * DOC-012 §3 — VisionObservation. `detectedElements`/`groundedTargets` (icon/widget detection,
 * DOC-012 §2's "P1" YOLO-class detector) aren't built — no such model or infra exists yet, tracked
 * in BACKLOG.md; OCR blocks alone are what T-100 actually delivers.
 */
data class VisionObservation(
    val ocrBlocks: List<OcrBlock>,
    val screenSummary: String,
)
