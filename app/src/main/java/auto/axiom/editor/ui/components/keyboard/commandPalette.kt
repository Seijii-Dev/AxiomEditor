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

package auto.axiom.editor.ui.components.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import auto.axiom.editor.keyboard.CommandPaletteManager
import auto.axiom.editor.keyboard.model.Command
import auto.axiom.editor.ui.extensions.harmonizeWithPrimary

@Composable
fun CommandPalette(
    commands: List<Command>,
    recentlyUsedCommands: List<Command>,
    modifier: Modifier = Modifier,
    onCommandSelected: (Command) -> Unit = {},
    onDismissRequest: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var query by remember { mutableStateOf("") }

    val recentFiltered = remember(query, recentlyUsedCommands) {
        if (query.isBlank()) recentlyUsedCommands.take(5)
        else recentlyUsedCommands.filter {
            it.name.contains(query, ignoreCase = true)
        }
    }

    val allFiltered = remember(query, commands, recentlyUsedCommands) {
        val recents = recentlyUsedCommands.toSet()
        commands
            .filter { it !in recents }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .sortedBy { it.name.lowercase() }
    }

    val hasResults = recentFiltered.isNotEmpty() || allFiltered.isNotEmpty()

    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 12.dp,
            tonalElevation = 4.dp
        ) {
            Column {
                // Search field
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            "Search commands…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val first = recentFiltered.firstOrNull() ?: allFiltered.firstOrNull()
                            if (first != null) {
                                onCommandSelected(first)
                                onDismissRequest()
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )

                // Results list
                LazyColumn(
                    modifier = Modifier.heightIn(max = 340.dp)
                ) {
                    // Recently used section
                    if (recentFiltered.isNotEmpty()) {
                        item {
                            CommandSectionHeader(title = "Recent")
                        }
                        items(recentFiltered) { command ->
                            CommandItem(
                                command = command,
                                isRecent = true,
                                onClick = {
                                    onCommandSelected(command)
                                    onDismissRequest()
                                }
                            )
                        }
                        if (allFiltered.isNotEmpty()) {
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }

                    // All commands section
                    if (allFiltered.isNotEmpty()) {
                        if (recentFiltered.isEmpty() || query.isNotBlank()) {
                            item {
                                CommandSectionHeader(
                                    title = if (query.isNotBlank()) "Commands" else "All commands"
                                )
                            }
                        } else {
                            item {
                                CommandSectionHeader(title = "All commands")
                            }
                        }
                        items(allFiltered) { command ->
                            CommandItem(
                                command = command,
                                isRecent = false,
                                onClick = {
                                    onCommandSelected(command)
                                    onDismissRequest()
                                }
                            )
                        }
                    }

                    // Empty state
                    if (!hasResults && query.isNotBlank()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No commands match \"$query\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Bottom padding
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun CommandSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        letterSpacing = 0.4.sp
    )
}

@Composable
private fun CommandItem(
    command: Command,
    isRecent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRecent) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
        } else {
            Spacer(modifier = Modifier.width(26.dp))
        }

        Text(
            text = command.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        command.keybinding?.let { shortcut ->
            Spacer(modifier = Modifier.width(8.dp))
            ShortcutBadge(shortcut = shortcut)
        }
    }
}

@Composable
private fun ShortcutBadge(shortcut: String) {
    // Split compound shortcuts like "Ctrl+Shift+P" into individual key chips
    val keys = shortcut.split("+", "  ").filter { it.isNotBlank() }

    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)) {
        keys.forEachIndexed { index, key ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            if (index < keys.lastIndex) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}
