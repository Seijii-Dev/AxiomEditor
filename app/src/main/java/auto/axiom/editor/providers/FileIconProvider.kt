package auto.axiom.editor.providers

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import auto.axiom.editor.file.File
import auto.axiom.editor.file.extension
import auto.axiom.editor.models.FileIcon
import auto.axiom.editor.resources.R
import auto.axiom.editor.app.BaseApplication.Companion.instance as app

/**
 * Class to provide File icons
 *
 * @author Felipe Teixeira
 */
object FileIconProvider {

    private var fileIcons: List<FileIcon> = mutableListOf()

    init {
        val fileIconsJson =
            app.assets.open("files/file_icons.json").bufferedReader().use { it.readText() }
        fileIcons = Gson().fromJson(fileIconsJson, object : TypeToken<List<FileIcon>>() {})
    }

    @SuppressLint("DiscouragedApi")
    fun findFileIconResource(file: File): Int {
        val fileIcon = findFileIconByExtension(file.extension) ?: return R.drawable.ic_file
        val resId = app.resources.getIdentifier(fileIcon.drawableName, "drawable", app.packageName)
        return if (resId == 0) R.drawable.ic_file else resId
    }

    private fun findFileIconByExtension(extension: String): FileIcon? =
        fileIcons.find { it.fileExtensions.contains(extension) }
}
