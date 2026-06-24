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

package com.darkrockstudios.libs.rtfparserkmp.converter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RtfToMarkdownTest {

    private fun convert(rtf: String): String = convertToMarkdown(rtf.encodeToByteArray())

    @Test
    fun emitsBoldItalicAndParagraphBreak() {
        val md = convert("{\\rtf1\\ansi\\b bold\\b0  and \\i italic\\i0 .\\par next}")
        assertEquals("**bold** and _italic_.\n\nnext", md)
    }

    @Test
    fun escapesMarkdownSpecialCharacters() {
        val md = convert("{\\rtf1\\ansi a*b_c}")
        assertEquals("a\\*b\\_c", md)
    }

    @Test
    fun destinationContentDoesNotLeak() {
        val md = convert("{\\rtf1\\ansi{\\fonttbl{\\f0 Arial;}}plain}")
        assertEquals("plain", md)
        assertFalse(md.contains("Arial"))
    }

    @Test
    fun groupScopedEmphasisDoesNotLeak() {
        val md = convert("{\\rtf1\\ansi {\\b bold} normal {\\i italic} end}")
        assertEquals("**bold** normal _italic_ end", md)
    }

    @Test
    fun nestedGroupEmphasisRestoresOuterState() {
        val md = convert("{\\rtf1\\ansi {\\b bold {\\i both} stillBold} plain}")
        assertEquals("**bold _both_ stillBold** plain", md)
    }

    @Test
    fun explicitToggleAndGroupScopingCombine() {
        val md = convert("{\\rtf1\\ansi {\\b on\\b0  off} after}")
        assertEquals("**on** off after", md)
    }
}
