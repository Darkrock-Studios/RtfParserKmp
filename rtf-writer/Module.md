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

For documents that need real formatting — fonts, colors, headings, page breaks, hyperlinks,
bookmarks, and paragraph layout — build an immutable `RtfDocument` and serialize it with
`RtfDocumentWriter` / `writeRtfDocument`. It's a strongly-typed alternative to hand-concatenating
control words: you describe the document as data and the writer does the rest. The rich model lives
entirely in `rtf-writer` and adds nothing to `rtf-core`; the styled-text round-trip model above is
untouched. Like the minimal writer, output is **7-bit ASCII** — every code unit above 127 is emitted
as a signed `\uN` escape with a `?` fallback, so the result is charset-free and round-trips cleanly
through the reader.

### The model at a glance

```
RtfDocument
├── defaultFont: RtfFont                 // \deff0 body font (always font index 0)
├── defaultFontSizeHalfPoints: Int       // document \fsN default (24 = 12pt)
├── info: RtfInfo?                        // {\info \title \author \subject \company}
├── generator: String?                   // {\*\generator ...}
├── codePage / defaultLanguage: Int      // \ansicpgN / \deflangN (cosmetic; output is ASCII)
└── blocks: List<RtfBlock>
    ├── RtfPageBreak                      // \page
    └── RtfParagraph
        ├── style: RtfParagraphStyle      // alignment, spacing, indents, keepWithNext, bottomBorder
        └── content: List<RtfInline>
            ├── RtfTextRun(text, RtfSpanStyle)   // bold/italic/underline/strike/super/sub, font, size, color
            ├── RtfLineBreak                     // \line
            ├── RtfTab                           // \tab
            ├── RtfHyperlink(target, content, kind)   // external URL or in-document bookmark
            └── RtfBookmark(name, content)            // a jump target wrapping its content
```

#### Fonts and colors are referenced by value

You never deal in `\fN` / `\cfN` index numbers. Build `RtfFont` and `RtfColor` values, attach them to
the runs that use them, and `RtfDocumentWriter` collects every referenced font (plus `defaultFont`)
and color into the document's `\fonttbl` / `\colortbl` and resolves the indices for you.

- The `defaultFont` is always font index 0.
- Color index 0 is reserved for the document default ("auto") color, so a run with `color = null`
  uses the reader's default text color and emits no `\cf`.

```kotlin
val accent = RtfColor.fromRgb(0x7E57C2)        // or RtfColor(126, 87, 194)
val mono = RtfFont("Consolas", RtfFontFamily.Modern)
RtfTextRun("code", RtfSpanStyle(font = mono, color = accent))
```

#### Units

RTF measures size in **half-points** and spacing/indents in **twips** (twentieths of a point;
**1440 twips = 1 inch**). The field names carry the unit so there's no ambiguity:

| Field | Unit | Example |
|---|---|---|
| `RtfSpanStyle.fontSizeHalfPoints` | half-points | `24` = 12pt, `72` = 36pt |
| `RtfParagraphStyle.spaceBeforeTwips` / `spaceAfterTwips` | twips | `240` = 12pt of space |
| `RtfParagraphStyle.firstLineIndentTwips` | twips (negative hangs) | `360` = 0.25", `-360` = hanging |
| `RtfParagraphStyle.leftIndentTwips` | twips | `720` = 0.5" |
| `RtfBorder.widthTwips` | twips | `10` = 0.5pt rule |

`RtfAlignment.Left` is the RTF default and emits no control word; `Center` / `Right` / `Justify`
emit `\qc` / `\qr` / `\qj`.

### A worked example

A title page, a contents entry linking to a chapter bookmark, and a formatted body paragraph:

```kotlin
import com.darkrockstudios.libs.rtfparserkmp.writer.*

val body = RtfFont("Georgia", RtfFontFamily.Roman)
val accent = RtfColor.fromRgb(0x7E57C2)

val doc = RtfDocument(
    defaultFont = body,
    defaultFontSizeHalfPoints = 24,                 // 12pt body
    info = RtfInfo(title = "My Story", author = "Adam"),
    generator = "Hammer 3.4.2",
    blocks = listOf(
        // Title page — big, centered, accent-colored.
        RtfParagraph(
            content = listOf(
                RtfTextRun("My Story", RtfSpanStyle(bold = true, fontSizeHalfPoints = 72, color = accent)),
            ),
            style = RtfParagraphStyle(
                alignment = RtfAlignment.Center,
                spaceBeforeTwips = 3600,
                spaceAfterTwips = 240,
            ),
        ),

        // Contents — a link that jumps to the "chapter1" bookmark.
        RtfPageBreak,
        RtfParagraph(
            content = listOf(
                RtfHyperlink(
                    target = "chapter1",
                    kind = RtfHyperlinkKind.Bookmark,
                    content = listOf(
                        RtfTextRun("1. Beginnings", RtfSpanStyle(underline = true, color = accent)),
                    ),
                ),
            ),
            style = RtfParagraphStyle(spaceAfterTwips = 160),
        ),

        // Chapter 1 — heading carries the bookmark target; body has a first-line indent and a link.
        RtfPageBreak,
        RtfParagraph(
            content = listOf(
                RtfBookmark(
                    name = "chapter1",
                    content = listOf(
                        RtfTextRun("1. Beginnings", RtfSpanStyle(bold = true, fontSizeHalfPoints = 48, color = accent)),
                    ),
                ),
            ),
            style = RtfParagraphStyle(spaceBeforeTwips = 360, spaceAfterTwips = 160, keepWithNext = true),
        ),
        RtfParagraph(
            content = listOf(
                RtfTextRun("It was a "),
                RtfTextRun("dark", RtfSpanStyle(italic = true)),
                RtfTextRun(" and stormy night. See "),
                RtfHyperlink(
                    target = "https://example.com",
                    content = listOf(RtfTextRun("the docs", RtfSpanStyle(underline = true, color = accent))),
                ),
                RtfTextRun("."),
            ),
            style = RtfParagraphStyle(firstLineIndentTwips = 360, spaceAfterTwips = 160),
        ),
    ),
)

val rtf: String = writeRtfDocument(doc)   // or RtfDocumentWriter().write(doc)
```

### Recipes

**A horizontal rule** — an empty paragraph with a bottom border:

```kotlin
RtfParagraph(emptyList(), RtfParagraphStyle(spaceAfterTwips = 160, bottomBorder = RtfBorder()))
```

**A monospaced inline code span** — reference a second font on just that run:

```kotlin
val mono = RtfFont("Consolas", RtfFontFamily.Modern)
RtfParagraph(listOf(
    RtfTextRun("Run "),
    RtfTextRun("ls -la", RtfSpanStyle(font = mono)),
    RtfTextRun(" to list files."),
))
```

**A hard line break inside a paragraph** — `RtfLineBreak` (use `RtfTab` for a tab):

```kotlin
RtfParagraph(listOf(RtfTextRun("line one"), RtfLineBreak, RtfTextRun("line two")))
```

### API reference

All types live in `com.darkrockstudios.libs.rtfparserkmp.writer`.

| Type | Purpose |
|---|---|
| `RtfDocument` | document root: blocks + default font/size + metadata |
| `RtfDocumentWriter` / `writeRtfDocument(doc)` | serialize a document to an RTF string |
| `RtfInfo` | `\info` metadata (title, author, subject, company) |
| `RtfFont` / `RtfFontFamily` | a font-table entry and its family class (`\froman`, `\fmodern`, …) |
| `RtfColor` (`.fromRgb`) | a color-table entry |
| `RtfBlock` → `RtfParagraph`, `RtfPageBreak` | block-level content |
| `RtfParagraphStyle` / `RtfAlignment` / `RtfBorder` / `RtfBorderStyle` | paragraph layout |
| `RtfInline` → `RtfTextRun`, `RtfLineBreak`, `RtfTab`, `RtfHyperlink`, `RtfBookmark` | inline content |
| `RtfSpanStyle` | character formatting for a run (style, font, size, color) |
| `RtfHyperlinkKind` | `Url` (external) or `Bookmark` (in-document jump) |

# Package com.darkrockstudios.libs.rtfparserkmp.writer

Everything the writer exposes: the `RtfWriter` / `writeRtf` styled-model serializer, the
`MarkdownToRtf` / `convertMarkdownToRtf` converter, and the rich `RtfDocument` authoring model with
`RtfDocumentWriter`.
