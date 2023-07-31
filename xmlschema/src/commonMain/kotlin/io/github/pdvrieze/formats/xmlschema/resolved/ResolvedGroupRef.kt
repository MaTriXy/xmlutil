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

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSGroupRef
import io.github.pdvrieze.formats.xmlschema.model.AnnotationModel
import io.github.pdvrieze.formats.xmlschema.model.GroupRefModel
import io.github.pdvrieze.formats.xmlschema.resolved.particles.ResolvedParticle
import io.github.pdvrieze.formats.xmlschema.types.T_AllNNI
import io.github.pdvrieze.formats.xmlschema.types.T_GroupRef
import nl.adaptivity.xmlutil.QName
import kotlin.reflect.KClass
import kotlin.reflect.cast

class ResolvedGroupRef(
    override val rawPart: XSGroupRef,
    override val schema: ResolvedSchemaLike,
    override val minOccurs: VNonNegativeInteger? = rawPart.minOccurs,
    override val maxOccurs: T_AllNNI? = rawPart.maxOccurs,
) : ResolvedGroupBase, GroupRefModel, T_GroupRef, ResolvedGroupParticle<ResolvedGlobalGroup>,
    ResolvedComplexType.ResolvedDirectParticle<ResolvedGlobalGroup> {
    val referencedGroup: ResolvedGlobalGroup by lazy { schema.modelGroup(rawPart.ref) }

    override val mdlAnnotations: AnnotationModel? get() = rawPart.annotation.models()

    override val mdlTerm: ResolvedGlobalGroup get() = schema.modelGroup(rawPart.ref)

    override val ref: QName get() = rawPart.ref

    override val mdlMinOccurs: VNonNegativeInteger get() = rawPart.minOccurs ?: VNonNegativeInteger.ONE

    override val mdlMaxOccurs: T_AllNNI get() = rawPart.maxOccurs ?: T_AllNNI.ONE


    override fun check(checkedTypes: MutableSet<QName>) {
        super<ResolvedGroupParticle>.check(checkedTypes)
        referencedGroup.check(checkedTypes)
    }

    override fun collectConstraints(collector: MutableList<ResolvedIdentityConstraint>) {}

    override fun normalizeTerm(
        minMultiplier: VNonNegativeInteger,
        maxMultiplier: T_AllNNI
    ): ResolvedParticle<ResolvedGlobalGroup> {
        return ResolvedGroupRef(
            rawPart,
            schema,
            minOccurs?.times(minMultiplier) ?: minMultiplier,
            maxOccurs?.times(maxMultiplier) ?: maxMultiplier,
        )
    }

    fun <T : ResolvedTerm> flattenToModelGroup(targetTerm: KClass<T>): ResolvedParticle<T> {
        val normalizedParticle = referencedGroup.mdlModelGroup.normalize(mdlMinOccurs, mdlMaxOccurs)
        check (targetTerm.isInstance(normalizedParticle.mdlTerm)) {
            "The model group is ${ref} is not valid when expecting $targetTerm"
        }
        @Suppress("UNCHECKED_CAST")
        return normalizedParticle as ResolvedParticle<T>
    }
}
