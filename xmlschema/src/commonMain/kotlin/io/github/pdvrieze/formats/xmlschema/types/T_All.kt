/*
 * Copyright (c) 2021.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package io.github.pdvrieze.formats.xmlschema.types

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger

/**
 * minOccurs is 0 or 1
 * maxOccurs is 0 or 1
 */
interface T_All: T_ExplicitGroup {
    override val minOccurs: VNonNegativeInteger? // 0 or 1
    override val maxOccurs: T_AllNNI.Value? // 0 or 1

    override val choices: List<Nothing> get() = emptyList()
    override val sequences: List<Nothing> get() = emptyList()
}
