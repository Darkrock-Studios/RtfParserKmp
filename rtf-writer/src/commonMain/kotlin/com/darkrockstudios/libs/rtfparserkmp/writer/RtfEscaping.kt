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

package com.darkrockstudios.libs.rtfparserkmp.writer

/**
 * Appends [text] to the builder as 7-bit ASCII RTF body text: the structural characters `\ { }` are
 * backslash-escaped, tab and newline become control words, a carriage return is dropped, and every
 * code unit above 127 (or a stray control character) is emitted as a signed `\uN` escape with a `?`
 * fallback so the output stays charset-free and portable.
 */
internal fun StringBuilder.appendRtfEscaped(text: String) {
    for (ch in text) {
        val code = ch.code
        when {
            ch == '\\' -> append("\\\\")
            ch == '{' -> append("\\{")
            ch == '}' -> append("\\}")
            ch == '\t' -> append("\\tab ")
            ch == '\n' -> append("\\line ")
            ch == '\r' -> {}
            code > 127 -> {
                val signed = if (code > 32767) code - 65536 else code
                append("\\u").append(signed).append("?")
            }

            code < 32 -> append("\\u").append(code).append("?")
            else -> append(ch)
        }
    }
}

/**
 * Appends a HYPERLINK field argument (the quoted URL / bookmark name). On top of [appendRtfEscaped]'s
 * rules, the double-quote and backslash are escaped for the field-code layer as `\"` and `\\` so a
 * quote inside the target cannot prematurely close the field's quoted argument. Each field-level
 * backslash is then itself RTF-escaped, so one source `"` reaches the stream as `\\"` and one source
 * `\` as `\\\\`. Targets without `"` or `\` are emitted identically to [appendRtfEscaped].
 */
internal fun StringBuilder.appendRtfHyperlinkTarget(target: String) {
    for (ch in target) {
        val code = ch.code
        when {
            ch == '"' -> append("\\\\\"")
            ch == '\\' -> append("\\\\\\\\")
            ch == '{' -> append("\\{")
            ch == '}' -> append("\\}")
            ch == '\t' -> append("\\tab ")
            ch == '\n' -> append("\\line ")
            ch == '\r' -> {}
            code > 127 -> {
                val signed = if (code > 32767) code - 65536 else code
                append("\\u").append(signed).append("?")
            }

            code < 32 -> append("\\u").append(code).append("?")
            else -> append(ch)
        }
    }
}
