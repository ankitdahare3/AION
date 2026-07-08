# DOC-009 — AUTOMATION ENGINE
**Project AION | v1.0 | 07 July 2026**

## 1. Routing (locked)
```
ToolCall → Official API plugin exists? → use it (Gmail/Calendar/Telegram APIs)
        → AppFunctions/MCP exposed?   → use it (Android 16+ path, growing)
        → else UI Automation (a11y-first, vision-assist)
```

## 2. A11y Pipeline (droidrun-pattern: structured text, not pixels)
AccessibilityNodeInfo tree → compress: interactive nodes only, {index, class, text, contentDesc, bounds, states} → stable element IDs (hash of role+text+position-bucket) → ≤2000 tokens.
ElementResolver: exact-id → fuzzy text → vision fallback (DOC-012).

## 3. ActionDispatcher
Primitives: tap(id|xy), longPress, swipe(dir|path), scrollTo(id), type(text, field), globalAction(BACK/HOME/RECENTS), launchApp(pkg), notification(action).
Via: AccessibilityService.dispatchGesture + performAction; Shizuku for `input`/`am`/`pm` shell ops when a11y insufficient; DeviceOwner for silent installs/policy (dedicated phone only).

## 4. Step Verification (mandatory per step)
expected_observation (from Planner) vs post-action a11y diff (500ms debounce). Confidence <0.7 → ReflectorAgent. Screenshots only when a11y ambiguous (battery).

## 5. Special surfaces
WebViews: a11y exposes DOM partially → enable JS a11y flags; fallback vision.
Games/canvas: pure vision path, turn-based only (real-time out of scope v1).
System dialogs/permissions: allowlisted auto-handling ONLY for AION's own setup; never for other apps' consent dialogs.

## 6. Rate & safety limits
≤1 action/300ms default; sensitive apps (DOC-017 list) observe-only; every action → AuditLogger before dispatch.
