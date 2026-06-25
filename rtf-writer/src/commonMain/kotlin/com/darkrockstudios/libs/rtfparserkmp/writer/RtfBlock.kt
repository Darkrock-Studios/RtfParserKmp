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

/** A block-level element in an [RtfDocument]. */
sealed interface RtfBlock

/** Horizontal paragraph alignment. [Left] is the RTF default and emits no control word. */
enum class RtfAlignment(internal val keyword: String?) {
    Left(null),
    Center("\\qc"),
    Right("\\qr"),
    Justify("\\qj"),
}

/** A line style for an [RtfBorder]. */
enum class RtfBorderStyle(internal val keyword: String) {
    Single("\\brdrs"),
    Double("\\brdrdb"),
    Thick("\\brdrth"),
    Dotted("\\brdrdot"),
    Dashed("\\brdrdash"),
}

/**
 * A paragraph border edge.
 *
 * @param widthTwips the `\brdrwN` line width in twips (twentieths of a point).
 */
data class RtfBorder(
    val style: RtfBorderStyle = RtfBorderStyle.Single,
    val widthTwips: Int = 10,
)

/**
 * Paragraph-level formatting. All measurements are in twips (twentieths of a point; 1440 = 1 inch);
 * [firstLineIndentTwips] may be negative for a hanging indent.
 *
 * @param spaceBeforeTwips `\sbN` space above the paragraph.
 * @param spaceAfterTwips `\saN` space below the paragraph.
 * @param firstLineIndentTwips `\fiN` first-line indent (negative hangs).
 * @param leftIndentTwips `\liN` left indent for the whole paragraph.
 * @param keepWithNext `\keepn` — keep this paragraph on the same page as the next.
 * @param bottomBorder a rule drawn below the paragraph; an empty paragraph with one renders a `\hr`.
 */
data class RtfParagraphStyle(
    val alignment: RtfAlignment = RtfAlignment.Left,
    val spaceBeforeTwips: Int = 0,
    val spaceAfterTwips: Int = 0,
    val firstLineIndentTwips: Int = 0,
    val leftIndentTwips: Int = 0,
    val keepWithNext: Boolean = false,
    val bottomBorder: RtfBorder? = null,
) {
    companion object {
        val Default = RtfParagraphStyle()
    }
}

/** A paragraph: styled inline [content] plus paragraph-level formatting. */
data class RtfParagraph(
    val content: List<RtfInline>,
    val style: RtfParagraphStyle = RtfParagraphStyle.Default,
) : RtfBlock

/** Forces a page break before the following content (`\page`). */
data object RtfPageBreak : RtfBlock
