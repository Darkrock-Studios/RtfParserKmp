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

/** The RTF font-family class emitted in the font table (`\froman`, `\fmodern`, …). */
enum class RtfFontFamily(internal val keyword: String) {
    Nil("\\fnil"),
    Roman("\\froman"),
    Swiss("\\fswiss"),
    Modern("\\fmodern"),
    Script("\\fscript"),
    Decorative("\\fdecor"),
    Technical("\\ftech"),
    Bidirectional("\\fbidi"),
}

/**
 * A font usable by [RtfDocument]. Fonts are referenced by value from an [RtfSpanStyle]; the writer
 * collects every referenced font (plus the document default) into the `\fonttbl` and assigns the
 * `\fN` indices, so callers never juggle indices themselves.
 *
 * @param name the font face name as it appears in the font table (e.g. `Georgia`).
 * @param family the RTF family class; readers fall back to it when the named face is unavailable.
 * @param charset the `\fcharsetN` value; `0` is the default ANSI charset.
 */
data class RtfFont(
    val name: String,
    val family: RtfFontFamily = RtfFontFamily.Nil,
    val charset: Int = 0,
)
