// Copyright 2026 The RtfParserKmp contributors
//
// Reads every *.rtf / *.xml pair from the upstream standard-parser corpus and emits
// Corpus.kt, with both the RTF bytes and the XML's UTF-8 bytes Base64-encoded.

import { readdirSync, readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(here, "..");

const dataDir = join(
  repoRoot,
  "reference/rtfparserkit/src/test/resources/com/rtfparserkit/parser/standard/data",
);

const outFile = join(
  repoRoot,
  "rtf-reader/src/commonTest/kotlin/com/darkrockstudios/libs/rtfparserkmp/parser/standard/Corpus.kt",
);

// Fixtures whose text runs select a legacy multibyte CJK codepage (932/936/949/950)
// and therefore route through platformDecodeBytes. These are decoded by Java charsets
// on jvm/android and the WHATWG TextDecoder on js/wasmJs, but are unsupported on the
// NotImplementedError fallback targets. Identified empirically: a fixture is multibyte
// CJK iff its decode throws NotImplementedError on a fallback target.
const MULTIBYTE_CJK = new Set([
  "test10001Encoding",
  "test950Encoding",
  "testJapaneseJisEncoding",
  "testJapaneseJisEncodingTwoFonts",
  "testKoreanEncoding",
  "testMultiByteHex",
  "testNecCharacters",
  "testUnicode",
]);

const names = readdirSync(dataDir)
  .filter((f) => f.endsWith(".rtf"))
  .map((f) => f.slice(0, -4))
  .sort();

const cases = [];
for (const name of names) {
  const rtf = readFileSync(join(dataDir, name + ".rtf"));
  const xml = readFileSync(join(dataDir, name + ".xml"));
  cases.push({
    name,
    rtfBase64: rtf.toString("base64"),
    expectedXmlBase64: xml.toString("base64"),
    multibyteCjk: MULTIBYTE_CJK.has(name),
  });
}

const header = `/*
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
`;

let body = `${header}
package com.darkrockstudios.libs.rtfparserkmp.parser.standard

internal data class CorpusCase(
    val name: String,
    val rtfBase64: String,
    val expectedXmlBase64: String,
    val multibyteCjk: Boolean,
)

internal object Corpus {
    val cases: List<CorpusCase> = listOf(
`;

for (const c of cases) {
  body += `        CorpusCase(\n`;
  body += `            name = ${JSON.stringify(c.name)},\n`;
  body += `            rtfBase64 = ${JSON.stringify(c.rtfBase64)},\n`;
  body += `            expectedXmlBase64 = ${JSON.stringify(c.expectedXmlBase64)},\n`;
  body += `            multibyteCjk = ${c.multibyteCjk},\n`;
  body += `        ),\n`;
}

body += `    )\n}\n`;

mkdirSync(dirname(outFile), { recursive: true });
writeFileSync(outFile, body);

console.log(`Wrote ${cases.length} cases to ${outFile}`);
