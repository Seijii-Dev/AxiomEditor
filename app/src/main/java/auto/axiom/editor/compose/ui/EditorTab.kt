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

package auto.axiom.editor.compose.ui

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.primaryContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import auto.axiom.editor.ui.screens.editor.EditorViewModel
import kiwi.orbit.compose.icons.Icons
import kiwi.orbit.compose.ui.controls.Icon
import kiwi.orbit.compose.ui.controls.Separator
import kiwi.orbit.compose.ui.controls.Text

@SuppressLint("MaterialDesignInsteadOrbitDesign")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTab(
    files: List<EditorViewModel.OpenedFile>,
    selectedFileIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClose: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onTabReselected: (Int) -> Unit = {},
    onCloseOthers: (Int) -> Unit = {},
    onCloseAll: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedFileIndex,
            modifier = modifier.fillMaxWidth(),
            edgePadding = 4.dp,
            divider = {},
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            indicator = {} // custom indicator via tab background
        ) {
            files.forEachIndexed { index, file ->
                var expanded by remember { mutableStateOf(false) }
                val isSelected = index == selectedFileIndex

                val tabBackground by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    else
                        MaterialTheme.colorScheme.surfaceContainerLow,
                    animationSpec = tween(150),
                    label = "tab_bg"
                )
                val contentAlpha by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    animationSpec = tween(150),
                    label = "tab_content"
                )

                Tab(
                    selected = isSelected,
                    onClick = {
                        if (index == selectedFileIndex) {
                            expanded = true
                            onTabReselected(index)
                        } else {
                            onTabSelected(index)
                        }
                    },
                    modifier = Modifier
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tabBackground)
                        .semantics {
                            contentDescription = "${file.file.name}${if (file.isModified) ", unsaved" else ""}"
                        },
                    content = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                        // Unsaved dot indicator (replaces the "*" prefix)
                        if (file.isModified) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }

                        Text(
                            text = file.file.name,
                            color = contentAlpha,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 13.sp
                            ),
                            maxLines = 1
                        )

                        Box {
                            Icon(
                                Icons.Close,
                                contentDescription = "Close ${file.file.name}",
                                tint = contentAlpha.copy(
                                    alpha = if (isSelected) 0.8f else 0.5f
                                ),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable { onTabClose(index) }
                            )

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Close") },
                                    onClick = {
                                        expanded = false
                                        onTabClose(index)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Close others") },
                                    onClick = {
                                        expanded = false
                                        onCloseOthers(index)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Close all") },
                                    onClick = {
                                        expanded = false
                                        onCloseAll()
                                    }
                                )
                            }
                        }
                    }
                    }
                )
            }
        }
    }

    Separator(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    )
}
