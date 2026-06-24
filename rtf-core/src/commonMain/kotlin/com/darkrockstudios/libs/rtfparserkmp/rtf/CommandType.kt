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

package com.darkrockstudios.libs.rtfparserkmp.rtf

/**
 * Enumeration of command types. See Appendix B of the RTF specification.
 */
enum class CommandType {
    /** This control word represents a special character. */
    Symbol,

    /** This control word ignores any parameter. */
    Flag,

    /** This control word distinguishes between the ON and OFF states for the given property. */
    Toggle,

    /** This control word requires a parameter. */
    Value,

    /** This control word starts a group or destination. It ignores any parameter. */
    Destination,

    /** Switch the character encoding used from this point in the document. */
    Encoding,
}
