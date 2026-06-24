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

package com.rtfparserkit.writer

import com.rtfparserkit.model.CharacterStyle
import com.rtfparserkit.model.Paragraph
import com.rtfparserkit.model.StyledDocument
import com.rtfparserkit.model.TextRun
import kotlin.test.Test
import kotlin.test.assertEquals

private const val HEADER = "{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0 Times New Roman;}}"

class RtfWriterTest {
    @Test
    fun plainSingleParagraph() {
        val doc = StyledDocument(
            listOf(Paragraph(listOf(TextRun("Hello")))),
        )
        val expected = "$HEADER\\plain {Hello}}"
        assertEquals(expected, RtfWriter().write(doc))
    }

    @Test
    fun boldThenItalicRuns() {
        val doc = StyledDocument(
            listOf(
                Paragraph(
                    listOf(
                        TextRun("Bold", CharacterStyle(bold = true)),
                        TextRun("Ital", CharacterStyle(italic = true)),
                    ),
                ),
            ),
        )
        val expected = "$HEADER\\plain {\\b Bold}{\\i Ital}}"
        assertEquals(expected, RtfWriter().write(doc))
    }

    @Test
    fun twoParagraphsSeparatedByPar() {
        val doc = StyledDocument(
            listOf(
                Paragraph(listOf(TextRun("One"))),
                Paragraph(listOf(TextRun("Two"))),
            ),
        )
        val expected = "$HEADER\\plain {One}\\par \\plain {Two}}"
        assertEquals(expected, RtfWriter().write(doc))
    }

    @Test
    fun nonAsciiAndLiteralEscapes() {
        val doc = StyledDocument(
            listOf(Paragraph(listOf(TextRun("café{}\\")))),
        )
        val expected = "$HEADER\\plain {caf\\u233?\\{\\}\\\\}}"
        assertEquals(expected, RtfWriter().write(doc))
    }

    @Test
    fun topLevelWriteRtfMatchesClass() {
        val doc = StyledDocument(listOf(Paragraph(listOf(TextRun("Hi")))))
        assertEquals(RtfWriter().write(doc), writeRtf(doc))
    }
}
