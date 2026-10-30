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

/** A piece of inline content inside an [RtfParagraph]. */
sealed interface RtfInline

/**
 * Character-level formatting for an [RtfTextRun]. A `null` [font], [fontSizeHalfPoints] or [color]
 * means "inherit the document/paragraph default" — the writer emits the corresponding control word
 * only when the field is set.
 *
 * @param fontSizeHalfPoints the `\fsN` size in half-points (24 = 12pt).
 */
data class RtfSpanStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val superscript: Boolean = false,
    val subscript: Boolean = false,
    val font: RtfFont? = null,
    val fontSizeHalfPoints: Int? = null,
    val color: RtfColor? = null,
) {
    companion object {
        /** No character formatting — plain text in the document's default font. */
        val Default = RtfSpanStyle()
    }
}

/** A contiguous run of [text] sharing a single [style]. */
data class RtfTextRun(
    val text: String,
    val style: RtfSpanStyle = RtfSpanStyle.Default,
) : RtfInline

/** A forced line break within a paragraph (`\line`). */
data object RtfLineBreak : RtfInline

/** A tab (`\tab`). */
data object RtfTab : RtfInline

/** Whether an [RtfHyperlink] points at an external URL or an in-document [RtfBookmark]. */
enum class RtfHyperlinkKind { Url, Bookmark }

/**
 * A hyperlink field. For [RtfHyperlinkKind.Url] the [target] is an external URL; for
 * [RtfHyperlinkKind.Bookmark] it is the name of an [RtfBookmark] elsewhere in the document. The
 * visible, clickable text is the styled [content] (set underline/color on its runs to taste).
 */
data class RtfHyperlink(
    val target: String,
    val content: List<RtfInline>,
    val kind: RtfHyperlinkKind = RtfHyperlinkKind.Url,
) : RtfInline

/** A named bookmark wrapping [content], the jump target for a [RtfHyperlinkKind.Bookmark] link. */
data class RtfBookmark(
    val name: String,
    val content: List<RtfInline>,
) : RtfInline

/**
 * A picture flowing inside a paragraph, like a large character. See [RtfPicture] for one on a line
 * of its own; the parameters are the same apart from style. The 96 dpi default size exceeds a
 * 6.5 inch text width for images wider than 624 px, so pass a goal for large images.
 *
 * @throws IllegalArgumentException if [bytes] is empty, a size is not positive, or a size or derived
 *   goal does not fit in an Int number of twips.
 */
class RtfInlinePicture(
    val bytes: ByteArray,
    val format: RtfPictureFormat,
    val widthPx: Int,
    val heightPx: Int,
    val goalWidthTwips: Int? = null,
    val goalHeightTwips: Int? = null,
) : RtfInline {
    internal val data = PictureData(bytes, format, widthPx, heightPx, goalWidthTwips, goalHeightTwips)

    override fun equals(other: Any?): Boolean =
        this === other || (other is RtfInlinePicture && data == other.data)

    override fun hashCode(): Int = data.hashCode()

    override fun toString(): String = "RtfInlinePicture($data)"
}
