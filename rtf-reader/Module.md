# Module rtf-reader

Turns RTF bytes into events, plain text, or Markdown. This is the module most consumers start with.

## Quick extract

```kotlin
import com.darkrockstudios.libs.rtfparserkmp.converter.extractPlainText
import com.darkrockstudios.libs.rtfparserkmp.converter.convertToMarkdown

val text: String = extractPlainText(rtfBytes)
val markdown: String = convertToMarkdown(rtfBytes)
```

## Drive the event stream

```kotlin
import com.darkrockstudios.libs.rtfparserkmp.parser.parseRtf

for (event in parseRtf(rtfBytes)) {
    when (event) {
        is RtfEvent.Text -> append(event.text)
        else -> {}
    }
}
```

`parseRtf` has two forms: one returns a `List<RtfEvent>`, the other pushes to an `RtfListener` you
supply. For full control over the input, instantiate `StandardRtfParser` and call `parse` with an
`RtfSource` — a faithful, SAX-style port of RTFParserKit's Standard parser. Malformed input throws
`RtfParseException`.

# Package com.darkrockstudios.libs.rtfparserkmp.converter

High-level conversions: `RtfTextExtractor` / `extractPlainText` and `RtfToMarkdown` / `convertToMarkdown`.

# Package com.darkrockstudios.libs.rtfparserkmp.parser

The `parseRtf` entry points over a `ByteArray`.

# Package com.darkrockstudios.libs.rtfparserkmp.parser.standard

The parser engine: the `RtfParser` interface, its `StandardRtfParser` implementation, and
`RtfParseException`.
