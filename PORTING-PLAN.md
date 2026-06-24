# RTFParserKit → Kotlin Multiplatform Port — Handoff Plan

**Purpose:** Port the [RTFParserKit](https://github.com/joniles/rtfparserkit) RTF parser
(Java, Apache 2.0) to Kotlin Multiplatform so it can run in `commonMain` for RTF *import*
in Hammer Editor. This document captures the findings of a prior investigation and is
written to be executed by a coding agent.

---

## 1. Objective & scope

- **In scope:** A KMP port of the **Standard** RTF parser path — read an RTF byte stream,
  emit events (text + formatting commands), reconstruct a styled-text model.
- **Targets:** Android, iOS, and JVM/desktop are required (Compose Multiplatform stack).
  JS/Wasm is an **open decision** — it affects only the charset `actual` (see §6). Confirm
  before wiring the `TextDecoder` path.
- **Not in this port:** Full RTF fidelity. RTF has ~1000+ control words across tables,
  fields, drawing objects, math, list-override tables, embedded images, revision marks,
  etc. A novel/world-building editor needs a small subset. The format is *designed* for
  partial readers (see §7) — implementing a subset is conformant, not a hack.
- **Writing RTF (export):** RTFParserKit is **read-only** (confirmed — no writer classes
  exist). If export is wanted, hand-roll a small writer; see §8. Do not pull a JVM writer
  library.

---

## 2. Source materials

### RTF specification (canonical reference)
- **RTF 1.9.1, Microsoft, March 2008** — this is the **final** revision; Microsoft froze
  RTF after this. 278 pages.
- Microsoft-hosted PDF (primary):
  `https://officeprotocoldoc.z19.web.core.windows.net/files/Archive_References/[MSFT-RTF].pdf`
- Mirror (Microsoft interoperability blob):
  `https://interoperability.blob.core.windows.net/files/Archive_References/[MSFT-RTF].pdf`
- Stable reference hub (Library of Congress format registry, links to all historical
  versions): `https://www.loc.gov/preservation/digital/formats/fdd/fdd000473.shtml`
- The sections that matter for a reader are the front matter (pp. 7–35): *Basic Entities*,
  *Conventions of an RTF Reader*, *Formal Syntax*, *Contents of an RTF File*, *Unicode RTF*.
  The remaining ~240 pages are a control-word dictionary you will mostly skip.

### Library to port
- Repo: `https://github.com/joniles/rtfparserkit`
- Maven: `com.github.joniles:rtfparserkit:1.16.0` (check for newer before starting)
- License: **Apache 2.0**, © 2013 Jon Iles
- Architecture: SAX-style. You implement `IRtfListener` (or subclass `RtfListenerAdaptor`)
  and override only the events you care about. Two parser implementations ship:
  - `RawRtfParser` — minimal processing, raw commands/data. (Not the target.)
  - `StandardRtfParser` — resolves encodings + Unicode, emits decoded strings. **Port this.**

---

## 3. Why this port is tractable

The codebase is ~33 source files. The dependency surface on the JVM is tiny and fully
enumerable (§4). The single genuinely hard problem is multi-codepage character decoding
(§6); everything else is mechanical translation.

Note on `rtf/Command.java` (3,701 lines): it is **just an `enum`** mapping control-word
strings to a `CommandType` (`Toggle`/`Value`/`Flag`/`Symbol`/…). Pure data, zero logic.
Translate it to a Kotlin `enum` (or a generated lookup map) mechanically — the line count
is not a measure of effort.

---

## 4. Complete JVM dependency surface

These are the *only* non-Kotlin imports in the library. All substitutions are listed.

| JVM import | Where | KMP replacement |
|---|---|---|
| `java.util.Map` / `HashMap` | several | `MutableMap` / `mutableMapOf` / `HashMap` (stdlib) |
| `java.util.Deque` / `ArrayDeque` | parser state stack | `kotlin.collections.ArrayDeque` |
| `java.util.List` / `ArrayList` | `UprHandler` | `MutableList` / `mutableListOf` |
| `java.util.Arrays` | `HexUtils` | stdlib array ops |
| `java.io.*` (InputStream, IOException, etc.) | `IRtfSource` impls, converters | Okio `BufferedSource` (already in the epub stack) |
| `java.nio.charset.Charset` | `StreamTextConverter` only | see §6 |
| `java.io.UnsupportedEncodingException` | `StandardRtfParser` | see §6 |
| `javax.xml.stream.*` | `RtfDumpListener` only (debug) | **drop** |
| `java.io.FileInputStream/FileOutputStream` | `RtfDump`, `ImageDump` (CLI/debug) | **drop** |

---

## 5. File-by-file disposition

**Translate to commonMain (core parse path):**
- `parser/standard/StandardRtfParser.java` (~469 lines) — the state machine. Core.
- `parser/standard/ParserState.java` (~43) — per-group state (encoding, font, formatting).
- `parser/standard/Encoding.java` (~245) — codepage→charset-name table. **Repurpose** for
  the new decoder (see §6) rather than keeping Java charset-name strings.
- `parser/standard/FontCharset.java` — `\fcharsetN` → codepage mapping.
- `parser/standard/UprHandler.java` — `\upr`/`\ud` Unicode-destination handling.
- `parser/standard/*Event.java`, `IParserEvent*.java`, `ParserEventType.java`,
  `DefaultEventHandler.java` — the event model. Mechanical.
- `parser/IRtfListener.java`, `IRtfParser.java`, `RtfListenerAdaptor.java` — interfaces.
- `rtf/Command.java`, `rtf/CommandType.java` — the command dictionary enum. Mechanical.
- `utils/HexUtils.java` — hex helpers for `\'xx`.

**Replace with `expect`/`actual` or Okio:**
- `parser/IRtfSource.java` — byte source with pushback. Define a small Kotlin interface;
  back it with an Okio `BufferedSource` impl + an in-memory string impl. (Replaces
  `RtfStreamSource`/`RtfStringSource`.)
- Byte→String decoding inside `StandardRtfParser` — see §6.

**Drop (debug/CLI only, not needed):**
- `utils/RtfDumpListener.java` (uses `javax.xml.stream`), `utils/RtfDump.java`,
  `utils/ImageDump.java`, `utils/ImageListener.java`.
- `converter/text/StreamTextConverter.java` (file/stream-bound). Keep the *logic* of
  `AbstractTextConverter`/`StringTextConverter` if a plain-text extractor is useful, but
  re-home it on the Okio/string source.

---

## 6. The core problem: multi-codepage byte→String decoding

This is the only part requiring real engineering judgment.

**Where it lives:** `StandardRtfParser.java` has exactly one load-bearing decode call
(around line 86):

```java
handleEvent(new StringEvent(new String(data, skipBytes, data.length - skipBytes, currentEncoding())));
```

`currentEncoding()` returns a **Java charset name** string (`"Cp1252"`, `"MacRoman"`,
`"MS932"`, …) resolved by `Encoding.java` / `FontCharset.java` from the document's
`\ansicpgN`, `\mac`/`\pc`/`\pca`, and per-font `\fcharsetN`/`\cpgN`. `processEncoding(...)`
and `processFontCharset(...)` set this state.

**Why it doesn't port for free:** On the JVM, `new String(bytes, "Cp1252")` works because
the JVM ships every charset. On Kotlin/Native (iOS) and JS/Wasm, the stdlib and
Okio/kotlinx-io guarantee **UTF-8 only**. Decoding legacy single- and multi-byte codepages
is not available out of the box.

**Chosen strategy — a decoding ladder (easiest → hardest):**

1. **`\uN` (Unicode control word) needs no charset at all.** It is a signed 16-bit int →
   `Char` (values > 32767 are negative: e.g. `\u-4064` = U+F020; add 65536). Implement in
   pure common code. **Modern Word/LibreOffice/Pages emit `\uN` for essentially all
   non-ASCII, so this path covers the large majority of real-world content.** `\ucN` sets
   how many ANSI fallback bytes to skip after each `\uN` (default 1, group-scoped) — port
   the skip-count stack faithfully (it already exists in the parser).

2. **Single-byte codepages → bundle 256-entry tables in `commonMain`.** Windows-1252,
   CP437, CP850, MacRoman, plus 1250/1251 as needed. These are tiny, deterministic, and
   platform-independent. Covers the Western + Mac `\'xx` cases, i.e. nearly all remaining
   real files.

3. **Multi-byte CJK (cp932/936/950) → `expect`/`actual`, or defer.** This is the only part
   that wants platform help:
   - `actual` (Android/JVM): `Charset.forName(name)` + decode.
   - `actual` (iOS): `CFStringConvertWindowsCodepageToEncoding(cp)` → `NSString(data:encoding:)`.
   - `actual` (JS/Wasm, *only if targeted*): `TextDecoder(label).decode(bytes)` — supports
     `windows-1252`, `shift_jis`, `big5`, `gbk`, `macintosh`, etc.
   - Acceptable alternative for v1: treat legacy-codepage CJK as a documented limitation.
     It is a rare import path for this app.

**Net:** define a single `expect fun decodeBytes(bytes: ByteArray, codepage: Int): String`
seam. Resolve `\uN` and the single-byte tables in common; route only the exotic codepages
to `actual`. Re-home `Encoding.java`/`FontCharset.java` to map RTF codepage numbers to this
seam instead of to Java charset-name strings.

---

## 7. RTF reader semantics to preserve (do NOT "simplify")

RTFParserKit already implements these correctly. They are the subtle, spec-mandated rules
where naive ports introduce data-corruption bugs. Preserve behavior exactly during
translation.

- **Reader model = stack machine.** `{` pushes state, `}` pops. Each control word either
  changes destination, changes a formatting property, or inserts a special character.
- **Unknown control words are ignored; unknown `{\*\...}` destinations are skipped whole**
  (discard through the matching `}`). This is what makes a subset reader conformant.
- **Control-word delimiter rule.** A single space after a control word is the delimiter and
  is consumed; any *further* spaces are literal text. `\b hello` → "hello"; `\b  hello` →
  " hello". A numeric parameter is delimited by the first non-digit.
- **`\uN` / `\ucN` skip logic** (see §6). A control word or `\binN` blob counts as one
  "character" for skip purposes; a brace ends skippable data early.
- **Escapes & symbols.** `\\ \{ \}` literal; `\~` non-breaking space, `\-` optional hyphen,
  `\_` non-breaking hyphen, `\tab`, `\emdash`/`\endash`, `\lquote`/`\rquote`/`\ldblquote`/
  `\rdblquote`, `\'xx` hex byte.
- **Raw CRLFs in the file are not text** — ignore them; real breaks come from `\par`/`\line`.
- **Group inheritance exception.** Nested groups inherit outer formatting *except*
  footnote/header/footer/annotation groups, which reset.
- **`\binN`** introduces N bytes of raw binary that must be consumed as data, not parsed.

Subset of control words worth surfacing as events for a novel editor: `\par`, `\line`,
`\pard` (paragraph reset), `\plain` (char reset), `\b \i \ul \strike \super \sub`, `\sN`
(style reference → heading detection via the header `{\stylesheet ...}`), and optionally
list control words.

---

## 8. Optional: RTF writer (export)

RTFParserKit cannot write RTF. If Hammer needs RTF *export*, hand-roll a small writer in
`commonMain`. Writing is far easier than reading and — importantly — **has no charset
problem**: keep the file 7-bit ASCII and emit all non-ASCII as `\uN` with an ASCII
fallback, so no `expect`/`actual` is needed at all.

Minimal writer outline:
- Header: `{\rtf1\ansi\deff0{\fonttbl{\f0 Times New Roman;}}`
- Escape `\`, `{`, `}` → `\\ \{ \}`
- Paragraphs → `\par`; line breaks → `\line`
- `\b…\b0`, `\i…\i0`, `\ul…\ulnone`
- Codepoint > 127 → `\uN?` (apply the signed-16-bit wrap for values > 32767)
- Close root `}`

This round-trips cleanly with the ported reader (same styled-text model in and out).

---

## 9. Suggested module / package layout

```
rtf-kmp/
  commonMain/
    rtf/            // Command, CommandType (ported enums)
    parser/         // RtfSource, IRtfListener, StandardRtfParser, ParserState, events
    charset/        // decodeBytes seam, single-byte tables, \uN handling
    writer/         // optional RtfWriter (charset-free)
  androidMain/      // actual decodeBytes via java.nio.charset.Charset
  jvmMain/          // actual decodeBytes via java.nio.charset.Charset (shareable w/ android)
  iosMain/          // actual decodeBytes via CFString/NSString
  jsMain/           // (only if targeted) actual decodeBytes via TextDecoder
  commonTest/       // corpus-based tests (see §10)
```

Use Okio for the byte source. Keep the event/listener API shape from the original — it's a
good design and minimizes downstream rework.

---

## 10. Validation strategy

The spec is *less* useful here than a corpus of real files. RTF emitters disagree in
subtle, vendor-specific ways. Build a test corpus of real `.rtf` exports from:

- Microsoft Word (Windows + Mac)
- LibreOffice / OpenOffice
- Apple Pages / TextEdit
- Scrivener (relevant to the novel-writing audience)
- Google Docs RTF export

For each, assert: correct paragraph segmentation, bold/italic/underline runs, smart quotes
and dashes, non-ASCII characters (both `\uN` and `\'xx` forms), and that unknown
destinations are skipped without leaking text. Port RTFParserKit's existing tests where
they exist and add corpus cases on top.

---

## 11. Licensing

Apache 2.0 permits this derivative. Requirements:
- Retain the Apache 2.0 license headers (© 2013 Jon Iles) on ported files.
- Include the upstream `LICENSE` and add a `NOTICE` crediting Jon Iles / RTFParserKit.
- New code can carry the project's own copyright alongside the retained upstream notice.

---

## 12. Open decisions (resolve before/early in the port)

1. **Target matrix:** JS/Wasm in scope, or Android + iOS + JVM only? Determines whether the
   `TextDecoder` `actual` is built now.
2. **CJK legacy codepages:** full `expect`/`actual` support in v1, or documented limitation?
3. **Export:** is an RTF *writer* (§8) part of this work, or reader-only for now?
4. **Byte source:** Okio `BufferedSource` (consistent with the epub stack) vs. kotlinx-io —
   pick one for the `RtfSource` impl.