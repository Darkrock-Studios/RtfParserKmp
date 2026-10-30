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
 * @param pageBreakBefore `\pagebb`: start this paragraph on a new page.
 */
data class RtfParagraphStyle(
    val alignment: RtfAlignment = RtfAlignment.Left,
    val spaceBeforeTwips: Int = 0,
    val spaceAfterTwips: Int = 0,
    val firstLineIndentTwips: Int = 0,
    val leftIndentTwips: Int = 0,
    val keepWithNext: Boolean = false,
    val bottomBorder: RtfBorder? = null,
    val pageBreakBefore: Boolean = false,
) {
    companion object {
        /** Left-aligned with no indents, spacing, or border — the implicit paragraph style. */
        val Default = RtfParagraphStyle()
    }
}

/** A paragraph: styled inline [content] plus paragraph-level formatting. */
data class RtfParagraph(
    val content: List<RtfInline>,
    val style: RtfParagraphStyle = RtfParagraphStyle.Default,
) : RtfBlock

/**
 * Forces a page break before the following content (`\page`).
 *
 * For a break that belongs to a paragraph, use [RtfParagraphStyle.pageBreakBefore] instead: a bare
 * `\page` lands at the start of the next paragraph, and Word places that line, with its space-before, on
 * the current page.
 */
data object RtfPageBreak : RtfBlock

/** An image encoding the writer can embed in a `\pict` group. */
enum class RtfPictureFormat(internal val keyword: String) {
    Png("\\pngblip"),
    Jpeg("\\jpegblip"),
}

/**
 * A picture on a line of its own: an [RtfInlinePicture] alone in a paragraph laid out with [style].
 *
 * @param bytes the encoded image file, in [format].
 * @param format the image encoding of [bytes].
 * @param widthPx the image's width in pixels (`\picwN`).
 * @param heightPx the image's height in pixels (`\pichN`).
 * @param goalWidthTwips the rendered width in twips (`\picwgoalN`). When only one goal is given the
 *   other follows from the pixel aspect ratio; when neither is, both come from the pixel size at 96 dpi
 *   (15 twips per pixel), which exceeds a 6.5 inch text width for images wider than 624 px, so pass
 *   a goal for large images.
 * @param goalHeightTwips the rendered height in twips (`\pichgoalN`).
 * @param style the layout of the paragraph holding the picture, such as its alignment and spacing.
 * @throws IllegalArgumentException if [bytes] is empty, a size is not positive, or a size or derived
 *   goal does not fit in an Int number of twips.
 */
class RtfPicture(
    val bytes: ByteArray,
    val format: RtfPictureFormat,
    val widthPx: Int,
    val heightPx: Int,
    val goalWidthTwips: Int? = null,
    val goalHeightTwips: Int? = null,
    val style: RtfParagraphStyle = RtfParagraphStyle.Default,
) : RtfBlock {
    internal val data = PictureData(bytes, format, widthPx, heightPx, goalWidthTwips, goalHeightTwips)

    override fun equals(other: Any?): Boolean =
        this === other || (other is RtfPicture && data == other.data && style == other.style)

    override fun hashCode(): Int = 31 * data.hashCode() + style.hashCode()

    override fun toString(): String = "RtfPicture($data, style=$style)"
}
