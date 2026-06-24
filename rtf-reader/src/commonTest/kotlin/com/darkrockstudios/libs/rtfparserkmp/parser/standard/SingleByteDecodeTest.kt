/*
 * Copyright 2026 The RtfParserKmp contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.darkrockstudios.libs.rtfparserkmp.parser.standard

import kotlin.test.Test
import kotlin.test.assertEquals

class SingleByteDecodeTest {

    private fun decodeOne(byteValue: Int, charsetName: String): Char {
        val s = decodeBytes(byteArrayOf(byteValue.toByte()), 0, 1, charsetName)
        assertEquals(1, s.length, "expected exactly one char for 0x${byteValue.toString(16)} in $charsetName")
        return s[0]
    }

    @Test
    fun cp1252EuroAndQuotes() {
        assertEquals('€', decodeOne(0x80, "Cp1252"))
        assertEquals('“', decodeOne(0x93, "Cp1252"))
        assertEquals('”', decodeOne(0x94, "Cp1252"))
        assertEquals('A', decodeOne(0x41, "Cp1252"))
    }

    @Test
    fun cp437FullBlock() {
        assertEquals('█', decodeOne(0xDB, "Cp437"))
    }

    @Test
    fun macRomanBullet() {
        assertEquals('•', decodeOne(0xA5, "MacRoman"))
    }

    @Test
    fun macCyrillicCapitalA() {
        assertEquals('А', decodeOne(0x80, "x-MacCyrillic"))
    }

    @Test
    fun decodesAcrossOffsetAndLength() {
        val bytes = byteArrayOf(0x00, 0x41.toByte(), 0x80.toByte(), 0x42.toByte(), 0x00)
        assertEquals("A€B", decodeBytes(bytes, 1, 3, "Cp1252"))
    }
}
