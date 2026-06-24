// Generates rtf-core Command.kt from the upstream RTFParserKit Command.java enum.
// Usage: node tools/generate-command-enum.mjs
import { readFileSync, writeFileSync } from "node:fs";

const SRC = "reference/rtfparserkit/src/main/java/com/rtfparserkit/rtf/Command.java";
const OUT = "rtf-core/src/commonMain/kotlin/com/darkrockstudios/libs/rtfparserkmp/rtf/Command.kt";

// Kotlin keywords that the parser treats specially at declaration/enum-entry position and so cannot
// be used as a bare identifier here. Includes hard keywords plus modifier/soft keywords (e.g.
// `annotation`, `value`, `data`) that begin a member declaration. Backticking any of these is safe.
const KOTLIN_RESERVED = new Set([
  // hard keywords
  "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
  "if", "in", "interface", "is", "null", "object", "package", "return",
  "super", "this", "throw", "true", "try", "typealias", "typeof", "val",
  "var", "when", "while",
  // modifier keywords
  "abstract", "actual", "annotation", "companion", "const", "crossinline",
  "data", "enum", "expect", "external", "final", "infix", "inline", "inner",
  "internal", "lateinit", "noinline", "open", "operator", "out", "override",
  "private", "protected", "public", "reified", "sealed", "suspend", "tailrec",
  "vararg",
  // soft keywords
  "by", "catch", "constructor", "delegate", "dynamic", "field", "file",
  "finally", "get", "import", "init", "param", "property", "receiver", "set",
  "setparam", "value", "where", "it",
]);

const java = readFileSync(SRC, "utf8");

// Match:   name("value", CommandType.Type)
const re = /(\w+)\("((?:[^"\\]|\\.)*)",\s*CommandType\.(\w+)\)/g;

const entries = [];
const seenKeywords = new Map();
let m;
while ((m = re.exec(java)) !== null) {
  const [, name, value, type] = m;
  if (seenKeywords.has(value)) {
    console.warn(`WARN duplicate keyword ${JSON.stringify(value)}: ${seenKeywords.get(value)} and ${name}`);
  } else {
    seenKeywords.set(value, name);
  }
  entries.push({ name, value, type });
}

if (entries.length < 1000) {
  throw new Error(`Only parsed ${entries.length} commands — parser likely broke.`);
}

const esc = (id) => (KOTLIN_RESERVED.has(id) ? "`" + id + "`" : id);

const header = `/*
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

package com.darkrockstudios.libs.rtfparserkmp.rtf

// Generated from the upstream RTFParserKit Command.java by tools/generate-command-enum.mjs.
// Do not edit by hand; regenerate instead.

/**
 * Enumeration of all RTF commands, mapping each control-word keyword to its [CommandType].
 */
enum class Command(
    val keyword: String,
    val commandType: CommandType,
) {
`;

const body = entries
  .map((e) => `    ${esc(e.name)}("${e.value}", CommandType.${e.type}),`)
  .join("\n");

const footer = `
    ;

    companion object {
        private val byKeyword: Map<String, Command> = buildMap {
            for (command in Command.entries) put(command.keyword, command)
            // A backslash immediately followed by a raw line break is treated as \\par.
            put("\\r", Command.par)
            put("\\n", Command.par)
        }

        /** Return the [Command] for the given control-word keyword, or null if unrecognised. */
        fun getInstance(keyword: String): Command? = byKeyword[keyword]
    }
}
`;

writeFileSync(OUT, header + body + footer + "\n");
console.log(`Wrote ${entries.length} commands to ${OUT}`);
