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

package com.axiomeditor.plugins.menu

/**
 * Represents a menu item.
 *
 * @property title The title of the menu item.
 * @property id The ID of the menu item.
 * @property shortcut The shortcut for the menu item (optional).
 * @property action The action to perform when the menu item is selected.
 */
data class MenuItem @JvmOverloads constructor(
    val title: String,
    val id: Int,
    val shortcut: String? = null,
    val action: MenuAction
)
