/*
 * Copyright (c) 2024.
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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.MpJvmDefaultWithCompatibility

/**
 * Interface that represents a namespace with prefix and uri.
 */
@MpJvmDefaultWithCompatibility
public interface Namespace {

    /**
     * Gets the prefix, returns "" if this is a default
     * namespace declaration.
     */
    public val prefix: String

    public operator fun component1(): String = prefix

    /**
     * Gets the uri bound to the prefix of this namespace
     */
    public val namespaceURI: String

    public operator fun component2(): String = namespaceURI

    public companion object : KSerializer<Namespace> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(Namespace::class.simpleName!!) {
            element("prefix", serialDescriptor<String>())
            element("namespaceURI", serialDescriptor<String>())
        }

        override fun deserialize(decoder: Decoder): Namespace {
            lateinit var prefix: String
            lateinit var namespaceUri: String
            decoder.decodeStructure(descriptor) {
                var index = decodeElementIndex(descriptor)
                while (index!=CompositeDecoder.DECODE_DONE) {
                    when (index) {
                        0 -> prefix = decodeStringElement(descriptor, index)
                        1 -> namespaceUri = decodeStringElement(descriptor, index)
                    }

                    index = decodeElementIndex(descriptor)
                }
            }
            return XmlEvent.NamespaceImpl(prefix, namespaceUri)
        }

        override fun serialize(encoder: Encoder, value: Namespace) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value.prefix)
                encodeStringElement(descriptor, 1, value.namespaceURI)
            }
        }

    }
}
