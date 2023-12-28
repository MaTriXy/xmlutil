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

package nl.adaptivity.xmlutil.util

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.*
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Node
import org.w3c.dom.parsing.XMLSerializer


public typealias JSCompactFragment = CompactFragment

/**
 * A class representing an xml fragment compactly.
 * Created by pdvrieze on 06/11/15.
 */
@Serializable(CompactFragmentSerializer::class)
public actual class CompactFragment : ICompactFragment {

    @Suppress("DEPRECATION")
    public actual class Factory : XmlDeserializerFactory<CompactFragment> {

        override fun deserialize(reader: XmlReader): CompactFragment {
            return CompactFragment.deserialize(reader)
        }
    }

    override val isEmpty: Boolean
        get() = contentString.isEmpty()


    override val namespaces: IterableNamespaceContext

    @Deprecated("In javascript this is not efficient, use contentString")
    override val content: CharArray
        get() = CharArray(contentString.length) { i -> contentString[i] }

    override val contentString: String

    public actual constructor(namespaces: Iterable<Namespace>, content: CharArray?) {
        this.namespaces = SimpleNamespaceContext.from(namespaces)
        this.contentString = content?.toString() ?: ""
    }

    /** Convenience constructor for content without namespaces.  */
    public actual constructor(content: String) : this(emptyList(), content)

    public constructor(documentFragment: DocumentFragment) : this(
        XMLSerializer().serializeToString(documentFragment)
                                                          )

    public constructor(node: Node) : this(XMLSerializer().serializeToString(node))

    /** Convenience constructor for content without namespaces.  */
    public actual constructor(namespaces: Iterable<Namespace>, content: String) {
        this.namespaces = SimpleNamespaceContext.from(namespaces)
        this.contentString = content
    }

    public actual constructor(orig: ICompactFragment) {
        namespaces = SimpleNamespaceContext.from(orig.namespaces)
        contentString = orig.contentString
    }

    override fun serialize(out: XmlWriter) {
        XMLFragmentStreamReader.from(this).let { reader: XmlReader ->
            out.serialize(reader)
        }
    }

    override fun getXmlReader(): XmlReader = XMLFragmentStreamReader.from(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        @Suppress("UnsafeCastFromDynamic")
        if (other == null || prototype != other.prototype) return false

        val that = other as ICompactFragment?

        if (namespaces != that!!.namespaces) return false
        return contentString == that.contentString

    }

    override fun hashCode(): Int {
        var result = namespaces.hashCode()
        result = 31 * result + contentString.hashCode()
        return result
    }

    override fun toString(): String {
        return buildString {
            append("{namespaces=[")
            namespaces.joinTo(this) { "\"${it.prefix} -> ${it.namespaceURI}\"" }

            append("], content=")
                .append(contentString)
                .append('}')
        }
    }


    public actual companion object {

        public val FACTORY: Factory = Factory()

        public actual fun deserialize(reader: XmlReader): CompactFragment {
            return reader.siblingsToFragment()
        }
    }
}


/**
 * Helper function that exposes the prototype object of javascript objects.
 */
private val Any.prototype: dynamic
    get() {

        inline fun prototype(o: dynamic): dynamic {
            return o.prototype
        }

        return prototype(this)
    }
