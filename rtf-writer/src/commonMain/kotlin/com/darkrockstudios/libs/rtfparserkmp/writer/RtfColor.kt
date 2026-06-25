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

/**
 * An RGB color usable by [RtfSpanStyle]. Colors are referenced by value; the writer collects every
 * referenced color into the `\colortbl` (index 0 is reserved for the document default "auto" color)
 * and assigns the `\cfN` indices.
 */
data class RtfColor(val red: Int, val green: Int, val blue: Int) {
    init {
        require(red in 0..255) { "red out of range: $red" }
        require(green in 0..255) { "green out of range: $green" }
        require(blue in 0..255) { "blue out of range: $blue" }
    }

    companion object {
        /** Builds a color from a packed `0xRRGGBB` integer; the high byte is ignored. */
        fun fromRgb(rgb: Int): RtfColor =
            RtfColor((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)
    }
}
