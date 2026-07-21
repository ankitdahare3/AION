package com.aion.host.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aion.host.security.ProviderKey
import com.aion.host.security.SecretVault
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.StatCard

/**
 * Mockup "About AION" screen — every line here reads a real source (installed APK's own
 * versionName via [android.content.pm.PackageManager], [pluginCount] from the same
 * `BuiltInPluginRegistry` init block that actually registers them, configured-provider count from
 * [SecretVault]) rather than a static "v2.4.1"-style placeholder.
 */
@Composable
fun AboutScreen(
    secretVault: SecretVault,
    pluginCount: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val versionName =
        remember {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        }
    val configuredProviders = ProviderKey.entries.count { secretVault.has(it) }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("About AION", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)
        Text(
            "A personal, voice-first Android AI agent — open-source, on-device where it can be, honest about what it can't do yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = AionColors.OnSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        StatCard("Version", versionName, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        StatCard("Plugins registered", "$pluginCount", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        StatCard("Providers configured", "$configuredProviders", modifier = Modifier.fillMaxWidth())
    }
}
