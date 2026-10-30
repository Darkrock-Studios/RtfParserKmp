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

import com.darkrockstudios.libs.rtfparserkmp.model.PictureFormat

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
        val command: com.darkrockstudios.libs.rtfparserkmp.rtf.Command,
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

    /**
     * A decoded `\pict` group. It follows the `pict` [Command] that opened the group and precedes the
     * group's [GroupEnd]; the group's hex payload and nested groups are not emitted as events.
     *
     * [width] and [height] are the raw `\picwN` / `\pichN` values, whose units vary by producer: the
     * spec says pixels for bitmaps, but Word writes hundredths of a millimetre even for PNG and JPEG.
     * Use [goalWidthTwips] / [goalHeightTwips], scaled by [scaleXPercent] / [scaleYPercent], for the
     * display size.
     *
     * @param bytes the image data, from the group's hex text or its `\binN` blob.
     * @param format the declared encoding, or `null` when the group names none the parser knows.
     * @param width the raw `\picwN` source width.
     * @param height the raw `\pichN` source height.
     * @param goalWidthTwips the `\picwgoalN` rendered width in twips, before scaling.
     * @param goalHeightTwips the `\pichgoalN` rendered height in twips, before scaling.
     * @param scaleXPercent the `\picscalexN` horizontal scale. Word keeps the goals at the picture's
     *   original size and records a user resize here.
     * @param scaleYPercent the `\picscaleyN` vertical scale, as for [scaleXPercent].
     */
    class Picture(
        val bytes: ByteArray,
        val format: PictureFormat?,
        val width: Int?,
        val height: Int?,
        val goalWidthTwips: Int?,
        val goalHeightTwips: Int?,
        val scaleXPercent: Int = 100,
        val scaleYPercent: Int = 100,
    ) : RtfEvent {
        override fun equals(other: Any?): Boolean =
            this === other || (
                other is Picture &&
                    bytes.contentEquals(other.bytes) &&
                    format == other.format &&
                    width == other.width &&
                    height == other.height &&
                    goalWidthTwips == other.goalWidthTwips &&
                    goalHeightTwips == other.goalHeightTwips &&
                    scaleXPercent == other.scaleXPercent &&
                    scaleYPercent == other.scaleYPercent
                )

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + format.hashCode()
            result = 31 * result + (width ?: 0)
            result = 31 * result + (height ?: 0)
            result = 31 * result + (goalWidthTwips ?: 0)
            result = 31 * result + (goalHeightTwips ?: 0)
            result = 31 * result + scaleXPercent
            result = 31 * result + scaleYPercent
            return result
        }

        override fun toString(): String =
            "Picture(size=${bytes.size}, format=$format, width=$width, height=$height, " +
                "goalWidthTwips=$goalWidthTwips, goalHeightTwips=$goalHeightTwips, " +
                "scaleXPercent=$scaleXPercent, scaleYPercent=$scaleYPercent)"
    }
}
