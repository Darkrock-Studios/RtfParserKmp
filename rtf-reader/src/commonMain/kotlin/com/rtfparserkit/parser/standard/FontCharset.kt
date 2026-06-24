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
 * Represents font character sets which may be encountered in an RTF file.
 */
internal object FontCharset {
    /**
     * Convert a font character set to an encoding name.
     */
    fun getCharset(parameter: Int): String? {
        var result: String? = null
        if (parameter >= 0 && parameter < MAPPING.size) {
            result = MAPPING[parameter]
        }
        return result
    }

    private val MAPPING: Array<String?> = arrayOfNulls<String>(256).apply {
        this[0] = "1252" // ANSI
        this[1] = null // Default
        this[2] = "1252" // Symbol - according to the specs this is codepage 42 "Symbol". What's the Java equivalent? 1252 seems to work...
        this[77] = "10000" // Mac Roman
        this[78] = "10001" // Mac Shift Jis
        this[79] = "10003" // Mac Hangul
        this[80] = "10008" // Mac GB2312
        this[81] = "10002" // Mac Big5
        this[82] = null // Mac Johab (old)
        this[83] = "10005" // Mac Hebrew
        this[84] = "10004" // Mac Arabic
        this[85] = "10006" // Mac Greek
        this[86] = "10081" // Mac Turkish
        this[87] = "10021" // Mac Thai
        this[88] = "10029" // Mac East Europe
        this[89] = "10007" // Mac Russian
        this[128] = "932" // Shift JIS
        this[129] = "949" // Hangul
        this[130] = "1361" // Johab
        this[134] = "936" // GB2312
        this[136] = "950" // Big5
        this[161] = "1253" // Greek
        this[162] = "1254" // Turkish
        this[163] = "1258" // Vietnamese
        this[177] = "1255" // Hebrew
        this[178] = "1256" // Arabic
        this[179] = null // Arabic Traditional (old)
        this[180] = null // Arabic user (old)
        this[181] = null // Hebrew user (old)
        this[186] = "1257" // Baltic
        this[204] = "1251" // Russian
        this[222] = "874" // Thai
        this[238] = "1250" // Eastern European
        this[254] = "437" // PC 437
        this[255] = "850" // OEM
    }
}
