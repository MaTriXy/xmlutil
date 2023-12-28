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

package nl.adaptivity.xmlutil

@Suppress("UNCHECKED_CAST")
/**
 * Interface that factories need to implement to handle be deserialization in a "shared"
 * non-reflective approach.
 *
 * Created by pdvrieze on 27/08/15.
 */
@Deprecated(
    "This should be replaced by kotlinx.serialization or the xmlserializable module",
    ReplaceWith("nl.adaptivity.xmlutil.xmlserializable.XmlDeserializerFactory")
)
public interface XmlDeserializerFactory<T> {

    /** Deserialize the object */
    public fun deserialize(reader: XmlReader): T
}
