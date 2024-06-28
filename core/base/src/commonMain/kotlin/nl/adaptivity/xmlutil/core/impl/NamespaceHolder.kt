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

package nl.adaptivity.xmlutil.core.impl

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XMLConstants.DEFAULT_NS_PREFIX
import nl.adaptivity.xmlutil.XMLConstants.NULL_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XML_NS_PREFIX
import nl.adaptivity.xmlutil.XMLConstants.XML_NS_URI


/**
 * A utility class that helps with maintaining a namespace context in a parser.
 * Created by pdvrieze on 16/11/15.
 */
@XmlUtilInternal
public open class NamespaceHolder : Iterable<Namespace> {
    private var nextAutoPrefixNo = 1
    private var nameSpaces = arrayOfNulls<String>(10)
    private var namespaceCounts = IntArray(20)
    public var depth: Int = 0
        private set

    public val namespacesAtCurrentDepth: List<Namespace>
        get() {
            val startIdx = if(depth == 0) 0 else (arrayUseAtDepth(depth - 1) shr 1)
            val endIdx = (arrayUseAtDepth(depth) shr 1)
            return List(endIdx-startIdx) { offset ->
                val i = startIdx+offset
                XmlEvent.NamespaceImpl(getPrefix(i), getNamespace(i))
            }
        }

    public fun incDepth() {
        ++depth
        if (depth >= namespaceCounts.size) {
            namespaceCounts = namespaceCounts.copyOf(namespaceCounts.size * 2)
        }
        namespaceCounts[depth] = if (depth == 0) 0 else namespaceCounts[depth - 1]
    }

    private fun namespaceIndicesAt(depth: Int): IntRange {
        val startIdx = if (depth == 0) 0 else arrayUseAtDepth(depth - 1) / 2
        val endIdx = arrayUseAtDepth(depth) / 2
        return startIdx until endIdx
    }

    public fun decDepth() {
        for (i in namespaceIndicesAt(depth)) {
            nameSpaces[prefixArrayPos(i)] = null
            nameSpaces[nsArrayPos(i)] = null
        }
        namespaceCounts[depth] = 0
        --depth
    }

    /**
     * The total amount of namespaces in this holder
     */
    public val totalNamespaceCount: Int
        get() = namespaceCounts[depth]

    private fun arrayUseAtDepth(depth: Int) =
        namespaceCounts[depth] * 2

    private fun prefixArrayPos(pairPos: Int) = pairPos * 2

    private fun nsArrayPos(pairPos: Int) = pairPos * 2 + 1

    private fun setPrefix(pos: Int, value: CharSequence?) {
        nameSpaces[prefixArrayPos(pos)] = value?.toString() ?: ""
    }

    private fun getPrefix(pos: Int): String =
        nameSpaces[prefixArrayPos(pos)]!!

    private fun setNamespace(pos: Int, value: CharSequence?) {
        nameSpaces[nsArrayPos(pos)] = value?.toString() ?: ""
    }

    private fun getNamespace(pos: Int): String =
        nameSpaces[nsArrayPos(pos)]!!


    public fun clear() {
        nameSpaces = arrayOfNulls(10)
        namespaceCounts = IntArray(20)
        depth = 0
    }

    public fun addPrefixToContext(ns: Namespace) {
        addPrefixToContext(ns.prefix, ns.namespaceURI)
    }


    public fun addPrefixToContext(prefix: CharSequence?, namespaceUri: CharSequence?) {
        val prevCounts = if (depth >= 1) namespaceCounts[depth - 1] else 0
        for (i in prevCounts until namespaceCounts[depth]) {
            if (getPrefix(i) == prefix && getNamespace(i) == namespaceUri) return
        }

        val nextPair = namespaceCounts[depth]
        if (nsArrayPos(nextPair) >= nameSpaces.size) enlargeNamespaceBuffer()

        setPrefix(nextPair, prefix)
        setNamespace(nextPair, namespaceUri)

        namespaceCounts[depth]+=1
    }

    private fun enlargeNamespaceBuffer() {
        nameSpaces = nameSpaces.copyOf(nameSpaces.size * 2)
    }


    // From first namespace
    public val namespaceContext: IterableNamespaceContext = object : IterableNamespaceContext {
        override fun getNamespaceURI(prefix: String): String? {
            return this@NamespaceHolder.getNamespaceUri(prefix)
        }

        override fun getPrefix(namespaceURI: String): String? {
            return this@NamespaceHolder.getPrefix(namespaceURI)
        }

        @Suppress("UNCHECKED_CAST")
        override fun freeze(): IterableNamespaceContext =
            SimpleNamespaceContext(nameSpaces.copyOfRange(0, totalNamespaceCount*2) as Array<String>)

        override fun iterator(): Iterator<Namespace> = this@NamespaceHolder.iterator()

        override fun getPrefixes(namespaceURI: String): Iterator<String> {
            return ((totalNamespaceCount - 1) downTo 0)
                .asSequence()
                .filter { getNamespace(it) == namespaceURI }
                .map { getPrefix(it) }
                .iterator()
        }
    }

    public fun getNamespaceUri(prefix: CharSequence): String? {
        val prefixStr = prefix.toString()
        var nsIdx = totalNamespaceCount
        while (--nsIdx >= 0) {
            if (prefixStr == getPrefix(nsIdx)) return getNamespace(nsIdx)
        }

        return when (prefixStr) {
            XML_NS_PREFIX -> XML_NS_URI
            XMLNS_ATTRIBUTE -> XMLNS_ATTRIBUTE_NS_URI
            "" -> ""
            else -> null
        }
    }

    public fun getPrefix(namespaceUri: CharSequence): String? {
        return when (val namespaceUriStr = namespaceUri.toString()) {
            XML_NS_URI -> XML_NS_PREFIX
            XMLNS_ATTRIBUTE_NS_URI -> XMLNS_ATTRIBUTE
            "" -> if ((0 until totalNamespaceCount).none { getPrefix(it).isEmpty() }) DEFAULT_NS_PREFIX else null
            else -> {
                ((totalNamespaceCount - 1) downTo 0)
                    .firstOrNull { i ->
                        getNamespace(i) == namespaceUriStr &&
                                ((i + 1) until totalNamespaceCount).none { j -> getPrefix(i) == getPrefix(j) }
                    }
                    ?.let { getPrefix(it) }
            }

        }
    }

    override fun iterator(): Iterator<Namespace> = object : Iterator<Namespace> {
        private var idx = 0
        override fun hasNext(): Boolean = idx < namespaceCounts[depth]

        override fun next(): Namespace {
            return XmlEvent.NamespaceImpl(getPrefix(idx), getNamespace(idx)).also {
                idx += 1
            }
        }
    }

    public fun nextAutoPrefix(): String {
        var prefix: String
        do {
            prefix = "n$nextAutoPrefixNo"
        } while (getNamespaceUri(prefix) != null)
        return prefix
    }

    /**
     * Look up a namespace uri declared at the current depth only
     */
    public fun namespaceAtCurrentDepth(prefix: String): String? {
        for (i in namespaceIndicesAt(depth) step 2) {
            if (nameSpaces[prefixArrayPos(i)] == prefix) {
                return nameSpaces[nsArrayPos(i)]
            }
        }
        return null
    }
}
