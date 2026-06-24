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

import com.rtfparserkit.parser.ByteArrayRtfSource
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.fail

@OptIn(ExperimentalEncodingApi::class)
class CorpusTest {
    @Test
    fun corpusMatchesUpstreamDump() {
        val failures = StringBuilder()
        var passed = 0
        var skipped = 0

        for (case in Corpus.cases) {
            val rtf = Base64.decode(case.rtfBase64)
            val expected = Base64.decode(case.expectedXmlBase64).decodeToString()

            val listener = DumpListener()
            try {
                StandardRtfParser().parse(ByteArrayRtfSource(rtf), listener)
            } catch (e: NotImplementedError) {
                skipped++
                continue
            } catch (e: Throwable) {
                failures.append("\n[${case.name}] threw ${e::class.simpleName}: ${e.message}")
                continue
            }

            val actual = listener.xml
            if (actual == expected) {
                passed++
            } else {
                failures.append("\n[${case.name}] mismatch at ${firstDivergence(expected, actual)}")
            }
        }

        if (failures.isNotEmpty()) {
            fail("Corpus: $passed/${Corpus.cases.size} passing ($skipped skipped on this target).$failures")
        }
    }

    private fun firstDivergence(expected: String, actual: String): String {
        val limit = minOf(expected.length, actual.length)
        var i = 0
        while (i < limit && expected[i] == actual[i]) i++
        val from = maxOf(0, i - 40)
        val exp = expected.substring(from, minOf(expected.length, i + 160))
        val act = actual.substring(from, minOf(actual.length, i + 160))
        return "offset $i\n  expected: ...$exp\n  actual:   ...$act"
    }
}
