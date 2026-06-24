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

import com.darkrockstudios.libs.rtfparserkmp.model.CharacterStyle
import com.darkrockstudios.libs.rtfparserkmp.model.Paragraph
import com.darkrockstudios.libs.rtfparserkmp.model.StyledDocument
import com.darkrockstudios.libs.rtfparserkmp.model.TextRun

/**
 * Serializes a [StyledDocument] to a charset-free RTF string.
 *
 * Output is 7-bit ASCII: every code unit above 127 is emitted as a signed `\uN` escape with a `?`
 * fallback. The body opens each paragraph with `\plain`, groups each run in its own `{...}` so
 * character formatting cannot leak between runs, and separates paragraphs with `\par`. No trailing
 * `\par` follows the last paragraph.
 */
class RtfWriter {
    fun write(document: StyledDocument): String {
        val sb = StringBuilder()
        sb.append("{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0 Times New Roman;}}")
        document.paragraphs.forEachIndexed { index, paragraph ->
            if (index > 0) sb.append("\\par ")
            appendParagraph(sb, paragraph)
        }
        sb.append("}")
        return sb.toString()
    }

    private fun appendParagraph(sb: StringBuilder, paragraph: Paragraph) {
        sb.append("\\plain ")
        for (run in paragraph.runs) {
            appendRun(sb, run)
        }
    }

    private fun appendRun(sb: StringBuilder, run: TextRun) {
        sb.append("{")
        appendStyleOpen(sb, run.style)
        appendEscapedText(sb, run.text)
        sb.append("}")
    }

    private fun appendStyleOpen(sb: StringBuilder, style: CharacterStyle) {
        if (style.bold) sb.append("\\b")
        if (style.italic) sb.append("\\i")
        if (style.underline) sb.append("\\ul")
        if (style.strikethrough) sb.append("\\strike")
        if (style.superscript) sb.append("\\super")
        if (style.subscript) sb.append("\\sub")
        if (style != CharacterStyle.PLAIN) sb.append(" ")
    }

    private fun appendEscapedText(sb: StringBuilder, text: String) {
        for (ch in text) {
            val code = ch.code
            when {
                ch == '\\' -> sb.append("\\\\")
                ch == '{' -> sb.append("\\{")
                ch == '}' -> sb.append("\\}")
                ch == '\t' -> sb.append("\\tab ")
                ch == '\n' -> sb.append("\\line ")
                ch == '\r' -> {}
                code > 127 -> {
                    val signed = if (code > 32767) code - 65536 else code
                    sb.append("\\u").append(signed).append("?")
                }
                code < 32 -> sb.append("\\u").append(code).append("?")
                else -> sb.append(ch)
            }
        }
    }
}

/** Convenience top-level entry point equivalent to `RtfWriter().write(document)`. */
fun writeRtf(document: StyledDocument): String = RtfWriter().write(document)
