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

import com.rtfparserkit.converter.convertToMarkdown
import com.rtfparserkit.converter.extractPlainText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Round-trips Markdown through [convertMarkdownToRtf] and back via the reader's converters.
 *
 * `RtfToMarkdown` tracks emphasis as a flat, document-global toggle that is only cleared by an
 * explicit `\b0`/`\i0`, which [RtfWriter] never emits — it relies on group scoping for resets.
 * So emphasis "leaks" forward once opened: the converter is faithful only when an emphasized span
 * runs to the end of the document. Tests that need an exact equality therefore put emphasis last;
 * the plain-text channel ([extractPlainText]) round-trips exactly regardless of style placement.
 */
class MarkdownRoundTripTest {
    private fun roundTripMarkdown(markdown: String): String =
        convertToMarkdown(convertMarkdownToRtf(markdown).encodeToByteArray())

    private fun roundTripText(markdown: String): String =
        extractPlainText(convertMarkdownToRtf(markdown).encodeToByteArray())

    @Test
    fun trailingBoldAndParagraphBreakArePreserved() {
        val markdown = "First paragraph is plain.\n\nThis paragraph ends in **bold**"
        assertEquals(markdown, roundTripMarkdown(markdown))
    }

    @Test
    fun trailingItalicIsPreserved() {
        val markdown = "A line that ends in _italic_"
        assertEquals(markdown, roundTripMarkdown(markdown))
    }

    @Test
    fun trailingCombinedBoldItalicNormalizesToNestedSpans() {
        assertEquals("A **_strong finish_**", roundTripMarkdown("A ***strong*** finish"))
    }

    @Test
    fun underscoreBoldFormNormalizesToAsterisks() {
        assertEquals("Plain then **bold to the end**", roundTripMarkdown("Plain then __bold to the end__"))
    }

    @Test
    fun plainTextOfStyledMarkdownRoundTripsExactly() {
        val markdown = "This is **bold** and _italic_.\n\nSecond paragraph."
        assertEquals("This is bold and italic.\nSecond paragraph.", roundTripText(markdown))
    }

    @Test
    fun emphasisIsPresentAfterRoundTrip() {
        val back = roundTripMarkdown("This is **bold** and _italic_.")
        assertTrue(back.contains("**"), "expected bold marker, got: $back")
        assertTrue(back.contains("_"), "expected italic marker, got: $back")
    }

    @Test
    fun unmatchedMarkerIsLiteral() {
        assertEquals("a \\* b", roundTripMarkdown("a * b"))
    }

    @Test
    fun multipleBlankLinesCollapseToSingleBreak() {
        assertEquals("First\n\nSecond", roundTripMarkdown("First\n\n\n\nSecond"))
    }
}
