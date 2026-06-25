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

/** Document metadata emitted in the `{\info ...}` group. Blank fields are omitted. */
data class RtfInfo(
    val title: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val company: String? = null,
)

/**
 * A rich, strongly-typed RTF document model for authoring arbitrarily complex output, the
 * builder-style counterpart to [StyledDocument]'s minimal round-trip model. Construct one directly
 * and serialize it with [RtfDocumentWriter].
 *
 * Fonts and colors are referenced by value from [RtfSpanStyle]; [RtfDocumentWriter] gathers every
 * referenced font (plus [defaultFont]) and color into the document's tables and resolves their
 * indices, so the model never deals in raw `\fN` / `\cfN` numbers.
 *
 * @param defaultFont the `\deff0` body font; always font index 0 in the table.
 * @param defaultFontSizeHalfPoints the document `\fsN` default in half-points (24 = 12pt).
 * @param codePage the `\ansicpgN` code page (cosmetic — output is 7-bit ASCII with `\uN` escapes).
 * @param defaultLanguage the `\deflangN` language id.
 */
data class RtfDocument(
    val blocks: List<RtfBlock>,
    val defaultFont: RtfFont,
    val defaultFontSizeHalfPoints: Int = 24,
    val info: RtfInfo? = null,
    val generator: String? = null,
    val codePage: Int = 1252,
    val defaultLanguage: Int = 1033,
)
