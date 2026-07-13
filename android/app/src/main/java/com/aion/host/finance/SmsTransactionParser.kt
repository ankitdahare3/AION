package com.aion.host.finance

import com.aion.host.communications.SmsItem

enum class TransactionDirection { DEBIT, CREDIT }
enum class TransactionCategory { FOOD, SHOPPING, SALARY, OTHER }

data class SmsTransaction(
    val amount: Double,
    val direction: TransactionDirection,
    val category: TransactionCategory,
    val merchant: String?,
    val timestampMs: Long,
    val rawBody: String,
)

/**
 * T-157 (EPIC 17) — the owner's own scoped choice for "Finance": parse Swiggy/Amazon/salary SMS
 * rather than a real bank API (not free, needs licensing in India). Pure text logic (no Android
 * import), so it's unit-testable without a device. Deliberately conservative: only returns a
 * transaction when BOTH a real amount AND a debit/credit keyword are found — this is what filters
 * out OTPs/promotional SMS naturally, without needing an explicit sender-ID allowlist (SMS sender
 * IDs vary too much across banks to hardcode reliably).
 */
object SmsTransactionParser {
    private val AMOUNT_REGEX = Regex("""(?:rs\.?|inr)\s?([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val DEBIT_KEYWORDS = listOf("debited", "debit", "spent", "paid", "withdrawn")
    private val CREDIT_KEYWORDS = listOf("credited", "credit", "received", "deposited")

    fun parse(sms: SmsItem): SmsTransaction? {
        val lower = sms.body.lowercase()

        val amount = AMOUNT_REGEX.find(lower)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null

        val isCredit = CREDIT_KEYWORDS.any { lower.contains(it) }
        val isDebit = DEBIT_KEYWORDS.any { lower.contains(it) }
        val direction =
            when {
                isCredit && !isDebit -> TransactionDirection.CREDIT
                isDebit -> TransactionDirection.DEBIT
                else -> return null
            }

        val category =
            when {
                lower.contains("swiggy") || lower.contains("zomato") -> TransactionCategory.FOOD
                lower.contains("amazon") || lower.contains("flipkart") -> TransactionCategory.SHOPPING
                lower.contains("salary") -> TransactionCategory.SALARY
                else -> TransactionCategory.OTHER
            }

        val merchant =
            when (category) {
                TransactionCategory.FOOD -> if (lower.contains("swiggy")) "Swiggy" else "Zomato"
                TransactionCategory.SHOPPING -> if (lower.contains("amazon")) "Amazon" else "Flipkart"
                TransactionCategory.SALARY -> "Salary"
                TransactionCategory.OTHER -> null
            }

        return SmsTransaction(amount, direction, category, merchant, sms.timestampMs, sms.body)
    }
}
