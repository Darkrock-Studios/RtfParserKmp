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

package com.rtfparserkit.model

/**
 * The character-level formatting carried by a [TextRun]. This is the shared vocabulary the reader
 * produces and the writer consumes.
 */
data class CharacterStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val superscript: Boolean = false,
    val subscript: Boolean = false,
) {
    companion object {
        val PLAIN = CharacterStyle()
    }
}

/** Paragraph-level formatting. [styleRef] is the `\sN` stylesheet index, used for heading detection. */
data class ParagraphStyle(
    val styleRef: Int? = null,
)

/** A contiguous run of [text] sharing a single [CharacterStyle]. */
data class TextRun(
    val text: String,
    val style: CharacterStyle = CharacterStyle.PLAIN,
)

/** A paragraph: a sequence of styled [runs] plus paragraph-level formatting. */
data class Paragraph(
    val runs: List<TextRun> = emptyList(),
    val style: ParagraphStyle = ParagraphStyle(),
) {
    /** The concatenated plain text of all runs. */
    val text: String get() = runs.joinToString(separator = "") { it.text }
}

/** A styled document: an ordered list of [paragraphs]. */
data class StyledDocument(
    val paragraphs: List<Paragraph> = emptyList(),
) {
    /** The plain text of the document, paragraphs separated by newlines. */
    val text: String get() = paragraphs.joinToString(separator = "\n") { it.text }
}
