package com.aion.host.devicestatus

import org.junit.Assert.assertEquals
import org.junit.Test

class ByteFormatTest {
    @Test
    fun `formats whole gigabytes with one decimal place`() {
        assertEquals("8.0 GB", formatGb(8L * 1024 * 1024 * 1024))
    }

    @Test
    fun `rounds a fractional gigabyte value`() {
        assertEquals("1.5 GB", formatGb((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun `zero bytes formats as zero, not a crash or NaN`() {
        assertEquals("0.0 GB", formatGb(0L))
    }
}
