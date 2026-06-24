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

package com.darkrockstudios.libs.rtfparserkmp.parser.standard

import com.darkrockstudios.libs.rtfparserkmp.parser.RtfListener
import com.darkrockstudios.libs.rtfparserkmp.parser.RtfSource
import com.darkrockstudios.libs.rtfparserkmp.rtf.Command
import com.darkrockstudios.libs.rtfparserkmp.utils.HexUtils

/**
 * This class implements a low level RTF parser. It performs the minimum amount
 * of processing on the data read from an RTF file, just passing the caller a
 * stream of events representing the commands and character bytes. In particular
 * the parser doesn't not deal with character encodings or the various
 * Unicode related commands which may be present in an RTF file.
 *
 * This code is based on the approach outlined in the sample C code provided in
 * the RTF Specification 1.9.1 (http://www.microsoft.com/en-gb/download/details.aspx?id=10725)
 */
internal class RawRtfParser {
    private lateinit var source: RtfSource
    private var groupDepth = 0
    private var parsingHex = false
    private lateinit var buffer: ByteBuffer
    private lateinit var listener: RtfListener

    /**
     * Parse RTF data from an input source.
     */
    fun parse(source: RtfSource, listener: RtfListener) {
        this.source = source
        this.listener = listener
        groupDepth = 0
        parsingHex = false
        buffer = ByteBuffer()

        listener.processDocumentStart()

        var ch: Int
        parsingHex = false

        while (true) {
            ch = source.read()
            if (ch == -1) {
                break
            }

            if (groupDepth < 0) {
                throw IllegalStateException("Group stack underflow")
            }

            when (ch.toChar()) {
                '{' -> handleGroupStart()

                '}' -> handleGroupEnd()

                '\\' -> handleCommand()

                '\r', '\n' -> {
                }

                '\t' -> {
                    handleCharacterData()
                    listener.processCommand(Command.tab, 0, false, false)
                }

                else -> handleCharacterByte(ch)
            }
        }

        if (groupDepth < 0) {
            throw IllegalStateException("Group stack underflow")
        }

        if (groupDepth > 0) {
            throw IllegalStateException("Unmatched brace")
        }

        listener.processDocumentEnd()
    }

    /**
     * Process a single character byte, or hex encoded character byte.
     */
    private fun handleCharacterByte(character: Int) {
        var ch = character
        if (parsingHex) {
            var b = HexUtils.parseHexDigit(ch) shl 4
            ch = source.read()
            if (ch == -1) {
                throw IllegalStateException("Unexpected end of file")
            }

            // Have encountered malformed RTF where only a single hex digit
            // has been supplied. e.g. \'AA\'B\'CC so we hit the next \
            // rather than getting a hex digit. Try to handle this specific
            // case gracefully by unreading the next character and working with
            // the single digit we have.
            if (ch == '\\'.code) {
                b = b shr 4
                source.unread(ch)
            } else {
                b += HexUtils.parseHexDigit(ch)
            }

            buffer.add(b)
            parsingHex = false
        } else {
            buffer.add(ch)
        }
    }

    /**
     * Read and process an RTF command.
     */
    private fun handleCommand() {
        var commandHasParameter = false
        var parameterIsNegative = false
        var parameterValue = 0
        val commandText = StringBuilder()
        val parameterText = StringBuilder()

        var ch = source.read()
        if (ch == -1) {
            throw RtfParseException("Unexpected end of file")
        }

        commandText.append(ch.toChar())

        if (!ch.toChar().isLetter()) {
            handleCommand(commandText, 0, commandHasParameter)
            return
        }

        while (true) {
            ch = source.read()
            if (ch == -1 || !ch.toChar().isLetter()) {
                break
            }
            commandText.append(ch.toChar())
            if (commandText.length > MAX_COMMAND_LENGTH) {
                break
            }
        }

        if (ch == -1) {
            throw RtfParseException("Unexpected end of file")
        }

        if (commandText.length > MAX_COMMAND_LENGTH) {
            throw IllegalArgumentException("Invalid keyword: $commandText")
        }

        if (ch == '-'.code) {
            parameterIsNegative = true
            ch = source.read()
            if (ch == -1) {
                throw RtfParseException("Unexpected end of file")
            }
        }
        if (ch.toChar().isDigit()) {
            commandHasParameter = true
            parameterText.append(ch.toChar())
            while (true) {
                ch = source.read()
                if (ch == -1 || !ch.toChar().isDigit()) {
                    break
                }
                parameterText.append(ch.toChar())
                if (parameterText.length > MAX_PARAMETER_LENGTH) {
                    break
                }
            }

            if (parameterText.length > MAX_PARAMETER_LENGTH) {
                throw IllegalArgumentException("Invalid parameter: $parameterText")
            }

            parameterValue = parameterText.toString().toInt()
            if (parameterIsNegative) {
                parameterValue = -parameterValue
            }
        }

        if (ch != ' '.code) {
            source.unread(ch)
        }

        handleCommand(commandText, parameterValue, commandHasParameter)
    }

    /**
     * Determine what to do with the extracted command.
     */
    private fun handleCommand(commandBuffer: StringBuilder, parameter: Int, hasParameter: Boolean) {
        val commandName = commandBuffer.toString()
        val command = Command.getInstance(commandName)

        //
        // Note that we silently ignore commands that we don't recognise
        //
        if (command != null) {
            if (command != Command.hex) {
                handleCharacterData()
            }

            when (command) {
                Command.bin -> handleBinaryData(parameter)

                Command.hex -> parsingHex = true

                else -> listener.processCommand(command, parameter, hasParameter, false)
            }
        }
    }

    /**
     * Pass accumulated character data to the listener.
     */
    private fun handleCharacterData() {
        val data = buffer.toArray()
        buffer.clear()
        listener.processCharacterBytes(data)
    }

    /**
     * Pass binary data to the listener.
     */
    private fun handleBinaryData(size: Int) {
        val data = ByteArray(size)
        val bytesRead = source.read(data)
        if (bytesRead != size) {
            throw RtfParseException("Unexpected end of file")
        }
        listener.processBinaryBytes(data)
    }

    /**
     * Inform the listener of a group start.
     */
    private fun handleGroupStart() {
        handleCharacterData()
        groupDepth++
        listener.processGroupStart()
    }

    /**
     * Inform the listener of a group end.
     */
    private fun handleGroupEnd() {
        handleCharacterData()
        listener.processGroupEnd()
        groupDepth--
    }

    private companion object {
        const val MAX_PARAMETER_LENGTH = 20
        const val MAX_COMMAND_LENGTH = 30
    }
}
