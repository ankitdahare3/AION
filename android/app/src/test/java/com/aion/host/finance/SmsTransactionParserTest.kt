package com.aion.host.finance

import com.aion.host.communications.SmsItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsTransactionParserTest {
    private fun sms(body: String) = SmsItem(address = "HDFCBK", body = body, timestampMs = 1_000L)

    @Test
    fun `swiggy debit is parsed as FOOD`() {
        val result = SmsTransactionParser.parse(sms("Rs.450.00 debited from A/c XX1234 for UPI/SWIGGY on 12-07-26. Avl Bal Rs.12,340.00"))

        assertEquals(450.00, result?.amount)
        assertEquals(TransactionDirection.DEBIT, result?.direction)
        assertEquals(TransactionCategory.FOOD, result?.category)
        assertEquals("Swiggy", result?.merchant)
    }

    @Test
    fun `zomato debit is parsed as FOOD`() {
        val result = SmsTransactionParser.parse(sms("Rs 320 spent at ZOMATO on your card ending 4321"))

        assertEquals(TransactionCategory.FOOD, result?.category)
        assertEquals("Zomato", result?.merchant)
    }

    @Test
    fun `amazon debit is parsed as SHOPPING`() {
        val result = SmsTransactionParser.parse(sms("Rs 799 spent on your card at AMAZON on 10-07-26"))

        assertEquals(799.0, result?.amount)
        assertEquals(TransactionDirection.DEBIT, result?.direction)
        assertEquals(TransactionCategory.SHOPPING, result?.category)
        assertEquals("Amazon", result?.merchant)
    }

    @Test
    fun `flipkart debit is parsed as SHOPPING`() {
        val result = SmsTransactionParser.parse(sms("INR 1,299.00 debited for FLIPKART order"))

        assertEquals(1299.0, result?.amount)
        assertEquals(TransactionCategory.SHOPPING, result?.category)
        assertEquals("Flipkart", result?.merchant)
    }

    @Test
    fun `salary credit is parsed as SALARY`() {
        val result = SmsTransactionParser.parse(sms("Your A/c XX5678 credited with Rs.45,000.00 towards SALARY CREDIT"))

        assertEquals(45000.0, result?.amount)
        assertEquals(TransactionDirection.CREDIT, result?.direction)
        assertEquals(TransactionCategory.SALARY, result?.category)
        assertEquals("Salary", result?.merchant)
    }

    @Test
    fun `case-insensitive and comma-separated amount still parses`() {
        val result = SmsTransactionParser.parse(sms("RS.12,340.00 DEBITED FROM YOUR ACCOUNT"))

        assertEquals(12340.0, result?.amount)
        assertEquals(TransactionDirection.DEBIT, result?.direction)
    }

    @Test
    fun `promotional sms with an amount but no debit-credit keyword is not a transaction`() {
        val result = SmsTransactionParser.parse(sms("Your OTP is 123456. Rs 500 cashback awaits on your next order!"))

        assertNull(result)
    }

    @Test
    fun `sms with no amount at all is not a transaction`() {
        val result = SmsTransactionParser.parse(sms("Your account statement is ready to view online"))

        assertNull(result)
    }

    @Test
    fun `unrecognized merchant still parses as OTHER with no merchant name`() {
        val result = SmsTransactionParser.parse(sms("Rs 200 debited for bill payment"))

        assertEquals(TransactionCategory.OTHER, result?.category)
        assertNull(result?.merchant)
    }
}
