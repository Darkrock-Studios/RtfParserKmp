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

package com.rtfparserkit.parser.standard

internal expect fun platformDecodeBytes(bytes: ByteArray, offset: Int, length: Int, charsetName: String): String

internal fun whatwgLabel(charsetName: String): String? = when (charsetName) {
    "MS932", "SJIS", "Shift_JIS" -> "shift_jis"
    "Cp936", "MS936" -> "gbk"
    "Cp949" -> "euc-kr"
    "Cp950" -> "big5"
    else -> null
}

internal fun decodeBytes(bytes: ByteArray, offset: Int, length: Int, charsetName: String): String {
    if (charsetName == "UTF-8") return bytes.decodeToString(offset, offset + length)

    val table = SINGLE_BYTE_TABLES[charsetName]
    if (table != null) {
        val sb = StringBuilder(length)
        for (i in offset until offset + length) {
            sb.append(table[bytes[i].toInt() and 0xFF])
        }
        return sb.toString()
    }

    return platformDecodeBytes(bytes, offset, length, charsetName)
}
