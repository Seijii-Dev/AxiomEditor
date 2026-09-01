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

package auto.axiom.editor.ui.screens.editor.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.blankj.utilcode.util.ClipboardUtils
import auto.axiom.editor.activities.Editor.LocalEditorDrawerNavController
import auto.axiom.editor.activities.Editor.LocalEditorDrawerState
import auto.axiom.editor.app.strings
import auto.axiom.editor.compose.ui.filetree.FileTreeView
import auto.axiom.editor.core.components.editor.FileOptionItem
import auto.axiom.editor.core.components.editor.FileOptionsSheet
import auto.axiom.editor.core.components.editor.NavigationSpace
import auto.axiom.editor.core.components.editor.NavigationSpaceItem
import auto.axiom.editor.core.components.editor.rememberNavigationSpaceState
import auto.axiom.editor.events.OnCreateFileEvent
import auto.axiom.editor.events.OnCreateFolderEvent
import auto.axiom.editor.events.OnRefreshFolderEvent
import auto.axiom.editor.extensions.openFile
import auto.axiom.editor.file.File
import auto.axiom.editor.git.GitViewModel
import auto.axiom.editor.resources.R.string
import auto.axiom.editor.ui.screens.EditorDrawerScreens
import auto.axiom.editor.ui.screens.editor.EditorViewModel
import auto.axiom.editor.ui.screens.editor.components.drawer.Heading
import auto.axiom.editor.ui.screens.editor.components.drawer.NavRail
import auto.axiom.editor.ui.screens.editor.components.drawer.OpenFolderActions
import auto.axiom.editor.ui.screens.file.FileExplorerViewModel
import auto.axiom.editor.utils.ApkInstaller
import kiwi.orbit.compose.ui.controls.Text
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

@Composable
fun EditorDrawerSheet(
    fileExplorerViewModel: FileExplorerViewModel,
    editorViewModel: EditorViewModel,
    gitViewModel: GitViewModel = viewModel()
) {
    val context = LocalContext.current
    val drawerState = LocalEditorDrawerState.current
    val navController = LocalEditorDrawerNavController.current
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("Files") }

    val openedFolder by fileExplorerViewModel.openedFolder.collectAsStateWithLifecycle()

    fun closeDrawer() {
        scope.launch {
            drawerState.apply {
                if (isOpen) close()
            }
        }
    }

    navController.addOnDestinationChangedListener { _, destination, _ ->
        when (destination.route) {
            EditorDrawerScreens.FileExplorer::class.qualifiedName -> {
                selectedItem = 0
                title = context.getString(string.files)
            }

            EditorDrawerScreens.GitManager::class.qualifiedName -> {
                selectedItem = 1
                title = context.getString(string.git)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        NavRail(
            selectedItemIndex = selectedItem
        )

        Column {
            Heading(
                title = title,
                onCloseDrawerRequest = { closeDrawer() }
            )

            NavHost(
                navController = LocalEditorDrawerNavController.current,
                startDestination = EditorDrawerScreens.FileExplorer
            ) {
                composable<EditorDrawerScreens.FileExplorer> {
                    openedFolder?.let { folder ->
                        var selectedFile by remember { mutableStateOf<File?>(null) }
                        var showNewFileDialog by remember { mutableStateOf(false) }
                        var selectedFolder by remember { mutableStateOf(folder) }
                        val rootNode by fileExplorerViewModel.rootNode.collectAsStateWithLifecycle()

                        LaunchedEffect(folder) {
                            fileExplorerViewModel.loadFileTree(folder)
                        }

                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            rootNode?.let { node ->
                                FileTreeView(
                                    rootNode = node,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f),
                                    onFileClick = {
                                        val file = it.file

                                        if (file.name.endsWith(".apk")) {
                                            ApkInstaller.installApplication(context, file)
                                        } else if (file.isValidText) {
                                            closeDrawer()
                                            editorViewModel.addFile(file)
                                        } else {
                                            context.openFile(file)
                                        }
                                    },
                                    onFileLongClick = { selectedFile = it.file }
                                )
                            } ?: run {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Loading Folder")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            NavigationSpaceActions {
                                when (it.id) {
                                    0 -> EventBus.getDefault().post(OnRefreshFolderEvent(folder))

                                    1 -> {
                                        selectedFolder = folder
                                        showNewFileDialog = true
                                    }

                                    99 -> {
                                        fileExplorerViewModel.closeFolder()
                                        gitViewModel.close()
                                    }
                                }
                            }

                            FileActionDialogs(
                                selectedFile = selectedFile,
                                openedFolder = folder,
                                onAddFileClick = {
                                    selectedFolder = it
                                    showNewFileDialog = true
                                },
                                onDissmissRequest = { selectedFile = null }
                            )

                            if (showNewFileDialog) {
                                NewFileDialog(
                                    path = selectedFolder,
                                    onFileCreated = {
                                        EventBus.getDefault().post(OnCreateFileEvent(it, folder))
                                    },
                                    onFolderCreated = {
                                        EventBus.getDefault().post(OnCreateFolderEvent(it, folder))
                                    },
                                    onDismissRequest = { showNewFileDialog = false },
                                )
                            }
                        }
                    } ?: run {
                        OpenFolderActions(
                            modifier = Modifier.fillMaxSize(),
                            fileExplorerViewModel = fileExplorerViewModel
                        )
                    }
                }

                composable<EditorDrawerScreens.GitManager> {
                    GitManager(fileExplorerViewModel = fileExplorerViewModel)
                }
            }
        }
    }
}

@Composable
fun NavigationSpaceActions(
    modifier: Modifier = Modifier,
    onItemClick: (NavigationSpaceItem) -> Unit
) {
    val refresh = stringResource(string.refresh)
    val add = stringResource(string.add)
    val close = stringResource(string.close)

    val navigationSpaceState = rememberNavigationSpaceState()
    LaunchedEffect(Unit) {
        navigationSpaceState.apply {
            add(
                NavigationSpaceItem(
                    id = 99,
                    icon = Icons.Rounded.Close,
                    title = close
                )
            )
            add(
                NavigationSpaceItem(
                    id = 0,
                    icon = Icons.Rounded.Refresh,
                    title = refresh
                )
            )
            add(
                NavigationSpaceItem(
                    id = 1,
                    icon = Icons.Rounded.Add,
                    title = add
                )
            )
        }
    }

    NavigationSpace(
        modifier = modifier,
        state = navigationSpaceState,
        onItemClick = onItemClick
    )
}

@Composable
fun FileActionDialogs(
    selectedFile: File?,
    onAddFileClick: (File) -> Unit = {},
    openedFolder: File,
    onDissmissRequest: () -> Unit
) {
    var renamableFile by remember { mutableStateOf<File?>(null) }
    var deletableFile by remember { mutableStateOf<File?>(null) }

    val add = stringResource(strings.add)
    val copyPath = stringResource(strings.file_copy_path)
    val deleteFile = stringResource(strings.file_delete)
    val renameFile = stringResource(strings.file_rename)

    val addIcon = Icons.Rounded.Add
    val copyPathIcon = Icons.Rounded.ContentCopy
    val deleteFileIcon = Icons.Rounded.DeleteForever
    val renameFileIcon = Icons.Rounded.DriveFileRenameOutline

    when {
        selectedFile != null -> {
            FileOptionsSheet(
                onDismissRequest = onDissmissRequest,
                options = {
                    mutableListOf<FileOptionItem>().apply {
                        if (selectedFile.isDirectory) {
                            add(
                                FileOptionItem(
                                    name = add,
                                    icon = addIcon,
                                    onClick = { onAddFileClick(selectedFile) }
                                )
                            )
                        }

                        add(
                            FileOptionItem(
                                name = copyPath,
                                icon = copyPathIcon,
                                onClick = { ClipboardUtils.copyText(selectedFile.absolutePath) }
                            )
                        )

                        add(
                            FileOptionItem(
                                name = renameFile,
                                icon = renameFileIcon,
                                onClick = { renamableFile = selectedFile }
                            )
                        )

                        add(
                            FileOptionItem(
                                name = deleteFile,
                                icon = deleteFileIcon,
                                onClick = { deletableFile = selectedFile }
                            )
                        )
                    }
                }
            )
        }

        deletableFile != null -> {
            DeleteFileDialog(
                file = deletableFile!!,
                openedFolder = openedFolder,
                onDismissRequest = { deletableFile = null }
            )
        }

        renamableFile != null -> {
            RenameFileDialog(
                file = renamableFile!!,
                openedFolder = openedFolder,
                onDismissRequest = { renamableFile = null }
            )
        }
    }
}
