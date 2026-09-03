package auto.axiom.editor.extensions

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/** Installed extension package and its validated manifest. */
data class InstalledExtension(
    val manifest: ExtensionManifest,
    val directory: File,
    val enabled: Boolean = true
) {
    val entryFile: File get() = File(directory, manifest.main)
}

class ExtensionPackageManager(private val context: Context) {
    private val root: File by lazy {
        File(context.getExternalFilesDir(null), "extensions").apply { mkdirs() }
    }

    fun listInstalled(): List<InstalledExtension> = root.listFiles()
        ?.filter { it.isDirectory }
        ?.mapNotNull { directory ->
            runCatching {
                val manifestFile = File(directory, "manifest.json")
                if (!manifestFile.isFile) return@runCatching null
                val manifest = ExtensionManifestValidator.parseAndValidate(manifestFile.readText())
                if (!File(directory, manifest.main).isFile) return@runCatching null
                InstalledExtension(manifest, directory, isEnabled(manifest.id))
            }.getOrNull()
        }
        ?.sortedBy { it.manifest.name.lowercase() }
        ?: emptyList()

    fun install(zipFile: File): InstalledExtension {
        require(zipFile.isFile) { "Extension package does not exist" }
        val staging = File(root, ".staging-${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            extractZip(zipFile, staging)
            val manifestFile = File(staging, "manifest.json")
            val manifest = ExtensionManifestValidator.parseAndValidate(manifestFile.readText())
            require(File(staging, manifest.main).isFile) { "Extension entry file is missing: ${manifest.main}" }

            val destination = File(root, manifest.id)
            if (destination.exists()) destination.deleteRecursively()
            check(staging.renameTo(destination)) { "Unable to install extension ${manifest.id}" }
            return InstalledExtension(manifest, destination, isEnabled(manifest.id))
        } finally {
            if (staging.exists()) staging.deleteRecursively()
        }
    }

    fun uninstall(id: String): Boolean = File(root, id).takeIf { it.isDirectory }?.deleteRecursively() == true

    fun setEnabled(id: String, enabled: Boolean) {
        context.getSharedPreferences("javascript_extensions", Context.MODE_PRIVATE)
            .edit().putBoolean("enabled:$id", enabled).apply()
    }

    private fun isEnabled(id: String): Boolean = context
        .getSharedPreferences("javascript_extensions", Context.MODE_PRIVATE)
        .getBoolean("enabled:$id", true)

    private fun extractZip(zipFile: File, destination: File) {
        ZipInputStream(FileInputStream(zipFile)).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                val target = File(destination, entry.name)
                val destinationPath = destination.canonicalPath + File.separator
                require(target.canonicalPath.startsWith(destinationPath)) {
                    "Extension package contains an unsafe path"
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
            }
        }
    }
}
