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

package com.darkrockstudios.libs.rtfparserkmp.parser

import com.darkrockstudios.libs.rtfparserkmp.converter.extractPlainText
import com.darkrockstudios.libs.rtfparserkmp.rtf.Command
import kotlin.test.Test
import kotlin.test.assertEquals

class PageBreakBeforeParsingTest {
    private val rtf = "{\\rtf1\\ansi\\pard\\pagebb Title\\par}"

    @Test
    fun pagebbIsDeliveredAsACommand() {
        val events = parseRtf(rtf.encodeToByteArray())
        val pagebb = events.filterIsInstance<RtfEvent.Command>().filter { it.command == Command.pagebb }
        assertEquals(listOf(RtfEvent.Command(Command.pagebb, 0, hasParameter = false, optional = false)), pagebb)
        assertEquals(listOf("Title"), events.filterIsInstance<RtfEvent.Text>().map { it.text })
    }

    @Test
    fun pagebbAddsNoCharactersToPlainText() {
        assertEquals("Title\n", extractPlainText(rtf.encodeToByteArray()))
    }
}
