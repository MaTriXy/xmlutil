/*
 * Copyright (c) 2020.
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

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import nl.adaptivity.xmlutil.XmlStreaming
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.xmlunit.builder.Input
import org.xmlunit.diff.Comparison
import org.xmlunit.diff.ComparisonResult
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.dom.DOMSource
import kotlin.test.assertEquals
import org.xmlunit.assertj.XmlAssert

class JvmSerializationTest {

    @Test
    fun `deserialize DOM node from xml`() {
        val contentText = "<tag>some text <b>some bold text<i>some bold italic text</i></b></tag>"
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().newDocument()
        val expectedObj = doc.createElementNS("", "tag").apply {
            appendChild(doc.createTextNode("some text "))
            appendChild(doc.createElementNS("","b").apply {
                appendChild(doc.createTextNode("some bold text"))
                appendChild(doc.createElementNS("", "i").apply {
                    appendChild(doc.createTextNode("some bold italic text"))
                })
            })
        }

        val xml = XML {
            autoPolymorphic = true
        }
        val deserialized = xml.parse(ElementSerializer, contentText)

        try {
            val expected = Input.fromNode(expectedObj)
            val actual = Input.fromNode(deserialized)
//            XmlAssert.
            XmlAssert.assertThat(expected).and(actual).areIdentical()

//            assertEquals(expectedObj, deserialized)
        } catch (e: AssertionError) {
            val expectedTxt = XmlStreaming.toString(DOMSource(expectedObj))
            val actualTxt = XmlStreaming.toString(DOMSource(deserialized))
            assertEquals(expectedTxt, actualTxt)
            throw e // if we reach here, throw anyway
        }
    }

    @Test
    fun `serialize DOM content to xml`() {
        val expected = "<tag>some text <b>some bold text<i>some bold italic text</i></b></tag>"
        val doc = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().newDocument()
        val element = doc.createElement("tag").apply {
            appendChild(doc.createTextNode("some text "))
            appendChild(doc.createElement("b").apply {
                appendChild(doc.createTextNode("some bold text"))
                appendChild(doc.createElement("i").apply {
                    appendChild(doc.createTextNode("some bold italic text"))
                })
            })
        }

        val xml = XML() {
            indentString = ""
            autoPolymorphic = true
        }

        val serialized = xml.stringify(ElementSerializer, element)
        assertEquals(expected, serialized)
    }

    @Test
    fun `serialize DOM content to json`() {
        val expected = "{\"localname\":\"tag\",\"attributes\":{},\"content\":[[\"text\",\"some text \"],[\"element\",{\"localname\":\"b\",\"attributes\":{},\"content\":[[\"text\",\"some bold text\"],[\"element\",{\"localname\":\"i\",\"attributes\":{},\"content\":[[\"text\",\"some bold italic text\"]]}]]}]]}"
        val doc = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().newDocument()
        val element = doc.createElement("tag").apply {
            appendChild(doc.createTextNode("some text "))
            appendChild(doc.createElement("b").apply {
                appendChild(doc.createTextNode("some bold text"))
                appendChild(doc.createElement("i").apply {
                    appendChild(doc.createTextNode("some bold italic text"))
                })
            })
        }

        val json = Json(JsonConfiguration.Stable)

        val serialized = json.stringify(ElementSerializer, element)
        assertEquals(expected, serialized)
    }

    @OptIn(UnstableDefault::class)
    @Test
    fun `deserialize DOM node from json`() {
        val contentText = "{\"localname\":\"tag\",\"attributes\":{},\"content\":[[\"text\",\"some text \"],[\"element\",{\"localname\":\"b\",\"attributes\":{},\"content\":[[\"text\",\"some bold text\"],[\"element\",{\"localname\":\"i\",\"attributes\":{},\"content\":[[\"text\",\"some bold italic text\"]]}]]}]]}"
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().newDocument()
        val expectedObj = doc.createElementNS("", "tag").apply {
            appendChild(doc.createTextNode("some text "))
            appendChild(doc.createElementNS("","b").apply {
                appendChild(doc.createTextNode("some bold text"))
                appendChild(doc.createElementNS("", "i").apply {
                    appendChild(doc.createTextNode("some bold italic text"))
                })
            })
        }

        val json = Json {
            isLenient = true
        }

        val deserialized = json.parse(ElementSerializer, contentText)

        try {
            val expected = Input.fromNode(expectedObj)
            val actual = Input.fromNode(deserialized)
//            XmlAssert.
            XmlAssert.assertThat(expected).and(actual).areIdentical()

//            assertEquals(expectedObj, deserialized)
        } catch (e: AssertionError) {
            val expectedTxt = XmlStreaming.toString(DOMSource(expectedObj))
            val actualTxt = XmlStreaming.toString(DOMSource(deserialized))
            assertEquals(expectedTxt, actualTxt)
            throw e // if we reach here, throw anyway
        }
    }

}
