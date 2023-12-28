/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.multiplatform.IOException


/**
 * Simple exception for xml related things.
 * @author Created by pdvrieze on 15/11/15.
 */
public open class XmlException : IOException {

    public constructor()

    public constructor(message: String) : super(message)

    public constructor(message: String, cause: Throwable) : super(message, cause)

    public constructor(cause: Throwable) : super(cause)

    public constructor(message: String, reader: XmlReader, cause: Throwable) :
            super("${reader.locationInfo ?: "Unknown position"} - $message", cause)

    public constructor(message: String, reader: XmlReader) :
            super("${reader.locationInfo ?: "Unknown position"} - $message")

    @Deprecated("Only use in Java, in kotlin just throw", ReplaceWith("throw this"))
    public fun doThrow(): Nothing {
        throw this
    }
}
