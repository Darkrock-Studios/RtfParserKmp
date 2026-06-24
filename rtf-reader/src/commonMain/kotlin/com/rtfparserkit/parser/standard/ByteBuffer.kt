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

package com.rtfparserkit.parser.standard

/**
 * Implements a simple byte array based buffer. Used to collect
 * data one byte at a time into a buffer, then pass the
 * collected data to the caller as an array.
 */
internal class ByteBuffer {
    private var bufferSize = 0
    private var buffer = ByteArray(INITIAL_BUFFER_CAPACITY)

    /**
     * Add a byte to the buffer.
     */
    fun add(b: Int) {
        if (bufferSize == buffer.size) {
            val newBuffer = ByteArray(buffer.size + (buffer.size shr 1))
            buffer.copyInto(newBuffer, 0, 0, bufferSize)
            buffer = newBuffer
        }

        buffer[bufferSize++] = b.toByte()
    }

    /**
     * Clear the buffer.
     */
    fun clear() {
        bufferSize = 0
    }

    /**
     * Return the buffer as an array.
     */
    fun toArray(): ByteArray = buffer.copyOf(bufferSize)

    /**
     * Determines if the buffer is empty.
     */
    fun isEmpty(): Boolean = bufferSize == 0

    private companion object {
        const val INITIAL_BUFFER_CAPACITY = 10240
    }
}
