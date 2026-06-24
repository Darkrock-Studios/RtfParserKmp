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

package com.rtfparserkit.io

import com.rtfparserkit.parser.RtfSource
import okio.BufferedSource

/**
 * An [RtfSource] backed by an okio [BufferedSource], with single-byte pushback.
 */
class OkioRtfSource(private val source: BufferedSource) : RtfSource {
    private var pushback = -1

    override fun read(): Int {
        if (pushback != -1) {
            val c = pushback
            pushback = -1
            return c
        }
        return if (source.exhausted()) -1 else source.readByte().toInt() and 0xFF
    }

    override fun unread(c: Int) {
        check(pushback == -1) { "Unread not possible" }
        pushback = c
    }

    override fun read(dest: ByteArray): Int {
        if (dest.isEmpty()) return 0

        var offset = 0
        if (pushback != -1) {
            dest[0] = pushback.toByte()
            pushback = -1
            offset = 1
        }

        while (offset < dest.size) {
            val n = source.read(dest, offset, dest.size - offset)
            if (n <= 0) break
            offset += n
        }

        return offset
    }
}

/** Wrap this okio [BufferedSource] as an [RtfSource]. */
fun BufferedSource.asRtfSource(): RtfSource = OkioRtfSource(this)
