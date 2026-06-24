/*
 * Copyright 2026 The RtfParserKmp contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.darkrockstudios.libs.rtfparserkmp.compose

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnnotatedStringToRtfTest {
    @Test
    fun producesValidRtfContainingTheText() {
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Bold") }
            append(" plain")
        }
        val rtf = annotatedStringToRtf(annotated)
        assertTrue(rtf.startsWith("{\\rtf1"), "expected an RTF header, got: ${rtf.take(20)}")
        assertTrue(rtf.trimEnd().endsWith("}"))
        assertTrue(rtf.contains("Bold"))
        assertTrue(rtf.contains("plain"))
    }

    @Test
    fun boldAndItalicRoundTripThroughRtf() {
        val original = buildAnnotatedString {
            append("This is ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("bold") }
            append(" and ")
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("italic") }
            append(".")
        }

        val recovered = rtfToAnnotatedString(annotatedStringToRtf(original).encodeToByteArray())

        assertEquals(original.text, recovered.text)

        val boldRange = recovered.spanStyles.first { it.item.fontWeight == FontWeight.Bold }
        assertEquals("bold", recovered.text.substring(boldRange.start, boldRange.end))

        val italicRange = recovered.spanStyles.first { it.item.fontStyle == FontStyle.Italic }
        assertEquals("italic", recovered.text.substring(italicRange.start, italicRange.end))
    }

    @Test
    fun paragraphsAndUnderlineRoundTrip() {
        val original = buildAnnotatedString {
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append("under") }
            append("\nsecond")
        }

        val recovered = rtfToAnnotatedString(annotatedStringToRtf(original).encodeToByteArray())

        assertEquals("under\nsecond", recovered.text)
        val underlineRange = recovered.spanStyles.first {
            it.item.textDecoration?.contains(TextDecoration.Underline) == true
        }
        assertEquals("under", recovered.text.substring(underlineRange.start, underlineRange.end))
    }

    @Test
    fun mergesOverlappingSpansAndSplitsParagraphsIntoModel() {
        val annotated = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) { append("BI") }
            append("\n")
            append("plain")
        }
        val doc = annotated.toStyledDocument()
        assertEquals(2, doc.paragraphs.size)
        val firstRun = doc.paragraphs[0].runs.single()
        assertEquals("BI", firstRun.text)
        assertTrue(firstRun.style.bold && firstRun.style.italic)
        assertEquals("plain", doc.paragraphs[1].runs.single().text)
    }
}
