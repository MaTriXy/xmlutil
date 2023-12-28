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

package nl.adaptivity.xmlutil.core.impl.dom

import nl.adaptivity.xmlutil.core.impl.idom.IAttr
import nl.adaptivity.xmlutil.core.impl.idom.INamedNodeMap
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import nl.adaptivity.xmlutil.dom2.Attr as Attr2

internal class WrappingNamedNodeMap(val delegate: NamedNodeMap) : INamedNodeMap {
    override val size: Int get() = delegate.length

    override fun item(index: Int): IAttr? {
        return delegate.item(index)?.wrapAttr()
    }

    override fun getNamedItem(qualifiedName: String): IAttr? {
        return delegate.getNamedItem(qualifiedName)?.wrapAttr()
    }

    override fun getNamedItemNS(namespace: String?, localName: String): IAttr? {
        return delegate.getNamedItemNS(namespace, localName)?.wrapAttr()
    }

    override fun setNamedItem(attr: Node): IAttr? {
        return delegate.setNamedItem(attr.unWrap())?.wrapAttr()
    }

    override fun setNamedItem(attr: Attr2): IAttr? {
        return delegate.setNamedItem(attr.unWrap())?.wrapAttr()
    }

    override fun setNamedItemNS(attr: Node): IAttr? {
        return delegate.setNamedItemNS(attr.unWrap())?.wrapAttr()
    }

    override fun setNamedItemNS(attr: Attr2): IAttr? {
        return delegate.setNamedItemNS(attr.unWrap())?.wrapAttr()
    }

    override fun removeNamedItem(qualifiedName: String): IAttr? {
        return delegate.removeNamedItem(qualifiedName)?.wrapAttr()
    }

    override fun removeNamedItemNS(namespace: String?, localName: String): IAttr? {
        return delegate.removeNamedItemNS(namespace, localName)?.wrapAttr()
    }

    override fun iterator(): Iterator<IAttr> {
        return IteratorImpl(delegate)
    }

    private class IteratorImpl(private val delegate: NamedNodeMap): Iterator<IAttr> {
        private var next: Int = 0
        override fun next(): IAttr {
            return delegate.item(next++).wrapAttr()
        }

        override fun hasNext(): Boolean {
            return next < delegate.length
        }
    }
}


