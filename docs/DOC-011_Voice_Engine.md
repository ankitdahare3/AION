# DOC-011 — VOICE ENGINE
**Project AION | v1.0 | 07 July 2026**

## 1. Stack decisions (ADR-011)
| Stage | Primary | Fallback | Why |
|---|---|---|---|
| Wake word | openWakeWord (custom "AION" model, ONNX) | Porcupine free tier | open-source, trainable, <1% FA/hr target |
| VAD | Silero VAD v5 | — | 1ms frames, tiny |
| STT | Whisper-class small multilingual int8 via whisper.cpp (streaming) | Google SpeechRecognizer (online) | Hinglish code-switch: Whisper multilingual proven; evaluate ai4bharat/IndicConformer during bench |
| TTS | Piper (hi + en voices) | Kokoro if quality bench wins; cloud TTS optional | on-device, fast |
| Speaker ID | ECAPA-TDNN embedding, owner enroll 30s | — | P1 |

Benchmark gate before freeze: WER on 500-utterance Hinglish set (self-recorded) — target <12%; wake latency <500ms; TTS start <300ms.

## 2. Session flow
WakeWord(FGS type=microphone, always-on) → earcon + overlay → streaming STT with partials → endpointing (Silero, 700ms silence) → Brain → streaming TTS. Barge-in: VAD during TTS → duck 150ms → confirm speech → stop TTS, resume STT.

## 3. Language handling
LanguageDetector per-utterance (hi/en/mixed). Response language mirrors user (Hinglish in → Hinglish out). All prompts/personas bilingual-aware.

## 4. Audio discipline
AudioFocus transient-may-duck; mic exclusively via one FGS; hardware AEC; privacy LED overlay indicator whenever mic hot (trust requirement).

## 5. Continuous mode (FR-V06)
After response, 8s follow-up window (no wake word), visual countdown, auto-close.
