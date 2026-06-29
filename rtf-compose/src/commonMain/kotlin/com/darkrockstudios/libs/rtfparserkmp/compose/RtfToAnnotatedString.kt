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

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.darkrockstudios.libs.rtfparserkmp.parser.ByteArrayRtfSource
import com.darkrockstudios.libs.rtfparserkmp.parser.RtfListener
import com.darkrockstudios.libs.rtfparserkmp.parser.standard.StandardRtfParser
import com.darkrockstudios.libs.rtfparserkmp.rtf.Command
import com.darkrockstudios.libs.rtfparserkmp.rtf.CommandType

/**
 * Builds a Compose [AnnotatedString] from an RTF document. Tracks bold ([Command.b]),
 * italic ([Command.i]), underline ([Command.ul]) and strikethrough ([Command.strike]) and
 * applies the matching [SpanStyle] to each body-text run. Body-text filtering matches
 * [com.darkrockstudios.libs.rtfparserkmp.converter.RtfToMarkdown]: only [Command.rtf],
 * [Command.pntext] and [Command.fldrslt] destinations contribute text.
 *
 * [Command.par], [Command.line] and [Command.row] yield a newline; [Command.tab] yields a tab.
 *
 * Character formatting is group-scoped: a snapshot of the formatting state is pushed at each
 * group start and restored at the matching group end, mirroring how RTF braces scope formatting
 * (e.g. `{\b bold}` resets bold at the `}`). Explicit `\b0`/`\i0`/`\ul0`/`\ulnone` toggles still
 * mutate the current state within a group.
 */
class RtfToAnnotatedString : RtfListener {
    private val builder = AnnotatedString.Builder()
    private var currentDestination = Command.rtf
    private val destinationStack = ArrayDeque<Command>()
    private val formattingStack = ArrayDeque<Formatting>()

    private var bold = false
    private var italic = false
    private var underline = false
    private var strikethrough = false

    /** The span style currently pushed onto the builder, if any. */
    private var openStyle: SpanStyle? = null

    /** Closes any open span and returns the accumulated [AnnotatedString]; call after parsing finishes. */
    fun build(): AnnotatedString {
        closeStyle()
        return builder.toAnnotatedString()
    }

    override fun processGroupStart() {
        destinationStack.addLast(currentDestination)
        formattingStack.addLast(Formatting(bold, italic, underline, strikethrough))
    }

    override fun processGroupEnd() {
        currentDestination = destinationStack.removeLast()
        val restored = formattingStack.removeLast()
        bold = restored.bold
        italic = restored.italic
        underline = restored.underline
        strikethrough = restored.strikethrough
    }

    override fun processString(string: String) {
        if (!isBodyDestination()) return
        appendStyled(string)
    }

    override fun processCommand(command: Command, parameter: Int, hasParameter: Boolean, optional: Boolean) {
        if (command.commandType == CommandType.Destination) {
            currentDestination = command
        }

        when (command) {
            Command.b -> bold = !hasParameter || parameter != 0
            Command.i -> italic = !hasParameter || parameter != 0
            Command.ul -> underline = !hasParameter || parameter != 0
            Command.ulnone -> underline = false
            Command.strike -> strikethrough = !hasParameter || parameter != 0
            Command.par, Command.line, Command.row -> if (isBodyDestination()) appendStyled("\n")
            Command.tab, Command.cell -> if (isBodyDestination()) appendStyled("\t")
            else -> {}
        }
    }

    private fun isBodyDestination(): Boolean =
        currentDestination == Command.rtf ||
            currentDestination == Command.pntext ||
            currentDestination == Command.fldrslt

    private fun currentStyle(): SpanStyle? {
        val decorations = buildList {
            if (underline) add(TextDecoration.Underline)
            if (strikethrough) add(TextDecoration.LineThrough)
        }
        val decoration = when (decorations.size) {
            0 -> null
            1 -> decorations[0]
            else -> TextDecoration.combine(decorations)
        }
        if (!bold && !italic && decoration == null) return null
        return SpanStyle(
            fontWeight = if (bold) FontWeight.Bold else null,
            fontStyle = if (italic) FontStyle.Italic else null,
            textDecoration = decoration,
        )
    }

    private fun appendStyled(text: String) {
        val style = currentStyle()
        if (style != openStyle) {
            closeStyle()
            if (style != null) {
                builder.pushStyle(style)
                openStyle = style
            }
        }
        builder.append(text)
    }

    private fun closeStyle() {
        if (openStyle != null) {
            builder.pop()
            openStyle = null
        }
    }

    private data class Formatting(
        val bold: Boolean,
        val italic: Boolean,
        val underline: Boolean,
        val strikethrough: Boolean,
    )
}

/**
 * Parse [bytes] as RTF and return a Compose [AnnotatedString] approximation.
 */
fun rtfToAnnotatedString(bytes: ByteArray): AnnotatedString {
    val converter = RtfToAnnotatedString()
    StandardRtfParser().parse(ByteArrayRtfSource(bytes), converter)
    return converter.build()
}
