# DOC-018 — PERFORMANCE
**Project AION | v1.0 | 07 July 2026**

## 1. Budgets (locked, from NFR)
| Metric | Target | Red line |
|---|---|---|
| Wake→listening | <1s | 2s |
| Simple command e2e | <5s | 8s |
| Automation step median | <10s | 15s |
| Local first-token (warm) | <1.2s | 2s |
| Idle battery (wake word on) | <3%/hr | 5%/hr |
| Task cloud cost | <$0.10 | $0.15 |
| Crash-free sessions | ≥99.5% | 99% |

## 2. Bench harness (tools/bench, "Everything Measurable" rule)
- micro: tokens/s per model/quant/backend; wake-word FA/FR; OCR ms; embed ms
- macro: 50-task internal benchmark (DOC-001 §12) — scripted device runs, JSON reports
- soak: 24h idle battery, 7d service uptime
- regression gate: PR fails if any budget regresses >10% (QAAgent + CI)

## 3. Optimization playbook
Context compression (a11y ≤2000 tok), skill-first execution (skip planning), prompt caching where provider supports, batch memory writes, Vulkan backend on capable GPUs, int4 default quant, lazy vision.

## 4. Thermal/battery governance
ThermalManager tiers: NORMAL / WARM(>40°C: defer background) / HOT(>42°C: local inference pause, cloud-only) / CRITICAL(>45°C: automation pause + notify). Battery <20%: background AI off; <10%: essentials only.

## 5. Dashboards
Dream Mode writes nightly perf report → on-device dashboard + exportable; weekly trend fed to ArchitectAgent proposals.
