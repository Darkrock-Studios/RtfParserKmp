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

class RtfTextExtractorTest {

    private fun extract(rtf: String): String = extractPlainText(rtf.encodeToByteArray())

    @Test
    fun extractsBodyTextAndMapsParToNewline() {
        val text = extract("{\\rtf1\\ansi bold\\b0 italic\\par second}")
        assertEquals("bolditalic\nsecond", text)
    }

    @Test
    fun mapsTabToTab() {
        val text = extract("{\\rtf1\\ansi a\\tab b}")
        assertEquals("a\tb", text)
    }

    @Test
    fun fontTableDestinationDoesNotLeak() {
        val rtf = "{\\rtf1\\ansi{\\fonttbl{\\f0 Arial;}}hello}"
        val text = extract(rtf)
        assertEquals("hello", text)
        assertFalse(text.contains("Arial"), "font table content leaked into output")
    }

    @Test
    fun generatorDestinationDoesNotLeak() {
        val rtf = "{\\rtf1\\ansi{\\*\\generator Riched20}body}"
        val text = extract(rtf)
        assertEquals("body", text)
        assertFalse(text.contains("Riched20"), "generator content leaked into output")
    }
}
