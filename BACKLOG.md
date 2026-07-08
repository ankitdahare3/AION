# BACKLOG.md — Proposed work not yet in TASKS.md

- **AionNotificationListener real ingestion logic** (android/app/src/main/java/com/aion/host/svc/AionNotificationListener.kt).
  Added 2026-07-09 as an empty shell purely so the "Notification access" PR-02 permission has a
  real component to grant (T-004). No task in TASKS.md currently owns building the actual
  `onNotificationPosted`/`onNotificationRemoved` content-ingestion + InjectionFilter wrapping
  (DOC-004 §6, DOC-009) — propose a task for this alongside or after T-040 (AionAccessibilityService),
  since both feed the same automation-context pipeline.
