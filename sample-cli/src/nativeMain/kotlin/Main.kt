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

import com.rtfparserkit.converter.RtfToMarkdown
import com.rtfparserkit.converter.RtfTextExtractor
import com.rtfparserkit.io.KotlinxIoRtfSource
import com.rtfparserkit.parser.RtfListener
import com.rtfparserkit.parser.standard.StandardRtfParser
import com.rtfparserkit.writer.convertMarkdownToRtf
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.writeString
import kotlin.system.exitProcess

private enum class Mode { TEXT, MARKDOWN, RTF }

private const val USAGE = """Usage: rtfcli <input> [--text|--markdown|--rtf] [-o <out>]

  <input>        path to an .rtf or .md file
  --text         extract plain text from RTF (default for .rtf input)
  --markdown     convert RTF to Markdown
  --rtf          convert Markdown to RTF (default for .md input)
  -o <out>       write result to <out> instead of stdout
  --help         show this message"""

fun main(args: Array<String>) {
    if (args.isEmpty() || args.any { it == "--help" }) {
        println(USAGE)
        return
    }

    var input: String? = null
    var mode: Mode? = null
    var output: String? = null

    var i = 0
    while (i < args.size) {
        when (val arg = args[i]) {
            "--text" -> mode = Mode.TEXT
            "--markdown" -> mode = Mode.MARKDOWN
            "--rtf" -> mode = Mode.RTF
            "-o" -> {
                i++
                if (i >= args.size) fail("Missing path after -o")
                output = args[i]
            }
            else -> {
                if (arg.startsWith("-")) fail("Unknown option: $arg")
                if (input != null) fail("Multiple input files given: $input and $arg")
                input = arg
            }
        }
        i++
    }

    val inputPath = input ?: fail("No input file given")
    val resolvedMode = mode ?: if (inputPath.endsWith(".md")) Mode.RTF else Mode.TEXT

    val result = when (resolvedMode) {
        Mode.TEXT, Mode.MARKDOWN -> rtfToText(inputPath, resolvedMode)
        Mode.RTF -> markdownToRtf(inputPath)
    }

    if (output != null) {
        writeFile(output, result)
    } else {
        println(result)
    }
}

private fun rtfToText(inputPath: String, mode: Mode): String = when (mode) {
    Mode.MARKDOWN -> RtfToMarkdown().also { parseRtfFile(inputPath, it) }.markdown
    else -> RtfTextExtractor().also { parseRtfFile(inputPath, it) }.text
}

private fun parseRtfFile(inputPath: String, listener: RtfListener) {
    try {
        SystemFileSystem.source(Path(inputPath)).buffered().use { source ->
            StandardRtfParser().parse(KotlinxIoRtfSource(source), listener)
        }
    } catch (e: Exception) {
        fail("Failed to parse '$inputPath': ${e.message}")
    }
}

private fun markdownToRtf(inputPath: String): String {
    val markdown = try {
        SystemFileSystem.source(Path(inputPath)).buffered().use { source ->
            source.readByteArray().decodeToString()
        }
    } catch (e: Exception) {
        fail("Failed to read '$inputPath': ${e.message}")
    }
    return convertMarkdownToRtf(markdown)
}

private fun writeFile(outputPath: String, content: String) {
    try {
        SystemFileSystem.sink(Path(outputPath)).buffered().use { sink ->
            sink.writeString(content)
        }
    } catch (e: Exception) {
        fail("Failed to write '$outputPath': ${e.message}")
    }
}

private fun fail(message: String): Nothing {
    println("error: $message")
    exitProcess(1)
}
