package com.aion.host

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aion.brain.MemoryStore
import com.aion.brain.ProviderRouter
import com.aion.host.about.AboutScreen
import com.aion.host.about.OfflineStatusScreen
import com.aion.host.brain.AionGraphFactory
import com.aion.host.brain.BuiltInPluginRegistry
import com.aion.host.brain.ChatScreen
import com.aion.host.brain.DeviceExplorationScheduler
import com.aion.host.brain.RealApprovalGate
import com.aion.host.brain.RoomCheckpointer
import com.aion.host.brain.VoiceCommandHistoryScreen
import com.aion.host.calendar.CalendarScreen
import com.aion.host.communications.CommunicationsScreen
import com.aion.host.devicestatus.DeviceStatusScreen
import com.aion.host.finance.FinanceScreen
import com.aion.host.memory.MemoryGraphScreen
import com.aion.host.memory.MemoryTimelineScreen
import com.aion.host.mockup.AgentsScreen
import com.aion.host.mockup.AiLearningScreen
import com.aion.host.mockup.BackupRestoreScreen
import com.aion.host.mockup.ClipboardNotesScreen
import com.aion.host.mockup.ContextOverlayScreen
import com.aion.host.mockup.DownloadsUpdatesScreen
import com.aion.host.mockup.HabitsGoalsScreen
import com.aion.host.mockup.HealthWellnessScreen
import com.aion.host.mockup.HolographicModeScreen
import com.aion.host.mockup.MediaStudioScreen
import com.aion.host.mockup.OrbOverlayScreen
import com.aion.host.mockup.ShortcutsScreen
import com.aion.host.mockup.SmartAutomationsScreen
import com.aion.host.mockup.SmartHomeScreen
import com.aion.host.mockup.TravelModeScreen
import com.aion.host.mockup.VisionModeScreen
import com.aion.host.notifications.NotificationsScreen
import com.aion.host.proactive.ProactiveSuggestionsScreen
import com.aion.host.security.AppLockGate
import com.aion.host.security.ApprovalGateService
import com.aion.host.security.ApprovalSheetHost
import com.aion.host.security.AuditLogScreen
import com.aion.host.security.AuditLogger
import com.aion.host.security.KillSwitch
import com.aion.host.security.KillSwitchOverlayService
import com.aion.host.security.SecretVault
import com.aion.host.security.SecretsScreen
import com.aion.host.security.SecurityCenterScreen
import com.aion.host.settings.SettingsScreen
import com.aion.host.setup.SetupWizardScreen
import com.aion.host.translate.TranslateScreen
import com.aion.host.ui.theme.AionBottomNav
import com.aion.host.ui.theme.AionNavItem
import com.aion.host.ui.theme.AionTheme
import com.aion.host.usage.UsageStatsScreen
import com.aion.host.voice.VoiceForegroundService
import com.aion.host.weather.WeatherScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private enum class Screen {
    HOME,
    SETUP,
    AUDIT_LOG,
    API_KEYS,
    CHAT,
    CALENDAR,
    COMMUNICATIONS,
    DEVICE_STATUS,
    USAGE_STATS,
    PROACTIVE,
    FINANCE,
    WEATHER,
    TRANSLATE,
    ABOUT,
    OFFLINE_STATUS,
    MEMORY_TIMELINE,
    NOTIFICATIONS,
    SECURITY_CENTER,
    AGENTS,
    SMART_AUTOMATIONS,
    MEMORY_GRAPH,
    SMART_HOME,
    MEDIA_STUDIO,
    VISION_MODE,
    CONTEXT_OVERLAY,
    ORB_OVERLAY,
    TRAVEL_MODE,
    HEALTH_WELLNESS,
    HABITS_GOALS,
    HOLOGRAPHIC_MODE,
    BACKUP_RESTORE,
    DOWNLOADS_UPDATES,
    SHORTCUTS,
    CLIPBOARD_NOTES,
    AI_LEARNING,
    VOICE_COMMAND_HISTORY,
    APPS_HUB,
    SETTINGS,
}

/**
 * DOC-020 S1 app skeleton / T-004 — hosts the PR-02 permission setup wizard as the launcher screen.
 * Extends [FragmentActivity] (not the plain `ComponentActivity` every other screen-hosting activity
 * in this app would otherwise use) because T-138's [AppLockGate]/[androidx.biometric.BiometricPrompt]
 * requires one — Compose's `setContent` works identically on either base class.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var auditLogger: AuditLogger

    @Inject
    lateinit var approvalGateService: ApprovalGateService

    @Inject
    lateinit var secretVault: SecretVault

    @Inject
    lateinit var graphFactory: AionGraphFactory

    @Inject
    lateinit var providerRouter: ProviderRouter

    @Inject
    lateinit var pluginRegistry: BuiltInPluginRegistry

    @Inject
    lateinit var realApprovalGate: RealApprovalGate

    @Inject
    lateinit var killSwitch: KillSwitch

    @Inject
    lateinit var memoryStore: MemoryStore

    @Inject
    lateinit var checkpointer: RoomCheckpointer

    private var resumeTrigger by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // EPIC 16 (2026-07-13) — T-116's runtime `actionBar?.hide()` workaround is now the real
        // fix instead: `Theme.Aion` (res/values/themes.xml) is a `NoActionBar` theme, so no action
        // bar is ever created in the first place — nothing to flash into view before hiding it.
        enableEdgeToEdge()
        setContent {
            // Antigravity-audit finding, 2026-07-13: this was a plain Activity field
            // (`mutableStateOf`, not `rememberSaveable`), so rotating the device re-created
            // MainActivity with `unlocked` reset to false — an already-unlocked session had to
            // pass the biometric prompt again on every rotation. T-138 — starts locked; a device
            // with neither biometrics nor a screen-lock credential enrolled skips the gate
            // entirely rather than stranding the owner outside their own app with no way in.
            var unlocked by rememberSaveable { mutableStateOf(!AppLockGate.canAuthenticate(this)) }
            if (unlocked) {
                AionApp(
                    resumeTrigger,
                    auditLogger,
                    approvalGateService,
                    secretVault,
                    graphFactory,
                    providerRouter,
                    pluginRegistry,
                    realApprovalGate,
                    killSwitch,
                    memoryStore,
                    checkpointer,
                )
            } else {
                LockScreen(onUnlockTap = {
                    AppLockGate.authenticate(this, onSuccess = { unlocked = true }, onFailure = {})
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTrigger++
    }
}

/** T-138 — auto-prompts on first composition; the button is a manual retry if that prompt is dismissed. */
@Composable
private fun LockScreen(onUnlockTap: () -> Unit) {
    LaunchedEffect(Unit) { onUnlockTap() }
    AionTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("AION is locked", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onUnlockTap) { Text("Unlock") }
            }
        }
    }
}

@Composable
private fun AionApp(
    resumeSignal: Int,
    auditLogger: AuditLogger,
    approvalGateService: ApprovalGateService,
    secretVault: SecretVault,
    graphFactory: AionGraphFactory,
    providerRouter: ProviderRouter,
    pluginRegistry: BuiltInPluginRegistry,
    realApprovalGate: RealApprovalGate,
    killSwitch: KillSwitch,
    memoryStore: MemoryStore,
    checkpointer: RoomCheckpointer,
) {
    // Antigravity-audit finding, 2026-07-13: these were `remember`, so a config change (rotation,
    // dark-mode toggle) reset which screen you were on and which services you'd toggled on —
    // `rememberSaveable` survives that (Screen is a Kotlin enum, inherently Serializable via
    // `java.lang.Enum`, so no custom Saver is needed).
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var overlayRunning by rememberSaveable { mutableStateOf(false) }
    var voiceRunning by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    // Antigravity-audit finding, 2026-07-13: with no back-stack at all, the system Back button
    // always exited the whole app instead of returning to the previous screen. This app is a flat,
    // one-level menu (no screen ever navigates to a third screen), so a full Navigation Compose
    // back-stack would be solving a deeper-hierarchy problem this app doesn't have — going back to
    // Home (EPIC 16's real dashboard, replacing Setup as the de facto landing screen) is the
    // correct, minimal fix. Back still exits normally once already on Home.
    BackHandler(enabled = screen != Screen.HOME) {
        screen = Screen.HOME
    }

    // T-120 (DOC-017 T4) — block screenshots/screen-recording/recents-thumbnail capture while raw
    // API key values are on screen; cleared again once the user navigates away.
    DisposableEffect(screen) {
        val window = (context as? Activity)?.window
        if (screen == Screen.API_KEYS) {
            window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    AionTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    when (screen) {
                        Screen.HOME ->
                            HomeScreen(
                                auditLogger,
                                secretVault,
                                voiceRunning,
                                onTapToSpeak = { screen = Screen.CHAT },
                                modifier = Modifier.weight(1f),
                            )
                        Screen.SETTINGS ->
                            SettingsScreen(
                                voiceRunning = voiceRunning,
                                onToggleVoice = {
                                    voiceRunning = !voiceRunning
                                    val intent = Intent(context, VoiceForegroundService::class.java)
                                    if (voiceRunning) {
                                        ContextCompat.startForegroundService(context, intent)
                                    } else {
                                        context.stopService(intent)
                                    }
                                },
                                overlayRunning = overlayRunning,
                                onToggleOverlay = {
                                    overlayRunning = !overlayRunning
                                    val intent = Intent(context, KillSwitchOverlayService::class.java)
                                    if (overlayRunning) context.startService(intent) else context.stopService(intent)
                                },
                                onExploreDevice = {
                                    DeviceExplorationScheduler.triggerNow(context)
                                    Toast.makeText(context, "Exploring device…", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        Screen.AUDIT_LOG -> AuditLogScreen(auditLogger, modifier = Modifier.weight(1f))
                        Screen.API_KEYS -> SecretsScreen(secretVault, auditLogger, modifier = Modifier.weight(1f))
                        Screen.SETUP -> SetupWizardScreen(resumeSignal, auditLogger, modifier = Modifier.weight(1f))
                        Screen.CALENDAR -> CalendarScreen(resumeSignal, modifier = Modifier.weight(1f))
                        Screen.COMMUNICATIONS -> CommunicationsScreen(resumeSignal, modifier = Modifier.weight(1f))
                        Screen.DEVICE_STATUS -> DeviceStatusScreen(modifier = Modifier.weight(1f))
                        Screen.USAGE_STATS -> UsageStatsScreen(resumeSignal, modifier = Modifier.weight(1f))
                        Screen.PROACTIVE ->
                            ProactiveSuggestionsScreen(
                                resumeSignal,
                                onOpenCalendar = { screen = Screen.CALENDAR },
                                modifier = Modifier.weight(1f),
                            )
                        Screen.FINANCE -> FinanceScreen(resumeSignal, modifier = Modifier.weight(1f))
                        Screen.WEATHER -> WeatherScreen(resumeSignal, modifier = Modifier.weight(1f))
                        Screen.TRANSLATE -> TranslateScreen(modifier = Modifier.weight(1f))
                        Screen.ABOUT ->
                            AboutScreen(
                                secretVault,
                                pluginRegistry.registeredPluginCount,
                                modifier = Modifier.weight(1f),
                            )
                        Screen.OFFLINE_STATUS -> OfflineStatusScreen(modifier = Modifier.weight(1f))
                        Screen.MEMORY_TIMELINE ->
                            MemoryTimelineScreen(memoryStore, resumeSignal, modifier = Modifier.weight(1f))
                        Screen.NOTIFICATIONS ->
                            NotificationsScreen(memoryStore, resumeSignal, modifier = Modifier.weight(1f))
                        Screen.SECURITY_CENTER ->
                            SecurityCenterScreen(auditLogger, killSwitch, modifier = Modifier.weight(1f))
                        Screen.AGENTS -> AgentsScreen(modifier = Modifier.weight(1f))
                        Screen.SMART_AUTOMATIONS -> SmartAutomationsScreen(modifier = Modifier.weight(1f))
                        Screen.MEMORY_GRAPH ->
                            MemoryGraphScreen(memoryStore, resumeSignal, modifier = Modifier.weight(1f))
                        Screen.SMART_HOME -> SmartHomeScreen(modifier = Modifier.weight(1f))
                        Screen.MEDIA_STUDIO -> MediaStudioScreen(modifier = Modifier.weight(1f))
                        Screen.VISION_MODE -> VisionModeScreen(modifier = Modifier.weight(1f))
                        Screen.CONTEXT_OVERLAY -> ContextOverlayScreen(modifier = Modifier.weight(1f))
                        Screen.ORB_OVERLAY -> OrbOverlayScreen(modifier = Modifier.weight(1f))
                        Screen.TRAVEL_MODE -> TravelModeScreen(modifier = Modifier.weight(1f))
                        Screen.HEALTH_WELLNESS -> HealthWellnessScreen(modifier = Modifier.weight(1f))
                        Screen.HABITS_GOALS -> HabitsGoalsScreen(modifier = Modifier.weight(1f))
                        Screen.HOLOGRAPHIC_MODE -> HolographicModeScreen(modifier = Modifier.weight(1f))
                        Screen.BACKUP_RESTORE -> BackupRestoreScreen(modifier = Modifier.weight(1f))
                        Screen.DOWNLOADS_UPDATES -> DownloadsUpdatesScreen(modifier = Modifier.weight(1f))
                        Screen.SHORTCUTS -> ShortcutsScreen(modifier = Modifier.weight(1f))
                        Screen.CLIPBOARD_NOTES -> ClipboardNotesScreen(modifier = Modifier.weight(1f))
                        Screen.AI_LEARNING -> AiLearningScreen(modifier = Modifier.weight(1f))
                        Screen.VOICE_COMMAND_HISTORY ->
                            VoiceCommandHistoryScreen(checkpointer, resumeSignal, modifier = Modifier.weight(1f))
                        Screen.APPS_HUB ->
                            AppsHubScreen(
                                destinations =
                                    listOf(
                                        "Setup" to { screen = Screen.SETUP },
                                        "Agents" to { screen = Screen.AGENTS },
                                        "Automations" to { screen = Screen.SMART_AUTOMATIONS },
                                        "Memory Graph" to { screen = Screen.MEMORY_GRAPH },
                                        "Smart Home" to { screen = Screen.SMART_HOME },
                                        "AI Studio" to { screen = Screen.MEDIA_STUDIO },
                                        "Vision Mode" to { screen = Screen.VISION_MODE },
                                        "Context Overlay" to { screen = Screen.CONTEXT_OVERLAY },
                                        "Global Orb Overlay" to { screen = Screen.ORB_OVERLAY },
                                        "Travel Mode" to { screen = Screen.TRAVEL_MODE },
                                        "Health & Wellness" to { screen = Screen.HEALTH_WELLNESS },
                                        "Habits & Goals" to { screen = Screen.HABITS_GOALS },
                                        "Holographic Mode" to { screen = Screen.HOLOGRAPHIC_MODE },
                                        "Backup & Restore" to { screen = Screen.BACKUP_RESTORE },
                                        "Downloads & Updates" to { screen = Screen.DOWNLOADS_UPDATES },
                                        "Shortcuts" to { screen = Screen.SHORTCUTS },
                                        "Clipboard & Notes" to { screen = Screen.CLIPBOARD_NOTES },
                                        "AION Learning" to { screen = Screen.AI_LEARNING },
                                        "Voice Command History" to { screen = Screen.VOICE_COMMAND_HISTORY },
                                        "Settings" to { screen = Screen.SETTINGS },
                                    ),
                                modifier = Modifier.weight(1f),
                            )
                        Screen.CHAT ->
                            ChatScreen(
                                graphFactory,
                                providerRouter,
                                pluginRegistry.manager,
                                realApprovalGate,
                                killSwitch,
                                modifier = Modifier.weight(1f),
                            )
                    }
                    // T-168 — the persistent glass bottom bar + glowing mic orb every Stitch mockup
                    // screen shares. "Apps" opens AppsHubScreen — every screen this import pass
                    // added (real and illustrative) lives there instead of bloating the debug row.
                    AionBottomNav(
                        left =
                            listOf(
                                AionNavItem(Icons.Filled.Home, "Home", screen == Screen.HOME) { screen = Screen.HOME },
                                AionNavItem(
                                    Icons.Filled.Apps,
                                    "Apps",
                                    screen == Screen.APPS_HUB,
                                ) { screen = Screen.APPS_HUB },
                            ),
                        right =
                            listOf(
                                AionNavItem(
                                    Icons.Filled.Notifications,
                                    "Alerts",
                                    screen == Screen.NOTIFICATIONS,
                                ) { screen = Screen.NOTIFICATIONS },
                                AionNavItem(
                                    Icons.Filled.Settings,
                                    "Settings",
                                    screen == Screen.API_KEYS,
                                ) { screen = Screen.API_KEYS },
                            ),
                        onMicClick = { screen = Screen.CHAT },
                    )
                }
                // SR-01/02 — sits above everything else; shows itself only when a side-effect
                // action is actually pending approval (none exist yet, ExecutorAgent is T-051).
                ApprovalSheetHost(approvalGateService)
            }
        }
    }
}
