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

package com.darkrockstudios.libs.rtfparserkmp.compose

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.darkrockstudios.libs.rtfparserkmp.model.CharacterStyle
import com.darkrockstudios.libs.rtfparserkmp.model.Paragraph
import com.darkrockstudios.libs.rtfparserkmp.model.StyledDocument
import com.darkrockstudios.libs.rtfparserkmp.model.TextRun
import com.darkrockstudios.libs.rtfparserkmp.writer.writeRtf

/**
 * Convert a Compose [AnnotatedString] to an RTF document string — the inverse of
 * [rtfToAnnotatedString]. Newlines split paragraphs; within each paragraph, contiguous characters
 * sharing the same effective formatting become one styled run.
 */
fun annotatedStringToRtf(annotatedString: AnnotatedString): String =
    writeRtf(annotatedString.toStyledDocument())

/**
 * Build a [StyledDocument] from this [AnnotatedString]. Each character's effective formatting is the
 * union of every overlapping [androidx.compose.ui.text.SpanStyle]: a `fontWeight` of
 * [FontWeight.Bold] or heavier maps to bold, [FontStyle.Italic] to italic, and
 * [TextDecoration.Underline] / [TextDecoration.LineThrough] to underline / strikethrough. Newlines
 * (`\n`) separate paragraphs and are not themselves emitted as text.
 */
fun AnnotatedString.toStyledDocument(): StyledDocument {
    val paragraphs = mutableListOf<Paragraph>()
    var runs = mutableListOf<TextRun>()
    val run = StringBuilder()
    var runStyle = CharacterStyle.PLAIN

    fun flushRun() {
        if (run.isNotEmpty()) {
            runs.add(TextRun(run.toString(), runStyle))
            run.clear()
        }
    }

    fun flushParagraph() {
        flushRun()
        paragraphs.add(Paragraph(runs))
        runs = mutableListOf()
    }

    for (index in text.indices) {
        val character = text[index]
        if (character == '\n') {
            flushParagraph()
            continue
        }
        val style = characterStyleAt(index)
        if (run.isEmpty()) {
            runStyle = style
        } else if (style != runStyle) {
            flushRun()
            runStyle = style
        }
        run.append(character)
    }
    flushParagraph()

    return StyledDocument(paragraphs)
}

private fun AnnotatedString.characterStyleAt(index: Int): CharacterStyle {
    var bold = false
    var italic = false
    var underline = false
    var strikethrough = false

    for (range in spanStyles) {
        if (index < range.start || index >= range.end) continue
        val style = range.item
        val weight = style.fontWeight
        if (weight != null && weight.weight >= FontWeight.Bold.weight) bold = true
        if (style.fontStyle == FontStyle.Italic) italic = true
        style.textDecoration?.let { decoration ->
            if (decoration.contains(TextDecoration.Underline)) underline = true
            if (decoration.contains(TextDecoration.LineThrough)) strikethrough = true
        }
    }

    return CharacterStyle(bold = bold, italic = italic, underline = underline, strikethrough = strikethrough)
}
