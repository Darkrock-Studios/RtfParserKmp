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

package com.darkrockstudios.libs.rtfparserkmp.io

import com.darkrockstudios.libs.rtfparserkmp.parser.RtfEvent
import com.darkrockstudios.libs.rtfparserkmp.parser.RtfListener
import com.darkrockstudios.libs.rtfparserkmp.parser.standard.StandardRtfParser
import com.darkrockstudios.libs.rtfparserkmp.rtf.Command
import kotlinx.io.Buffer
import kotlinx.io.writeString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinxIoRtfSourceTest {

    private fun buffer(rtf: String): Buffer = Buffer().apply { writeString(rtf) }

    @Test
    fun extractsTextFromBufferSource() {
        val source = KotlinxIoRtfSource(buffer("""{\rtf1\ansi Hello, world!\par}"""))
        val extractor = TextCollector()
        StandardRtfParser().parse(source, extractor)
        assertEquals("Hello, world!\n", extractor.text)
    }

    @Test
    fun asRtfSourceExtensionParses() {
        val extractor = TextCollector()
        StandardRtfParser().parse(buffer("""{\rtf1 Foo}""").asRtfSource(), extractor)
        assertEquals("Foo", extractor.text)
    }

    @Test
    fun emitsExpectedEvents() {
        val events = ArrayList<RtfEvent>()
        StandardRtfParser().parse(
            KotlinxIoRtfSource(buffer("""{\rtf1 Bar}""")),
            CollectingListener(events),
        )
        assertTrue(events.any { it is RtfEvent.Text && it.text == "Bar" })
        assertTrue(events.any { it is RtfEvent.Command && it.command == Command.rtf })
    }

    @Test
    fun readSingleBytesWithPushback() {
        val src = KotlinxIoRtfSource(buffer("AB"))
        val a = src.read()
        assertEquals('A'.code, a)
        src.unread(a)
        assertEquals('A'.code, src.read())
        assertEquals('B'.code, src.read())
        assertEquals(-1, src.read())
    }

    @Test
    fun bulkReadHonoursPushbackAndFillsFully() {
        val src = KotlinxIoRtfSource(buffer("HELLO"))
        val first = src.read()
        assertEquals('H'.code, first)
        src.unread(first)

        val dest = ByteArray(5)
        val count = src.read(dest)
        assertEquals(5, count)
        assertEquals("HELLO", dest.decodeToString())
        assertEquals(-1, src.read())
    }

    @Test
    fun bulkReadReturnsShortCountOnlyAtEof() {
        val src = KotlinxIoRtfSource(buffer("XYZ"))
        val dest = ByteArray(8)
        val count = src.read(dest)
        assertEquals(3, count)
        assertEquals("XYZ", dest.copyOf(3).decodeToString())
    }

    @Test
    fun unreadRejectsDoublePushback() {
        val src = KotlinxIoRtfSource(buffer("Z"))
        val z = src.read()
        src.unread(z)
        var threw = false
        try {
            src.unread(z)
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun readReportsEmptyBuffer() {
        val src = KotlinxIoRtfSource(Buffer())
        assertEquals(-1, src.read())
    }

    private class TextCollector : RtfListener {
        private val sb = StringBuilder()
        val text: String get() = sb.toString()
        override fun processString(string: String) {
            sb.append(string)
        }
        override fun processCommand(command: Command, parameter: Int, hasParameter: Boolean, optional: Boolean) {
            if (command == Command.par) sb.append('\n')
        }
    }

    private class CollectingListener(private val events: MutableList<RtfEvent>) : RtfListener {
        override fun processString(string: String) {
            events.add(RtfEvent.Text(string))
        }
        override fun processCommand(command: Command, parameter: Int, hasParameter: Boolean, optional: Boolean) {
            events.add(RtfEvent.Command(command, parameter, hasParameter, optional))
        }
    }
}
