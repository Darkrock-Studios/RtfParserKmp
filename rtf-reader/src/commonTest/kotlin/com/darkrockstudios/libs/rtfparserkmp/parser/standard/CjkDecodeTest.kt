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

/**
 * Asserts vendor-stable multibyte CJK mappings where TextDecoder (js/wasmJs) and Java's
 * charsets (jvm/android) agree. Skips on targets where multibyte CJK is unsupported
 * (apple/linux/mingw/wasmWasi), so this asserts on jvm/android/js/wasmJs and skips elsewhere.
 */
class CjkDecodeTest {

    private fun decode(bytes: ByteArray, charsetName: String): String? =
        try {
            decodeBytes(bytes, 0, bytes.size, charsetName)
        } catch (e: NotImplementedError) {
            null
        }

    @Test
    fun shiftJisHiraganaA() {
        val actual = decode(byteArrayOf(0x82.toByte(), 0xA0.toByte()), "MS932") ?: return
        assertEquals("あ", actual)
    }

    @Test
    fun gbkZhong() {
        val actual = decode(byteArrayOf(0xD6.toByte(), 0xD0.toByte()), "Cp936") ?: return
        assertEquals("中", actual)
    }

    @Test
    fun big5Zhong() {
        val actual = decode(byteArrayOf(0xA4.toByte(), 0xA4.toByte()), "Cp950") ?: return
        assertEquals("中", actual)
    }

    @Test
    fun eucKrGa() {
        val actual = decode(byteArrayOf(0xB0.toByte(), 0xA1.toByte()), "Cp949") ?: return
        assertEquals("가", actual)
    }
}
