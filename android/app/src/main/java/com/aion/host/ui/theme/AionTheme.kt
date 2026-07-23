package com.aion.host.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.aion.host.R

/**
 * T-168 (2026-07-21, owner-requested) — replaces EPIC 16's approximated palette with the exact
 * Material 3 tokens extracted from the owner's real Stitch mockup export
 * (`stitch_aion_ai_assistant.zip`) — every screen's `tailwind.config` color block was diffed
 * across the whole ~45-screen export and found consistent (same hex per token everywhere it
 * appears), so these are real, not guessed. [Glow] and the three status colors are Stitch's own
 * custom tokens layered on top of the Material 3 set (not part of `ColorScheme` itself) — the
 * cyan glow border/orb effect every screen uses, and the three semantic alert colors.
 */
object AionColors {
    val Primary = Color(0xFFA8E8FF)
    val OnPrimary = Color(0xFF003642)
    val PrimaryContainer = Color(0xFF00D4FF)
    val OnPrimaryContainer = Color(0xFF00586B)
    val InversePrimary = Color(0xFF00677E)
    val PrimaryFixed = Color(0xFFB4EBFF)
    val OnPrimaryFixed = Color(0xFF001F27)
    val OnPrimaryFixedVariant = Color(0xFF004E5F)

    /** The signature cyan glow accent (`primary-fixed-dim` in every Stitch screen) — borders, the mic orb, active-state rings. */
    val Glow = Color(0xFF3CD7FF)

    val Secondary = Color(0xFFD1BCFF)
    val OnSecondary = Color(0xFF3C0090)
    val SecondaryContainer = Color(0xFF7000FF)
    val OnSecondaryContainer = Color(0xFFDDCDFF)
    val SecondaryFixed = Color(0xFFE9DDFF)
    val OnSecondaryFixed = Color(0xFF23005B)
    val OnSecondaryFixedVariant = Color(0xFF5700C9)

    val Tertiary = Color(0xFFFFD9A1)
    val OnTertiary = Color(0xFF432C00)
    val TertiaryContainer = Color(0xFFFEB528)
    val OnTertiaryContainer = Color(0xFF6C4900)
    val TertiaryFixed = Color(0xFFFFDEAE)
    val TertiaryFixedDim = Color(0xFFFFBA3D)
    val OnTertiaryFixed = Color(0xFF281900)
    val OnTertiaryFixedVariant = Color(0xFF604100)

    val Background = Color(0xFF0E1417)
    val OnBackground = Color(0xFFDDE3E7)
    val Surface = Color(0xFF0E1417)
    val OnSurface = Color(0xFFDDE3E7)
    val SurfaceVariant = Color(0xFF2F3639)
    val OnSurfaceVariant = Color(0xFFBBC9CF)
    val SurfaceTint = Color(0xFF3CD7FF)
    val SurfaceDim = Color(0xFF0E1417)
    val SurfaceBright = Color(0xFF333A3D)
    val SurfaceContainerLowest = Color(0xFF080F12)
    val SurfaceContainerLow = Color(0xFF161D1F)
    val SurfaceContainer = Color(0xFF1A2123)
    val SurfaceContainerHigh = Color(0xFF242B2E)
    val SurfaceContainerHighest = Color(0xFF2F3639)
    val InverseSurface = Color(0xFFDDE3E7)
    val InverseOnSurface = Color(0xFF2B3134)

    val Outline = Color(0xFF859398)
    val OutlineVariant = Color(0xFF3C494E)

    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)

    // Semantic status colors — used for battery/health/security-good, warnings, and destructive
    // states, distinct from the Material-role error/tertiary colors above.
    val SecurityGreen = Color(0xFF00FF9D)
    val AlertOrange = Color(0xFFFF8C00)
    val AlertRed = Color(0xFFFF3B3B)
}

// ── Google Fonts provider ────────────────────────────────────────────────
// Downloads Space Grotesk at runtime; the fallback is the default system sans-serif (no crash
// if offline/Play Services unavailable). The provider cert fingerprint array is shipped inside
// the google-fonts artifact itself (R.array.com_google_android_gms_fonts_certs).
private val GoogleFontProvider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

private val SpaceGroteskFont = GoogleFont("Space Grotesk")

val SpaceGroteskFamily =
    FontFamily(
        Font(googleFont = SpaceGroteskFont, fontProvider = GoogleFontProvider, weight = FontWeight.Light),
        Font(googleFont = SpaceGroteskFont, fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
        Font(googleFont = SpaceGroteskFont, fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
        Font(googleFont = SpaceGroteskFont, fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = SpaceGroteskFont, fontProvider = GoogleFontProvider, weight = FontWeight.Bold),
    )

// ── Typography — exact Stitch "Aetheric Interface" specs ────────────────
// display-lg:          48px / 700 / 1.1 / -0.02em
// display-lg-mobile:   32px / 700 / 1.2 / -0.02em
// headline-md:         24px / 500 / 1.4 /  0.01em
// body-lg:             18px / 400 / 1.6 /  0em
// body-sm:             14px / 400 / 1.5 /  0.02em
// label-caps:          12px / 600 / 1.2 /  0.1em
private val AionTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                lineHeight = (48 * 1.1).sp,
                letterSpacing = (-0.02).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = (32 * 1.2).sp,
                letterSpacing = (-0.02).sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = (28 * 1.2).sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = (32 * 1.2).sp,
                letterSpacing = (-0.02).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                lineHeight = (24 * 1.4).sp,
                letterSpacing = 0.01.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                lineHeight = (20 * 1.4).sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = (22 * 1.3).sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = (18 * 1.4).sp,
                letterSpacing = 0.01.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = (14 * 1.4).sp,
                letterSpacing = 0.01.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = (18 * 1.6).sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = (14 * 1.5).sp,
                letterSpacing = 0.02.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = (12 * 1.5).sp,
                letterSpacing = 0.02.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = (14 * 1.2).sp,
                letterSpacing = 0.04.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = (12 * 1.2).sp,
                letterSpacing = 0.1.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = (10 * 1.2).sp,
                letterSpacing = 0.06.sp,
            ),
    )

// The Material 3 "Fixed"/"FixedDim" color roles (primaryFixed, primaryFixedDim, etc.) that Stitch's
// own token set includes aren't parameters on this project's resolved ColorScheme.darkColorScheme()
// overload — kept as plain AionColors constants instead (AionColors.Glow is Stitch's own
// `primary-fixed-dim`, the cyan glow accent) and referenced directly wherever a screen needs them,
// same as every other non-ColorScheme Stitch token (SecurityGreen/AlertOrange/AlertRed) already is.
private val AionDarkColorScheme =
    darkColorScheme(
        primary = AionColors.Primary,
        onPrimary = AionColors.OnPrimary,
        primaryContainer = AionColors.PrimaryContainer,
        onPrimaryContainer = AionColors.OnPrimaryContainer,
        inversePrimary = AionColors.InversePrimary,
        secondary = AionColors.Secondary,
        onSecondary = AionColors.OnSecondary,
        secondaryContainer = AionColors.SecondaryContainer,
        onSecondaryContainer = AionColors.OnSecondaryContainer,
        tertiary = AionColors.Tertiary,
        onTertiary = AionColors.OnTertiary,
        tertiaryContainer = AionColors.TertiaryContainer,
        onTertiaryContainer = AionColors.OnTertiaryContainer,
        background = AionColors.Background,
        onBackground = AionColors.OnBackground,
        surface = AionColors.Surface,
        onSurface = AionColors.OnSurface,
        surfaceVariant = AionColors.SurfaceVariant,
        onSurfaceVariant = AionColors.OnSurfaceVariant,
        surfaceTint = AionColors.SurfaceTint,
        surfaceDim = AionColors.SurfaceDim,
        surfaceBright = AionColors.SurfaceBright,
        surfaceContainerLowest = AionColors.SurfaceContainerLowest,
        surfaceContainerLow = AionColors.SurfaceContainerLow,
        surfaceContainer = AionColors.SurfaceContainer,
        surfaceContainerHigh = AionColors.SurfaceContainerHigh,
        surfaceContainerHighest = AionColors.SurfaceContainerHighest,
        inverseSurface = AionColors.InverseSurface,
        inverseOnSurface = AionColors.InverseOnSurface,
        outline = AionColors.Outline,
        outlineVariant = AionColors.OutlineVariant,
        error = AionColors.Error,
        onError = AionColors.OnError,
        errorContainer = AionColors.ErrorContainer,
        onErrorContainer = AionColors.OnErrorContainer,
    )

@Composable
fun AionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AionDarkColorScheme,
        typography = AionTypography,
        content = content,
    )
}
