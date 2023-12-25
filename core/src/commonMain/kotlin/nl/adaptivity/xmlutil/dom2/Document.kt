/*
 * Copyright (c) 2022.
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

package nl.adaptivity.xmlutil.dom2

public interface Document : Node {

    public fun getImplementation(): DOMImplementation = getImplementation()

    public fun getDoctype(): DocumentType? = getDoctype()

    public fun getDocumentElement(): Element? = getDocumentElement()

    public fun getInputEncoding(): String? = getInputEncoding()

    public fun importNode(node: Node, deep: Boolean = false): Node

    public fun adoptNode(node: Node): Node

    public fun createAttribute(localName: String): Attr

    public fun createAttributeNS(namespace: String?, qualifiedName: String): Attr

    public fun createElement(localName: String): Element

    public fun createElementNS(namespaceURI: String, qualifiedName: String): Element

    public fun createDocumentFragment(): DocumentFragment

    public fun createTextNode(data: String): Text

    public fun createCDATASection(data: String): CDATASection

    public fun createComment(data: String): Comment

    public fun createProcessingInstruction(target: String, data: String): ProcessingInstruction

}

@Deprecated("Use implementation", ReplaceWith("implementation.supportsWhitespaceAtToplevel"))
public val Document.supportsWhitespaceAtToplevel: Boolean get() = getImplementation().supportsWhitespaceAtToplevel


public inline val Document.implementation: DOMImplementation get() = getImplementation()
public inline val Document.doctype: DocumentType? get() = getDoctype()
public inline val Document.documentElement: Element? get() = getDocumentElement()
public inline val Document.inputEncoding: String? get() = getInputEncoding()


public fun Document.importNode(node: Node): Node = importNode(node, false)
public inline val Document.characterSet: String? get() = getInputEncoding()
