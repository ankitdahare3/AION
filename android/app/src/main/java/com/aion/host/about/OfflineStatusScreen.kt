package com.aion.host.about

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel

/**
 * Mockup "Offline Mode" screen — the one real, checkable fact here is whether this device
 * currently has internet connectivity at all ([ConnectivityManager], a live check, not cached).
 * Deliberately does NOT show a fake "local model active" state: no on-device LLM adapter exists
 * in this codebase yet (`local-llamacpp` was removed from providers.yaml — see BACKLOG.md — since
 * nothing implements it), so every real goal genuinely needs a network round-trip today.
 */
@Composable
fun OfflineStatusScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val online = remember { hasInternetConnectivity(context) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Connectivity", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)
        GlassPanel(modifier = Modifier.padding(top = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (online) "Online" else "Offline",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (online) AionColors.SecurityGreen else AionColors.Error,
                )
                Text(
                    if (online) {
                        "AION can reach its cloud providers for planning and chat."
                    } else {
                        "No internet detected — cloud planning and chat won't work right now. Calendar, " +
                            "Communications, Device Status, and App Usage still read real on-device data."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AionColors.OnSurfaceVariant,
                )
            }
        }
    }
}

private fun hasInternetConnectivity(context: Context): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
