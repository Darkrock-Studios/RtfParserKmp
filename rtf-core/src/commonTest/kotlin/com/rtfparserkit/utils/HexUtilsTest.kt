/*
 * Copyright 2026 The RtfParserKmp contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.rtfparserkit.utils

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HexUtilsTest {
    @Test
    fun parsesEachHexDigit() {
        assertEquals(0, HexUtils.parseHexDigit('0'.code))
        assertEquals(9, HexUtils.parseHexDigit('9'.code))
        assertEquals(10, HexUtils.parseHexDigit('a'.code))
        assertEquals(10, HexUtils.parseHexDigit('A'.code))
        assertEquals(15, HexUtils.parseHexDigit('f'.code))
        assertEquals(15, HexUtils.parseHexDigit('F'.code))
    }

    @Test
    fun rejectsInvalidHexDigit() {
        assertFailsWith<IllegalArgumentException> { HexUtils.parseHexDigit('g'.code) }
        assertFailsWith<IllegalArgumentException> { HexUtils.parseHexDigit(' '.code) }
        assertFailsWith<IllegalArgumentException> { HexUtils.parseHexDigit(200) }
    }

    @Test
    fun parsesHexStringToBytes() {
        assertContentEquals(byteArrayOf(0x00, 0x1f, 0xab.toByte(), 0xff.toByte()), HexUtils.parseHexString("001fABff"))
    }

    @Test
    fun rejectsOddLengthHexString() {
        assertFailsWith<IllegalArgumentException> { HexUtils.parseHexString("abc") }
    }
}
