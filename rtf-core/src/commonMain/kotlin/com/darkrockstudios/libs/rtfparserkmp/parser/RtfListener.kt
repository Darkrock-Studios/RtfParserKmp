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

package com.darkrockstudios.libs.rtfparserkmp.parser

import com.darkrockstudios.libs.rtfparserkmp.rtf.Command

/**
 * Receives events from an RTF parser. Every method has a no-op default, so implementors override
 * only the events they care about (replacing the upstream `RtfListenerAdaptor`).
 */
interface RtfListener {
    /** The parser has started reading the document. */
    fun processDocumentStart() {}

    /** The parser has reached the end of the document. */
    fun processDocumentEnd() {}

    /** The start of a group was encountered. */
    fun processGroupStart() {}

    /** The end of a group was encountered. */
    fun processGroupEnd() {}

    /**
     * Raw character bytes "as read" from the file, before any encoding has been applied.
     * Emitted by the low-level parser; most consumers want [processString] instead.
     */
    fun processCharacterBytes(data: ByteArray) {}

    /** Raw binary bytes (from a `\binN` blob). */
    fun processBinaryBytes(data: ByteArray) {}

    /** Decoded string data; no further processing is required. */
    fun processString(string: String) {}

    /**
     * A command read from the file. [parameter] is meaningful only when [hasParameter] is true.
     * When [optional] is set, readers may choose not to implement the command.
     */
    fun processCommand(command: Command, parameter: Int, hasParameter: Boolean, optional: Boolean) {}
}
