/*
 * Copyright 2013 Jon Iles
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

import com.darkrockstudios.libs.rtfparserkmp.parser.ByteArrayRtfSource
import com.darkrockstudios.libs.rtfparserkmp.parser.RtfListener
import com.darkrockstudios.libs.rtfparserkmp.parser.standard.StandardRtfParser
import com.darkrockstudios.libs.rtfparserkmp.rtf.Command
import com.darkrockstudios.libs.rtfparserkmp.rtf.CommandType

/**
 * Extracts plain text from an RTF document. Text is accumulated only while the current
 * destination is one of the body destinations ([Command.rtf], [Command.pntext], [Command.fldrslt]);
 * content in destinations such as `fonttbl`, `stylesheet` or `info` is ignored.
 *
 * [Command.par], [Command.line] and [Command.row] map to a newline; [Command.tab] and
 * [Command.cell] map to a tab.
 */
class RtfTextExtractor : RtfListener {
    private val builder = StringBuilder()
    private var currentDestination = Command.rtf
    private val destinationStack = ArrayDeque<Command>()

    /** The plain text accumulated so far. */
    val text: String
        get() = builder.toString()

    override fun processGroupStart() {
        destinationStack.addLast(currentDestination)
    }

    override fun processGroupEnd() {
        currentDestination = destinationStack.removeLast()
    }

    override fun processString(string: String) {
        when (currentDestination) {
            Command.rtf, Command.pntext, Command.fldrslt -> builder.append(string)
            else -> {}
        }
    }

    override fun processCommand(command: Command, parameter: Int, hasParameter: Boolean, optional: Boolean) {
        if (command.commandType == CommandType.Destination) {
            currentDestination = command
        }

        when (command) {
            Command.par, Command.line, Command.row -> builder.append('\n')
            Command.tab, Command.cell -> builder.append('\t')
            else -> {}
        }
    }
}

/**
 * Parse [bytes] as RTF and return the extracted plain text.
 */
fun extractPlainText(bytes: ByteArray): String {
    val extractor = RtfTextExtractor()
    StandardRtfParser().parse(ByteArrayRtfSource(bytes), extractor)
    return extractor.text
}
