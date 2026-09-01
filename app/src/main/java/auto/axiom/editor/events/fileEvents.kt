package auto.axiom.editor.events

import auto.axiom.editor.app.Folder
import auto.axiom.editor.file.File

data class OnDeleteFileEvent(val file: File, val openedFolder: File)

data class OnCreateFileEvent(val file: File, val openedFolder: File)

data class OnCreateFolderEvent(val file: File, val openedFolder: File)

data class OnRefreshFolderEvent(val openedFolder: File)

data class OnRenameFileEvent(val oldFile: File, val newFile: File, val openedFolder: File)

data class OnOpenFolderEvent(val folder: Folder)
