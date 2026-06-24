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

package com.rtfparserkit.parser.standard

import com.rtfparserkit.parser.RtfEvent
import com.rtfparserkit.parser.RtfListener
import com.rtfparserkit.parser.RtfSource
import com.rtfparserkit.rtf.Command
import com.rtfparserkit.rtf.CommandType

/**
 * Parses RTF data from an input source, passing events based on the RTF content to a listener.
 */
interface RtfParser {
    /**
     * Parse RTF data from an input source.
     */
    fun parse(source: RtfSource, listener: RtfListener)
}

/**
 * This class builds on the RawRtfParser to provide a parser which can
 * deal with character encodings and Unicode. All of the character data it reads
 * is presented back to the client as Unicode strings to make it as simple as
 * possible to deal with.
 */
class StandardRtfParser : RtfParser, RtfListener {
    private lateinit var handler: ParserEventHandler
    private val handlerStack = ArrayDeque<ParserEventHandler>()

    private var state = ParserState()
    private val stack = ArrayDeque<ParserState>()
    private var skipBytes = 0
    private val m_fontEncodings: MutableMap<Int, String> = mutableMapOf()

    /**
     * Main entry point: parse RTF data from the input stream, and pass events based on
     * the RTF content to the listener.
     */
    override fun parse(source: RtfSource, listener: RtfListener) {
        handler = DefaultEventHandler(listener)
        val reader = RawRtfParser()
        reader.parse(source, this)
    }

    /**
     * Handle event from the RawRtfParser.
     */
    override fun processGroupStart() {
        handleEvent(RtfEvent.GroupStart)
        stack.addLast(state)
        state = ParserState(state)
    }

    /**
     * Handle event from the RawRtfParser.
     */
    override fun processGroupEnd() {
        handleEvent(RtfEvent.GroupEnd)
        state = stack.removeLast()
    }

    /**
     * Handle event from the RawRtfParser.
     */
    override fun processCharacterBytes(data: ByteArray) {
        if (data.isNotEmpty()) {
            if (skipBytes < data.size) {
                handleEvent(RtfEvent.Text(decodeBytes(data, skipBytes, data.size - skipBytes, currentEncoding())))
            }
            skipBytes = 0
        }
    }

    /**
     * Determine which encoding to use, one defined by the current font, or the current default encoding.
     */
    private fun currentEncoding(): String {
        // Assume font 0 if a font has not been set explicitly
        if (!state.currentFontExplicitlySet) {
            state.currentFontExplicitlySet = true
            state.currentFontEncoding = m_fontEncodings[0]
        }
        return state.currentFontEncoding ?: state.currentEncoding
    }

    /**
     * Handle event from the RawRtfParser.
     */
    override fun processDocumentStart() {
        handleEvent(RtfEvent.DocumentStart)
    }

    /**
     * Handle event from the RawRtfParser.
     */
    override fun processDocumentEnd() {
        handleEvent(RtfEvent.DocumentEnd)
    }

    /**
     * Handle event from the RawRtfParser.
     */
    override fun processBinaryBytes(data: ByteArray) {
        handleEvent(RtfEvent.BinaryBytes(data))
    }

    /**
     * Handle event from the RawRtfParser.
     */
    override fun processString(string: String) {
        handleEvent(RtfEvent.Text(string))
    }

    /**
     * Handle event from the RawRtfParser.
     */
    override fun processCommand(command: Command, parameter: Int, hasParameter: Boolean, optional: Boolean) {
        if (command.commandType == CommandType.Encoding) {
            processEncoding(command, hasParameter, parameter)
        } else {
            var optionalFlag = false

            val lastEvent = handler.lastEvent
            if (lastEvent is RtfEvent.Command && lastEvent.command == Command.optionalcommand) {
                handler.removeLastEvent()
                optionalFlag = true
            }

            when (command) {
                Command.u -> processUnicode(parameter)

                Command.uc -> processUnicodeAlternateSkipCount(parameter)

                Command.upr -> processUpr(RtfEvent.Command(command, parameter, hasParameter, optionalFlag))

                Command.emdash -> processCharacter('—')

                Command.endash -> processCharacter('–')

                Command.emspace -> processCharacter(' ')

                Command.enspace -> processCharacter(' ')

                Command.qmspace -> processCharacter(' ')

                Command.bullet -> processCharacter('•')

                Command.lquote -> processCharacter('‘')

                Command.rquote -> processCharacter('’')

                Command.ldblquote -> processCharacter('“')

                Command.rdblquote -> processCharacter('”')

                Command.backslash -> processCharacter('\\')

                Command.opencurly -> processCharacter('{')

                Command.closecurly -> processCharacter('}')

                Command.f -> {
                    processFont(parameter)
                    handleCommand(command, parameter, hasParameter, optionalFlag)
                }

                Command.fcharset -> {
                    processFontCharset(parameter)
                    handleCommand(command, parameter, hasParameter, optionalFlag)
                }

                Command.cpg -> {
                    processFontCodepage(parameter)
                    handleCommand(command, parameter, hasParameter, optionalFlag)
                }

                else -> handleCommand(command, parameter, hasParameter, optionalFlag)
            }
        }
    }

    /**
     * Set the current font and current font encoding in the state.
     */
    private fun processFont(parameter: Int) {
        state.currentFontExplicitlySet = true
        state.currentFont = parameter
        state.currentFontEncoding = m_fontEncodings[parameter]
    }

    /**
     * Set the charset for the current font.
     */
    private fun processFontCharset(parameter: Int) {
        setFontEncoding(FontCharset.getCharset(parameter))
    }

    private fun processFontCodepage(parameter: Int) {
        setFontEncoding(parameter.toString())
    }

    private fun setFontEncoding(charset: String?) {
        if (charset != null) {
            val encoding = Encoding.LOCALEID_MAPPING[charset]
            if (encoding != null) {
                m_fontEncodings[state.currentFont] = encoding
            }
        }
    }

    /**
     * Switch the encoding based on the RTF command received.
     */
    private fun processEncoding(command: Command, hasParameter: Boolean, parameter: Int) {
        val encoding: String? = when (command) {
            Command.ansi -> Encoding.ANSI_ENCODING

            Command.pc -> Encoding.PC_ENCODING

            Command.pca -> Encoding.PCA_ENCODING

            Command.mac -> Encoding.MAC_ENCODING

            Command.ansicpg ->
                if (hasParameter) Encoding.LOCALEID_MAPPING[unsignedValue(parameter).toString()] else null

            else -> null
        }

        if (encoding == null) {
            throw IllegalArgumentException(
                "Unsupported encoding command " + command.keyword + (if (hasParameter) parameter.toString() else ""),
            )
        }

        state.currentEncoding = encoding
    }

    /**
     * Process an RTF command parameter representing a Unicode character.
     */
    private fun processUnicode(parameter: Int) {
        processCharacter(unsignedValue(parameter).toChar())
        skipBytes = state.unicodeAlternateSkipCount
    }

    /**
     * Set the number of bytes to skip after a Unicode character.
     */
    private fun processUnicodeAlternateSkipCount(parameter: Int) {
        state.unicodeAlternateSkipCount = parameter
    }

    /**
     * Process a upr command: consume all of the RTF commands relating to this
     * and emit events representing the Unicode content.
     */
    private fun processUpr(command: RtfEvent) {
        val uprHandler = UprHandler(handler)
        uprHandler.handleEvent(command)

        handlerStack.addLast(handler)
        handler = uprHandler
    }

    /**
     * Process a single character.
     */
    private fun processCharacter(c: Char) {
        handleEvent(RtfEvent.Text(c.toString()))
    }

    /**
     * Process an RTF command.
     */
    private fun handleCommand(command: Command, parameter: Int, hasParameter: Boolean, optional: Boolean) {
        handleEvent(RtfEvent.Command(command, parameter, hasParameter, optional))
    }

    /**
     * Pass an event to the event handler, pop the event handler stack if the current
     * event handler has consumed all of the events it can.
     */
    private fun handleEvent(event: RtfEvent) {
        handler.handleEvent(event)
        if (handler.isComplete) {
            handler = handlerStack.removeLast()
        }
    }

    private fun unsignedValue(parameter: Int): Int {
        var value = parameter
        if (value < 0) {
            value += 65536
        }
        return value
    }
}
