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

package nl.adaptivity.xmlutil.dom

@Suppress(
    "ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING",
    "NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING"
)
public actual external interface CharacterData: Node {

    public var data: String

    public actual fun substringData(offset: Int, count: Int): String

    public actual fun appendData(data: String)

    public actual fun insertData(offset: Int, data: String)

    public actual fun deleteData(offset: Int, count: Int)

    public actual fun replaceData(offset: Int, count: Int, data: String)
}

@Suppress("NOTHING_TO_INLINE")
public actual inline fun CharacterData.getData(): String = data
@Suppress("NOTHING_TO_INLINE")
public actual inline fun CharacterData.setData(value: String) { data = value }
