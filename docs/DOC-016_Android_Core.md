# DOC-016 — ANDROID CORE
**Project AION | v1.0 | 07 July 2026**

## 1. App structure
Kotlin 2.x + Jetpack Compose; minSdk 31, target latest. Processes: :core, :automation, :inference (DOC-003 §4). Hilt DI; kotlinx.serialization; WorkManager (Dream Mode); DataStore (config).

## 2. Services
- VoiceFgs (type=microphone): wake word + STT host
- AutomationFgs (type=specialUse, a11y binding lifecycle)
- CaptureFgs (type=mediaProjection, on-demand only)
- InferenceService (:inference, bound, killable)
Boot: Device Owner → PERSISTENT flags + BootReceiver chain; watchdog restarts (NFR-07).

## 3. Privileged setup (dedicated phone provisioning — first-class documented path)
1. Factory reset → skip account → adb provision Device Owner:
   `dpm set-device-owner com.aion.host/.AdminReceiver`
2. Grant a11y, notification listener, usage access, overlay via setup wizard
3. Install Shizuku → adb start; AION binds ShizukuBridge
4. Battery optimization ignore; disable AAPM (user choice on dedicated device, DOC-001 §9.3)
DeviceOwner powers used: silent grant runtime perms, keep-alive, lock-task (kiosk P2), policy-safe app installs. NEVER used to hide activity from user.

## 4. Compat matrix
API 31-33: MediaProjection screenshot path; API 34+: FGS typed permissions enforced; API 36 (Android 17): AAPM interactions documented; each Android release → ADR review (DOC-001 risk #1).

## 5. UI surfaces
Overlay bubble (status + kill-switch SR-03), Approval sheet (voice+visual), Chat screen, Memory browser, Plugin/Skill manager, Setup wizard, Audit log viewer.
