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

package com.darkrockstudios.libs.rtfparserkmp.writer

/** The fields, validation, equality and serialization shared by [RtfPicture] and [RtfInlinePicture]. */
internal class PictureData(
    val bytes: ByteArray,
    val format: RtfPictureFormat,
    val widthPx: Int,
    val heightPx: Int,
    val goalWidthTwips: Int?,
    val goalHeightTwips: Int?,
) {
    private val resolvedGoalWidthTwips: Int
    private val resolvedGoalHeightTwips: Int

    init {
        require(bytes.isNotEmpty()) { "Picture bytes must not be empty" }
        require(widthPx in 1..MAX_PX && heightPx in 1..MAX_PX) {
            "Picture size must be between 1 and $MAX_PX px, was ${widthPx}x$heightPx px"
        }
        require(goalWidthTwips == null || goalWidthTwips > 0) { "goalWidthTwips must be positive, was $goalWidthTwips" }
        require(goalHeightTwips == null || goalHeightTwips > 0) { "goalHeightTwips must be positive, was $goalHeightTwips" }

        resolvedGoalWidthTwips = when {
            goalWidthTwips != null -> goalWidthTwips
            goalHeightTwips != null -> scaleGoal(goalHeightTwips, widthPx, heightPx)
            else -> widthPx * TWIPS_PER_PIXEL
        }
        resolvedGoalHeightTwips = when {
            goalHeightTwips != null -> goalHeightTwips
            goalWidthTwips != null -> scaleGoal(goalWidthTwips, heightPx, widthPx)
            else -> heightPx * TWIPS_PER_PIXEL
        }
    }

    /**
     * Appends the `{\pict ...}` group. A missing goal is derived from the other one by the pixel aspect
     * ratio, or from the pixel size at 96 dpi when both are missing.
     */
    fun appendTo(sb: StringBuilder) {
        sb.append("{\\pict").append(format.keyword)
            .append("\\picw").append(widthPx)
            .append("\\pich").append(heightPx)
            .append("\\picwgoal").append(resolvedGoalWidthTwips)
            .append("\\pichgoal").append(resolvedGoalHeightTwips)
            .append(' ')
        // Readers ignore newlines inside the pict destination, so the hex can be wrapped freely.
        bytes.forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xFF
            sb.append(HEX_DIGITS[value shr 4]).append(HEX_DIGITS[value and 0xF])
            if ((index + 1) % BYTES_PER_LINE == 0 || index == bytes.lastIndex) sb.append('\n')
        }
        sb.append('}')
    }

    /** [knownGoal] * [numeratorPx] / [denominatorPx], rounded to the nearest twip and at least 1. */
    private fun scaleGoal(knownGoal: Int, numeratorPx: Int, denominatorPx: Int): Int {
        val scaled = (knownGoal.toLong() * numeratorPx + denominatorPx / 2) / denominatorPx
        require(scaled <= Int.MAX_VALUE) { "Derived picture goal $scaled twips is too large" }
        return scaled.toInt().coerceAtLeast(1)
    }

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is PictureData &&
                bytes.contentEquals(other.bytes) &&
                format == other.format &&
                widthPx == other.widthPx &&
                heightPx == other.heightPx &&
                goalWidthTwips == other.goalWidthTwips &&
                goalHeightTwips == other.goalHeightTwips
            )

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + widthPx
        result = 31 * result + heightPx
        result = 31 * result + (goalWidthTwips ?: 0)
        result = 31 * result + (goalHeightTwips ?: 0)
        return result
    }

    override fun toString(): String =
        "size=${bytes.size}, format=$format, widthPx=$widthPx, heightPx=$heightPx, " +
            "goalWidthTwips=$goalWidthTwips, goalHeightTwips=$goalHeightTwips"

    private companion object {
        /** 1440 twips per inch at 96 pixels per inch. */
        const val TWIPS_PER_PIXEL = 15

        /** The largest pixel size whose 96 dpi goal still fits in an Int. */
        const val MAX_PX = Int.MAX_VALUE / TWIPS_PER_PIXEL
        const val BYTES_PER_LINE = 64
        const val HEX_DIGITS = "0123456789abcdef"
    }
}
