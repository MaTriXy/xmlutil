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

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSComplexContent
import nl.adaptivity.xmlutil.QName

class ResolvedComplexContent(
    parent: ResolvedComplexType,
    override val rawPart: XSComplexContent,
    schema: ResolvedSchemaLike
) : ResolvedComplexTypeContent(schema) {

    val derivation: ResolvedDerivation by lazy {
        when (val d = rawPart.derivation) {
            is XSComplexContent.XSExtension -> ResolvedComplexExtension(d, parent, schema)
            is XSComplexContent.XSRestriction -> ResolvedComplexRestriction(d, parent, schema)
        }
    }


    override fun check(checkedTypes: MutableSet<QName>, inheritedTypes: SingleLinkedList<QName>) {
        derivation.check(checkedTypes, inheritedTypes)
    }

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {
        derivation.term?.collectConstraints(collector)
    }
}
