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

package auto.axiom.editor.ui.screens.file

import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import auto.axiom.editor.PreferenceKeys
import auto.axiom.editor.compose.ui.filetree.FileTreeNode
import auto.axiom.editor.compose.ui.filetree.createFileTreeFromPath
import auto.axiom.editor.events.OnOpenFolderEvent
import auto.axiom.editor.events.OnRefreshFolderEvent
import auto.axiom.editor.file.File
import auto.axiom.editor.git.GitManager
import auto.axiom.editor.preferences.defaultPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class FileExplorerViewModel : ViewModel() {
    private val _openedFolder = MutableStateFlow<File?>(null)
    val openedFolder get() = _openedFolder.asStateFlow()

    private val _isGitRepo = MutableStateFlow(false)
    val isGitRepo get() = _isGitRepo.asStateFlow()

    private val _rootNode = MutableStateFlow<FileTreeNode?>(null)
    val rootNode get() = _rootNode.asStateFlow()

    fun loadFileTree(folder: File) {
        _rootNode.update { null }

        viewModelScope.launch {
            val node = createFileTreeFromPath(folder)
            _rootNode.update { node }
        }
    }

    fun openFolder(path: File) {
        defaultPrefs.edit(commit = true) {
            putString(
                PreferenceKeys.RECENT_FOLDER_5,
                defaultPrefs.getString(PreferenceKeys.RECENT_FOLDER_4, "")
            )
            putString(
                PreferenceKeys.RECENT_FOLDER_4,
                defaultPrefs.getString(PreferenceKeys.RECENT_FOLDER_3, "")
            )
            putString(
                PreferenceKeys.RECENT_FOLDER_3,
                defaultPrefs.getString(PreferenceKeys.RECENT_FOLDER_2, "")
            )
            putString(
                PreferenceKeys.RECENT_FOLDER_2,
                defaultPrefs.getString(PreferenceKeys.RECENT_FOLDER_1, "")
            )
            putString(PreferenceKeys.RECENT_FOLDER_1, path.absolutePath)
        }
        _openedFolder.update { path }
        EventBus.getDefault().post(OnOpenFolderEvent(path))
    }

    fun closeFolder() {
        _openedFolder.update { null }
    }

    private fun updateGitRepoStatus(file: File) {
        _isGitRepo.update {
            file.asRawFile()?.let { jfile ->
                GitManager.isGitRepository(jfile)
                    .also { if (it) GitManager.instance.initialize(jfile) }
            } ?: false
        }
    }

    fun checkIfGitRepo() {
        _openedFolder.value?.let { file ->
            updateGitRepoStatus(file)
        }
    }

    fun refreshFolder() {
        _openedFolder.value?.let {
            updateGitRepoStatus(it)
            EventBus.getDefault().post(OnRefreshFolderEvent(it))
        }
    }
}
