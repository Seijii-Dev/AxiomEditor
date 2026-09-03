package auto.axiom.editor.extensions

/** Lifecycle contract implemented by the JavaScript runtime adapter. */
interface JavaScriptExtensionRuntime {
    fun activate(extension: InstalledExtension, bridge: JavaScriptExtensionBridge): Result<Unit>
    fun deactivate(extension: InstalledExtension): Result<Unit>
}

/** Narrow host API exposed to JavaScript extensions after permission checks. */
interface JavaScriptExtensionBridge {
    fun getSelectedText(): String
    fun replaceSelection(text: String)
    fun getCurrentFilePath(): String?
    fun registerCommand(id: String, title: String, callback: () -> Unit): Result<Unit>
    fun showNotification(message: String)
    fun getSetting(key: String): String?
    fun setSetting(key: String, value: String): Result<Unit>
}

class ExtensionPermissionException(permission: String) : SecurityException(
    "Extension permission is required: $permission"
)

fun ExtensionManifest.requires(permission: String): Boolean = permission in permissions
