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

package com.darkrockstudios.libs.rtfparserkmp.compose

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RtfToAnnotatedStringTest {

    private fun rtf(content: String) = rtfToAnnotatedString(content.encodeToByteArray())

    @Test
    fun plainTextWithParagraphNewline() {
        val result = rtf("""{\rtf1\ansi {\b Bold} and {\i italic}\par next}""")
        assertEquals("Bold and italic\nnext", result.text)
    }

    @Test
    fun boldAndItalicSpanRanges() {
        val result = rtf("""{\rtf1\ansi {\b Bold} and {\i italic}\par next}""")
        val text = result.text

        val boldStart = text.indexOf("Bold")
        val boldSpan = result.spanStyles.firstOrNull {
            it.start == boldStart && it.end == boldStart + "Bold".length
        }
        assertNotNull(boldSpan, "expected a span over 'Bold'")
        assertEquals(FontWeight.Bold, boldSpan.item.fontWeight)
        assertNull(boldSpan.item.fontStyle)

        val italicStart = text.indexOf("italic")
        val italicSpan = result.spanStyles.firstOrNull {
            it.start == italicStart && it.end == italicStart + "italic".length
        }
        assertNotNull(italicSpan, "expected a span over 'italic'")
        assertEquals(FontStyle.Italic, italicSpan.item.fontStyle)
        assertNull(italicSpan.item.fontWeight)
    }

    @Test
    fun underlineAndStrikethroughCombine() {
        val result = rtf("""{\rtf1\ansi {\ul\strike both}}""")
        assertEquals("both", result.text)
        val span = result.spanStyles.single()
        assertEquals(0, span.start)
        assertEquals("both".length, span.end)
        val decoration = span.item.textDecoration
        assertNotNull(decoration)
        assertTrue(decoration.contains(TextDecoration.Underline))
        assertTrue(decoration.contains(TextDecoration.LineThrough))
    }

    @Test
    fun destinationContentDoesNotLeak() {
        val result = rtf("""{\rtf1\ansi{\fonttbl{\f0 Arial;}}plain}""")
        assertEquals("plain", result.text)
    }

    @Test
    fun groupScopingResetsFormatting() {
        val result = rtf("""{\rtf1\ansi {\b bold}plain}""")
        assertEquals("boldplain", result.text)
        val span = result.spanStyles.single()
        assertEquals(0, span.start)
        assertEquals("bold".length, span.end)
        assertEquals(FontWeight.Bold, span.item.fontWeight)
    }
}
