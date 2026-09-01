/*
 * This file is part of Axiom Editor.
 *
 * Axiom Editor is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Axiom Editor is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Axiom Editor.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsvks.monaco.option

@JvmInline
value class AccessibilitySupport private constructor(val value: Int) {
    companion object {
        /**
         * This should be the browser case where it is not known if a screen reader is attached or no.
         */
        val Unknown = AccessibilitySupport(0)
        val Disabled = AccessibilitySupport(1)
        val Enabled = AccessibilitySupport(2)
    }
}