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

package com.rtfparserkit.parser.standard

internal expect fun platformDecodeBytes(bytes: ByteArray, offset: Int, length: Int, charsetName: String): String

internal fun decodeBytes(bytes: ByteArray, offset: Int, length: Int, charsetName: String): String =
    if (charsetName == "UTF-8") bytes.decodeToString(offset, offset + length) else platformDecodeBytes(bytes, offset, length, charsetName)
