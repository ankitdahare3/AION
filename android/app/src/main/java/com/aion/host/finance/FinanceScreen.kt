package com.aion.host.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aion.host.communications.SmsReader
import com.aion.host.ui.theme.AionColors
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * T-157 (EPIC 17) — real Finance screen, sourced entirely from `SmsReader` (T-152) +
 * `SmsTransactionParser`. No bank API — the owner's own scoped choice, since real bank
 * integration needs account-aggregator licensing in India, not a free option. Reads more
 * messages than `CommunicationsScreen` (100 vs 20) since transactional SMS are often sparse
 * among a phone's recent messages (OTPs/promos dominate the most-recent slice).
 */
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

    val totalDebit = transactions.filter { it.direction == TransactionDirection.DEBIT }.sumOf { it.amount }
    val totalCredit = transactions.filter { it.direction == TransactionDirection.CREDIT }.sumOf { it.amount }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Finance", style = MaterialTheme.typography.headlineSmall, color = AionColors.OnBackground)
        if (transactions.isEmpty()) {
            Text(
                "No Swiggy/Amazon/salary SMS found, or SMS access hasn't been granted yet (Setup screen).",
                style = MaterialTheme.typography.bodyMedium,
                color = AionColors.OnSurfaceMuted,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
                Text(
                    "Spent: ${formatRupees(totalDebit)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = AionColors.Error,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Received: ${formatRupees(totalCredit)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = AionColors.Success,
                )
            }
            LazyColumn { items(transactions) { TransactionRow(it) } }
        }
    }
}

@Composable
private fun TransactionRow(txn: SmsTransaction) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(txn.merchant ?: txn.category.name, style = MaterialTheme.typography.bodyLarge, color = AionColors.OnBackground)
            Text(
                dateTimeFormat.format(Date(txn.timestampMs)),
                style = MaterialTheme.typography.bodySmall,
                color = AionColors.OnSurfaceMuted,
            )
        }
        Text(
            (if (txn.direction == TransactionDirection.DEBIT) "-" else "+") + formatRupees(txn.amount),
            style = MaterialTheme.typography.bodyLarge,
            color = if (txn.direction == TransactionDirection.DEBIT) AionColors.Error else AionColors.Success,
        )
    }
    HorizontalDivider()
}

private fun formatRupees(amount: Double): String = String.format(Locale.ROOT, "₹%,.2f", amount)

private val dateTimeFormat: DateFormat = DateFormat.getDateInstance(DateFormat.SHORT)
