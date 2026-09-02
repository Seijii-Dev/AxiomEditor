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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
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

private val INDENT_WIDTH = 16.dp
private val ICON_SIZE   = 16.dp
private val ROW_HEIGHT  = 32.dp

/**
 * File tree widget.
 *
 * ## Layout fix
 *
 * The original implementation placed the entire recursive composable tree inside a single
 * `LazyColumn { item { … } }` block wrapped by `horizontalScroll`. That combination is broken:
 *
 * 1. `LazyColumn` gives its single `item` **unbounded height**, so `fillMaxSize()` on inner
 *    boxes collapses to zero — the indent guide boxes disappeared.
 * 2. `horizontalScroll` gives the `Row`s **unbounded width**, which means `Modifier.weight(1f)`
 *    on the `Text` has nothing to divide — it collapses to zero width and the text is invisible.
 *
 * Fix: use a plain `Column` inside a `verticalScroll` + `horizontalScroll` box. The column has
 * `widthIn(min = …)` so rows always have a concrete lower-bound width. The `Text` then uses
 * `Modifier.wrapContentWidth()` instead of `weight(1f)`.
 *
 * A proper virtualized solution would flatten the recursive tree into a `LazyColumn` item list,
 * but that's a larger refactor; the scroll approach is correct for the directory sizes a mobile
 * code editor typically opens.
 */
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
        // Two-axis scroll: horizontal for deep nesting, vertical for tall trees.
        // widthIn(min) gives each Row a concrete minimum width so text is always visible.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .widthIn(min = 240.dp)   // ensures Row width is always bounded
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                FileTreeNodeItem(
                    node = rootNode,
                    depth = 0,
                    onFileClick = onFileClick,
                    onFileLongClick = onFileLongClick,
                )
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
    onFileLongClick: (FileTreeNode) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(depth == 0) }
    val children = remember { mutableStateListOf<FileTreeNode>() }

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
        label = "chevron_rotation",
    )

    Column {
        // ── Row ──────────────────────────────────────────────────────────────
        // NOTE: do NOT use fillMaxWidth() here — the parent horizontalScroll
        // gives an unbounded width, so fillMaxWidth() would be unbounded too.
        // We let the Row size to its content instead.
        Row(
            modifier = Modifier
                .height(ROW_HEIGHT)
                .combinedClickable(
                    onClick = {
                        if (node.file.isDirectory) isExpanded = !isExpanded
                        else onFileClick(node)
                    },
                    onLongClick = { onFileLongClick(node) },
                )
                .clip(RoundedCornerShape(4.dp))
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            // ── Indent guides ────────────────────────────────────────────────
            // Use explicit fixed-width boxes so they don't collapse.
            repeat(depth) { level ->
                Box(
                    modifier = Modifier
                        .width(INDENT_WIDTH)
                        .height(ROW_HEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    if (level < depth - 1) {
                        // Continuous guide for ancestor levels
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(ROW_HEIGHT)
                                .background(
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                        )
                    }
                    // The last indent level has no guide — the chevron fills that slot
                }
            }

            // ── Chevron ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (node.file.isDirectory) {
                    if (hasChildren || isExpanded) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(chevronRotation),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                    }
                    // empty dir → no chevron, still occupies 20dp
                }
            }

            // ── File / folder icon ───────────────────────────────────────────
            Box(
                modifier = Modifier.size(ICON_SIZE),
                contentAlignment = Alignment.Center,
            ) {
                if (node.file.isDirectory) {
                    val iconPath = FileIcons.getSvgIconForFolder(
                        folderPath = node.file.path,
                        isExpanded = isExpanded,
                        isLight = isLight,
                    )
                    if (iconPath == "files/icons/folder.svg") {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.FolderOpen
                                          else Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(0xFFFFCA28),
                            modifier = Modifier.size(ICON_SIZE),
                        )
                    } else {
                        Image(
                            bitmap = rememberSvgAssetImageBitmap(iconPath),
                            contentDescription = null,
                            modifier = Modifier.size(ICON_SIZE),
                        )
                    }
                } else {
                    val iconPath = FileIcons.getSvgIconForFile(
                        filePath = node.file.path,
                        isLight = isLight,
                    )
                    if (iconPath == "files/icons/file.svg") {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(ICON_SIZE),
                        )
                    } else {
                        Image(
                            bitmap = rememberSvgAssetImageBitmap(iconPath),
                            contentDescription = null,
                            modifier = Modifier.size(ICON_SIZE),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // ── Name ─────────────────────────────────────────────────────────
            // Use wrapContentWidth() — NOT weight(1f).
            // weight() requires a bounded-width parent; in a horizontalScroll
            // container the parent width is infinite, so weight collapses to 0.
            Text(
                text = node.file.name,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (node.file.isDirectory) 1f else 0.9f
                ),
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }

        // ── Children ──────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded && hasChildren,
            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                  + fadeIn(),
            exit  = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                  + fadeOut(),
        ) {
            Column {
                children.forEach { child ->
                    FileTreeNodeItem(
                        node = child,
                        depth = depth + 1,
                        onFileClick = onFileClick,
                        onFileLongClick = onFileLongClick,
                    )
                }
            }
        }
    }
}
