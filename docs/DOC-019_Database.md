# DOC-019 — DATABASE
**Project AION | v1.0 | 07 July 2026**

## ADR-019: Room (SQLite) + sqlite-vec extension. ObjectBox rejected (proprietary core, migration lock-in). SQLCipher for at-rest encryption over OS FBE (defense in depth).

## 1. Schema (v1, migrations via Room)
```
conversations(id, started_at, summary)
turns(id, conv_id→, role, text, lang, ts)
memories(id, kind[fact|pref|profile], text, confidence, provenance,
         pii_tags, created, accessed, decay_score, deleted_soft)
episodes(id, goal, plan_json, outcome, failure_class, latency_ms,
         cost_usd, app_pkg, ts)
skills(id, yaml, version, status[proposed|approved|active|retired],
       success_count, fail_count, approved_at)
element_maps(app_pkg, app_version, screen_hash, selector_json, confidence, ts)
providers_stats(provider, task_type, success_ema, latency_ema, cost_ema, updated)
plugins(id, version, status, permissions_json, installed_at)
audit_log(seq, prev_hash, hash, actor, action, payload_json, ts)  -- hash-chained
vec_index: sqlite-vec virtual tables per store (384-dim)
config: DataStore(proto) — not SQL
```

## 2. Access layer
Room DAOs + Repository per store (DOC-010 mapping); all writes through MemoryAgent/AuditLogger — no direct DAO use from plugins (sandbox facade only).

## 3. Ops
WAL mode; nightly VACUUM in Dream Mode; size budget 2GB warn/4GB hard (evict per decay policy); encrypted backup/export (user-initiated) to file → portability (DOC-001 §3).
