package auto.axiom.editor.extensions

import com.google.gson.GsonBuilder

/** Versioned manifest for a JavaScript extension installed by Axiom Editor. */
data class ExtensionManifest(
    val id: String,
    val name: String,
    val version: String,
    val main: String,
    val description: String = "",
    val author: String = "",
    val engines: Engines = Engines(),
    val permissions: List<String> = emptyList()
) {
    data class Engines(val axiom: String = ">=1.0.0")

    companion object {
        private val gson = GsonBuilder().create()

        fun fromJson(json: String): ExtensionManifest =
            gson.fromJson(json, ExtensionManifest::class.java)
    }

    fun toJson(): String = gson.toJson(this)
}

object ExtensionPermissions {
    const val EDITOR_READ = "editor.read"
    const val EDITOR_WRITE = "editor.write"
    const val COMMANDS_REGISTER = "commands.register"
    const val MENUS_REGISTER = "menus.register"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val FILESYSTEM_READ = "filesystem.read"
    const val FILESYSTEM_WRITE = "filesystem.write"
    const val NETWORK = "network"

    val known = setOf(
        EDITOR_READ,
        EDITOR_WRITE,
        COMMANDS_REGISTER,
        MENUS_REGISTER,
        NOTIFICATIONS,
        SETTINGS,
        FILESYSTEM_READ,
        FILESYSTEM_WRITE,
        NETWORK
    )
}

class ExtensionManifestException(message: String) : IllegalArgumentException(message)

object ExtensionManifestValidator {
    private val idPattern = Regex("^[a-z][a-z0-9]*(\\.[a-z0-9-]+)+$")
    private val versionPattern = Regex("^[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][A-Za-z0-9.-]+)?$")

    fun parseAndValidate(json: String): ExtensionManifest {
        val manifest = try {
            ExtensionManifest.fromJson(json)
        } catch (error: Exception) {
            throw ExtensionManifestException("Invalid manifest JSON: ${error.message ?: "malformed JSON"}")
        }

        if (!idPattern.matches(manifest.id)) {
            throw ExtensionManifestException("Manifest id must be a reverse-domain identifier")
        }
        if (manifest.name.isBlank()) throw ExtensionManifestException("Manifest name is required")
        if (!versionPattern.matches(manifest.version)) {
            throw ExtensionManifestException("Manifest version must use semantic versioning")
        }
        if (manifest.main.isBlank() || manifest.main.startsWith("/") || manifest.main.contains("..")) {
            throw ExtensionManifestException("Manifest main must be a relative file path")
        }
        val unknownPermissions = manifest.permissions.filterNot(ExtensionPermissions.known::contains)
        if (unknownPermissions.isNotEmpty()) {
            throw ExtensionManifestException("Unknown permissions: ${unknownPermissions.joinToString()}")
        }
        if (manifest.permissions.distinct().size != manifest.permissions.size) {
            throw ExtensionManifestException("Manifest permissions must not contain duplicates")
        }
        return manifest
    }
}
