/*
 * Copyright 2026 The RtfParserKmp contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.rtfparserkit.rtf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CommandTest {
    @Test
    fun resolvesKnownKeywords() {
        assertSame(Command.b, Command.getInstance("b"))
        assertSame(Command.par, Command.getInstance("par"))
        assertSame(Command.hex, Command.getInstance("'"))
        assertSame(Command.backslash, Command.getInstance("\\"))
    }

    @Test
    fun unknownKeywordResolvesToNull() {
        assertNull(Command.getInstance("definitelynotacommand"))
    }

    @Test
    fun rawLineBreaksAliasToParagraph() {
        assertSame(Command.par, Command.getInstance("\r"))
        assertSame(Command.par, Command.getInstance("\n"))
    }

    @Test
    fun carriesKeywordAndType() {
        assertEquals("'", Command.hex.keyword)
        assertEquals(CommandType.Symbol, Command.hex.commandType)
        assertEquals(CommandType.Toggle, Command.b.commandType)
        assertEquals(CommandType.Value, Command.absh.commandType)
        assertEquals(CommandType.Encoding, Command.ansi.commandType)
        assertEquals(CommandType.Encoding, Command.ansicpg.commandType)
    }

    @Test
    fun lookupCoversEntireEnum() {
        assertTrue(Command.entries.size > 1500, "expected the full command dictionary")
        for (command in Command.entries) {
            assertSame(command, Command.getInstance(command.keyword), "round-trip failed for ${command.name}")
        }
    }
}
