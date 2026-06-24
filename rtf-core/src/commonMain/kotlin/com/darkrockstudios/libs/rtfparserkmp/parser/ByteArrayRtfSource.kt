/*
 * Copyright 2013 Jon Iles
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

package com.darkrockstudios.libs.rtfparserkmp.parser

/**
 * An [RtfSource] backed by an in-memory byte array. This is the dependency-free default source;
 * streaming sources live in the optional `rtf-io-*` adapter modules.
 */
class ByteArrayRtfSource(private val data: ByteArray) : RtfSource {
    private var index = 0

    override fun read(): Int =
        if (index >= data.size) -1 else data[index++].toInt() and 0xFF

    override fun unread(c: Int) {
        check(index > 0) { "Unread not possible" }
        index--
    }

    override fun read(dest: ByteArray): Int {
        val available = data.size - index
        if (available <= 0) return 0
        val count = minOf(dest.size, available)
        data.copyInto(dest, destinationOffset = 0, startIndex = index, endIndex = index + count)
        index += count
        return count
    }
}
