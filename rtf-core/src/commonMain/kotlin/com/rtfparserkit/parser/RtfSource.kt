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

package com.rtfparserkit.parser

/**
 * The source of RTF data for the parser to consume, one byte at a time, with single-byte pushback.
 */
interface RtfSource {
    /** Read a single byte as an unsigned value 0..255, or -1 at end of input. */
    fun read(): Int

    /** Push back a single byte so it will be returned again by the next [read]. */
    fun unread(c: Int)

    /**
     * Read bytes into [dest]. Implementations must read repeatedly if necessary to fill [dest],
     * returning a count smaller than `dest.size` only at end of input — the `\binN` reader treats a
     * short count as a truncated document.
     */
    fun read(dest: ByteArray): Int
}
