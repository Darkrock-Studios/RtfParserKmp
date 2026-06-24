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

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.darkrockstudios.libs.rtfparserkmp.parser.standard

private class JsByteBuffer(val handle: JsAny)

private fun newBuffer(length: Int): JsByteBuffer = JsByteBuffer(jsNewUint8Array(length))

private fun jsNewUint8Array(length: Int): JsAny = js("new Uint8Array(length)")

private fun jsSetByte(buffer: JsAny, index: Int, value: Int) {
    js("buffer[index] = value")
}

private fun jsDecode(label: String, buffer: JsAny): String =
    js("new TextDecoder(label).decode(buffer)")

internal actual fun platformDecodeBytes(bytes: ByteArray, offset: Int, length: Int, charsetName: String): String {
    val label = whatwgLabel(charsetName)
        ?: throw NotImplementedError("codepage $charsetName not yet supported on this target")
    val buffer = newBuffer(length)
    for (i in 0 until length) {
        jsSetByte(buffer.handle, i, bytes[offset + i].toInt() and 0xFF)
    }
    return jsDecode(label, buffer.handle)
}
