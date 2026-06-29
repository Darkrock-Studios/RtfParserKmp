# Module rtf-io-okio

Adapts an [Okio](https://square.github.io/okio/) `BufferedSource` to the `RtfSource` the parser reads
from, so you can parse straight from a stream without buffering the whole document into a `ByteArray`
first.

```kotlin
import com.darkrockstudios.libs.rtfparserkmp.io.asRtfSource
import com.darkrockstudios.libs.rtfparserkmp.parser.standard.StandardRtfParser

StandardRtfParser().parse(source.asRtfSource(), listener)   // source: okio BufferedSource
```

# Package com.darkrockstudios.libs.rtfparserkmp.io

`OkioRtfSource` and the `BufferedSource.asRtfSource()` extension that wraps an Okio `BufferedSource`.
