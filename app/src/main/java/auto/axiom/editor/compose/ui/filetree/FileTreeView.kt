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

package auto.axiom.editor.compose.ui.filetree

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import auto.axiom.editor.compose.LocalDarkMode
import auto.axiom.editor.compose.ui.graphics.rememberSvgAssetImageBitmap
import auto.axiom.editor.core.FileIcons

private val indentWidth = 16.dp
private val iconSize = 16.dp
private val rowHeight = 30.dp

@SuppressLint("MaterialDesignInsteadOrbitDesign")
@Composable
fun FileTreeView(
    rootNode: FileTreeNode,
    modifier: Modifier = Modifier,
    onFileClick: (FileTreeNode) -> Unit,
    onFileLongClick: (FileTreeNode) -> Unit = {},
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    FileTreeNodeItem(
                        node = rootNode,
                        depth = 0,
                        onFileClick = onFileClick,
                        onFileLongClick = onFileLongClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileTreeNodeItem(
    node: FileTreeNode,
    depth: Int,
    onFileClick: (FileTreeNode) -> Unit,
    onFileLongClick: (FileTreeNode) -> Unit
) {
    var isExpanded by remember { mutableStateOf(depth == 0) }
    val children = remember { mutableStateListOf<FileTreeNode>() }
    var isHovered by remember { mutableStateOf(false) }

    LaunchedEffect(node) {
        children.clear()
        node.children.collect { childNode ->
            children.add(childNode)
        }
    }

    val hasChildren = node.file.isDirectory && children.isNotEmpty()
    val isLight = LocalDarkMode.current.not()

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chevron_rotation"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .combinedClickable(
                    onClick = {
                        if (node.file.isDirectory) {
                            isExpanded = !isExpanded
                        } else {
                            onFileClick(node)
                        }
                    },
                    onLongClick = { onFileLongClick(node) }
                )
                .clip(RoundedCornerShape(4.dp))
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indent guides — thin vertical lines for each depth level
            repeat(depth) { level ->
                Box(
                    modifier = Modifier
                        .width(indentWidth)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (level == depth - 1) {
                        // Last level: show the guide
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }

            // Chevron or spacer
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasChildren) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(chevronRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else if (node.file.isDirectory) {
                    // Empty directory — still reserve space
                    Spacer(modifier = Modifier.width(14.dp))
                }
            }

            // File/folder icon
            Box(
                modifier = Modifier.size(iconSize),
                contentAlignment = Alignment.Center
            ) {
                if (node.file.isDirectory) {
                    val customIconPath = FileIcons.getSvgIconForFolder(
                        node.file.path,
                        isExpanded = false
                    )
                    if (customIconPath == "files/icons/folder.svg") {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(0xFFFFCA28),
                            modifier = Modifier.size(iconSize)
                        )
                    } else {
                        Image(
                            bitmap = rememberSvgAssetImageBitmap(
                                FileIcons.getSvgIconForFolder(
                                    folderPath = node.file.path,
                                    isExpanded = isExpanded,
                                    isLight = isLight
                                )
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                } else {
                    val customIconPath = FileIcons.getSvgIconForFile(node.file.path)
                    if (customIconPath == "files/icons/file.svg") {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(iconSize)
                        )
                    } else {
                        Image(
                            bitmap = rememberSvgAssetImageBitmap(
                                FileIcons.getSvgIconForFile(
                                    filePath = node.file.path,
                                    isLight = isLight
                                )
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Name
            Text(
                text = node.file.name,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = if (node.file.isDirectory)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            // Loading spinner for directories being loaded
            val isLoading = node.file.isDirectory &&
                !node.file.asRawFile()?.list().isNullOrEmpty() &&
                children.isEmpty()

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        // Children
        AnimatedVisibility(
            visible = isExpanded && hasChildren,
            enter = expandVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = shrinkVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut()
        ) {
            Column {
                children.forEach { childNode ->
                    FileTreeNodeItem(
                        node = childNode,
                        depth = depth + 1,
                        onFileClick = onFileClick,
                        onFileLongClick = onFileLongClick
                    )
                }
            }
        }
    }
}
