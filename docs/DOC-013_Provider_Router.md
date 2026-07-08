# DOC-013 — PROVIDER ROUTER
**Project AION | v1.0 | 07 July 2026**

## 1. Registry (config-driven, zero-code swap — NFR-10)
providers.yaml entries: {id, kind: local|openai_compat|anthropic|google|installed_app, endpoint, models[], caps{vision,tools,context,stream}, cost{in,out}, tier: local|free|paid, privacy: on_device|cloud}

Launch set: local-llamacpp, groq, openrouter-free, gemini, openai, anthropic, deepseek, ollama-lan. Installed-app fallback = P2.

## 2. Selection algorithm
```
candidates = registry.filter(caps ⊇ task.needs)
score = w1*taskTypeScore(learned) + w2*tierPreference(local>free>paid)
      + w3*latencyEMA + w4*privacyFit − w5*costEstimate
pick max; on failure → next candidate (max 3), record failure class
```
Failure classes: AUTH, QUOTA, RATE_LIMIT, TIMEOUT, SERVER, CONTENT_FILTER, BAD_OUTPUT (schema fail). QUOTA/AUTH → cooldown 6h; RATE_LIMIT → backoff.

## 3. Normalization layer
Single internal BrainRequest/BrainResult; adapters per API family (OpenAI-compat covers Groq/OpenRouter/DeepSeek/Ollama/vLLM/LM Studio). Tool-call JSON normalized; schema-validated; BAD_OUTPUT auto-retries once with repair prompt.

## 4. Budget guard
Daily cloud budget (default $1) + per-task ceiling ($0.15, NFR-06). Exceed → degrade to free/local + inform user honestly.

## 5. Telemetry
Per call: provider, model, tokens, latency, cost, outcome → CostTracker + BrainLearning scorecards (DOC-008 §4).
