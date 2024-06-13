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

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil.xmlserializable

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import java.io.CharArrayReader
import java.io.CharArrayWriter
import java.io.Reader
import java.io.StringWriter
import nl.adaptivity.xmlutil.XmlSerializable as XmlSerializableCompat


@Deprecated("Use the version using the new serializable")
public fun XmlSerializableCompat.toReader(): Reader =
    wrap().toReader()

/**
 * Create a reader that can be used to read the xml serialization of the element.
 */
@Throws(XmlException::class)
public fun XmlSerializable.toReader(): Reader {
    val buffer = CharArrayWriter()
    @Suppress("CAST_NEVER_SUCCEEDS")
    xmlStreaming.newWriter(buffer as Writer).use {
        serialize(it)

    }
    return CharArrayReader(buffer.toCharArray())
}

@Deprecated("Use the version using the new serializable")
public fun XmlSerializableCompat.serialize(writer: Writer) {
    wrap().serialize(writer)
}


/**
 * Serialize the object to XML
 */
@Throws(XmlException::class)
public fun XmlSerializable.serialize(writer: Writer) {
    xmlStreaming.newWriter(writer, repairNamespaces = true, xmlDeclMode = XmlDeclMode.None).use { serialize(it) }
}

public fun XmlSerializable.toString(flags: Int): String {
    return StringWriter().apply {
        @Suppress("CAST_NEVER_SUCCEEDS")
        xmlStreaming.newWriter(
            this as Writer,
            flags.and(FLAG_REPAIR_NS) == FLAG_REPAIR_NS,
            if (flags.and(FLAG_OMIT_XMLDECL) == FLAG_OMIT_XMLDECL) XmlDeclMode.None else XmlDeclMode.Auto
        ).use { writer ->
            serialize(writer)
        }
    }.toString()
}

public fun toString(serializable: XmlSerializable): String = serializable.toString(
    DEFAULT_FLAGS
)

/**
 * Do bulk toString conversion of a list. Note that this is serialization, not dropping tags.
 * @receiver The source list.
 *
 * @return A result list
 */
@JvmName("toString")
public fun Iterable<XmlSerializable>.toSerializedStrings(): List<String> {
    return this.map { it.toString(DEFAULT_FLAGS) }
}
