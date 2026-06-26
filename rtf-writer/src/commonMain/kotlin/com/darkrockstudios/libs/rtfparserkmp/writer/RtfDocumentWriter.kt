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
 * Serializes an [RtfDocument] to a charset-free RTF string.
 *
 * Output is 7-bit ASCII: every code unit above 127 is emitted as a signed `\uN` escape with a `?`
 * fallback (see [appendRtfEscaped]). The font and color tables are derived from the fonts and colors
 * actually referenced by the document (plus [RtfDocument.defaultFont]); each run is wrapped in its
 * own `{...}` group so character formatting cannot leak between runs.
 */
class RtfDocumentWriter {
    fun write(document: RtfDocument): String {
        val (fonts, colors) = collectResources(document)
        val fontIndex = fonts.withIndex().associate { (i, font) -> font to i }
        // Color index 0 is the reserved "auto" entry, so referenced colors start at 1.
        val colorIndex = colors.withIndex().associate { (i, color) -> color to i + 1 }
        val ctx = WriteContext(fontIndex, colorIndex)

        val sb = StringBuilder()
        writeHeader(sb, document, fonts, colors)
        writeInfo(sb, document.info)
        document.generator?.let {
            sb.append("{\\*\\generator ")
            sb.appendRtfEscaped(it)
            sb.append(";}")
        }
        sb.append("\\fs").append(document.defaultFontSizeHalfPoints).append('\n')
        for (block in document.blocks) {
            writeBlock(sb, block, ctx)
        }
        sb.append("}")
        return sb.toString()
    }

    private fun writeHeader(
        sb: StringBuilder,
        document: RtfDocument,
        fonts: List<RtfFont>,
        colors: List<RtfColor>,
    ) {
        sb.append("{\\rtf1\\ansi\\ansicpg").append(document.codePage)
            .append("\\deff0\\deflang").append(document.defaultLanguage)

        sb.append("{\\fonttbl")
        fonts.forEachIndexed { index, font ->
            sb.append("{\\f").append(index).append(font.family.keyword)
                .append("\\fcharset").append(font.charset).append(' ')
            sb.appendRtfEscaped(font.name)
            sb.append(";}")
        }
        sb.append('}')

        // Index 0 is the default "auto" color (the leading ';'); referenced colors follow.
        sb.append("{\\colortbl;")
        for (color in colors) {
            sb.append("\\red").append(color.red)
                .append("\\green").append(color.green)
                .append("\\blue").append(color.blue).append(';')
        }
        sb.append('}')
    }

    private fun writeInfo(sb: StringBuilder, info: RtfInfo?) {
        if (info == null) return
        val fields = listOf(
            "title" to info.title,
            "author" to info.author,
            "subject" to info.subject,
            "company" to info.company,
        ).filter { !it.second.isNullOrEmpty() }
        if (fields.isEmpty()) return

        sb.append("{\\info")
        for ((name, value) in fields) {
            sb.append("{\\").append(name).append(' ')
            sb.appendRtfEscaped(value!!)
            sb.append('}')
        }
        sb.append('}')
    }

    private fun writeBlock(sb: StringBuilder, block: RtfBlock, ctx: WriteContext) {
        when (block) {
            RtfPageBreak -> sb.append("\\page\n")
            is RtfParagraph -> writeParagraph(sb, block, ctx)
        }
    }

    private fun writeParagraph(sb: StringBuilder, paragraph: RtfParagraph, ctx: WriteContext) {
        val style = paragraph.style
        sb.append("\\pard")
        style.alignment.keyword?.let(sb::append)
        if (style.spaceBeforeTwips != 0) sb.append("\\sb").append(style.spaceBeforeTwips)
        if (style.spaceAfterTwips != 0) sb.append("\\sa").append(style.spaceAfterTwips)
        if (style.leftIndentTwips != 0) sb.append("\\li").append(style.leftIndentTwips)
        if (style.firstLineIndentTwips != 0) sb.append("\\fi").append(style.firstLineIndentTwips)
        if (style.keepWithNext) sb.append("\\keepn")
        style.bottomBorder?.let {
            sb.append("\\brdrb").append(it.style.keyword).append("\\brdrw").append(it.widthTwips)
        }
        sb.append(' ')

        for (inline in paragraph.content) {
            writeInline(sb, inline, ctx)
        }
        sb.append("\\par\n")
    }

    private fun writeInline(sb: StringBuilder, inline: RtfInline, ctx: WriteContext) {
        when (inline) {
            is RtfTextRun -> writeRun(sb, inline, ctx)
            RtfLineBreak -> sb.append("\\line ")
            RtfTab -> sb.append("\\tab ")
            is RtfHyperlink -> writeHyperlink(sb, inline, ctx)
            is RtfBookmark -> writeBookmark(sb, inline, ctx)
        }
    }

    private fun writeRun(sb: StringBuilder, run: RtfTextRun, ctx: WriteContext) {
        val style = run.style
        sb.append('{')
        val start = sb.length
        if (style.bold) sb.append("\\b")
        if (style.italic) sb.append("\\i")
        if (style.underline) sb.append("\\ul")
        if (style.strikethrough) sb.append("\\strike")
        if (style.superscript) sb.append("\\super")
        if (style.subscript) sb.append("\\sub")
        style.font?.let { sb.append("\\f").append(ctx.fontIndex.getValue(it)) }
        style.fontSizeHalfPoints?.let { sb.append("\\fs").append(it) }
        style.color?.let { sb.append("\\cf").append(ctx.colorIndex.getValue(it)) }
        // Separate control words from the text with a space, but only if any were emitted.
        if (sb.length > start) sb.append(' ')
        sb.appendRtfEscaped(run.text)
        sb.append('}')
    }

    private fun writeHyperlink(sb: StringBuilder, link: RtfHyperlink, ctx: WriteContext) {
        sb.append("{\\field{\\*\\fldinst HYPERLINK ")
        // The bookmark switch is a literal backslash-l, escaped as '\\l' in the RTF stream.
        if (link.kind == RtfHyperlinkKind.Bookmark) sb.append("\\\\l ")
        sb.append('"')
        sb.appendRtfHyperlinkTarget(link.target)
        sb.append("\"}{\\fldrslt {")
        for (inline in link.content) {
            writeInline(sb, inline, ctx)
        }
        sb.append("}}}")
    }

    private fun writeBookmark(sb: StringBuilder, bookmark: RtfBookmark, ctx: WriteContext) {
        sb.append("{\\*\\bkmkstart ")
        sb.appendRtfEscaped(bookmark.name)
        sb.append('}')
        for (inline in bookmark.content) {
            writeInline(sb, inline, ctx)
        }
        sb.append("{\\*\\bkmkend ")
        sb.appendRtfEscaped(bookmark.name)
        sb.append('}')
    }

    /** One pass over the document gathering the fonts (including the default) and colors it references. */
    private fun collectResources(document: RtfDocument): Pair<List<RtfFont>, List<RtfColor>> {
        val fonts = LinkedHashSet<RtfFont>().apply { add(document.defaultFont) }
        val colors = LinkedHashSet<RtfColor>()
        forEachRunInBlocks(document.blocks) { run ->
            run.style.font?.let(fonts::add)
            run.style.color?.let(colors::add)
        }
        return fonts.toList() to colors.toList()
    }

    private fun forEachRunInBlocks(blocks: List<RtfBlock>, action: (RtfTextRun) -> Unit) {
        for (block in blocks) {
            if (block is RtfParagraph) forEachRunInInlines(block.content, action)
        }
    }

    private fun forEachRunInInlines(inlines: List<RtfInline>, action: (RtfTextRun) -> Unit) {
        for (inline in inlines) {
            when (inline) {
                is RtfTextRun -> action(inline)
                is RtfHyperlink -> forEachRunInInlines(inline.content, action)
                is RtfBookmark -> forEachRunInInlines(inline.content, action)
                RtfLineBreak, RtfTab -> {}
            }
        }
    }

    private class WriteContext(
        val fontIndex: Map<RtfFont, Int>,
        val colorIndex: Map<RtfColor, Int>,
    )
}

/** Convenience top-level entry point equivalent to `RtfDocumentWriter().write(document)`. */
fun writeRtfDocument(document: RtfDocument): String = RtfDocumentWriter().write(document)
