package com.aion.host

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.aion.brain.ProviderRouter
import com.aion.host.brain.AionGraphFactory
import com.aion.host.brain.BuiltInPluginRegistry
import com.aion.host.brain.ChatScreen
import com.aion.host.brain.DeviceExplorationScheduler
import com.aion.host.brain.RealApprovalGate
import com.aion.host.calendar.CalendarScreen
import com.aion.host.communications.CommunicationsScreen
import com.aion.host.devicestatus.DeviceStatusScreen
import com.aion.host.finance.FinanceScreen
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
                    // T-116 finding — 4 TextButtons with no width constraint overflow a real
                    // phone's screen width (font-scale/density-dependent; never showed up on the
                    // emulator's default settings). Without horizontalScroll, Compose's Row squeezes
                    // the last child ("Explore Device") into a tall, narrow multi-line wrap instead
                    // of just clipping — same "4th+ item unreachable" shape as SecretsScreen's T-120
                    // vertical-scroll fix, here on the horizontal axis.
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { screen = Screen.HOME }) {
                            Text("Home")
                        }
                        TextButton(onClick = {
                            screen = if (screen == Screen.SETUP) Screen.HOME else Screen.SETUP
                        }) {
                            Text(if (screen == Screen.SETUP) "Back to Home" else "Setup")
                        }
                        TextButton(onClick = {
                            overlayRunning = !overlayRunning
                            val intent = Intent(context, KillSwitchOverlayService::class.java)
                            if (overlayRunning) context.startService(intent) else context.stopService(intent)
                        }) {
                            Text(if (overlayRunning) "Hide Kill-Switch" else "Show Kill-Switch")
                        }
                        TextButton(onClick = {
                            screen = if (screen == Screen.API_KEYS) Screen.HOME else Screen.API_KEYS
                        }) {
                            Text(if (screen == Screen.API_KEYS) "Back to Home" else "API Keys")
                        }
                        TextButton(onClick = {
                            screen = if (screen == Screen.AUDIT_LOG) Screen.HOME else Screen.AUDIT_LOG
                        }) {
                            Text(if (screen == Screen.AUDIT_LOG) "Back to Home" else "Audit Log")
                        }
                        TextButton(onClick = {
                            screen = if (screen == Screen.CALENDAR) Screen.HOME else Screen.CALENDAR
                        }) {
                            Text(if (screen == Screen.CALENDAR) "Back to Home" else "Calendar")
                        }
                        TextButton(onClick = {
                            screen = if (screen == Screen.COMMUNICATIONS) Screen.HOME else Screen.COMMUNICATIONS
                        }) {
                            Text(if (screen == Screen.COMMUNICATIONS) "Back to Home" else "Communications")
                        }
                        TextButton(onClick = {
                            screen = if (screen == Screen.DEVICE_STATUS) Screen.HOME else Screen.DEVICE_STATUS
                        }) {
                            Text(if (screen == Screen.DEVICE_STATUS) "Back to Home" else "Device Status")
                        }
                        TextButton(onClick = {
                            screen = if (screen == Screen.USAGE_STATS) Screen.HOME else Screen.USAGE_STATS
                        }) {
                            Text(if (screen == Screen.USAGE_STATS) "Back to Home" else "App Usage")
                        }
                        TextButton(onClick = {
                            screen = if (screen == Screen.PROACTIVE) Screen.HOME else Screen.PROACTIVE
                        }) {
                            Text(if (screen == Screen.PROACTIVE) "Back to Home" else "Suggestions")
                        }
                        TextButton(onClick = {
                            screen = if (screen == Screen.FINANCE) Screen.HOME else Screen.FINANCE
                        }) {
                            Text(if (screen == Screen.FINANCE) "Back to Home" else "Finance")
                        }
                        TextButton(onClick = {
                            screen = if (screen == Screen.WEATHER) Screen.HOME else Screen.WEATHER
                        }) {
                            Text(if (screen == Screen.WEATHER) "Back to Home" else "Weather")
                        }
                        TextButton(onClick = {
                            screen = if (screen == Screen.TRANSLATE) Screen.HOME else Screen.TRANSLATE
                        }) {
                            Text(if (screen == Screen.TRANSLATE) "Back to Home" else "Translate")
                        }
                        TextButton(onClick = {
                            DeviceExplorationScheduler.triggerNow(context)
                            Toast.makeText(context, "Exploring device…", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Explore Device")
                        }
                        TextButton(onClick = {
                            screen = if (screen == Screen.CHAT) Screen.HOME else Screen.CHAT
                        }) {
                            Text(if (screen == Screen.CHAT) "Back to Home" else "Talk to AION")
                        }
                        // T-010 — manual toggle since VoiceSessionManager (T-015) doesn't exist yet
                        // to decide when the FGS should run on its own.
                        TextButton(onClick = {
                            voiceRunning = !voiceRunning
                            val intent = Intent(context, VoiceForegroundService::class.java)
                            if (voiceRunning) {
                                ContextCompat.startForegroundService(context, intent)
                            } else {
                                context.stopService(intent)
                            }
                        }) {
                            Text(if (voiceRunning) "Stop Voice" else "Start Voice")
                        }
                    }
                    when (screen) {
                        Screen.HOME ->
                            HomeScreen(
                                auditLogger,
                                secretVault,
                                voiceRunning,
                                onTapToSpeak = { screen = Screen.CHAT },
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
                    // screen shares. Wired to the 5 real screens closest to the mockup's own 5 icons
                    // rather than the mockup's literal (often fantasy) icon set — Files/Notifications
                    // don't have dedicated real screens yet, so this points at the closest genuine
                    // equivalents (Setup as a utility hub, Audit Log as the real activity feed).
                    AionBottomNav(
                        left =
                            listOf(
                                AionNavItem(Icons.Filled.Home, "Home", screen == Screen.HOME) { screen = Screen.HOME },
                                AionNavItem(
                                    Icons.Filled.Apps,
                                    "Apps",
                                    screen == Screen.SETUP,
                                ) { screen = Screen.SETUP },
                            ),
                        right =
                            listOf(
                                AionNavItem(
                                    Icons.Filled.Notifications,
                                    "Alerts",
                                    screen == Screen.AUDIT_LOG,
                                ) { screen = Screen.AUDIT_LOG },
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
