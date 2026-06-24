# RtfParserKmp

[![Maven Central](https://img.shields.io/maven-central/v/com.darkrockstudios/rtf-reader?label=Maven%20Central)](https://central.sonatype.com/artifact/com.darkrockstudios/rtf-reader)
[![CI](https://github.com/Darkrock-Studios/RtfParserKmp/actions/workflows/ci.yml/badge.svg)](https://github.com/Darkrock-Studios/RtfParserKmp/actions/workflows/ci.yml)

An idiomatic **Kotlin Multiplatform** RTF reader and writer.

RtfParserKmp is a port of [**RTFParserKit**](https://github.com/joniles/rtfparserkit) by Jon Iles
(Apache 2.0) — its proven, SAX-style Standard parser, re-homed into `commonMain` so it runs on the JVM,
JS, WebAssembly, and every Kotlin/Native target with no platform code in the hot path. The parsing
engine and the entire command dictionary are faithful translations of the original; the public API is
reshaped to be idiomatic Kotlin (sealed event types, listener interfaces with default methods, a
dependency-free byte source).

> **Acknowledgement.** This library is a derivative work of RTFParserKit, © 2013 Jon Iles, used under
> the Apache License 2.0. See [`NOTICE`](NOTICE). New code is © 2026 The RtfParserKmp contributors.

## Modules

Each module publishes as its own Maven artifact under the group `com.darkrockstudios`, so you
depend only on what you use.

| Artifact | Purpose | Runtime deps |
|---|---|---|
| `rtf-core` | Shared model: `Command`/`CommandType`, `RtfEvent`, `RtfListener`, `RtfSource`/`ByteArrayRtfSource`, styled-text model | none |
| `rtf-reader` | `StandardRtfParser`, `parseRtf`, charset decoding, `RtfTextExtractor`, `RtfToMarkdown` | `rtf-core` |
| `rtf-writer` | `RtfWriter` (styled model → RTF), `MarkdownToRtf` | `rtf-core` |
| `rtf-io-kotlinx` | `RtfSource` over a kotlinx-io `Source` | `rtf-core`, kotlinx-io |
| `rtf-io-okio` | `RtfSource` over an Okio `BufferedSource` | `rtf-core`, Okio |
| `rtf-compose` | RTF ↔ Compose `AnnotatedString` (`rtfToAnnotatedString` / `annotatedStringToRtf`) | `rtf-core`, `rtf-reader`, `rtf-writer`, Compose `ui-text` |

A non-published `sample-cli` (Kotlin/Native) demonstrates the stack end to end. `rtf-compose` targets the
Compose-supported subset (JVM, Android, wasmJs, iOS, macOS); the other modules cover the full matrix.

```kotlin
dependencies {
    implementation("com.darkrockstudios:rtf-reader:0.1.0")
    implementation("com.darkrockstudios:rtf-writer:0.1.0")        // optional, for export
    implementation("com.darkrockstudios:rtf-io-kotlinx:0.1.0")    // optional, for streaming I/O
}
```

## Supported targets

JVM · Android · JS (IR, browser + node) · wasmJs · wasmWasi · iOS (x64/arm64/simulatorArm64) ·
macOS (x64/arm64) · tvOS · watchOS · Linux (x64/arm64) · Windows (mingwX64).

Core parsing — including `\uN` Unicode, all single-byte codepages (Windows-125x, CP437/850/874,
Mac Roman and friends), and UTF-8 — runs identically on **every** target with zero platform code.

## Usage

### Read

```kotlin
import com.darkrockstudios.libs.rtfparserkmp.parser.parseRtf
import com.darkrockstudios.libs.rtfparserkmp.converter.extractPlainText
import com.darkrockstudios.libs.rtfparserkmp.converter.convertToMarkdown

val bytes: ByteArray = /* your .rtf */
val text: String = extractPlainText(bytes)
val markdown: String = convertToMarkdown(bytes)

// Or drive the event stream yourself:
for (event in parseRtf(bytes)) { /* RtfEvent.Text, .Command, GroupStart, ... */ }
```

Implement `RtfListener` (every method has a no-op default) for streaming, or `when` over the sealed
`RtfEvent` from `parseRtf`.

### Write

```kotlin
import com.darkrockstudios.libs.rtfparserkmp.writer.convertMarkdownToRtf
import com.darkrockstudios.libs.rtfparserkmp.writer.writeRtf
import com.darkrockstudios.libs.rtfparserkmp.model.*

val rtf: String = convertMarkdownToRtf("This is **bold** and _italic_.")

// Or from the styled-text model directly:
val doc = StyledDocument(listOf(Paragraph(listOf(TextRun("Hello", CharacterStyle(bold = true))))))
val rtf2: String = writeRtf(doc)
```

The writer emits 7-bit ASCII (non-ASCII as `\uN`), so it has no charset dependency and round-trips
cleanly with the reader.

### Streaming I/O

```kotlin
import com.darkrockstudios.libs.rtfparserkmp.io.asRtfSource          // kotlinx-io or okio
import com.darkrockstudios.libs.rtfparserkmp.parser.StandardRtfParser

StandardRtfParser().parse(source.asRtfSource(), myListener)   // source: kotlinx-io Source / okio BufferedSource
```

### Compose

```kotlin
import com.darkrockstudios.libs.rtfparserkmp.compose.rtfToAnnotatedString
import androidx.compose.foundation.text.BasicText

val styled: AnnotatedString = rtfToAnnotatedString(bytes)   // bold/italic/underline -> SpanStyle
// BasicText(styled)

val rtf: String = annotatedStringToRtf(styled)             // and back the other way
```

### CLI

```
rtfcli <input> [--text | --markdown | --rtf] [-o <out>]
```
`.rtf` input → `--text` (default) or `--markdown`; `.md` input → `--rtf`.

## Validation

The 22 real-world `.rtf` fixtures from upstream (Word, LibreOffice, Pages, encodings, `\upr`, negative
Unicode, …) are embedded and compared byte-for-byte against their expected event dumps — green on JVM,
and on every target for the single-byte/UTF-8 subset. CI runs the suite across Linux, macOS, and
Windows runners.

## Current limitations

- **Legacy multibyte CJK** (codepages 932/936/949/950 via `\'xx`) decodes on **JVM, Android, JS, wasmJs,
  and all Apple targets** (web via `TextDecoder`, Apple via `CFString`). Only the remaining native
  targets — Linux, Windows (mingw), and wasmWasi — throw for those legacy bytes. `\uN` Unicode (what
  modern Word/LibreOffice/Pages emit) and all single-byte codepages work on every target.

## License

Apache License 2.0 — see [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE).
