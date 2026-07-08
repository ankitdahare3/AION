# DOC-010 — MEMORY ENGINE
**Project AION | v1.0 | 07 July 2026**

## 1. Stores (all on-device, Room + sqlite-vec)
| Store | Contents | Retention |
|---|---|---|
| ShortTerm | session turns, working state | session, ring buffer 50 |
| LongTerm | facts, preferences, profile | permanent, user-editable |
| Episodic | task records {goal, steps, outcome} | 90d rolling, promote on repeat |
| Skill | learned procedures + stats | permanent, versioned |
| Experience | per-app successful paths, ElementMaps | app-version scoped |
| VectorIndex | embeddings over all above | synced |

## 2. Embeddings
Local model (Gemma-embed class / bge-small-int8) via llama.cpp, 384-dim, ~10ms/item on 8GB device. NEVER cloud — memories don't leave device (NFR-09).

## 3. Write policy
Post-task: MemoryAgent extracts {facts?, preference?, episode} via local model with JSON schema. Confidence <0.6 → discard. PII tagging at write (contact, financial, health) → drives redaction (DOC-017).

## 4. Read policy (ContextBuilder)
hybrid: vector top-k=8 → rerank by recency×importance → dedupe → ≤5 into context, each ≤80 tokens summary form.

## 5. Consolidation (Dream Mode)
merge near-dupes, decay unaccessed episodic (half-life 30d), promote 3×-repeated facts, rebuild index.

## 6. User rights (FR-M08)
Memory browser UI: search/view/edit/delete; "AION, bhool jao X" → soft-delete 7d → purge. Export/import encrypted JSON (portability dream, DOC-001 §3).
