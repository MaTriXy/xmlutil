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


/**
 * Annotation to signify that the annotated code is internal to the XmlUtil module, and no API
 * stability is guaranteed.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_GETTER)
@RequiresOptIn("This function is internal to the XmlUtil modules. No api stability is guaranteed", RequiresOptIn.Level.ERROR)
public annotation class XmlUtilInternal

/**
 * Annotation to signify that the annotated code is internal to the XmlUtil module, and no API
 * stability is guaranteed.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY_GETTER)
@RequiresOptIn("This function/type should have been internal to the XmlUtil modules. Its use is deprecated", RequiresOptIn.Level.WARNING)
public annotation class XmlUtilDeprecatedInternal

/**
 * Annotation to signify that the annotated code is experimental, with limited compatibility support.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS, AnnotationTarget.CONSTRUCTOR)
@RequiresOptIn("This XML Serialization code is not stable and may be subject to binary and source incompatible changes", RequiresOptIn.Level.WARNING)
public annotation class ExperimentalXmlUtilApi
