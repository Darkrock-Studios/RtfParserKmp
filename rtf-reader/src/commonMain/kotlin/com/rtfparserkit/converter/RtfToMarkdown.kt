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

package com.rtfparserkit.converter

import com.rtfparserkit.parser.ByteArrayRtfSource
import com.rtfparserkit.parser.RtfListener
import com.rtfparserkit.parser.standard.StandardRtfParser
import com.rtfparserkit.rtf.Command
import com.rtfparserkit.rtf.CommandType

/**
 * Converts an RTF document to a Markdown approximation. Tracks the bold ([Command.b]) and
 * italic ([Command.i]) toggles and wraps body text runs in `**` and `_`. Body-text filtering
 * matches [RtfTextExtractor]: only [Command.rtf], [Command.pntext] and [Command.fldrslt]
 * destinations contribute text.
 *
 * [Command.par] yields a paragraph break (blank line), [Command.line] a hard break, and
 * [Command.tab] a tab. Markdown-significant characters in body text are escaped.
 *
 * Emphasis is tracked as flat document-global toggles honoring explicit `\b0`/`\i0`, not as
 * group-scoped state; bold/italic set inside a group are not restored at the closing brace.
 */
class RtfToMarkdown : RtfListener {
    private val builder = StringBuilder()
    private var currentDestination = Command.rtf
    private val destinationStack = ArrayDeque<Command>()

    private var bold = false
    private var italic = false
    private var emphasisOpen = false

    /** The Markdown accumulated so far, with trailing whitespace trimmed. */
    val markdown: String
        get() {
            closeEmphasis()
            return builder.toString().trimEnd()
        }

    override fun processGroupStart() {
        destinationStack.addLast(currentDestination)
    }

    override fun processGroupEnd() {
        currentDestination = destinationStack.removeLast()
    }

    override fun processString(string: String) {
        if (!isBodyDestination()) return
        openEmphasis()
        builder.append(escape(string))
    }

    override fun processCommand(command: Command, parameter: Int, hasParameter: Boolean, optional: Boolean) {
        if (command.commandType == CommandType.Destination) {
            currentDestination = command
        }

        when (command) {
            Command.b -> setBold(!hasParameter || parameter != 0)
            Command.i -> setItalic(!hasParameter || parameter != 0)
            Command.par -> if (isBodyDestination()) {
                closeEmphasis()
                builder.append("\n\n")
            }
            Command.line -> if (isBodyDestination()) {
                closeEmphasis()
                builder.append("  \n")
            }
            Command.tab, Command.cell -> if (isBodyDestination()) {
                builder.append('\t')
            }
            Command.row -> if (isBodyDestination()) {
                closeEmphasis()
                builder.append('\n')
            }
            else -> {}
        }
    }

    private fun isBodyDestination(): Boolean =
        currentDestination == Command.rtf ||
            currentDestination == Command.pntext ||
            currentDestination == Command.fldrslt

    private fun setBold(value: Boolean) {
        if (value != bold) {
            closeEmphasis()
            bold = value
        }
    }

    private fun setItalic(value: Boolean) {
        if (value != italic) {
            closeEmphasis()
            italic = value
        }
    }

    private fun openEmphasis() {
        if (!emphasisOpen && (bold || italic)) {
            if (bold) builder.append("**")
            if (italic) builder.append("_")
            emphasisOpen = true
        }
    }

    private fun closeEmphasis() {
        if (emphasisOpen) {
            if (italic) builder.append("_")
            if (bold) builder.append("**")
            emphasisOpen = false
        }
    }

    private fun escape(text: String): String {
        val out = StringBuilder(text.length)
        for (ch in text) {
            if (ch in MARKDOWN_SPECIAL) out.append('\\')
            out.append(ch)
        }
        return out.toString()
    }

    private companion object {
        val MARKDOWN_SPECIAL = "\\`*_{}[]<>#".toSet()
    }
}

/**
 * Parse [bytes] as RTF and return a Markdown approximation.
 */
fun convertToMarkdown(bytes: ByteArray): String {
    val converter = RtfToMarkdown()
    StandardRtfParser().parse(ByteArrayRtfSource(bytes), converter)
    return converter.markdown
}
