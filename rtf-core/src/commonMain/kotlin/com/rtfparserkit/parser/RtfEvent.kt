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

package com.rtfparserkit.parser

/**
 * A single event emitted while parsing an RTF document — the idiomatic sealed-type replacement for
 * the upstream per-event classes. Consumers can drive a `when` over these, or implement [RtfListener].
 */
sealed interface RtfEvent {
    /** The parser has started reading the document. */
    data object DocumentStart : RtfEvent

    /** The parser has reached the end of the document. */
    data object DocumentEnd : RtfEvent

    /** The start of a group. */
    data object GroupStart : RtfEvent

    /** The end of a group. */
    data object GroupEnd : RtfEvent

    /** A decoded run of text. */
    data class Text(val text: String) : RtfEvent

    /** A command, with its optional integer [parameter]. */
    data class Command(
        val command: com.rtfparserkit.rtf.Command,
        val parameter: Int,
        val hasParameter: Boolean,
        val optional: Boolean,
    ) : RtfEvent

    /** Raw binary bytes from a `\binN` blob. */
    class BinaryBytes(val bytes: ByteArray) : RtfEvent {
        override fun equals(other: Any?): Boolean =
            this === other || (other is BinaryBytes && bytes.contentEquals(other.bytes))

        override fun hashCode(): Int = bytes.contentHashCode()

        override fun toString(): String = "BinaryBytes(size=${bytes.size})"
    }
}
