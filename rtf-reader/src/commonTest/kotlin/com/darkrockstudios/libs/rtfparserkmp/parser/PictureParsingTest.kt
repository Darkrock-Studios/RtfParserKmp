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

package com.darkrockstudios.libs.rtfparserkmp.parser

import com.darkrockstudios.libs.rtfparserkmp.converter.convertToMarkdown
import com.darkrockstudios.libs.rtfparserkmp.converter.extractPlainText
import com.darkrockstudios.libs.rtfparserkmp.model.PictureFormat
import com.darkrockstudios.libs.rtfparserkmp.rtf.Command
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun cmd(command: Command, parameter: Int? = null, optional: Boolean = false) =
    RtfEvent.Command(command, parameter ?: 0, hasParameter = parameter != null, optional = optional)

private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
private const val PNG_SIGNATURE_HEX = "89504e470d0a1a0a"

private const val PNG_DOCUMENT =
    "{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0 Times;}}\n" +
        "Before\\par\n" +
        "{\\pict\\pngblip\\picw2\\pich1\\picwgoal30\\pichgoal15\n" +
        "89504e47\n" +
        "0D0A1A0A}\n" +
        "\\par After\\par}"

class PictureParsingTest {
    private fun events(rtf: String): List<RtfEvent> = parseRtf(rtf.encodeToByteArray())

    private fun pictures(rtf: String): List<RtfEvent.Picture> = events(rtf).filterIsInstance<RtfEvent.Picture>()

    private fun assertBalanced(events: List<RtfEvent>) {
        var depth = 0
        for (event in events) {
            if (event == RtfEvent.GroupStart) depth++
            if (event == RtfEvent.GroupEnd) depth--
            assertTrue(depth >= 0, "group end without a start in $events")
        }
        assertEquals(0, depth, "unbalanced groups in $events")
    }

    @Test
    fun pictureHexDoesNotLeakIntoPlainText() {
        assertEquals("Before\n\nAfter\n", extractPlainText(PNG_DOCUMENT.encodeToByteArray()))
    }

    @Test
    fun pictureHexDoesNotLeakIntoMarkdown() {
        // The extra blank paragraph is the one that held the picture.
        assertEquals("Before\n\n\n\nAfter", convertToMarkdown(PNG_DOCUMENT.encodeToByteArray()))
    }

    @Test
    fun pngPictureIsOneEventWithBytesFormatAndSizes() {
        val events = events(PNG_DOCUMENT)
        val picture = events.filterIsInstance<RtfEvent.Picture>().single()
        assertContentEquals(PNG_SIGNATURE, picture.bytes)
        assertEquals(PictureFormat.Png, picture.format)
        assertEquals(2, picture.width)
        assertEquals(1, picture.height)
        assertEquals(30, picture.goalWidthTwips)
        assertEquals(15, picture.goalHeightTwips)
        assertEquals(100, picture.scaleXPercent)
        assertEquals(100, picture.scaleYPercent)

        assertEquals(RtfEvent.Picture(PNG_SIGNATURE.copyOf(), PictureFormat.Png, 2, 1, 30, 15), picture)
        assertFalse(events.any { it is RtfEvent.Text && ("8950" in it.text || "0D0A" in it.text) })
        assertBalanced(events)
    }

    @Test
    fun pictureSitsBetweenThePictCommandAndTheGroupEnd() {
        val events = events(PNG_DOCUMENT)
        val index = events.indexOfFirst { it is RtfEvent.Picture }
        assertEquals(cmd(Command.pict), events[index - 1])
        assertEquals(RtfEvent.GroupStart, events[index - 2])
        assertEquals(RtfEvent.GroupEnd, events[index + 1])
    }

    @Test
    fun jpegPictureWithoutGoals() {
        val picture = pictures("{\\rtf1\\ansi a{\\pict\\jpegblip\\picw4\\pich3 ffd8ffe0}b}").single()
        assertContentEquals(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()), picture.bytes)
        assertEquals(PictureFormat.Jpeg, picture.format)
        assertEquals(4, picture.width)
        assertEquals(3, picture.height)
        assertNull(picture.goalWidthTwips)
        assertNull(picture.goalHeightTwips)
    }

    @Test
    fun wordStylePictureKeepsRawSizesAndScale() {
        val rtf = "{\\rtf1\\ansi {\\pict\\picscalex50\\picscaley75\\pngblip" +
            "\\picw11906\\pich8467\\picwgoal6750\\pichgoal4800 $PNG_SIGNATURE_HEX}}"
        assertEquals(
            RtfEvent.Picture(PNG_SIGNATURE, PictureFormat.Png, 11906, 8467, 6750, 4800, 50, 75),
            pictures(rtf).single(),
        )
        assertNotEquals(
            RtfEvent.Picture(PNG_SIGNATURE, PictureFormat.Png, 11906, 8467, 6750, 4800, 50, 76),
            pictures(rtf).single(),
        )
    }

    @Test
    fun unknownFormatIsNull() {
        assertNull(pictures("{\\rtf1\\ansi {\\pict\\picw1\\pich1 00ff}}").single().format)
    }

    @Test
    fun wordShppictPairYieldsOnePicture() {
        val rtf = "{\\rtf1\\ansi Before" +
            "{\\*\\shppict{\\pict\\pngblip\\picw1\\pich1 $PNG_SIGNATURE_HEX}}" +
            "{\\nonshppict{\\pict\\wmetafile8\\picw26\\pich26 0100090000}}" +
            "After}"
        val expected = listOf(
            RtfEvent.DocumentStart,
            RtfEvent.GroupStart,
            cmd(Command.rtf, 1),
            RtfEvent.Text("Before"),
            RtfEvent.GroupStart,
            cmd(Command.shppict, optional = true),
            RtfEvent.GroupStart,
            cmd(Command.pict),
            RtfEvent.Picture(PNG_SIGNATURE, PictureFormat.Png, 1, 1, null, null),
            RtfEvent.GroupEnd,
            RtfEvent.GroupEnd,
            RtfEvent.GroupStart,
            cmd(Command.nonshppict),
            RtfEvent.GroupStart,
            cmd(Command.pict),
            RtfEvent.GroupEnd,
            RtfEvent.GroupEnd,
            RtfEvent.Text("After"),
            RtfEvent.GroupEnd,
            RtfEvent.DocumentEnd,
        )
        assertEquals(expected, events(rtf))
        assertEquals("BeforeAfter", extractPlainText(rtf.encodeToByteArray()))
    }

    @Test
    fun wordFloatingShapeYieldsOnlyTheShapePicture() {
        val rtf = "{\\rtf1\\ansi A{\\shp{\\*\\shpinst\\shpleft0\\shptop0\\shpright300\\shpbottom150" +
            "{\\sp{\\sn shapeType}{\\sv 75}}" +
            "{\\sp{\\sn pib}{\\sv {\\pict\\pngblip\\picw1\\pich1 $PNG_SIGNATURE_HEX}}}}" +
            "{\\shprslt\\pard{\\pict\\wmetafile8\\picw26\\pich26 0100090000}}}B}"
        val events = events(rtf)
        val picture = events.filterIsInstance<RtfEvent.Picture>().single()
        assertEquals(PictureFormat.Png, picture.format)
        assertContentEquals(PNG_SIGNATURE, picture.bytes)
        assertEquals(2, events.count { it == cmd(Command.pict) })
        assertBalanced(events)
        assertEquals("AB", extractPlainText(rtf.encodeToByteArray()))
    }

    @Test
    fun pictureAfterAFallbackGroupIsKept() {
        val rtf = "{\\rtf1\\ansi {\\nonshppict{\\pict\\wmetafile8 0100}}{\\shprslt{\\pict\\wmetafile8 0100}}" +
            "{\\pict\\pngblip $PNG_SIGNATURE_HEX}}"
        assertEquals(PictureFormat.Png, pictures(rtf).single().format)
    }

    @Test
    fun pictureInsideFieldResultIsEmittedWithoutLeakingHex() {
        val rtf = "{\\rtf1\\ansi a{\\field{\\*\\fldinst INCLUDEPICTURE \"x.png\"}" +
            "{\\fldrslt {\\pict\\pngblip\\picw1\\pich1 $PNG_SIGNATURE_HEX}}}b}"
        val events = events(rtf)
        assertContentEquals(PNG_SIGNATURE, events.filterIsInstance<RtfEvent.Picture>().single().bytes)
        assertBalanced(events)
        assertEquals("ab", extractPlainText(rtf.encodeToByteArray()))
    }

    @Test
    fun nestedGroupsInsideThePictGroupAreIgnored() {
        val rtf = "{\\rtf1\\ansi {\\pict{\\*\\picprop{\\sp{\\sn shapeType}{\\sv 75}}}" +
            "\\picscalex100\\pngblip\\picw1\\pich1\\bliptag-12345{\\*\\blipuid 0123456789abcdef0123456789abcdef}" +
            "$PNG_SIGNATURE_HEX}}"
        assertContentEquals(PNG_SIGNATURE, pictures(rtf).single().bytes)
        assertEquals("", extractPlainText(rtf.encodeToByteArray()))
    }

    @Test
    fun binaryPayloadIsUsedDirectly() {
        val rtf = "{\\rtf1\\ansi {\\pict\\pngblip\\picw1\\pich1\\bin4 ABCD}z}"
        assertContentEquals("ABCD".encodeToByteArray(), pictures(rtf).single().bytes)
        assertEquals("z", extractPlainText(rtf.encodeToByteArray()))
    }

    @Test
    fun malformedHexYieldsNoPictureAndDoesNotThrow() {
        val nonHex = "{\\rtf1\\ansi a{\\pict\\pngblip 89504e47zz}b}"
        assertTrue(pictures(nonHex).isEmpty())
        assertEquals("ab", extractPlainText(nonHex.encodeToByteArray()))

        val oddLength = "{\\rtf1\\ansi a{\\pict\\pngblip 89504e4}b}"
        assertTrue(pictures(oddLength).isEmpty())
        assertEquals("ab", extractPlainText(oddLength.encodeToByteArray()))

        assertTrue(pictures("{\\rtf1\\ansi {\\pict\\pngblip}}").isEmpty())
    }

    @Test
    fun nonAsciiDigitYieldsNoPicture() {
        // \u-240 is U+FF10 FULLWIDTH DIGIT ZERO, which Char.digitToIntOrNull(16) would accept.
        val rtf = "{\\rtf1\\ansi a{\\pict\\pngblip 89504e470d0a1a0\\u-240?}b}"
        val events = events(rtf)
        assertTrue(events.none { it is RtfEvent.Picture })
        assertBalanced(events)
        assertEquals("ab", extractPlainText(rtf.encodeToByteArray()))
    }

    @Test
    fun pictInsideUprKeepsOnlyTheUnicodeVersion() {
        val rtf = "{\\rtf1\\ansi A" +
            "{\\upr{\\pict\\jpegblip\\picw1\\pich1 ffd8}{\\*\\ud{\\pict\\pngblip\\picw1\\pich1 $PNG_SIGNATURE_HEX}}}" +
            "B}"
        val events = events(rtf)
        val picture = events.filterIsInstance<RtfEvent.Picture>().single()
        assertEquals(PictureFormat.Png, picture.format)
        assertContentEquals(PNG_SIGNATURE, picture.bytes)
        assertBalanced(events)
        assertEquals("AB", extractPlainText(rtf.encodeToByteArray()))
    }
}
