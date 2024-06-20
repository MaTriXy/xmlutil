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

@file:Suppress("EXPERIMENTAL_API_USAGE")

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.Namespace
import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.serialization.structure.SafeXmlDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import nl.adaptivity.xmlutil.toQname

internal abstract class XmlCodecBase internal constructor(
    val serializersModule: SerializersModule,
    val config: XmlConfig
) {

    internal abstract val namespaceContext: NamespaceContext

    companion object {

        @OptIn(ExperimentalSerializationApi::class)
        internal fun SerialDescriptor.declRequestedName(
            parentNamespace: Namespace,
            annotation: XmlSerialName?
        ): QName {
            annotation?.let { return it.toQName(serialName, parentNamespace) }
            return serialName.substringAfterLast('.').toQname(parentNamespace)
        }

        /**
         * TODO: move to policy
         * TODO: use the XmlSerialDescriptor rather than type name, and maybe use known types.
         * This function is used by the decoder to try to expand a shortened type name. It is the
         * opposite of [tryShortenTypeName].
         */
        internal fun String.expandTypeNameIfNeeded(parentType: String?): String {
            if (parentType == null || !startsWith('.')) return this
            val parentPkg = parentType.lastIndexOf('.').let { idx ->
                if (idx < 0) return substring(1)
                parentType.substring(0, idx)
            }
            return "$parentPkg$this"
        }

        /**
         * TODO: move to policy
         * This function is used by the encoder to try shorten a type name. It is the
         * opposite of [tryShortenTypeName].
         */
        internal fun String.tryShortenTypeName(parentType: String?): String {
            if (parentType == null) return this

            val parentPkg = parentType.lastIndexOf('.').let { idx ->
                if (idx < 0) return this
                parentType.substring(0, idx)
            }
            if (startsWith(parentPkg) && indexOf('.', parentPkg.length + 1) < 0) {
                return substring(parentPkg.length) // include starting . to signal relative type
            }
            return this
        }

    }

    @Suppress("RedundantInnerClassModifier") // The actual children must be inner
    abstract class XmlCodec<out D : SafeXmlDescriptor>(
        protected val xmlDescriptor: D
    ) {
        val serialName: QName get() = xmlDescriptor.tagName
    }

    internal abstract inner class XmlTagCodec<out D : XmlDescriptor>(val xmlDescriptor: D) {

        val config get() = this@XmlCodecBase.config
        val serializersModule: SerializersModule get() = this@XmlCodecBase.serializersModule

        val serialName: QName get() = xmlDescriptor.tagName

        protected abstract val namespaceContext: NamespaceContext

        internal fun QName.normalize(): QName {
            return copy(prefix = "")
        }

    }
}
