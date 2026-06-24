/*
 * Copyright 2013 Jon Iles
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

package com.darkrockstudios.libs.rtfparserkmp.utils

/**
 * Utilities for working with the hex numbers used by the `\'xx` RTF escape.
 */
object HexUtils {
    /** Parse a single hex digit, given as a byte value (0..255), returning its value 0..15. */
    fun parseHexDigit(ch: Int): Int =
        ch.toChar().digitToIntOrNull(radix = 16)
            ?: throw IllegalArgumentException("Invalid hex digit $ch")

    /** Convert a string of hex digits into an array of bytes. */
    fun parseHexString(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Invalid hex string" }
        return ByteArray(hex.length / 2) { byteIndex ->
            val stringIndex = byteIndex * 2
            val value = (parseHexDigit(hex[stringIndex].code) shl 4) + parseHexDigit(hex[stringIndex + 1].code)
            value.toByte()
        }
    }
}
