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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.core.impl.idom.IDocumentFragment
import nl.adaptivity.xmlutil.core.impl.idom.INode
import nl.adaptivity.xmlutil.core.impl.idom.INodeList
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom2.NodeType

internal class DocumentFragmentImpl(ownerDocument: DocumentImpl) : NodeImpl(ownerDocument), IDocumentFragment {
    override fun getPreviousSibling(): Nothing? = null
    override fun getNextSibling(): Nothing? = null

    override var parentNode: INode?
        get() = null
        set(_) {
            throw UnsupportedOperationException()
        }

    @Suppress("PropertyName")
    internal val _childNodes: NodeListImpl = NodeListImpl()

    override fun getChildNodes(): INodeList = _childNodes

    override val nodetype: NodeType get() = NodeType.DOCUMENT_FRAGMENT_NODE

    override fun getNodeName(): String = "#document-fragment"

    override fun getFirstChild(): INode? = _childNodes.elements.firstOrNull()

    override fun getLastChild(): INode? = _childNodes.elements.lastOrNull()

    override fun getTextContent(): String = buildString {
        for (n in getChildNodes()) {
            appendTextContent(n)
        }
    }

    override fun setTextContent(value: String) {
        _childNodes.elements.clear()
        appendChild(ownerDocument.createTextNode(value))
    }

    override fun appendChild(node: INode): INode {
        val n = checkNode(node)
        _childNodes.elements.add(n)
        n.parentNode = this
        return node
    }

    override fun replaceChild(oldChild: INode, newChild: INode): INode {
        val oldIdx = _childNodes.elements.indexOf(checkNode(oldChild))
        if (oldIdx < 0) throw DOMException("Old child not found")

        _childNodes.elements[oldIdx].parentNode = null
        val n = checkNode(newChild)
        _childNodes.elements[oldIdx] = n
        n.parentNode = this

        return oldChild
    }

    override fun removeChild(node: INode): INode {
        val c = checkNode(node)

        if (!_childNodes.elements.remove(c)) throw DOMException("Node not found")
        c.parentNode = null

        return c
    }

    override fun lookupPrefix(namespace: String): String? = null

    override fun lookupNamespaceURI(prefix: String): String? = null
}
