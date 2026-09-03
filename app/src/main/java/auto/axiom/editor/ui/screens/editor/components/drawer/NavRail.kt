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

package auto.axiom.editor.ui.screens.editor.components.drawer

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import auto.axiom.editor.activities.Editor.LocalEditorDrawerNavController
import auto.axiom.editor.resources.R
import auto.axiom.editor.ui.navigateSingleTop
import auto.axiom.editor.ui.screens.EditorDrawerScreens

@Composable
fun NavRail(
    modifier: Modifier = Modifier,
    selectedItemIndex: Int
) {
    val context = LocalContext.current
    val navController = LocalEditorDrawerNavController.current
    val filesLabel = context.getString(R.string.files)

    NavigationRail(
        modifier = modifier.widthIn(max = 60.dp)
    ) {
        NavigationRailItem(
            icon = {
                Icon(
                    imageVector = if (selectedItemIndex == 0) Icons.Rounded.Folder else Icons.Outlined.Folder,
                    contentDescription = filesLabel,
                    modifier = Modifier.size(20.dp),
                )
            },
            label = {
                Text(
                    text = filesLabel,
                    overflow = TextOverflow.Ellipsis
                )
            },
            alwaysShowLabel = true,
            selected = selectedItemIndex == 0,
            onClick = {
                navController.navigateSingleTop(EditorDrawerScreens.FileExplorer)
            }
        )
    }
}
