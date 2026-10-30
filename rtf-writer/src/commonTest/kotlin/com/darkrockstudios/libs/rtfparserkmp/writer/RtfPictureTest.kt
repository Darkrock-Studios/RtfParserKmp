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

package com.darkrockstudios.libs.rtfparserkmp.writer

import com.darkrockstudios.libs.rtfparserkmp.converter.extractPlainText
import com.darkrockstudios.libs.rtfparserkmp.model.PictureFormat
import com.darkrockstudios.libs.rtfparserkmp.parser.RtfEvent
import com.darkrockstudios.libs.rtfparserkmp.parser.parseRtf
import com.darkrockstudios.libs.rtfparserkmp.utils.HexUtils
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private val TIMES = RtfFont("Times New Roman", RtfFontFamily.Roman)

private const val TIMES_HEADER =
    "{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033" +
        "{\\fonttbl{\\f0\\froman\\fcharset0 Times New Roman;}}" +
        "{\\colortbl;}"

private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
private val JPEG_PREFIX = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())

class RtfPictureTest {
    private fun write(vararg blocks: RtfBlock): String =
        RtfDocumentWriter().write(RtfDocument(blocks = blocks.toList(), defaultFont = TIMES))

    @Test
    fun pngBlockWithGoalsDerivedAt96Dpi() {
        val rtf = write(RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, widthPx = 200, heightPx = 100))
        val expected = "$TIMES_HEADER\\fs24\n" +
            "\\pard {\\pict\\pngblip\\picw200\\pich100\\picwgoal3000\\pichgoal1500 89504e470d0a1a0a\n}\\par\n}"
        assertEquals(expected, rtf)
    }

    @Test
    fun jpegBlockWithExplicitGoals() {
        val rtf = write(
            RtfPicture(
                JPEG_PREFIX,
                RtfPictureFormat.Jpeg,
                widthPx = 640,
                heightPx = 480,
                goalWidthTwips = 6552,
                goalHeightTwips = 4914,
            ),
        )
        val expected = "$TIMES_HEADER\\fs24\n" +
            "\\pard {\\pict\\jpegblip\\picw640\\pich480\\picwgoal6552\\pichgoal4914 ffd8ffe0\n}\\par\n}"
        assertEquals(expected, rtf)
    }

    @Test
    fun centeredBlock() {
        val centered = RtfParagraphStyle(alignment = RtfAlignment.Center)
        val rtf = write(RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, 1, 1, style = centered))
        val expected = "$TIMES_HEADER\\fs24\n" +
            "\\pard\\qc {\\pict\\pngblip\\picw1\\pich1\\picwgoal15\\pichgoal15 89504e470d0a1a0a\n}\\par\n}"
        assertEquals(expected, rtf)
    }

    @Test
    fun blockParagraphStyleIsWrittenLikeAParagraphs() {
        val style = RtfParagraphStyle(
            alignment = RtfAlignment.Right,
            spaceBeforeTwips = 120,
            spaceAfterTwips = 240,
            leftIndentTwips = 720,
            firstLineIndentTwips = -360,
            keepWithNext = true,
            bottomBorder = RtfBorder(),
        )
        val picture = write(RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, 1, 1, style = style))
        val paragraph = write(RtfParagraph(listOf(RtfTextRun("x")), style))
        val prefix = "\\pard\\qr\\sb120\\sa240\\li720\\fi-360\\keepn\\brdrb\\brdrs\\brdrw10 "
        assertEquals("$TIMES_HEADER\\fs24\n$prefix{x}\\par\n}", paragraph)
        assertEquals(
            "$TIMES_HEADER\\fs24\n$prefix" +
                "{\\pict\\pngblip\\picw1\\pich1\\picwgoal15\\pichgoal15 89504e470d0a1a0a\n}\\par\n}",
            picture,
        )
    }

    @Test
    fun onlyGoalWidthDerivesHeightFromAspectRatio() {
        val rtf = write(RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, 20, 10, goalWidthTwips = 6552))
        val expected = "$TIMES_HEADER\\fs24\n" +
            "\\pard {\\pict\\pngblip\\picw20\\pich10\\picwgoal6552\\pichgoal3276 89504e470d0a1a0a\n}\\par\n}"
        assertEquals(expected, rtf)
    }

    @Test
    fun onlyGoalHeightDerivesWidthFromAspectRatio() {
        val rtf = write(RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, 20, 10, goalHeightTwips = 1000))
        assertTrue("\\picwgoal2000\\pichgoal1000 " in rtf, rtf)
    }

    @Test
    fun derivedGoalRoundsToNearestTwipAndIsAtLeastOne() {
        fun goals(widthPx: Int, heightPx: Int, goalWidth: Int? = null, goalHeight: Int? = null): String =
            write(RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, widthPx, heightPx, goalWidth, goalHeight))
                .substringAfter("\\pich$heightPx").substringBefore(' ')
        assertEquals("\\picwgoal100\\pichgoal33", goals(3, 1, goalWidth = 100))
        assertEquals("\\picwgoal100\\pichgoal67", goals(3, 2, goalWidth = 100))
        assertEquals("\\picwgoal67\\pichgoal100", goals(2, 3, goalHeight = 100))
        assertEquals("\\picwgoal1\\pichgoal1", goals(1000, 1, goalWidth = 1))
        assertEquals("\\picwgoal1\\pichgoal1", goals(1, 1000, goalHeight = 1))
    }

    @Test
    fun largeGoalIsDerivedWithoutIntOverflow() {
        // 2_000_000_000 * 3 overflows an Int; the result fits.
        val rtf = write(RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, 4, 3, goalWidthTwips = 2_000_000_000))
        assertTrue("\\picwgoal2000000000\\pichgoal1500000000 " in rtf, rtf)
    }

    @Test
    fun inlinePictureBetweenRuns() {
        val paragraph = RtfParagraph(
            listOf(
                RtfTextRun("a"),
                RtfInlinePicture(PNG_SIGNATURE, RtfPictureFormat.Png, 2, 2),
                RtfTextRun("b"),
            ),
        )
        val expected = "$TIMES_HEADER\\fs24\n" +
            "\\pard {a}{\\pict\\pngblip\\picw2\\pich2\\picwgoal30\\pichgoal30 89504e470d0a1a0a\n}{b}\\par\n}"
        assertEquals(expected, write(paragraph))
    }

    @Test
    fun inlinePictureInsideHyperlink() {
        val paragraph = RtfParagraph(
            listOf(
                RtfHyperlink(
                    target = "https://example.com",
                    content = listOf(RtfInlinePicture(PNG_SIGNATURE, RtfPictureFormat.Png, 2, 2)),
                ),
            ),
        )
        val rtf = write(paragraph)
        val expected = "$TIMES_HEADER\\fs24\n" +
            "\\pard {\\field{\\*\\fldinst HYPERLINK \"https://example.com\"}{\\fldrslt " +
            "{{\\pict\\pngblip\\picw2\\pich2\\picwgoal30\\pichgoal30 89504e470d0a1a0a\n}}}}\\par\n}"
        assertEquals(expected, rtf)

        val picture = parseRtf(rtf.encodeToByteArray()).filterIsInstance<RtfEvent.Picture>().single()
        assertContentEquals(PNG_SIGNATURE, picture.bytes)
        assertEquals("\n", extractPlainText(rtf.encodeToByteArray()))
    }

    @Test
    fun hexWrapsAt64BytesPerLine() {
        val bytes = ByteArray(150) { it.toByte() }
        val rtf = write(RtfPicture(bytes, RtfPictureFormat.Png, 10, 15))
        val hex = rtf.substringAfter("\\pichgoal225 ").substringBefore('}')
        val lines = hex.split('\n')
        assertEquals(listOf(128, 128, 44, 0), lines.map { it.length })
        assertContentEquals(bytes, HexUtils.parseHexString(hex.replace("\n", "")))
    }

    @Test
    fun plainTextSkipsThePicture() {
        val rtf = write(
            RtfParagraph(listOf(RtfTextRun("Before"))),
            RtfPicture(ByteArray(100) { 0xAB.toByte() }, RtfPictureFormat.Png, 10, 10),
            RtfParagraph(listOf(RtfTextRun("After"))),
        )
        val text = extractPlainText(rtf.encodeToByteArray())
        assertEquals("Before\n\nAfter\n", text)
        assertFalse("abab" in text)
    }

    @Test
    fun roundTripsThroughTheReader() {
        val bytes = ByteArray(300) { (it * 7).toByte() }
        val rtf = write(
            RtfParagraph(listOf(RtfTextRun("x"))),
            RtfPicture(
                bytes,
                RtfPictureFormat.Png,
                20,
                10,
                goalWidthTwips = 6552,
                style = RtfParagraphStyle(alignment = RtfAlignment.Center),
            ),
            RtfParagraph(listOf(RtfTextRun("y"), RtfInlinePicture(JPEG_PREFIX, RtfPictureFormat.Jpeg, 3, 4))),
        )
        val pictures = parseRtf(rtf.encodeToByteArray()).filterIsInstance<RtfEvent.Picture>()
        assertEquals(
            listOf(
                RtfEvent.Picture(bytes, PictureFormat.Png, 20, 10, 6552, 3276),
                RtfEvent.Picture(JPEG_PREFIX, PictureFormat.Jpeg, 3, 4, 45, 60),
            ),
            pictures,
        )
    }

    @Test
    fun rejectsInvalidPictures() {
        assertFailsWith<IllegalArgumentException> { RtfPicture(ByteArray(0), RtfPictureFormat.Png, 1, 1) }
        assertFailsWith<IllegalArgumentException> { RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, 0, 1) }
        assertFailsWith<IllegalArgumentException> { RtfInlinePicture(PNG_SIGNATURE, RtfPictureFormat.Png, 1, -1) }
        assertFailsWith<IllegalArgumentException> {
            RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, 1, 1, goalWidthTwips = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            RtfInlinePicture(PNG_SIGNATURE, RtfPictureFormat.Png, 1, 1, goalHeightTwips = -5)
        }
    }

    @Test
    fun rejectsSizesWhoseGoalsWouldOverflow() {
        val max = Int.MAX_VALUE / 15
        RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, max, max)
        assertFailsWith<IllegalArgumentException> { RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, max + 1, 1) }
        assertFailsWith<IllegalArgumentException> { RtfInlinePicture(PNG_SIGNATURE, RtfPictureFormat.Png, 1, max + 1) }
        assertFailsWith<IllegalArgumentException> {
            RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, 1, 2, goalWidthTwips = Int.MAX_VALUE)
        }
        assertFailsWith<IllegalArgumentException> {
            RtfInlinePicture(PNG_SIGNATURE, RtfPictureFormat.Png, 2, 1, goalHeightTwips = Int.MAX_VALUE)
        }
    }

    @Test
    fun equalityComparesByteContent() {
        val a = RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, 1, 1)
        assertEquals(a, RtfPicture(PNG_SIGNATURE.copyOf(), RtfPictureFormat.Png, 1, 1))
        assertEquals(a.hashCode(), RtfPicture(PNG_SIGNATURE.copyOf(), RtfPictureFormat.Png, 1, 1).hashCode())
        assertNotEquals(
            a,
            RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, 1, 1, style = RtfParagraphStyle(alignment = RtfAlignment.Right)),
        )
        assertNotEquals(a, RtfPicture(PNG_SIGNATURE, RtfPictureFormat.Png, 1, 1, goalWidthTwips = 15))
        assertEquals(
            RtfInlinePicture(JPEG_PREFIX, RtfPictureFormat.Jpeg, 2, 2),
            RtfInlinePicture(JPEG_PREFIX.copyOf(), RtfPictureFormat.Jpeg, 2, 2),
        )
    }
}
