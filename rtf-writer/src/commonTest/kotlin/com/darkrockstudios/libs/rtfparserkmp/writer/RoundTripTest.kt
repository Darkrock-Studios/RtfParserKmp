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

import com.darkrockstudios.libs.rtfparserkmp.converter.convertToMarkdown
import com.darkrockstudios.libs.rtfparserkmp.converter.extractPlainText
import com.darkrockstudios.libs.rtfparserkmp.model.CharacterStyle
import com.darkrockstudios.libs.rtfparserkmp.model.Paragraph
import com.darkrockstudios.libs.rtfparserkmp.model.StyledDocument
import com.darkrockstudios.libs.rtfparserkmp.model.TextRun
import com.darkrockstudios.libs.rtfparserkmp.parser.RtfEvent
import com.darkrockstudios.libs.rtfparserkmp.parser.parseRtf
import com.darkrockstudios.libs.rtfparserkmp.rtf.Command
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoundTripTest {
    private val document = StyledDocument(
        listOf(
            Paragraph(
                listOf(
                    TextRun("This is "),
                    TextRun("bold", CharacterStyle(bold = true)),
                    TextRun(" and "),
                    TextRun("italic", CharacterStyle(italic = true)),
                    TextRun("."),
                ),
            ),
            Paragraph(listOf(TextRun("Second paragraph."))),
        ),
    )

    @Test
    fun plainTextSurvivesWriteThenExtract() {
        val rtf = RtfWriter().write(document).encodeToByteArray()
        assertEquals(document.text, extractPlainText(rtf))
    }

    @Test
    fun boldAndItalicCommandsAppearInEvents() {
        val rtf = RtfWriter().write(document).encodeToByteArray()
        val commands = parseRtf(rtf).filterIsInstance<RtfEvent.Command>().map { it.command }
        assertTrue(commands.contains(Command.b), "expected a \\b command")
        assertTrue(commands.contains(Command.i), "expected an \\i command")
    }

    @Test
    fun emphasisSurvivesWriteThenMarkdown() {
        val rtf = RtfWriter().write(document).encodeToByteArray()
        val markdown = convertToMarkdown(rtf)
        assertEquals("This is **bold** and _italic_.\n\nSecond paragraph.", markdown)
    }
}
