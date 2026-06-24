/*
 * Copyright 2026 The RtfParserKmp contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.rtfparserkit.parser

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ByteArrayRtfSourceTest {
    @Test
    fun readsBytesAsUnsignedThenEof() {
        val source = ByteArrayRtfSource(byteArrayOf(0x41, 0x00, 0xFF.toByte()))
        assertEquals(0x41, source.read())
        assertEquals(0x00, source.read())
        assertEquals(0xFF, source.read())
        assertEquals(-1, source.read())
    }

    @Test
    fun unreadReturnsThePreviousByte() {
        val source = ByteArrayRtfSource(byteArrayOf(0x61, 0x62))
        assertEquals(0x61, source.read())
        source.unread(0x61)
        assertEquals(0x61, source.read())
        assertEquals(0x62, source.read())
    }

    @Test
    fun unreadAtStartFails() {
        val source = ByteArrayRtfSource(byteArrayOf(0x61))
        assertFailsWith<IllegalStateException> { source.unread(0x61) }
    }

    @Test
    fun bulkReadFillsBufferAndReportsCount() {
        val source = ByteArrayRtfSource(byteArrayOf(1, 2, 3, 4, 5))
        source.read() // consume the first byte
        val dest = ByteArray(3)
        assertEquals(3, source.read(dest))
        assertContentEquals(byteArrayOf(2, 3, 4), dest)
        assertEquals(5, source.read())
    }

    @Test
    fun bulkReadReturnsZeroAtEof() {
        val source = ByteArrayRtfSource(ByteArray(0))
        assertEquals(0, source.read(ByteArray(4)))
    }

    @Test
    fun bulkReadShorterThanRequestedReportsShortCount() {
        val source = ByteArrayRtfSource(byteArrayOf(1, 2, 3))
        source.read() // 2 bytes remain
        val dest = ByteArray(5)
        assertEquals(2, source.read(dest))
        assertContentEquals(byteArrayOf(2, 3, 0, 0, 0), dest)
    }
}
