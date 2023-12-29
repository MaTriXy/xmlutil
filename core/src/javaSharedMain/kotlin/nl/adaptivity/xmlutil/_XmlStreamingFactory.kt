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

import nl.adaptivity.xmlutil.core.impl.AppendableWriter
import nl.adaptivity.xmlutil.core.impl.CharsequenceReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.MpJvmDefaultWithoutCompatibility
import java.io.*
import javax.xml.transform.Result
import javax.xml.transform.Source

@Suppress(
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING",
    "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING",
    "NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING",
    "DeprecatedCallableAddReplaceWith"
)
@MpJvmDefaultWithoutCompatibility
public actual interface XmlStreamingFactory {

    @Deprecated("Use version with xmlDeclMode")
    public fun newWriter(writer: Writer, repairNamespaces: Boolean = false, omitXmlDecl: Boolean): XmlWriter =
        newWriter(writer, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    public fun newWriter(
        writer: Writer,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
    ): XmlWriter

    @Deprecated("Use version with xmlDeclMode")
    public fun newWriter(
        outputStream: OutputStream,
        encoding: String,
        repairNamespaces: Boolean = false,
        omitXmlDecl: Boolean
    ): XmlWriter =
        newWriter(outputStream, encoding, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    public fun newWriter(
        outputStream: OutputStream,
        encoding: String,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
    ): XmlWriter

    @Deprecated("Use version with xmlDeclMode")
    public fun newWriter(result: Result, repairNamespaces: Boolean = false, omitXmlDecl: Boolean): XmlWriter =
        newWriter(result, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    public fun newWriter(
        result: Result,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
    ): XmlWriter

    @Deprecated("Use version with xmlDeclMode")
    public fun newWriter(output: Appendable, repairNamespaces: Boolean = false, omitXmlDecl: Boolean): XmlWriter =
        newWriter(AppendableWriter(output), repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    public fun newWriter(
        output: Appendable,
        repairNamespaces: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
    ): XmlWriter = newWriter(AppendableWriter(output), repairNamespaces, xmlDeclMode)

    public fun newReader(source: Source): XmlReader

    public fun newReader(reader: Reader): XmlReader

    public fun newReader(inputStream: InputStream, encoding: String = "UTF-8"): XmlReader

    public fun newReader(input: CharSequence): XmlReader = newReader(CharsequenceReader(input))

    public fun newReader(input: String): XmlReader = newReader(StringReader(input))
}
