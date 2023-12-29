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

package nl.adaptivity.xmlutil.core.impl.idom

import nl.adaptivity.xmlutil.dom2.NodeListIterator
import nl.adaptivity.xmlutil.dom.NodeList as NodeList1
import nl.adaptivity.xmlutil.dom2.Node as Node2
import nl.adaptivity.xmlutil.dom2.NodeList as NodeList2

public interface INodeList : NodeList1, NodeList2, Collection<Node2> {
    @Deprecated("Use size", ReplaceWith("size"))
    public override fun getLength(): Int = size

    override fun item(index: Int): INode?

    override fun get(index: Int): INode? {
        return item(index)
    }

    override fun iterator(): Iterator<INode> {
        return NodeListIterator(this)
    }

    public override fun contains(element: Node2): Boolean {
        return asSequence().contains(element)
    }

    public override fun containsAll(elements: Collection<Node2>): Boolean {
        return elements.all { contains(it) } // inefficient
    }

    public override fun isEmpty(): Boolean = getLength() == 0
}
