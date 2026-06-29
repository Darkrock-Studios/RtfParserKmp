# Module rtf-compose

Bridges RTF and Compose's `AnnotatedString`, so you can render RTF directly in `BasicText` and convert
styled Compose text back to RTF. Targets the Compose subset: JVM/Desktop, Android, wasmJs, iOS, macOS.

```kotlin
import com.darkrockstudios.libs.rtfparserkmp.compose.rtfToAnnotatedString
import com.darkrockstudios.libs.rtfparserkmp.compose.annotatedStringToRtf

val styled: AnnotatedString = rtfToAnnotatedString(rtfBytes)   // bold/italic/underline -> SpanStyle
// BasicText(styled)

val rtf: String = annotatedStringToRtf(styled)                 // and back again
```

For streaming or listener-style use, `RtfToAnnotatedString` is the `RtfListener` behind
`rtfToAnnotatedString`; `AnnotatedString.toStyledDocument()` exposes the styled-model conversion that
the writer path builds on.

# Package com.darkrockstudios.libs.rtfparserkmp.compose

The conversion entry points: `rtfToAnnotatedString` / `RtfToAnnotatedString` and
`annotatedStringToRtf` / `AnnotatedString.toStyledDocument`.
