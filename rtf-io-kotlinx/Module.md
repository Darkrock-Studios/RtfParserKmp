# Module rtf-io-kotlinx

Adapts a [kotlinx-io](https://github.com/Kotlin/kotlinx-io) `Source` to the `RtfSource` the parser
reads from, so you can parse straight from a stream without buffering the whole document into a
`ByteArray` first.

```kotlin
import com.darkrockstudios.libs.rtfparserkmp.io.asRtfSource
import com.darkrockstudios.libs.rtfparserkmp.parser.standard.StandardRtfParser

StandardRtfParser().parse(source.asRtfSource(), listener)   // source: kotlinx-io Source
```

# Package com.darkrockstudios.libs.rtfparserkmp.io

`KotlinxIoRtfSource` and the `Source.asRtfSource()` extension that wraps a kotlinx-io `Source`.
