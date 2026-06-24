// Copyright 2026 The RtfParserKmp contributors
//
// Single-file JDK program (run: java tools/gen-single-byte-tables.java).
// Generates rtf-reader/src/commonMain/kotlin/com/darkrockstudios/libs/rtfparserkmp/parser/standard/SingleByteTables.kt
// by decoding bytes 0..255 with each named single-byte charset and emitting a 256-char mapping.

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class gen_single_byte_tables {

    static final String[] CHARSETS = {
        "Cp1252", "Cp437", "Cp850", "MacRoman", "Cp874",
        "Cp1250", "Cp1251", "Cp1253", "Cp1254", "Cp1255", "Cp1256", "Cp1257", "Cp1258",
        "x-MacArabic", "x-MacHebrew", "x-MacCyrillic", "x-MacRomania", "x-MacUkraine",
        "x-MacThai", "x-MacCentralEurope", "x-MacIceland", "x-MacTurkish", "x-MacCroatian"
    };

    public static void main(String[] args) throws IOException {
        byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            allBytes[i] = (byte) i;
        }

        List<String> skipped = new ArrayList<>();
        StringBuilder entries = new StringBuilder();

        for (String name : CHARSETS) {
            Charset cs;
            try {
                cs = Charset.forName(name);
            } catch (Exception e) {
                skipped.add(name);
                continue;
            }

            char[] table = new char[256];
            for (int i = 0; i < 256; i++) {
                String s = new String(allBytes, i, 1, cs);
                // A well-behaved single-byte charset yields exactly one char per byte.
                table[i] = s.length() >= 1 ? s.charAt(0) : '�';
            }

            entries.append("    \"").append(name).append("\" to \"");
            for (int i = 0; i < 256; i++) {
                entries.append(String.format("\\u%04X", (int) table[i]));
            }
            entries.append("\",\n");
        }

        StringBuilder out = new StringBuilder();
        out.append("/*\n");
        out.append(" * Copyright 2026 The RtfParserKmp contributors\n");
        out.append(" *\n");
        out.append(" * Licensed under the Apache License, Version 2.0 (the \"License\");\n");
        out.append(" * you may not use this file except in compliance with the License.\n");
        out.append(" * You may obtain a copy of the License at\n");
        out.append(" *\n");
        out.append(" *     http://www.apache.org/licenses/LICENSE-2.0\n");
        out.append(" *\n");
        out.append(" * Unless required by applicable law or agreed to in writing, software\n");
        out.append(" * distributed under the License is distributed on an \"AS IS\" BASIS,\n");
        out.append(" * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n");
        out.append(" * See the License for the specific language governing permissions and\n");
        out.append(" * limitations under the License.\n");
        out.append(" */\n\n");
        out.append("package com.darkrockstudios.libs.rtfparserkmp.parser.standard\n\n");
        out.append("internal val SINGLE_BYTE_TABLES: Map<String, String> = mapOf(\n");
        out.append(entries);
        out.append(")\n");

        Path target = Paths.get("rtf-reader", "src", "commonMain", "kotlin",
            "com", "rtfparserkit", "parser", "standard", "SingleByteTables.kt");
        Files.createDirectories(target.getParent());
        Files.writeString(target, out.toString());

        System.out.println("Wrote " + target.toAbsolutePath());
        if (skipped.isEmpty()) {
            System.out.println("Skipped charsets: none");
        } else {
            System.out.println("Skipped charsets: " + String.join(", ", skipped));
        }
    }
}
