package com.aion.host.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** EPIC 16 (2026-07-13, owner-requested) — the dark/glowing-blue palette from the reference
 * mockup. Values are kept in sync with `res/values/colors.xml` (the native window
 * background/status bar uses the same hex values) rather than duplicated ad hoc. */
object AionColors {
    val Background = Color(0xFF0A0E1A)
    val Surface = Color(0xFF141824)
    val SurfaceVariant = Color(0xFF1C2130)
    val Primary = Color(0xFF4A9EFF)
    val OnPrimary = Color(0xFF0A0E1A)
    val OnBackground = Color(0xFFF5F6FA)
    val OnSurfaceMuted = Color(0xFF8B92A5)
    val Success = Color(0xFF34D399)
    val Error = Color(0xFFF87171)
    val Outline = Color(0xFF2A3040)
}

private val AionDarkColorScheme =
    darkColorScheme(
        primary = AionColors.Primary,
        onPrimary = AionColors.OnPrimary,
        background = AionColors.Background,
        onBackground = AionColors.OnBackground,
        surface = AionColors.Surface,
        onSurface = AionColors.OnBackground,
        surfaceVariant = AionColors.SurfaceVariant,
        onSurfaceVariant = AionColors.OnSurfaceMuted,
        error = AionColors.Error,
        outline = AionColors.Outline,
    )

@Composable
fun AionTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AionDarkColorScheme, content = content)
}
