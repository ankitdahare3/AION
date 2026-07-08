# DOC-014 — LOCAL AI
**Project AION | v1.0 | 07 July 2026**

## ADR-001 — Local runtime: **DECIDED: llama.cpp (JNI) primary**
- Why: broadest GGUF model coverage (Gemma/Qwen/Phi day-one), mature Android builds (Vulkan/OpenCL + ARM NEON), server-independent, hot-swap models by file.
- ExecuTorch: WATCH status — re-bench each quarter (NPU delegates maturing); MLC-LLM: rejected v1 (per-model compile friction); AICore/Gemini Nano: OPPORTUNISTIC — use via ML Kit GenAI when device supports (foreground-only limits noted), never a dependency.

## 1. Model slots (config, not code)
| Slot | Default | RAM (int4) | Job |
|---|---|---|---|
| chat/planner-lite | Qwen3-4B-instruct | ~2.6GB | intent, chat, memory extraction |
| embedder | bge-small / gemma-embed | ~120MB | vectors |
| vision (P1) | Gemma-4-E4B-vision | ~3.5GB | screen understanding |
| asr | whisper-small-int8 | ~500MB | DOC-011 |
Swap policy: inference process (:inference) loads ≤1 LLM at a time; LRU unload; models on /sdcard/AION/models with checksum manifest.

## 2. Performance targets (8GB device, Snapdragon 7-class)
prefill ≥250 tok/s, decode ≥12 tok/s (4B int4); first-token <1.2s warm. Bench harness in tools/bench (DOC-018) gates any model/runtime change.

## 3. Thermal/battery integration
ThermalManager listener: >42°C → halve context, pause background inference; on battery <20% → local-only mode for background tasks, cloud allowed for foreground user requests.

## 4. Download/update
Model registry JSON (repo-hosted) → Dream Mode checks → user approves download (size shown) → resume-capable fetch → checksum → activate.
