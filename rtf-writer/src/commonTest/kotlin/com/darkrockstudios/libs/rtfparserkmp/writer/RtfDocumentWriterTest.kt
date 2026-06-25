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

import com.darkrockstudios.libs.rtfparserkmp.converter.extractPlainText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val TIMES = RtfFont("Times New Roman", RtfFontFamily.Roman)

/** Header for a document whose only font is [TIMES] and which references no colors. */
private const val TIMES_HEADER =
    "{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033" +
        "{\\fonttbl{\\f0\\froman\\fcharset0 Times New Roman;}}" +
        "{\\colortbl;}"

class RtfDocumentWriterTest {
    @Test
    fun minimalDocument() {
        val doc = RtfDocument(
            blocks = listOf(RtfParagraph(listOf(RtfTextRun("Hello")))),
            defaultFont = TIMES,
        )
        val expected = "$TIMES_HEADER\\fs24\n\\pard {Hello}\\par\n}"
        assertEquals(expected, RtfDocumentWriter().write(doc))
    }

    @Test
    fun collectsFontsAndColorsIntoTablesAndReferencesThem() {
        val body = RtfFont("Georgia", RtfFontFamily.Roman)
        val mono = RtfFont("Consolas", RtfFontFamily.Modern)
        val accent = RtfColor.fromRgb(0x7E57C2)
        val doc = RtfDocument(
            blocks = listOf(
                RtfParagraph(
                    listOf(
                        RtfTextRun("Title", RtfSpanStyle(bold = true, fontSizeHalfPoints = 72, color = accent)),
                        RtfTextRun("code", RtfSpanStyle(font = mono)),
                    ),
                ),
            ),
            defaultFont = body,
        )
        val expected =
            "{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033" +
                "{\\fonttbl{\\f0\\froman\\fcharset0 Georgia;}{\\f1\\fmodern\\fcharset0 Consolas;}}" +
                "{\\colortbl;\\red126\\green87\\blue194;}" +
                "\\fs24\n\\pard {\\b\\fs72\\cf1 Title}{\\f1 code}\\par\n}"
        assertEquals(expected, RtfDocumentWriter().write(doc))
    }

    @Test
    fun paragraphProperties() {
        val doc = RtfDocument(
            blocks = listOf(
                RtfParagraph(
                    listOf(RtfTextRun("x")),
                    RtfParagraphStyle(
                        alignment = RtfAlignment.Center,
                        spaceBeforeTwips = 360,
                        spaceAfterTwips = 240,
                        firstLineIndentTwips = 360,
                        leftIndentTwips = 720,
                        keepWithNext = true,
                    ),
                ),
            ),
            defaultFont = TIMES,
        )
        val expected = "$TIMES_HEADER\\fs24\n\\pard\\qc\\sb360\\sa240\\li720\\fi360\\keepn {x}\\par\n}"
        assertEquals(expected, RtfDocumentWriter().write(doc))
    }

    @Test
    fun bottomBorderRendersHorizontalRule() {
        val doc = RtfDocument(
            blocks = listOf(
                RtfParagraph(
                    emptyList(),
                    RtfParagraphStyle(spaceAfterTwips = 160, bottomBorder = RtfBorder()),
                ),
            ),
            defaultFont = TIMES,
        )
        val expected = "$TIMES_HEADER\\fs24\n\\pard\\sa160\\brdrb\\brdrs\\brdrw10 \\par\n}"
        assertEquals(expected, RtfDocumentWriter().write(doc))
    }

    @Test
    fun pageBreak() {
        val doc = RtfDocument(
            blocks = listOf(
                RtfPageBreak,
                RtfParagraph(listOf(RtfTextRun("After"))),
            ),
            defaultFont = TIMES,
        )
        val expected = "$TIMES_HEADER\\fs24\n\\page\n\\pard {After}\\par\n}"
        assertEquals(expected, RtfDocumentWriter().write(doc))
    }

    @Test
    fun externalHyperlink() {
        val doc = RtfDocument(
            blocks = listOf(
                RtfParagraph(
                    listOf(
                        RtfHyperlink(
                            target = "https://example.com",
                            content = listOf(RtfTextRun("here", RtfSpanStyle(underline = true))),
                        ),
                    ),
                ),
            ),
            defaultFont = TIMES,
        )
        val expected = "$TIMES_HEADER\\fs24\n\\pard " +
            "{\\field{\\*\\fldinst HYPERLINK \"https://example.com\"}{\\fldrslt {{\\ul here}}}}" +
            "\\par\n}"
        assertEquals(expected, RtfDocumentWriter().write(doc))
    }

    @Test
    fun bookmarkAndLocalLink() {
        val doc = RtfDocument(
            blocks = listOf(
                RtfParagraph(
                    listOf(
                        RtfHyperlink(
                            target = "chapter1",
                            content = listOf(RtfTextRun("1. Start")),
                            kind = RtfHyperlinkKind.Bookmark,
                        ),
                    ),
                ),
                RtfParagraph(
                    listOf(
                        RtfBookmark("chapter1", listOf(RtfTextRun("Start", RtfSpanStyle(bold = true)))),
                    ),
                ),
            ),
            defaultFont = TIMES,
        )
        val expected = "$TIMES_HEADER\\fs24\n" +
            "\\pard {\\field{\\*\\fldinst HYPERLINK \\\\l \"chapter1\"}{\\fldrslt {{1. Start}}}}\\par\n" +
            "\\pard {\\*\\bkmkstart chapter1}{\\b Start}{\\*\\bkmkend chapter1}\\par\n}"
        assertEquals(expected, RtfDocumentWriter().write(doc))
    }

    @Test
    fun lineBreakAndTab() {
        val doc = RtfDocument(
            blocks = listOf(
                RtfParagraph(
                    listOf(
                        RtfTextRun("a"),
                        RtfLineBreak,
                        RtfTextRun("b"),
                        RtfTab,
                        RtfTextRun("c"),
                    ),
                ),
            ),
            defaultFont = TIMES,
        )
        val expected = "$TIMES_HEADER\\fs24\n\\pard {a}\\line {b}\\tab {c}\\par\n}"
        assertEquals(expected, RtfDocumentWriter().write(doc))
    }

    @Test
    fun infoAndGenerator() {
        val doc = RtfDocument(
            blocks = listOf(RtfParagraph(listOf(RtfTextRun("x")))),
            defaultFont = TIMES,
            info = RtfInfo(title = "My Story", author = "Adam"),
            generator = "Hammer 3.4.2",
        )
        val expected = "$TIMES_HEADER" +
            "{\\info{\\title My Story}{\\author Adam}}" +
            "{\\*\\generator Hammer 3.4.2;}" +
            "\\fs24\n\\pard {x}\\par\n}"
        assertEquals(expected, RtfDocumentWriter().write(doc))
    }

    @Test
    fun escapesNonAsciiAndStructuralCharacters() {
        val doc = RtfDocument(
            blocks = listOf(RtfParagraph(listOf(RtfTextRun("café{}\\")))),
            defaultFont = TIMES,
        )
        val expected = "$TIMES_HEADER\\fs24\n\\pard {caf\\u233?\\{\\}\\\\}\\par\n}"
        assertEquals(expected, RtfDocumentWriter().write(doc))
    }

    @Test
    fun topLevelWriteRtfDocumentMatchesClass() {
        val doc = RtfDocument(
            blocks = listOf(RtfParagraph(listOf(RtfTextRun("Hi")))),
            defaultFont = TIMES,
        )
        assertEquals(RtfDocumentWriter().write(doc), writeRtfDocument(doc))
    }

    @Test
    fun roundTripsThroughReader() {
        val doc = RtfDocument(
            blocks = listOf(
                RtfParagraph(listOf(RtfTextRun("First paragraph"))),
                RtfParagraph(listOf(RtfTextRun("Second paragraph"))),
            ),
            defaultFont = TIMES,
        )
        val rtf = RtfDocumentWriter().write(doc)
        val text = extractPlainText(rtf.encodeToByteArray())
        assertTrue("First paragraph" in text, "missing first paragraph in: $text")
        assertTrue("Second paragraph" in text, "missing second paragraph in: $text")
    }
}
