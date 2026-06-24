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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFStringConvertEncodingToNSStringEncoding
import platform.CoreFoundation.CFStringConvertWindowsCodepageToEncoding
import platform.CoreFoundation.kCFStringEncodingInvalidId
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.dataWithBytes

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformDecodeBytes(bytes: ByteArray, offset: Int, length: Int, charsetName: String): String {
    val codepage = windowsCodepage(charsetName)
        ?: throw NotImplementedError("codepage $charsetName decoding not supported on this target")

    val cfEncoding = CFStringConvertWindowsCodepageToEncoding(codepage.convert())
    if (cfEncoding == kCFStringEncodingInvalidId) {
        throw NotImplementedError("codepage $charsetName has no Apple text encoding")
    }
    val nsEncoding = CFStringConvertEncodingToNSStringEncoding(cfEncoding)

    if (length == 0) return ""

    val decoded = bytes.usePinned { pinned ->
        val data = NSData.dataWithBytes(pinned.addressOf(offset), length.convert())
        NSString.create(data, nsEncoding)
    } ?: throw RtfParseException("Unable to decode $charsetName bytes")

    return decoded.toString()
}

private fun windowsCodepage(charsetName: String): Int? = when (charsetName) {
    "MS932", "SJIS", "Shift_JIS" -> 932
    "Cp936", "MS936" -> 936
    "Cp949" -> 949
    "Cp950" -> 950
    else -> null
}
