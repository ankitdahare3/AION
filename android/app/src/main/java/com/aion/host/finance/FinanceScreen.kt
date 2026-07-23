package com.aion.host.finance

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aion.host.communications.SmsReader
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.GlassPanel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinanceScreen(
    resumeSignal: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val smsReader = remember { SmsReader(context) }

    fun loadTransactions() = smsReader.recentMessages(limit = 100).mapNotNull(SmsTransactionParser::parse)

    var transactions by remember { mutableStateOf(loadTransactions()) }
    LaunchedEffect(resumeSignal) { transactions = loadTransactions() }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "alpha",
    )

    Box(modifier = modifier.fillMaxSize().background(AionColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(AionColors.Surface.copy(alpha = 0.6f))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = AionColors.OnSurfaceVariant)
                Text(
                    "AION",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AionColors.Glow,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                )
                Box {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = AionColors.OnSurfaceVariant,
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(
                                    8.dp,
                                ).background(AionColors.Primary, CircleShape)
                                .align(Alignment.TopEnd),
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 24.dp,
                        bottom = 100.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Title Section
                item {
                    Column {
                        Text(
                            "Finance Overview",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Text(
                                SimpleDateFormat("EEEE h:mm a", Locale.getDefault()).format(Date()),
                                style = MaterialTheme.typography.bodySmall,
                                color = AionColors.OnSurfaceVariant,
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier =
                                    Modifier
                                        .size(
                                            6.dp,
                                        ).background(AionColors.Glow.copy(alpha = alpha), CircleShape),
                            )
                        }
                    }
                }

                // Net Worth Card
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                        Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                            // Blob
                            Box(
                                modifier =
                                    Modifier
                                        .size(120.dp)
                                        .align(Alignment.TopEnd)
                                        .background(
                                            Brush.radialGradient(
                                                colors =
                                                    listOf(
                                                        AionColors.Primary.copy(alpha = 0.1f),
                                                        Color.Transparent,
                                                    ),
                                            ),
                                        ),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Column {
                                    Text(
                                        "NET WORTH",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AionColors.OnSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                    )
                                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
                                        Text(
                                            "₹",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = AionColors.Glow,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "24,58,620",
                                            style = MaterialTheme.typography.displaySmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 12.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = Color(0xFF00e676),
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "+1,25,430 (5.37%) vs last month",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF00e676),
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                                // Sparkline
                                Canvas(modifier = Modifier.size(80.dp, 40.dp)) {
                                    val path =
                                        Path().apply {
                                            moveTo(0f, size.height * 0.8f)
                                            quadraticBezierTo(
                                                size.width * 0.2f,
                                                size.height * 0.9f,
                                                size.width * 0.3f,
                                                size.height * 0.6f,
                                            )
                                            quadraticBezierTo(
                                                size.width * 0.5f,
                                                size.height * 0.5f,
                                                size.width * 0.5f,
                                                size.height * 0.5f,
                                            )
                                            quadraticBezierTo(
                                                size.width * 0.7f,
                                                size.height * 0.7f,
                                                size.width * 0.7f,
                                                size.height * 0.7f,
                                            )
                                            quadraticBezierTo(
                                                size.width * 1f,
                                                size.height * 0.1f,
                                                size.width * 1f,
                                                size.height * 0.1f,
                                            )
                                        }
                                    drawPath(path, color = Color(0xFF00e676), style = Stroke(width = 3.dp.toPx()))
                                }
                            }
                        }
                    }
                }

                // Accounts Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Accounts", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Text(
                                "VIEW ALL",
                                style = MaterialTheme.typography.labelSmall,
                                color = AionColors.Primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        AccountItem(Icons.Filled.AccountBalance, "Bank Balance", "₹ 8,32,620", AionColors.Primary)
                        AccountItem(Icons.Filled.Analytics, "Investments", "₹ 12,85,400", AionColors.Tertiary)
                        AccountItem(Icons.Filled.Payments, "Cash", "₹ 1,40,600", AionColors.Outline)
                    }
                }

                // Recent Transactions
                item {
                    Column {
                        Text(
                            "Recent Transactions",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )

                        GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (transactions.isEmpty()) {
                                    // Mock Transactions from Design
                                    MockTransaction(
                                        Icons.Filled.Restaurant,
                                        Color(0xFFffa726),
                                        "Swiggy",
                                        "Today",
                                        "- ₹ 486",
                                        false,
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    MockTransaction(
                                        Icons.Filled.ShoppingBag,
                                        Color(0xFFfdd835),
                                        "Amazon",
                                        "Yesterday",
                                        "- ₹ 1,299",
                                        false,
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    MockTransaction(
                                        Icons.Filled.Wallet,
                                        Color(0xFF66bb6a),
                                        "Salary",
                                        "16 May",
                                        "+ ₹ 75,000",
                                        true,
                                    )
                                } else {
                                    transactions.take(5).forEachIndexed { index, txn ->
                                        RealTransactionRow(txn)
                                        if (index < transactions.lastIndex && index < 4) {
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Detailed Report Button
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(AionColors.Primary, RoundedCornerShape(12.dp))
                        .clickable { }
                        .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Description, contentDescription = null, tint = AionColors.OnPrimary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Show Detailed Report",
                    style = MaterialTheme.typography.labelLarge,
                    color = AionColors.OnPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AccountItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    amount: String,
    color: Color,
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 8.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = color.copy(alpha = 0.4f),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 8.dp.toPx(),
                        )
                    }.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .background(AionColors.SurfaceContainerHighest, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodySmall, color = AionColors.OnSurfaceVariant)
                    Text(
                        amount,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AionColors.Outline)
        }
    }
}

@Composable
private fun MockTransaction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    date: String,
    amount: String,
    isPositive: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
                Text(date, style = MaterialTheme.typography.bodySmall, color = AionColors.OnSurfaceVariant)
            }
        }
        Text(
            amount,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isPositive) Color(0xFF00e676) else AionColors.Error,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RealTransactionRow(txn: SmsTransaction) {
    val isPositive = txn.direction == TransactionDirection.CREDIT
    val icon = if (isPositive) Icons.Filled.Wallet else Icons.Filled.ShoppingBag
    val color = if (isPositive) Color(0xFF66bb6a) else Color(0xFFfdd835)

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    txn.merchant ?: txn.category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    DateFormat.getDateInstance(DateFormat.SHORT).format(Date(txn.timestampMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = AionColors.OnSurfaceVariant,
                )
            }
        }
        Text(
            (if (!isPositive) "-" else "+") + String.format(Locale.ROOT, " ₹%,.2f", txn.amount),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isPositive) Color(0xFF00e676) else AionColors.Error,
            fontWeight = FontWeight.Bold,
        )
    }
}
