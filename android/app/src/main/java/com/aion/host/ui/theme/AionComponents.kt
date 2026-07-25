package com.aion.host.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * T-168 — the shared building blocks every Stitch-mockup screen reuses (glass panel, cyan glow
 * border/orb, the bottom nav bar with its elevated mic button). Built once here so ~45 individual
 * screens don't each hand-roll their own version — the whole point of a design system.
 *
 * Real backdrop blur (Stitch's CSS `backdrop-filter: blur(20px)`, i.e. blurring whatever is
 * BEHIND a panel) isn't a native Compose primitive the way it is in CSS — approximated instead
 * with a semi-transparent surface-container fill + a subtle light border, the conventional
 * Compose "glassmorphism" technique, which reads the same against this app's mostly-dark
 * backgrounds. True per-pixel backdrop blur would need a RenderNode/RenderEffect capture-and-blur
 * pass per panel — real added complexity for a visual difference that's marginal here; revisit
 * only if a specific screen's background content makes the difference visible.
 */

private val GlassBorderColor = Color.White.copy(alpha = 0.12f)

/** Stitch's `.glass-panel` class: surface-container at 60% opacity + a faint light border. Add [glow] for `.cyan-glow-border`. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    glow: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier =
            modifier
                .then(if (glow) Modifier.glowShadow(shape) else Modifier)
                .clip(shape)
                .background(AionColors.SurfaceContainer.copy(alpha = 0.6f))
                .border(1.dp, if (glow) AionColors.Glow else GlassBorderColor, shape),
    ) {
        content()
    }
}

/** Stitch's `.cyan-glow-border` outer shadow half — the border stroke itself is applied by the caller (see [GlassPanel]). */
fun Modifier.glowShadow(
    shape: Shape,
    color: Color = AionColors.Glow,
): Modifier =
    this.shadow(
        elevation = 8.dp,
        shape = shape,
        ambientColor = color.copy(alpha = 0.5f),
        spotColor = color.copy(alpha = 0.5f),
    )

/** One nav-bar destination. [selected] renders it in the glowing cyan active state. */
data class AionNavItem(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * The 5-slot bottom bar every Stitch screen shares: two nav items, an elevated glowing mic orb in
 * the center (always navigates to Chat — AION's real "give it a goal" entry point), two more nav
 * items. Matches every mockup screen's own footer nav exactly (Home / Apps / [orb] / Alerts /
 * Profile), just wired to this app's real screens instead of static mockup icons.
 */
@Composable
fun AionBottomNav(
    left: List<AionNavItem>,
    right: List<AionNavItem>,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    Box(
        modifier = modifier.fillMaxWidth().height(80.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(barShape)
                    .background(AionColors.Surface.copy(alpha = 0.85f))
                    .border(1.dp, GlassBorderColor, barShape),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                left.forEach { NavIcon(it, Modifier.weight(1f)) }
                Spacer(Modifier.width(56.dp))
                right.forEach { NavIcon(it, Modifier.weight(1f)) }
            }
        }
        Box(modifier = Modifier.align(Alignment.TopCenter).offset(y = (-16).dp)) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            ambientColor = AionColors.PrimaryContainer,
                            spotColor = AionColors.PrimaryContainer,
                        ).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AionColors.PrimaryContainer, AionColors.Primary)))
                        .clickable(onClick = onMicClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "Talk to AION", tint = AionColors.OnPrimary)
            }
        }
    }
}

@Composable
private fun NavIcon(
    item: AionNavItem,
    modifier: Modifier = Modifier,
) {
    val tint = if (item.selected) AionColors.Glow else AionColors.Outline
    Box(modifier = modifier.clickable(onClick = item.onClick), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(item.icon, contentDescription = item.label, tint = tint)
            Text(item.label, style = MaterialTheme.typography.labelSmall, color = tint)
        }
    }
}

/**
 * A titled row with a horizontal progress bar (Stitch's "Research Agent 92%"-style rows —
 * Agents Dashboard, Skills, Performance Monitor). [progress] is 0f..1f.
 */
@Composable
fun ProgressRow(
    label: String,
    progress: Float,
    modifier: Modifier = Modifier,
    trailingText: String = "${(progress * 100).toInt()}%",
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = AionColors.OnBackground)
            Text(trailingText, style = MaterialTheme.typography.bodyMedium, color = AionColors.Glow)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AionColors.SurfaceVariant),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AionColors.Glow),
            )
        }
    }
}

/**
 * A screen built only to visually match a Stitch mockup for a feature AION doesn't actually have
 * yet (no real backend: multi-agent orchestration, smart-home control, health tracking, etc.).
 * [note] is the one honest line every such screen carries, same spirit as `ProactiveSuggestions`'
 * "no fake Remind-me button" rule — this doesn't fabricate a REAL action or real device state, it
 * just shows what the design looks like.
 */
@Composable
fun IllustrativeScreen(
    title: String,
    note: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)
        Text(
            note,
            style = MaterialTheme.typography.bodySmall,
            color = AionColors.OnSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        content()
    }
}

/** A glass-panel stat display: a big value with a small label above it. */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = AionColors.OnBackground,
) {
    GlassPanel(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = AionColors.OnSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = valueColor)
        }
    }
}

/** Every Stitch screen's header: the `AION` wordmark, an optional back arrow, an optional trailing action icon. */
@Composable
fun AionTopBar(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
    // Additive slot for screens needing more than the one trailingIcon (e.g. ChatScreen's
    // New Chat + History icons alongside its existing mute toggle) — every other caller leaves
    // this null and is unaffected.
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = AionColors.OnBackground,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Spacer(Modifier.width(16.dp))
        }
        Text(
            title ?: "AION",
            style = MaterialTheme.typography.titleLarge,
            color = if (title == null) AionColors.Primary else AionColors.OnBackground,
            modifier = Modifier.weight(1f),
        )
        if (trailingIcon != null) {
            Icon(
                trailingIcon,
                contentDescription = null,
                tint = AionColors.OnSurfaceVariant,
                modifier = Modifier.clickable(onClick = onTrailingClick ?: {}),
            )
        }
        trailingContent?.invoke(this)
    }
}

/**
 * The shared page shell every Stitch-mockup screen uses: [AionTopBar] + scrollable content +
 * [AionBottomNav]. Screens supply just their own middle content — the header/footer chrome (the
 * whole point of importing this as a design system rather than 45 one-off layouts) lives here once.
 */
@Composable
fun MockupScaffold(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().background(AionColors.Background)) {
        AionTopBar(title, onBack, trailingIcon, onTrailingClick)
        Column(
            modifier =
                Modifier
                    .weight(
                        1f,
                    ).fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            content = content,
        )
    }
}
