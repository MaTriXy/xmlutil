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

import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.dom2.DOMImplementation
import nl.adaptivity.xmlutil.dom2.Node

public interface IXmlStreaming {

    public fun setFactory(factory: XmlStreamingFactory?)

    public fun newReader(input: CharSequence): XmlReader

    public fun newReader(reader: Reader): XmlReader

    @ExperimentalXmlUtilApi
    public fun newReader(source: Node): XmlReader

    public fun newGenericReader(input: CharSequence): XmlReader

    public fun newGenericReader(reader: Reader): XmlReader

    @ExperimentalXmlUtilApi
    public fun newWriter(): DomWriter

    @ExperimentalXmlUtilApi
    public fun newWriter(dest: Node): DomWriter

    public val genericDomImplementation: DOMImplementation
}
