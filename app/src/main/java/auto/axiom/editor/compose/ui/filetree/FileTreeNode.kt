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
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.vector.ImageVector
import auto.axiom.editor.file.File
import auto.axiom.editor.file.extension
import auto.axiom.editor.ui.icon.LanguageCpp
import auto.axiom.editor.ui.icons.Icons
import auto.axiom.editor.ui.icons.LanguageC
import auto.axiom.editor.ui.icons.LanguageCsharp
import auto.axiom.editor.ui.icons.LanguageCss3
import auto.axiom.editor.ui.icons.LanguageGo
import auto.axiom.editor.ui.icons.LanguageHtml5
import auto.axiom.editor.ui.icons.LanguageJava
import auto.axiom.editor.ui.icons.LanguageJavascript
import auto.axiom.editor.ui.icons.LanguageKotlin
import auto.axiom.editor.ui.icons.LanguageLua
import auto.axiom.editor.ui.icons.LanguageMarkdown
import auto.axiom.editor.ui.icons.LanguagePhp
import auto.axiom.editor.ui.icons.LanguagePython
import auto.axiom.editor.ui.icons.LanguageRust
import auto.axiom.editor.ui.icons.LanguageSwift
import auto.axiom.editor.ui.icons.LanguageTypescript
import auto.axiom.editor.ui.icons.LanguageXml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Data class representing a file or directory in the file tree
 */
data class FileTreeNode(
    val file: File,
    val isLoading: Boolean = false,
    val children: Flow<FileTreeNode> = emptyFlow()
)

fun createFileTreeFromPath(file: File): FileTreeNode {
    return FileTreeNode(
        file = file,
        children = if (file.isDirectory) {
            flow {
                val files = withContext(Dispatchers.IO) {
                    file.listFiles()?.toList().orEmpty()
                }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

                files.forEach { emit(createFileTreeFromPath(it)) }
            }.flowOn(Dispatchers.IO)
        } else {
            emptyFlow()
        }
    )
}

@SuppressLint("MaterialDesignInsteadOrbitDesign")
fun getIconForFile(node: FileTreeNode): ImageVector {
    return when (node.file.extension) {
        "c" -> Icons.LanguageC
        "cpp" -> Icons.LanguageCpp
        "cs" -> Icons.LanguageCsharp
        "css" -> Icons.LanguageCss3
        "html" -> Icons.LanguageHtml5
        "go" -> Icons.LanguageGo
        "lua" -> Icons.LanguageLua
        "rs" -> Icons.LanguageRust
        "md" -> Icons.LanguageMarkdown
        "php" -> Icons.LanguagePhp
        "py" -> Icons.LanguagePython
        "swift" -> Icons.LanguageSwift
        "java" -> Icons.LanguageJava
        "js", "jsx" -> Icons.LanguageJavascript
        "kt", "kts" -> Icons.LanguageKotlin
        "ts", "tsx" -> Icons.LanguageTypescript
        "xml" -> Icons.LanguageXml
        "jpg", "jpeg", "png", "gif", "bmp", "svg", "ico" -> androidx.compose.material.icons.Icons.Default.Image
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "ogv" -> androidx.compose.material.icons.Icons.Default.VideoFile
        else -> androidx.compose.material.icons.Icons.AutoMirrored.Filled.InsertDriveFile
    }
}
