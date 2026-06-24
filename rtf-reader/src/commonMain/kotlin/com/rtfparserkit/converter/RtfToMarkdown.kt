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
 * Character formatting is group-scoped: a snapshot of the bold/italic state is pushed at each
 * group start and restored at the matching group end, mirroring how RTF braces scope formatting
 * (e.g. `{\b bold}` resets bold at the `}`). Explicit `\b0`/`\i0` toggles still mutate the
 * current state within a group.
 */
class RtfToMarkdown : RtfListener {
    private val builder = StringBuilder()
    private var currentDestination = Command.rtf
    private val destinationStack = ArrayDeque<Command>()
    private val formattingStack = ArrayDeque<Formatting>()

    private var bold = false
    private var italic = false

    /** Emphasis markers actually emitted into [builder] and not yet closed, in open order. */
    private val openMarkers = ArrayDeque<Marker>()

    /** The Markdown accumulated so far, with trailing whitespace trimmed. */
    val markdown: String
        get() {
            closeEmphasis()
            return builder.toString().trimEnd()
        }

    override fun processGroupStart() {
        destinationStack.addLast(currentDestination)
        formattingStack.addLast(Formatting(bold, italic))
    }

    override fun processGroupEnd() {
        currentDestination = destinationStack.removeLast()
        val restored = formattingStack.removeLast()
        bold = restored.bold
        italic = restored.italic
        closeDisabledMarkers()
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
            bold = value
            if (!value) closeDisabledMarkers()
        }
    }

    private fun setItalic(value: Boolean) {
        if (value != italic) {
            italic = value
            if (!value) closeDisabledMarkers()
        }
    }

    private fun isActive(marker: Marker): Boolean = when (marker) {
        Marker.BOLD -> bold
        Marker.ITALIC -> italic
    }

    /** Opens any marker whose style is active but not yet emitted, in bold-then-italic order. */
    private fun openEmphasis() {
        if (bold && Marker.BOLD !in openMarkers) {
            builder.append(Marker.BOLD.text)
            openMarkers.addLast(Marker.BOLD)
        }
        if (italic && Marker.ITALIC !in openMarkers) {
            builder.append(Marker.ITALIC.text)
            openMarkers.addLast(Marker.ITALIC)
        }
    }

    /** Closes every open marker whose style is now off, reopening any still-active span nested above it. */
    private fun closeDisabledMarkers() {
        val deepestDisabled = openMarkers.indexOfFirst { !isActive(it) }
        if (deepestDisabled < 0) return
        val reopen = ArrayDeque<Marker>()
        while (openMarkers.size > deepestDisabled) {
            val top = openMarkers.removeLast()
            builder.append(top.text)
            if (isActive(top)) reopen.addLast(top)
        }
        while (reopen.isNotEmpty()) {
            val m = reopen.removeLast()
            builder.append(m.text)
            openMarkers.addLast(m)
        }
    }

    private fun closeEmphasis() {
        while (openMarkers.isNotEmpty()) {
            builder.append(openMarkers.removeLast().text)
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

    private class Formatting(val bold: Boolean, val italic: Boolean)

    private enum class Marker(val text: String) {
        BOLD("**"),
        ITALIC("_"),
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
