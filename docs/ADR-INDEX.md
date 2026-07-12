# ADR INDEX
- ADR-001 Local runtime = llama.cpp (DOC-014) ✅
- ADR-002 Orchestration = custom Kotlin "AION Graph", LangGraph-pattern (DOC-004) ✅
- ADR-011 Voice stack = openWakeWord+Silero+whisper.cpp+Piper (DOC-011) ✅ (bench-gated)
- ADR-011a Interim voice = Android platform SpeechRecognizer + TextToSpeech as a stepping stone (owner-approved 2026-07-12, GAP_REPORT.md; T-135/T-136). ADR-011 remains the final stack — revisit when T-032 (local brain) lands, since offline voice without an offline brain has no user value
- ADR-019 DB = Room+sqlite-vec+SQLCipher (DOC-019) ✅
- ADR-003 (pending) AppFunctions migration trigger criteria — revisit each Android release
