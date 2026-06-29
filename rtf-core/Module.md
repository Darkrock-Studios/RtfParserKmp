# Module rtf-core

The shared, dependency-free foundation every other RtfParserKmp module builds on. It defines the RTF
event model, the listener and source abstractions, the full command dictionary, and the styled-text
document model — all in `commonMain`, with no platform code.

You rarely depend on `rtf-core` directly: pull in `rtf-reader` to parse or `rtf-writer` to emit, and
this module comes along transitively. Depend on it on its own only when you build your own parser
front-end or listener.

## The event model

Parsing an RTF stream produces a flat sequence of `RtfEvent`s — `DocumentStart`/`DocumentEnd`,
`GroupStart`/`GroupEnd`, `Text`, `Command`, and `BinaryBytes`. Consume it either by implementing
`RtfListener` (every method has a no-op default, so override only what you care about) or by walking
the sealed `RtfEvent` list that `rtf-reader`'s `parseRtf` returns.

```kotlin
class TitleGrabber : RtfListener {
    val sb = StringBuilder()
    override fun processString(string: String) { sb.append(string) }
}
```

## Commands

`Command` is the exhaustive enum of RTF control words, each tagged with a `CommandType` — `Symbol`,
`Flag`, `Toggle`, `Value`, `Destination`, or `Encoding` — describing how the parser treats it.

## Reading from your own bytes

`RtfSource` is the one-byte-at-a-time input the parser reads from, with `unread` for lookahead.
`ByteArrayRtfSource` wraps a `ByteArray`; the `rtf-io-kotlinx` and `rtf-io-okio` modules adapt
streaming sources.

## Styled-text model

`StyledDocument` → `Paragraph` → `TextRun`, where each run carries a `CharacterStyle` and each
paragraph a `ParagraphStyle`. It is the common currency between the reader's converters and the writer.

# Package com.darkrockstudios.libs.rtfparserkmp.parser

The event model and parsing inputs: `RtfEvent`, `RtfListener`, `RtfSource`, and `ByteArrayRtfSource`.

# Package com.darkrockstudios.libs.rtfparserkmp.rtf

The RTF command dictionary: the `Command` enum and its `CommandType` classification.

# Package com.darkrockstudios.libs.rtfparserkmp.model

The styled-text document model — `StyledDocument`, `Paragraph`, `TextRun`, `CharacterStyle`,
`ParagraphStyle` — shared by the reader and writer.

# Package com.darkrockstudios.libs.rtfparserkmp.utils

Small shared helpers, such as `HexUtils` for parsing `\'xx` hex escapes.
