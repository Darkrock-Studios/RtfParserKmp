# Module rtf-writer

Emits RTF — from the simple styled-text model, from Markdown, or from a rich authoring model with
fonts, colors, headings, page breaks, hyperlinks and bookmarks. All output is 7-bit ASCII (non-ASCII
escaped as `\uN`), so the writer has no charset dependency and round-trips cleanly with `rtf-reader`.

## From Markdown

```kotlin
import com.darkrockstudios.libs.rtfparserkmp.writer.convertMarkdownToRtf

val rtf: String = convertMarkdownToRtf("This is **bold** and _italic_.")
```

## From the styled-text model

```kotlin
import com.darkrockstudios.libs.rtfparserkmp.writer.writeRtf
import com.darkrockstudios.libs.rtfparserkmp.model.*

val doc = StyledDocument(
    listOf(Paragraph(listOf(TextRun("Hello", CharacterStyle(bold = true)))))
)
val rtf: String = writeRtf(doc)
```

## Rich authoring

For documents that need real formatting, build an `RtfDocument` from `RtfParagraph` / `RtfTextRun`
(plus `RtfFont`, `RtfColor`, `RtfHyperlink`, `RtfBookmark`, `RtfPageBreak`, and paragraph layout via
`RtfParagraphStyle`) and serialize it with `RtfDocumentWriter`. See the
[Writing rich RTF guide](https://github.com/Darkrock-Studios/RtfParserKmp/blob/main/docs/WRITING-RICH-RTF.md).

# Package com.darkrockstudios.libs.rtfparserkmp.writer

Everything the writer exposes: the `RtfWriter` / `writeRtf` styled-model serializer, the
`MarkdownToRtf` / `convertMarkdownToRtf` converter, and the rich `RtfDocument` authoring model with
`RtfDocumentWriter`.
