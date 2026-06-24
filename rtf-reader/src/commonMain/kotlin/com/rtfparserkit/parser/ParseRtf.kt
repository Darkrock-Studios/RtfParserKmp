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

package com.rtfparserkit.parser

import com.rtfparserkit.parser.standard.StandardRtfParser

/**
 * Parse the RTF [bytes], passing the resulting events to [listener].
 */
fun parseRtf(bytes: ByteArray, listener: RtfListener) {
    StandardRtfParser().parse(ByteArrayRtfSource(bytes), listener)
}

/**
 * Parse the RTF [bytes], collecting and returning the resulting events.
 */
fun parseRtf(bytes: ByteArray): List<RtfEvent> {
    val events = ArrayList<RtfEvent>()
    parseRtf(bytes, CollectingRtfListener(events))
    return events
}

private class CollectingRtfListener(private val events: MutableList<RtfEvent>) : RtfListener {
    override fun processDocumentStart() {
        events.add(RtfEvent.DocumentStart)
    }

    override fun processDocumentEnd() {
        events.add(RtfEvent.DocumentEnd)
    }

    override fun processGroupStart() {
        events.add(RtfEvent.GroupStart)
    }

    override fun processGroupEnd() {
        events.add(RtfEvent.GroupEnd)
    }

    override fun processBinaryBytes(data: ByteArray) {
        events.add(RtfEvent.BinaryBytes(data))
    }

    override fun processString(string: String) {
        events.add(RtfEvent.Text(string))
    }

    override fun processCommand(command: com.rtfparserkit.rtf.Command, parameter: Int, hasParameter: Boolean, optional: Boolean) {
        events.add(RtfEvent.Command(command, parameter, hasParameter, optional))
    }
}
