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

package nl.adaptivity.xmlutil.core

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.EventType.*
import nl.adaptivity.xmlutil.core.impl.NamespaceHolder
import nl.adaptivity.xmlutil.core.impl.multiplatform.Reader
import nl.adaptivity.xmlutil.core.internal.isNameChar11
import nl.adaptivity.xmlutil.core.internal.isNameCodepoint
import nl.adaptivity.xmlutil.core.internal.isNameStartChar
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

private const val BUF_SIZE = 4096

@ExperimentalXmlUtilApi
/**
 * @param reader Reader for the input
 * @param encoding The encoding to record, note this doesn't impact the actual parsing (that is handled in the reader)
 * @param relaxed If `true` ignore various syntax and namespace errors
 * @param ignorePos If `true` don't record current position (line/offset)
 */
public class KtXmlReader internal constructor(
    private val reader: Reader,
    encoding: String?,
    public val relaxed: Boolean = false,
    ignorePos: Boolean = false
) : XmlReader {

    public constructor(reader: Reader, relaxed: Boolean = false) : this(reader, null, relaxed)

    private var line: Int
    private var column: Int
    private var offset: Int

    init {
        if (ignorePos) {
            line = -1
            column = -1
            offset = -1
        } else {
            line = 1
            column = 0
            offset = 0
        }
    }

    public val ignorePos: Boolean get() = offset < 0

    private var _eventType: EventType? = null //START_DOCUMENT // Already have this state
    public override val eventType: EventType
        get() = _eventType ?: throw IllegalStateException("Not yet started")

    override val isStarted: Boolean
        get() = _eventType != null

    private var entityName: String? = null

    public override val localName: String
        get() = when (_eventType) {
            ENTITY_REF -> entityName ?: throw XmlException("Missing entity name")
            START_ELEMENT, END_ELEMENT -> elementStack[depth - 1].localName ?: throw XmlException("Missing local name")
            else -> throw IllegalStateException("Local name not accessible outside of element tags")
        }

    public override val namespaceURI: String
        get() = when (_eventType) {
            START_ELEMENT, END_ELEMENT -> elementStack[depth - 1].namespace ?: throw XmlException(
                "Missing namespace",
                extLocationInfo
            )

            else -> throw IllegalStateException("Local name not accessible outside of element tags")
        }

    public override val prefix: String
        get() = when (_eventType) {
            START_ELEMENT, END_ELEMENT -> elementStack[depth - 1].prefix ?: throw XmlException(
                "Missing prefix",
                extLocationInfo
            )

            else -> throw IllegalStateException("Local name not accessible outside of element tags")
        }

    private var isSelfClosing = false

    override val attributeCount: Int
        get() = attributes.size

    private var attributes: AttributesCollection = AttributesCollection()

    override var encoding: String? = encoding
        private set

    public override var version: String? = null
        private set

    public override var standalone: Boolean? = null
        private set


    private var srcBufPos: Int = 0
    private var srcBufCount: Int = 0

    //    private val srcBuf = CharArray(8192)
    private var bufLeft = CharArray(BUF_SIZE)
    private var bufRight: CharArray// = CharArray(BUF_SIZE)

    init { // Read the first buffers on creation, rather than delayed
        var cnt = reader.readUntilFullOrEOF(bufLeft)
        require(cnt >= 0) { "Trying to parse an empty file (that is not valid XML)" }
        if (cnt < BUF_SIZE) {
            bufRight = CharArray(0)
            srcBufCount = cnt
        } else {
            val newRight = CharArray(BUF_SIZE)
            bufRight = newRight
            cnt = reader.readUntilFullOrEOF(newRight).coerceAtLeast(0) // in case the EOF is exactly at the boundary
            srcBufCount = BUF_SIZE + cnt
        }

        if (bufLeft[0].code == 0x0feff) {
            srcBufPos = 1 /* drop BOM */
        }
    }


    /**
     * A separate peek buffer seems simpler than managing
     * wrap around in the first level read buffer
     */
    private val peek = IntArray(2)
    private var peekCount = 0

    private var entityMap = HashMap(INITIAL_ENTITY_MAP)

    private val namespaceHolder = NamespaceHolder()

    public override val depth: Int
        get() = namespaceHolder.depth

    private var elementStack: ElementStack = ElementStack()

    /** Target buffer for storing incoming text (including aggregated resolved entities)  */
    private var txtBuf = CharArray(256)

    /** Write position   */
    private var txtBufPos = 0

    private var isWhitespace = false

    //    private int stackMismatch = 0;
    private var error: String? = null

    private var wasCR = false

    private var unresolved = false
    private var token = false
    private var state = State.BEFORE_START

    override val namespaceDecls: List<Namespace>
        get() = namespaceHolder.namespacesAtCurrentDepth

    override val namespaceContext: IterableNamespaceContext
        get() = namespaceHolder.namespaceContext


    override fun close() {
        //NO-Op
    }

    private fun adjustNsp(fullName: String): Boolean {
        var hasActualAttributeWithPrefix = false

        // Loop through all attributes to collect namespace attributes and split name into prefix/localName.
        // Namespaces will not be set yet (as the namespace declaring attribute may be afterwards)
        var attrIdx = 0
        while (attrIdx < (attributes.size)) {
            val attr = attributes[attrIdx]

            var attrName: String? = attr.localName
            val cIndex = attrName!!.indexOf(':')
            var prefix: String
            if (cIndex >= 0) {
                prefix = attrName.substring(0, cIndex)
                attrName = attrName.substring(cIndex + 1)
            } else if (attrName == "xmlns") {
                prefix = attrName
                attrName = null
            } else {
                attr.namespace = XMLConstants.NULL_NS_URI // the namespace for the attribute must be the null namespace
                attr.prefix = XMLConstants.DEFAULT_NS_PREFIX
                attrIdx += 1
                continue
            }
            if (prefix != "xmlns") {
                hasActualAttributeWithPrefix = true
                attrIdx += 1
            } else {
                namespaceHolder.addPrefixToContext(attrName, attributes[attrIdx].value)
                if (attrName != null && attributes[attrIdx].value == "") error("illegal empty namespace")

                //  prefixMap = new PrefixMap (prefixMap, attrName, attr.getValue ());
                attributes.removeAttr(attr)
            }
        }
        if (hasActualAttributeWithPrefix) {
            var i = attributes.size - 1
            while (i >= 0) {
                var attrName = attributes[i].localName!!
                val cIndex = attrName.indexOf(':')
                if (cIndex == 0 && !relaxed) {
                    throw RuntimeException("illegal attribute name: $attrName at $this")
                } else if (cIndex != -1) {
                    val attrPrefix = attrName.substring(0, cIndex)
                    attrName = attrName.substring(cIndex + 1)
                    val attrNs = namespaceHolder.getNamespaceUri(attrPrefix)
                    if (attrNs == null && !relaxed) throw RuntimeException("Undefined Prefix: $attrPrefix in $this")
                    attributes[i].namespace = attrNs
                    attributes[i].prefix = attrPrefix
                    attributes[i].localName = attrName

                }
                i -= 1
            }
        }

        val cIdx = fullName.indexOf(':')
        if (cIdx == 0) error("illegal tag name: $fullName")
        val prefix: String
        val localName: String
        if (cIdx != -1) {
            prefix = fullName.substring(0, cIdx)
            localName = fullName.substring(cIdx + 1)
        } else {
            prefix = ""
            localName = fullName
        }

        val ns = namespaceHolder.getNamespaceUri(prefix) ?: run {
            if (cIdx >= 0) error("undefined prefix: $prefix")
            XMLConstants.NULL_NS_URI
        }
        val d = depth - 1
        elementStack[d].prefix = prefix
        elementStack[d].localName = localName
        elementStack[d].namespace = ns

        return hasActualAttributeWithPrefix
    }

    private fun error(desc: String) {
        if (relaxed) {
            if (error == null) error = "ERR: $desc"
        } else exception(desc)
    }

    private fun exception(desc: String): Nothing {
        throw XmlException(
            when {
                desc.length < 100 -> desc
                else -> "${desc.substring(0, 100)}\n"
            },
            this
        )
    }

    private fun parseUnexpectedOrWS() {
        when (_eventType!!) {
            START_DOCUMENT -> {
                error("Unexpected START_DOCUMENT in state $state")
                parseStartTag(true) // parse it to ignore it
            }
            START_ELEMENT -> {
                error("Unexpected start tag after document body")
                parseStartTag(false)
            }
            END_ELEMENT -> {
                error("Unexpected end tag outside of body")
                parseEndTag()
            }

            ATTRIBUTE,
            IGNORABLE_WHITESPACE,
            COMMENT -> throw UnsupportedOperationException("Comments/WS are always allowed - they may start the document tough")

            TEXT -> {
                pushText('<', !token)
                when {
                    isWhitespace -> _eventType = IGNORABLE_WHITESPACE
                    else -> error("Non-whitespace text where not expected: '${text}'")
                }
            }

            CDSECT -> {
                error("CData sections are not supported outside of the document body")
                parseCData(token)
            }

            DOCDECL -> {
                error("Document declarations are not supported outside the preamble")
                parseDoctype(token)
            }

            END_DOCUMENT -> {
                error("End of document before end of document element")
            }

            ENTITY_REF -> {
                error("Entity reference outside document body")
                pushEntity()
            }

            PROCESSING_INSTRUCTION -> {
                error("Processing instruction inside document body")
                parsePI(token)
            }
        }
    }

    private fun nextImplDocStart() {
        attributes.clear()

        val eventType = peekType()
        if (eventType == START_DOCUMENT) {
            read() // <
            read() // ?
            parseStartTag(true)
            if (attributeCount < 1 || "version" != attributes[0].localName) error("version expected")
            version = attributes[0].value
            var pos = 1
            if (pos < attributeCount && "encoding" == attributes[1].localName) {
                encoding = attributes[1].value
                pos++
            }
            if (pos < attributeCount && "standalone" == attributes[pos].localName) {
                when (val st = attributes[pos].value) {
                    "yes" -> standalone = true
                    "no" -> standalone = false
                    else -> error("illegal standalone value: $st")
                }
                pos++
            }
            if (pos != attributeCount) error("illegal xmldecl")
            isWhitespace = true
            txtBufPos = 0
        } // if it is not a doc start synthesize an event.
        _eventType = START_DOCUMENT
        state = State.START_DOC
        return
    }

    /**
     * common base for next and nextToken. Clears the state, except from
     * txtPos and whitespace. Does not set the type variable  */
    private fun nextImplPreamble() {
        attributes.clear()

        error?.let { e ->
            push(e)

            this.error = null
            _eventType = COMMENT
            return
        }

        _eventType = peekType()
        when (_eventType) {
            PROCESSING_INSTRUCTION -> parsePI(token)

            START_ELEMENT -> {
                state = State.BODY // this must start the body
                parseStartTag(false)
            }

            DOCDECL -> {
                read("DOCTYPE")
                parseDoctype(token)
            }

            COMMENT -> parseComment(token)

            else -> parseUnexpectedOrWS()
        }
    }

    /**
     * common base for next and nextToken. Clears the state, except from
     * txtPos and whitespace. Does not set the type variable  */
    private fun nextImplBody() {
        // Depth is only decreased *after* the end element.
        if (_eventType == END_ELEMENT) namespaceHolder.decDepth()

        // degenerated needs to be handled before error because of possible
        // processor expectations(!)
        if (isSelfClosing) {
            isSelfClosing = false
            _eventType = END_ELEMENT
            if (depth == 1) state = State.POST
            return
        }

        attributes.clear()
        error?.let { e ->
            push(e)

            this.error = null
            _eventType = COMMENT
            return
        }

        //            text = null;
        _eventType = peekType()
        when (_eventType) {

            COMMENT -> parseComment(token)

            ENTITY_REF -> pushEntity()

            START_ELEMENT -> parseStartTag(false)

            END_ELEMENT -> {
                parseEndTag()
                if (depth == 1) state = State.POST
            }

            TEXT -> {
                pushText('<', !token)
                if (isWhitespace) _eventType = IGNORABLE_WHITESPACE
            }

            CDSECT -> parseCData(token)

            else -> parseUnexpectedOrWS()

        }
    }

    /**
     * Parse only the post part of the document. *misc* = Comment | PI | S
     */
    private fun nextImplPost() {
        if (_eventType == END_ELEMENT) namespaceHolder.decDepth()

        attributes.clear()

        // degenerated needs to be handled before error because of possible
        // processor expectations(!)
        if (isSelfClosing) {
            isSelfClosing = false
            _eventType = END_ELEMENT
            return
        }
        error?.let { e ->
            push(e)

            this.error = null
            _eventType = COMMENT
            return
        }

        //            text = null;
        _eventType = peekType()
        when (_eventType) {
            PROCESSING_INSTRUCTION -> parsePI(token)

            COMMENT -> parseComment(token)

            END_DOCUMENT -> {
                state = State.EOF
                return
            }

            else -> parseUnexpectedOrWS()
        }
    }

    private fun readTagContentUntil(delim: Char, push: Boolean) {
        var c = read()
        while (c >= 0) {
            if (c == delim.code && peek(0) == '>'.code) {
                read()
                return
            }
            if (push) pushChar(c.toChar())
            c = read()
        }
        error(UNEXPECTED_EOF)
    }

    private fun parsePI(push: Boolean) {
        read() // <
        read() // '?'
        readTagContentUntil('?', push)
    }

    private fun parseComment(push: Boolean) {
        read() // <
        read() // '!'
        read() // '-
        read('-')

        var c = read()
        while (c >= 0) {
            if (c == '-'.code && peek(0) == '-'.code && (!relaxed || peek(1) == '>'.code)) {
                if (peek(1) != '>'.code) {
                    exception("illegal comment delimiter: --->")
                } else {
                    read()
                    read()
                    return
                }
            }
            if (push) pushChar(c.toChar())
            c = read()
        }
        error(UNEXPECTED_EOF)
    }

    private fun parseCData(push: Boolean) {
        read() // <
        read() // '['
        read("[CDATA[")

        var c = read()
        while (c >= 0) {
            if (c == ']'.code && peek(0) ==']'.code && peek(1) == '>'.code) {
                read()
                read()
                return
            }
            if (push) pushChar(c.toChar())
            c = read()
        }
        error(UNEXPECTED_EOF)
    }

    /** precondition: &lt! consumed  */
    private fun parseDoctype(push: Boolean) {
        var nesting = 1
        var quote: Char? = null

        // read();
        while (true) {
            val i = read()
            when (i) {
                '\''.code,
                '"'.code -> when (quote) {
                    null -> quote = i.toChar()
                    i.toChar() -> quote = null
                }

                '-'.code -> if (quote == '!') {
                    if (push) pushChar('-')

                    var c = read()
                    if (push) pushChar(c)
                    if (c != '-'.code) continue

                    c = read()
                    if (push) pushChar(c)
                    if (c != '>'.code) continue

                    quote = null
                }

                '['.code -> if (quote == null && nesting == 1) ++nesting

                ']'.code -> if (quote == null) {
                    if (push) pushChar(']')
                    val c = read()
                    if (push) pushChar(i)
                    if (c != '>'.code) continue
                    if (nesting != 2) error("Invalid nesting of document type declaration: $nesting")
                    return
                }

                '<'.code -> if (quote == null) {
                    if (nesting < 2) error("Doctype with internal subset must have an opening '['")

                    if (push) pushChar('<')
                    var c = read()
                    if (push) pushChar(c)
                    if (c != '!'.code) {
                        nesting++; continue
                    }

                    c = read()
                    if (push) pushChar(c)
                    if (c != '-'.code) {
                        nesting++; continue
                    }

                    c = read()
                    if (push) pushChar(c)
                    if (c != '-'.code) {
                        nesting++; continue
                    }
                    quote = '!' // marker for comment
                }

                '>'.code -> if (quote == null) {
                    when (--nesting) {
                        1 -> error("Missing closing ']' for doctype")
                        0 -> return
                    }
                }
            }
            if (push) pushChar(i)
        }
    }

    /* precondition: &lt;/ consumed */

    /* precondition: &lt;/ consumed */
    private fun parseEndTag() {
        read() // '<'
        read() // '/'
        val fullName = readName()
        skip()
        read('>')
        val spIdx = depth - 1
        if (depth == 0) {
            error("element stack empty")
            _eventType = COMMENT
            return
        }
        if (!relaxed) {
            val expectedPrefix = elementStack[spIdx].prefix ?: exception("Missing prefix")
            val expectedLocalName = elementStack[spIdx].localName ?: exception("Missing localname")
            val expectedFullname = when {
                expectedPrefix.isEmpty() -> expectedLocalName
                else -> "$expectedPrefix:$expectedLocalName"
            }

            if (fullName != expectedFullname) {
                error("expected: /${elementStack[spIdx].fullName} read: $fullName")
            }
        }
    }

    private fun peekType(): EventType {
        return when (peek(0)) {
            -1 -> END_DOCUMENT
            '&'.code -> ENTITY_REF
            '<'.code -> when (peek(1)) {
                '/'.code -> END_ELEMENT
                '?'.code -> when {
                    // order backwards to ensure
                    peek(2) == 'x'.code && peek(3) == 'm'.code &&
                            peek(4) == 'l'.code && !isNameCodepoint(peek(5)) ->
                        START_DOCUMENT

                    else -> PROCESSING_INSTRUCTION
                }

                '!'.code -> when(peek(2)) {
                    '-'.code -> COMMENT
                    '['.code -> CDSECT
                    else -> DOCDECL
                }
                else -> START_ELEMENT
            }

            else -> TEXT
        }
    }

    private operator fun get(pos: Int): String {
        return txtBuf.concatToString(pos, pos + (txtBufPos - pos))
    }

    private fun pushRange(buffer: CharArray, start: Int, endExcl: Int) {
        val count = endExcl - start
        val minSizeNeeded = txtBufPos + count
        if (minSizeNeeded + 1 >= txtBuf.size) { // +1 to have enough space for 2 surrogates, if needed
            txtBuf = txtBuf.copyOf((minSizeNeeded * 5) / 3 + 4)
        }

        buffer.copyInto(txtBuf, txtBufPos, start, endExcl)
        txtBufPos += count
    }

    private fun push(s: String) {
        val minSizeNeeded = txtBufPos + s.length
        if (minSizeNeeded >= txtBuf.size) { // +1 to have enough space for 2 surrogates, if needed
            txtBuf = txtBuf.copyOf((minSizeNeeded * 5) / 3 + 4)
        }
        for (c in s) {
            txtBuf[txtBufPos++] = c
        }
    }

    private fun pushChar(c: Char) {
        if (txtBufPos + 1 >= txtBuf.size) { // +1 to have enough space for 2 surrogates, if needed
            txtBuf = txtBuf.copyOf((txtBufPos * 5) / 3 + 4)
        }
        txtBuf[txtBufPos++] = c
    }

    private fun pushChar(cp: Int) {
        if (cp < 0) {
            error(UNEXPECTED_EOF)
        } else {
            pushChar(cp.toChar())
        }
    }

    private fun pushCodePoint(c: Int) {
        if (c < 0) error("UNEXPECTED EOF")

        if (txtBufPos + 1 >= txtBuf.size) { // +1 to have enough space for 2 surrogates, if needed
            txtBuf = txtBuf.copyOf((txtBufPos * 5) / 3 + 4)
        }
        if (c > 0xffff) { // This comparison works as surrogates are in the 0xd800-0xdfff range
            // write high Unicode value as surrogate pair
            val offset = c - 0x010000
            txtBuf[txtBufPos++] = ((offset ushr 10) + 0xd800).toChar() // high surrogate
            txtBuf[txtBufPos++] = ((offset and 0x3ff) + 0xdc00).toChar() // low surrogate
        } else {
            txtBuf[txtBufPos++] = c.toChar()
        }
    }

    /** Sets name and attributes  */
    private fun parseStartTag(xmldecl: Boolean) {
        read() // < or ?
        val fullName = readName()
        attributes.clear(0)
        while (true) {
            skip()
            when (val c = peek(0)) {
                '?'.code -> {
                    if (!xmldecl) error("? found outside of xml declaration")
                    read()
                    read('>')
                    return
                }

                '/'.code -> {
                    if (xmldecl) error("/ found to close xml declaration")
                    isSelfClosing = true
                    read()
                    if (relaxed) {
                        error = "ERR: Whitespace between empty content tag closing elements"
                        while (isXmlWhitespace(peek(0).toChar())) read()
                    }
                    read('>')
                    break
                }

                '>'.code -> {
                    if (xmldecl) error("xml declaration must be closed by '?>', not '>'")
                    read()
                    break
                }

                -1 -> {
                    error(UNEXPECTED_EOF)
                    return
                }

                ' '.code, '\t'.code, '\n'.code, '\r'.code -> {
                    next() // ignore whitespace
                }

                else -> when {
                    isNameStartChar(c.toChar()) -> {
                        val attrName = readName()

                        if (attrName.isEmpty()) {
                            error("attr name expected")
                            //type = COMMENT;
                            break
                        }
                        skip()
                        if (peek(0) != '='.code) {
                            if (!relaxed) {
                                error("Attr.value missing '='. $attrName, found: ${peek(0).toChar()}")
                            }
                            attributes.addNoNS(attrName, attrName)
                        } else {
                            read('=')
                            skip()
                            val delimiter = peek(0)
                            val p = txtBufPos
                            when (delimiter) {
                                '\''.code, '"'.code -> {
                                    read()
                                    // This is an attribute, we don't care about whitespace content
                                    pushNonWSText(delimiter.toChar(), true)
                                    read()
                                }

                                else -> {
                                    if (!relaxed) error("attr value delimiter missing!")
                                    pushWSDelimAttrValue(true)
                                }
                            }


                            attributes.addNoNS(attrName, get(p))

                            txtBufPos = p
                        }

                    }

                    else -> {
                        error("unexpected character in tag($fullName): '${c.toChar()}'")
                        read()
                    }
                }
            }

        }

        val d = depth
        namespaceHolder.incDepth()
        elementStack.ensureCapacity(depth)

        elementStack[d].fullName = fullName

        if (PROCESS_NAMESPACES) {
            adjustNsp(fullName)
        } else {
            elementStack[d].namespace = ""
            elementStack[d].prefix = ""
            elementStack[d].localName = fullName
        }
    }

    /**
     * result: if the setName parameter is set,
     * the name of the entity is stored in "name"
     */
    private fun pushEntity() {
        pushChar(read()) // &
        val pos = txtBufPos
        val first = peek(0)
        if (first < 0) error(UNEXPECTED_EOF)
        if (first == '#'.code) {
            pushChar(read().toChar())
            when (val c = peek(0)) {
                -1 -> {}// do nothing
                'x'.code, // hex char refs
                in '0'.code..'9'.code -> pushChar(read().toChar())

                else -> {
                    error("Unexpected start of numeric entity reference ${c.toChar()} (in ${get(0)})")
                }
            }
            while (true) {
                when (val c = peek(0)) {
                    -1 -> error(UNEXPECTED_EOF)
                    ';'.code -> {
                        read()
                        break
                    }

                    in 'a'.code..'f'.code, // allow hex
                    in 'A'.code..'F'.code, // allow hex
                    in '0'.code..'9'.code -> pushChar(read().toChar())

                    else -> {
                        error("Unexpected content in numeric entity reference: ${c.toChar()} (in ${get(0)}")
                        break
                    }
                }
            }
        } else {
            if (!isNameStartChar(first.toChar())) {
                if (relaxed || first < 0) {
                    println("broken entity ${get(pos - 1)}")
                    return
                }
                error("Entity reference does not start with name char ${get(pos - 1)}${first.toChar()}")
            }
            pushChar(read().toChar()) // no need to further processing in the while loop

            while (true) {
                val c = peek(0)
                if (c == ';'.code) {
                    read()
                    break
                }
                if (!isNameChar11(c.toChar())) {
                    if (!relaxed) {
                        error("unterminated entity ref")
                    }
                    println("broken entitiy: " + get(pos - 1))

                    return
                }
                pushChar(read().toChar())
            }

        }

        val code = get(pos)
        txtBufPos = pos - 1 // reset the output buffer to before the entity.
        if (token && _eventType == ENTITY_REF) {
            entityName = code
        }
        if (code[0] == '#') {
            val c = if (code[1] == 'x') code.substring(2).toInt(16) else code.substring(1).toInt()
            pushCodePoint(c)
            return
        }
        val result = entityMap[code]
        unresolved = result == null
        if (result == null) {
            if (!token) error("unresolved: &$code;")
        } else {
            push(result)
        }
    }

    /**
     * General text push/parse algorithm.
     * Content:
     * '<': parse to any token (for nextToken ())
     * ']': CDATA section
     * Attributes:
     * '"': parse to quote
     * NO LONGER SUPPORTED - use pushTextWsDelim ' ': parse to whitespace or '>'
     *
     * @param resolveEntities `true` if entities should be resolved inline, `false` if entity is a start of
     */
    private fun pushText(delimiter: Char, resolveEntities: Boolean) {
//        if (delimiter == ' ') return pushTextWsDelim(resolveEntities)

        var bufCount = srcBufCount
        var innerLoopEnd = minOf(bufCount, BUF_SIZE)
        var curPos = srcBufPos


        while (curPos < innerLoopEnd) {
            when (bufLeft[curPos]) {
                ' ', '\t', '\n', '\r' -> break // whitespace
                '\u0000' -> ++curPos
                else -> {
                    return pushNonWSText(delimiter, resolveEntities)
                }
            }
        }

        var left: Int = curPos
        var right: Int = -1
        var notFinished = true

        outer@ while (curPos < bufCount && notFinished) { // loop through all buffer iterations
            var continueInNonWSMode = false
            inner@ while (curPos < innerLoopEnd) {
                when (val nextChar = bufLeft[curPos]) {
                    ' ', '\t', '\r', '\n' -> {
                        ++curPos
                    }

                    delimiter -> {
                        notFinished = false
                        right = curPos
                        break@inner // outer will actually give the result.
                    }

                    else -> {
                        continueInNonWSMode = true
                        right = curPos
                        break@inner
                    }
                }
            }
            if (curPos == innerLoopEnd) {
                right = curPos
            }
            if (right > 0) {
                pushRange(bufLeft, left, right) // ws delimited is never WS
                right = -1
            }

            if (curPos == BUF_SIZE) { // swap the buffers
                srcBufPos = curPos
                swapBuffer()
                curPos = srcBufPos
                bufCount = srcBufCount
                innerLoopEnd = minOf(bufCount, BUF_SIZE)
            }
            if (continueInNonWSMode) {
                srcBufPos = curPos
                return pushNonWSText(delimiter, resolveEntities)
            }
            left = curPos

        }
        // We didn't return through pushNonWSText, so it is WS
        isWhitespace = true
        srcBufPos = curPos
    }

    private fun pushNonWSText(delimiter: Char, resolveEntities: Boolean) {
        var bufCount = srcBufCount
        var innerLoopEnd = minOf(bufCount, BUF_SIZE)
        var curPos = srcBufPos

        var left: Int = curPos
        var right: Int = -1
        var cbrCount = 0
        var notFinished = true

        outer@ while (curPos < bufCount && notFinished) { // loop through all buffer iterations
            inner@ while (curPos < innerLoopEnd) {
                when (bufLeft[curPos]) {
                    delimiter -> {
                        notFinished = false
                        right = curPos
                        break@inner // outer will actually give the result.
                    }

                    ' ', '\t', '\r', '\n' -> ++curPos
                    '&' -> {
                        if (resolveEntities) {
                            if (left == curPos) { // start with entity
                                srcBufPos = curPos
                                pushEntity()
                                curPos = srcBufPos
                                left = curPos
                            } else { // read all items before entity (then after it will hit the other case)
                                right = curPos
                                break@inner
                            }
                        } else {
                            right = curPos
                            notFinished = false
                            break@inner
                        }
                    }

                    ']' -> {
                        ++cbrCount
                        ++curPos
                    }

                    '>' -> {
                        if (cbrCount >= 2) error("Illegal ]]>")
                        ++curPos
                    }

                    else -> {
                        ++curPos
                    }
                }
            }
            if (curPos == innerLoopEnd) {
                right = curPos
            }
            if (right > 0) {
                pushRange(bufLeft, left, right) // ws delimited is never WS
                right = -1
            }

            if (curPos == BUF_SIZE) { // swap the buffers
                srcBufPos = curPos
                swapBuffer()
                curPos = srcBufPos
                bufCount = srcBufCount
                innerLoopEnd = minOf(bufCount, BUF_SIZE)
            }
            left = curPos

        }
        isWhitespace = false
        srcBufPos = curPos
    }

    /** Push attribute delimited by whitespace */
    private fun pushWSDelimAttrValue(resolveEntities: Boolean) {
        var bufCount = srcBufCount
        var leftEnd = minOf(bufCount, BUF_SIZE)
        var left: Int
        var right: Int
        var curPos = srcBufPos
        var notFinished = true

        outer@ while (curPos < bufCount && notFinished) { // loop through all buffer iterations
            left = curPos
            right = -1

            inner@ while (curPos < leftEnd) {
                when (/*val nextChar =*/ bufLeft[curPos]) {
                    ' ', '\t', '\r', '\n', '>' -> {
                        right = curPos
                        ++curPos
                        notFinished = false
                        break@inner
                    }

                    '&' -> {
                        if (resolveEntities) {
                            if (left == curPos) { // start with entity
                                pushEntity()
                                curPos = srcBufPos
                            } else { // read all items before entity (then after it will hit the other case)
                                right = curPos
                                break@inner
                            }
                        } else ++curPos
                    }

                    else -> ++curPos
                }
            }
            if (right > 0) {
                pushRange(bufLeft, left, right) // ws delimited is never WS
            }

            if (curPos == BUF_SIZE) { // swap the buffers
                srcBufPos = curPos
                swapBuffer()
                curPos = srcBufPos
                bufCount = srcBufCount
                leftEnd = minOf(bufCount, BUF_SIZE)
            }
        }
        srcBufPos = curPos
    }

    private fun read(s: String) {
        for (c in s) {
            val d = read()
            if (c.code != d) error("Found unexpected character '$d' while parsing '$s'")
        }
    }

    private fun read(c: Char) {
        val a = read()
        if (a != c.code) error("expected: '" + c + "' actual: '" + a.toChar() + "'")
    }

    private fun read(): Int {
        val pos = srcBufPos
        if (pos >= srcBufCount) return -1
        if (pos + 2 >= BUF_SIZE) return readAcross()

        val next = pos + 1
        when (val ch = bufLeft[pos]) {
            '\u0000' -> { // should not happen at end of file (or really generally at all)
                srcBufPos = next + 1
                return bufLeft[next].code
            }

            '\r' -> {
                bufLeft[srcBufPos] = '\n'
                if (next < srcBufCount && bufLeft[next] == '\n') {
                    bufLeft[next] = '\u0000'
                    srcBufPos = next + 1
                } else {
                    srcBufPos = next
                }
                if (!ignorePos) {
                    line += 1
                    column = 0
                }
                return '\n'.code
            }

            '\n' -> {
                srcBufPos = next
                if (!ignorePos) {
                    line += 1
                    column = 0
                }
                return '\n'.code
            }

            else -> {
                srcBufPos = next
                return ch.code
            }
        }
    }

    private fun swapBuffer() {
        val oldLeft = bufLeft
        bufLeft = bufRight
        bufRight = oldLeft
        srcBufPos -= BUF_SIZE
        val rightBufCount = srcBufCount - BUF_SIZE
        if (rightBufCount >= BUF_SIZE) {
            val newRead = reader.readUntilFullOrEOF(bufRight)
            srcBufCount = when {
                newRead < 0 -> rightBufCount
                else -> rightBufCount + newRead
            }
        } else {
            srcBufCount = rightBufCount
        }
    }

    private fun readAcross(): Int {
        var pos = srcBufPos
        if (pos >= BUF_SIZE) {
            swapBuffer()
            pos -= BUF_SIZE
        }

        val next = pos + 1
        when (val ch = bufLeft[pos]) {
            '\u0000' -> { // should not happen at end of file (or really generally at all)
                srcBufPos = next
                return readAcross() // just recurse
            }

            '\r' -> {
                bufLeft[srcBufPos] = '\n'
                if (next < srcBufCount && getBuf(next) == '\n') {
                    setBuf(next, '\u0000')
                    srcBufPos = next + 1
                } else {
                    srcBufPos = next
                }
                if (!ignorePos) {
                    line += 1
                    column = 0
                }
                return '\n'.code
            }

            '\n' -> {
                srcBufPos = next
                if (!ignorePos) {
                    line += 1
                    column = 0
                }
                return '\n'.code
            }

            else -> {
                srcBufPos = next
                return ch.code
            }
        }
    }

    /** Does never read more than needed  */
    private fun peek(pos: Int): Int {
        // In this case we *may* need the right buffer, otherwise not
        // optimize this implementation for the "happy" path
        if (srcBufPos + (pos shl 2 + 1) >= BUF_SIZE) return peekAcross(pos)
        var current = srcBufPos
        var peekCount = pos

        while (current < srcBufCount) {
            var chr: Char = bufLeft[current]
            when (chr) {
                '\u0000' -> ++current
                '\r' -> {
                    chr = '\n' // update the char
                    bufLeft[current] = '\n' // replace it with LF (\n)
                    if (bufLeft[current + 1] == '\r') {
                        // Note also as we are separated from the edge of the buffer setting this is valid even
                        // beyond the end of the file
                        bufLeft[current++] = '\u0000' // 0 is not a valid XML CHAR, so we can skip it
                    }
                }

                else -> ++current
            }
            if (peekCount-- == 0) return chr.code
        }
        return -1
    }

    /**
     * Pessimistic implementation of peek that allows checks across into the "right" buffer
     */
    private fun peekAcross(pos: Int): Int {
        var current = srcBufPos
        var peekCount = pos

        while (current < srcBufCount) {
            var chr: Char = getBuf(current)
            when (chr) {
                '\u0000' -> ++current
                '\r' -> {
                    chr = '\n' // update the char
                    setBuf(current, '\n') // replace it with LF (\n)
                    if (current + 1 < srcBufCount && getBuf(current + 1) == '\r') {
                        setBuf(current++, '\u0000') // 0 is not a valid XML CHAR, so we can skip it
                    }
                }

                else -> ++current
            }
            if (peekCount-- == 0) return chr.code
        }
        return -1
    }

    private fun getBuf(pos: Int): Char {
        val split = pos - BUF_SIZE
        return when {
            split < 0 -> bufLeft[pos]
            else -> bufRight[split]
        }
    }

    private fun setBuf(pos: Int, value: Char) {
        val split = pos - BUF_SIZE
        when {
            split < 0 -> bufLeft[pos] = value
            else -> bufRight[split] = value
        }
    }

    private fun readName(): String {
        val pos = txtBufPos
        var c = peek(0)
        if (c < 0 || !isNameStartChar(c.toChar())) error("name expected, found: ${c.toChar()}")
        do {
            pushChar(read().toChar())
            c = peek(0)
        } while (c >= 0 && isNameChar11(c.toChar()))
        val result = get(pos)
        txtBufPos = pos
        return result
    }

    private fun skip() {
        while (true) {
            val c = peek(0)
            if (c > ' '.code || c == -1) break // More sane
            read()
        }
    }


    override fun getNamespacePrefix(namespaceUri: String): String? {
        return namespaceHolder.getPrefix(namespaceUri)
    }

    override fun getNamespaceURI(prefix: String): String? {
        return namespaceHolder.getNamespaceUri(prefix)
    }

    private fun getPositionDescription(): String {
        val et = this._eventType ?: return ("<!--Parsing not started yet-->")

        val buf = StringBuilder(et.name)
        buf.append(' ')
        when {
            et == START_ELEMENT || et == END_ELEMENT -> {
                if (isSelfClosing) buf.append("(empty) ")
                buf.append('<')
                if (et == END_ELEMENT) buf.append('/')
                if (elementStack[depth].prefix != null) buf.append("{$namespaceURI}$prefix:")
                buf.append(name)

                for (x in 0 until attributes.size) {
                    buf.append(' ')
                    val a = attributes[x]
                    if (a.namespace != null) {
                        buf.append('{').append(a.namespace).append('}').append(a.prefix).append(':')
                    }
                    buf.append("${a.localName}='${a.value}'")
                }

                buf.append('>')
            }

            et == IGNORABLE_WHITESPACE -> {}
            et != TEXT -> buf.append(text)
            isWhitespace -> buf.append(
                "(whitespace)"
            )

            else -> {
                var textCpy = text
                if (textCpy.length > 16) textCpy = textCpy.substring(0, 16) + "..."
                buf.append(textCpy)
            }
        }
        if (offset >= 0) {
            buf.append("@$line:$column in ")
        }
        buf.append(reader.toString())
        return buf.toString()
    }

    override fun toString(): String {
        return "KtXmlReader [${getPositionDescription()}]"
    }

    @Deprecated(
        "Use extLocationInfo as that allows more detailed information",
        replaceWith = ReplaceWith("extLocationInfo?.toString()")
    )
    override val locationInfo: String
        get() = if (offset >= 0) "$line:$column" else "<unknown>"

    override val extLocationInfo: XmlReader.LocationInfo
        get() = XmlReader.ExtLocationInfo(col = column, line = line, offset = offset)

    public fun getLineNumber(): Int {
        return line
    }

    public fun getColumnNumber(): Int {
        return column
    }

    override fun isWhitespace(): Boolean {
        val et = eventType
        if (et != TEXT && et != IGNORABLE_WHITESPACE && et != CDSECT) exception(ILLEGAL_TYPE)
        return isWhitespace
    }

    override val text: String
        get() = when {
            eventType.isTextElement -> get(0)
            else -> throw XmlException("The element is not text, it is: $eventType")
        }

    override val piTarget: String
        get() {
            check(eventType == PROCESSING_INSTRUCTION)
            return get(0).substringBefore(' ')
        }

    override val piData: String
        get() {
            check(eventType == PROCESSING_INSTRUCTION)
            return get(0).substringAfter(' ', "")
        }

    public fun isEmptyElementTag(): Boolean {
        if (_eventType != START_ELEMENT) exception(ILLEGAL_TYPE)
        return isSelfClosing
    }

    override fun getAttributeNamespace(index: Int): String {
        return attributes[index].namespace!!
    }

    override fun getAttributeLocalName(index: Int): String {
        return attributes[index].localName!!
    }

    override fun getAttributePrefix(index: Int): String {
        return attributes[index].prefix!!
    }

    override fun getAttributeValue(index: Int): String {
        return attributes[index].value!!
    }

    override fun getAttributeValue(nsUri: String?, localName: String): String? {
        for (attrIdx in 0 until attributes.size) {
            val attr = attributes[attrIdx]
            if (attr.localName == localName && (nsUri == null || attr.namespace == nsUri)) {
                return attr.value
            }
        }
        return null
    }

    override fun next(): EventType {
        isWhitespace = true
        txtBufPos = 0
        token = true
        when (state) {
            State.BEFORE_START -> nextImplDocStart()

            State.START_DOC,
            State.DOCTYPE_DECL -> nextImplPreamble()

            State.BODY -> nextImplBody()
            State.POST -> nextImplPost()
            State.EOF -> error("Reading past end of file")
        }
        return eventType
    }

    override fun hasNext(): Boolean {
        return _eventType != END_DOCUMENT
    }

    override fun nextTag(): EventType {
        do {
            next()
        } while (_eventType?.isIgnorable == true || (_eventType == TEXT && isWhitespace))

        if (_eventType != END_ELEMENT && _eventType != START_ELEMENT) exception("unexpected type")
        return eventType
    }

    override fun require(type: EventType, namespace: String?, name: String?) {
        if (type != this._eventType || (namespace != null && namespace != elementStack[depth - 1].namespace)
            || (name != null && name != elementStack[depth - 1].localName)
        ) {
            exception("expected: $type {$namespace}$name, found: $_eventType {$namespaceURI}$localName")
        }
    }

    private companion object {
        const val UNEXPECTED_EOF = "Unexpected EOF"
        const val ILLEGAL_TYPE = "Wrong event type"

        const val PROCESS_NAMESPACES = true

        val INITIAL_ENTITY_MAP = HashMap<String, String>(8).also {
            it["amp"] = "&"
            it["apos"] = "'"
            it["gt"] = ">"
            it["lt"] = "<"
            it["quot"] = "\""
        }

        @JvmStatic
        private fun Reader.readUntilFullOrEOF(buffer: CharArray): Int {
            val bufSize = buffer.size
            var totalRead: Int = read(buffer, 0, bufSize)
            if (totalRead < 0) return -1
            while (totalRead < bufSize) {
                val lastRead = read(buffer, totalRead, bufSize - totalRead)
                if (lastRead < 0) return totalRead
                totalRead += lastRead
            }
            return totalRead
        }
    }

    private class ElementStack {
        var data: Array<String?> = arrayOfNulls(16)
            private set

        operator fun get(idx: Int) = ElementDelegate(idx)

        fun ensureCapacity(required: Int) {
            val requiredCapacity = required * 4
            if (data.size >= requiredCapacity) return

            data = data.copyOf(requiredCapacity + 16)
        }

    }

    @JvmInline
    private value class ElementDelegate(val index: Int)

    private var ElementDelegate.namespace: String?
        get() {
            if (index !in 0..depth) throw IndexOutOfBoundsException()
            return elementStack.data[index * 4]
        }
        set(value) {
            elementStack.data[index * 4] = value
        }

    private var ElementDelegate.prefix: String?
        get() {
            if (index !in 0..depth) throw IndexOutOfBoundsException()
            return elementStack.data[index * 4 + 1]
        }
        set(value) {
            elementStack.data[index * 4 + 1] = value
        }

    private var ElementDelegate.localName: String?
        get() {
            if (index !in 0..depth) throw IndexOutOfBoundsException()
            return elementStack.data[index * 4 + 2]
        }
        set(value) {
            elementStack.data[index * 4 + 2] = value
        }

    private var ElementDelegate.fullName: String?
        get() {
            if (index !in 0..depth) throw IndexOutOfBoundsException()
            return elementStack.data[index * 4 + 3]
        }
        set(value) {
            elementStack.data[index * 4 + 3] = value
        }

    private class AttributesCollection {
        var data: Array<String?> = arrayOfNulls(16)
            private set

        var size: Int = 0
            private set

        operator fun get(index: Int): AttributeDelegate = AttributeDelegate(index)

        fun removeAttr(attr: AttributeDelegate) {
            data.copyInto(data, attr.index * 4, attr.index * 4 + 4, ((size--) * 4))
            data.fill(null, size * 4, size * 4 + 4)
        }

        fun clear(newSize: Int = -1) {
            if (size > 0) {
                data.fill(null, 0, size * 4)
            }
            size = newSize
        }

        fun ensureCapacity(required: Int) {
            val requiredSize = required * 4
            if (data.size >= requiredSize) return

            data = data.copyOf(requiredSize + 16)
        }

        fun addNoNS(attrName: String, attrValue: String) {
            size = if (size < 0) 1 else size + 1

            ensureCapacity(size)
            var i = size * 4 - 4

            data[i++] = ""
            data[i++] = null
            data[i++] = attrName
            data[i] = attrValue
        }
    }

    @JvmInline
    private value class AttributeDelegate(val index: Int)

    private var AttributeDelegate.namespace: String?
        get() {
            if (index !in 0..attributes.size) throw IndexOutOfBoundsException()
            return attributes.data[index * 4]
        }
        set(value) {
            attributes.data[index * 4] = value
        }

    private var AttributeDelegate.prefix: String?
        get() {
            if (index !in 0..attributes.size) throw IndexOutOfBoundsException()
            return attributes.data[index * 4 + 1]
        }
        set(value) {
            attributes.data[index * 4 + 1] = value
        }

    private var AttributeDelegate.localName: String?
        get() {
            if (index !in 0..attributes.size) throw IndexOutOfBoundsException()
            return attributes.data[index * 4 + 2]
        }
        set(value) {
            attributes.data[index * 4 + 2] = value
        }

    private var AttributeDelegate.value: String?
        get() {
            if (index !in 0..attributes.size) throw IndexOutOfBoundsException()
            return attributes.data[index * 4 + 3]
        }
        set(value) {
            attributes.data[index * 4 + 3] = value
        }


    private enum class State {
        /** Parsing hasn't started yet */
        BEFORE_START,

        /** At or past parsing the xml header */
        START_DOC,

        /** At or past parsing the document type definition */
        DOCTYPE_DECL,

        /** Parsing the main document element */
        BODY,

        /** At end of main document element end tag, or after it*/
        POST,

        /** At end of file */
        EOF
    }

}
