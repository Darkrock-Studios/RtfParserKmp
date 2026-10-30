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

package com.darkrockstudios.libs.rtfparserkmp.model

/** The image encoding of an RTF `\pict` group, named after the control word that declares it. */
enum class PictureFormat {
    /** `\pngblip` */
    Png,

    /** `\jpegblip` */
    Jpeg,

    /** `\emfblip`: an Enhanced Metafile. */
    Emf,

    /** `\wmetafileN`: a Windows Metafile. */
    Wmf,

    /** `\pmmetafileN`: an OS/2 Presentation Manager metafile. */
    Os2Metafile,

    /** `\macpict`: a QuickDraw PICT. */
    MacPict,

    /** `\dibitmapN`: a device-independent bitmap. */
    DeviceIndependentBitmap,

    /** `\wbitmapN`: a device-dependent bitmap. */
    DeviceDependentBitmap,
}
