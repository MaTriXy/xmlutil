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

package nl.adaptivity.xmlutil.core.impl.idom

import nl.adaptivity.xmlutil.core.impl.dom.unWrap
import org.w3c.dom.Node
import nl.adaptivity.xmlutil.dom.Document as Document1
import nl.adaptivity.xmlutil.dom.Node as Node1
import nl.adaptivity.xmlutil.dom2.Document as Document2
import nl.adaptivity.xmlutil.dom2.Node as Node2
import org.w3c.dom.Node as DomNode

public interface IDocument : INode, Document1, Document2 {
    override val implementation: IDOMImplementation

    override val doctype: IDocumentType?

    public val documentURI: String

    override val documentElement: IElement?

    override fun getInputEncoding(): String = inputEncoding

    override fun getImplementation(): IDOMImplementation = implementation

    override fun getDoctype(): IDocumentType? = doctype

    override fun getDocumentElement(): IElement? = documentElement

    override fun createElement(localName: String): IElement

    override fun createDocumentFragment(): IDocumentFragment

    override fun createTextNode(data: String): IText

    override fun createCDATASection(data: String): ICDATASection

    override fun createComment(data: String): IComment

    override fun createProcessingInstruction(target: String, data: String): IProcessingInstruction

    override fun adoptNode(node: Node1): INode = adoptNode(node.unWrap())

    override fun adoptNode(node: Node2): INode = adoptNode(node.unWrap())

    public fun adoptNode(node: INode): INode = adoptNode(node.delegate)

    public fun adoptNode(node: DomNode): INode

    override fun createAttribute(localName: String): IAttr

    override fun createAttributeNS(namespace: String?, qualifiedName: String): IAttr

    override fun createElementNS(namespaceURI: String, qualifiedName: String): IElement

    public fun importNode(node: Node1): INode = importNode(node, false)

    override fun importNode(node: Node1, deep: Boolean): INode = importNode(node.unWrap(), deep)

    override fun importNode(node: Node2, deep: Boolean): INode = importNode(node.unWrap(), deep)

    public fun importNode(node: INode, deep: Boolean = false): INode = importNode(node.delegate, deep)

    public fun importNode(node: Node, deep: Boolean = false): INode
}
