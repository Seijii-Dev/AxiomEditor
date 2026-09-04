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

package auto.axiom.editor.ui.screens

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import auto.axiom.editor.resources.R

sealed class PluginScreens(
    val route: String,
    val title: String,
    val icon: (@Composable () -> Unit)? = null
) {
    data object Installed : PluginScreens(
        route = "installed",
        title = "Installed",
        icon = {
            Icon(ImageVector.vectorResource(R.drawable.ic_download), contentDescription = null)
        }
    )

    data object Extensions : PluginScreens(
        route = "extensions",
        title = "Extensions"
    )
}