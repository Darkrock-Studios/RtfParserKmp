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
 * Converts a small Markdown subset into a [StyledDocument], then to RTF via [RtfWriter].
 *
 * Supported subset:
 * - Paragraphs are separated by one or more blank lines.
 * - Inline bold: `**text**` or `__text__`.
 * - Inline italic: `*text*` or `_text_`.
 * - Combined bold+italic: `***text***` (or the `___text___` underscore form).
 *
 * Headings, lists and links are out of scope: `#`, `-`, `[` and friends are treated as literal text.
 * An unmatched emphasis marker is emitted as a literal character rather than opening a span.
 */
class MarkdownToRtf {
    /** Parses [markdown] into a [StyledDocument], one [Paragraph] per blank-line-separated block. */
    fun convert(markdown: String): StyledDocument {
        val blocks = splitParagraphs(markdown)
        val paragraphs = blocks.map { Paragraph(parseInline(it)) }
        return StyledDocument(paragraphs)
    }

    private fun splitParagraphs(markdown: String): List<String> {
        val normalized = markdown.replace("\r\n", "\n").replace('\r', '\n')
        val blocks = ArrayList<String>()
        val current = StringBuilder()
        for (line in normalized.split('\n')) {
            if (line.isBlank()) {
                if (current.isNotEmpty()) {
                    blocks.add(current.toString())
                    current.clear()
                }
            } else {
                if (current.isNotEmpty()) current.append('\n')
                current.append(line)
            }
        }
        if (current.isNotEmpty()) blocks.add(current.toString())
        return blocks
    }

    private fun parseInline(text: String): List<TextRun> {
        val runs = ArrayList<TextRun>()
        val pending = StringBuilder()
        var bold = false
        var italic = false
        var i = 0

        fun flush() {
            if (pending.isNotEmpty()) {
                runs.add(TextRun(pending.toString(), CharacterStyle(bold = bold, italic = italic)))
                pending.clear()
            }
        }

        while (i < text.length) {
            val marker = matchMarker(text, i, bold, italic)
            if (marker != null) {
                flush()
                bold = marker.boldAfter
                italic = marker.italicAfter
                i += marker.length
            } else {
                pending.append(text[i])
                i++
            }
        }
        flush()
        if (runs.isEmpty()) runs.add(TextRun("", CharacterStyle.PLAIN))
        return runs
    }

    private fun matchMarker(text: String, index: Int, bold: Boolean, italic: Boolean): Marker? {
        val ch = text[index]
        if (ch != '*' && ch != '_') return null
        val runLength = markerRun(text, index, ch)

        val tripleOpen = runLength >= 3 && !bold && !italic
        val tripleClose = runLength >= 3 && bold && italic
        if (tripleOpen && hasClosing(text, index + 3, ch, 3)) {
            return Marker(3, boldAfter = true, italicAfter = true)
        }
        if (tripleClose) {
            return Marker(3, boldAfter = false, italicAfter = false)
        }

        val doubleOpen = runLength >= 2 && !bold
        val doubleClose = runLength >= 2 && bold
        if (doubleOpen && hasClosing(text, index + 2, ch, 2)) {
            return Marker(2, boldAfter = true, italicAfter = italic)
        }
        if (doubleClose) {
            return Marker(2, boldAfter = false, italicAfter = italic)
        }

        val singleOpen = !italic
        val singleClose = italic
        if (singleOpen && hasClosing(text, index + 1, ch, 1)) {
            return Marker(1, boldAfter = bold, italicAfter = true)
        }
        if (singleClose) {
            return Marker(1, boldAfter = bold, italicAfter = false)
        }
        return null
    }

    private fun markerRun(text: String, index: Int, ch: Char): Int {
        var n = 0
        var i = index
        while (i < text.length && text[i] == ch) {
            n++
            i++
        }
        return n
    }

    private fun hasClosing(text: String, from: Int, ch: Char, width: Int): Boolean {
        var i = from
        while (i < text.length) {
            if (text[i] == ch) {
                val run = markerRun(text, i, ch)
                if (run >= width) return true
                i += run
            } else {
                i++
            }
        }
        return false
    }

    private class Marker(
        val length: Int,
        val boldAfter: Boolean,
        val italicAfter: Boolean,
    )
}

/** Convenience entry point: parse [markdown] and serialize the resulting document to RTF. */
fun convertMarkdownToRtf(markdown: String): String =
    RtfWriter().write(MarkdownToRtf().convert(markdown))
