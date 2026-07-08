# DOC-012 — VISION ENGINE
**Project AION | v1.0 | 07 July 2026**

## 1. Role
Vision is the FALLBACK, a11y is primary (DOC-009). Vision activates when: a11y tree empty/partial (games, canvas, some WebViews), ElementResolver ambiguity, or verification needs pixels.

## 2. Pipeline
Capture: AccessibilityService.takeScreenshot() (API 31+) → MediaProjection FGS fallback.
→ Preprocess: downscale to 1080-max, region-of-interest crop when known
→ OCR: ML Kit Text Recognition v2 (on-device, hi+en scripts)
→ UI detection: on-device YOLO-class icon/widget detector (train on RICO+ custom; P1)
→ Screen understanding (hard cases): multimodal LLM — local Gemma-4-E4B-vision if bench passes, else cloud vision model via Router (screen image leaves device ONLY with per-app user consent — DOC-017)

## 3. Outputs
VisionObservation {ocr_blocks[], detected_elements[], screen_summary, grounded_targets[{label, bbox, confidence}]} → merged with a11y tree by ElementResolver (a11y wins on conflict).

## 4. Grounding for taps
bbox center → gesture; confidence <0.75 → ask user or replan. Every vision-grounded tap logged with crop thumbnail for reflection.

## 5. Budgets
Vision path adds ≤2.5s/step target; screenshots purged after task unless failure (kept 7d for Reflector); no continuous screen recording, ever.
