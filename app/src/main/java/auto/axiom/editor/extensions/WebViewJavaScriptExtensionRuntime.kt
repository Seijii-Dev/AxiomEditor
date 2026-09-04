package auto.axiom.editor.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Host operations supplied by the active Axiom editor session. */
interface JavaScriptExtensionHost {
    fun selectedText(): String
    fun replaceSelection(text: String)
    fun currentFilePath(): String?
    fun registerCommand(extensionId: String, commandId: String, title: String, callback: () -> Unit): Result<Unit>
    fun showNotification(message: String)
    fun getSetting(extensionId: String, key: String): String?
    fun setSetting(extensionId: String, key: String, value: String): Result<Unit>
}

/**
 * Restricted WebView adapter for Acode-style JavaScript extensions.
 * JavaScript receives only the explicit `axiom` bridge; file and network access are disabled.
 */
class WebViewJavaScriptExtensionRuntime(
    private val context: Context,
    private val host: JavaScriptExtensionHost
) : JavaScriptExtensionRuntime {
    private val webViews = ConcurrentHashMap<String, WebView>()
    private val nextCallbackId = AtomicInteger(0)

    @SuppressLint("SetJavaScriptEnabled")
    override fun activate(extension: InstalledExtension, bridge: JavaScriptExtensionBridge): Result<Unit> {
        if (!extension.enabled) return Result.failure(IllegalStateException("Extension is disabled"))
        if (!extension.entryFile.isFile) return Result.failure(IllegalArgumentException("Extension entry file is missing"))

        return runCatching {
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = false
                webViewClient = WebViewClient()
                addJavascriptInterface(Bridge(extension, host), "AxiomNative")
            }
            webViews[extension.manifest.id] = webView
            val source = extension.entryFile.readText()
            val bootstrap = """
                (function() {
                  'use strict';
                  const axiom = {
                    editor: {
                      getSelectedText: () => AxiomNative.getSelectedText(),
                      replaceSelection: (text) => AxiomNative.replaceSelection(String(text)),
                      currentFile: () => AxiomNative.getCurrentFilePath()
                    },
                    commands: {
                      register: (command) => {
                        const callbackId = 'axiom_cb_' + Math.random().toString(36).slice(2);
                        window[callbackId] = command.execute;
                        return AxiomNative.registerCommand(command.id, command.title, callbackId);
                      }
                    },
                    notifications: { show: (message) => AxiomNative.showNotification(String(message)) },
                    settings: {
                      get: (key) => AxiomNative.getSetting(String(key)),
                      set: (key, value) => AxiomNative.setSetting(String(key), String(value))
                    }
                  };
                  $source
                  if (typeof activate === 'function') activate(axiom);
                })();
            """.trimIndent()
            webView.loadDataWithBaseURL("https://axiom.extension.local/", "<html><body><script>$bootstrap</script></body></html>", "text/html", "UTF-8", null)
        }
    }

    override fun deactivate(extension: InstalledExtension): Result<Unit> = runCatching {
        webViews.remove(extension.manifest.id)?.destroy()
    }

    private inner class Bridge(
        private val extension: InstalledExtension,
        private val host: JavaScriptExtensionHost
    ) {
        @JavascriptInterface fun getSelectedText(): String = host.selectedText()
        @JavascriptInterface fun replaceSelection(text: String) {
            if (extension.manifest.requires(ExtensionPermissions.EDITOR_WRITE)) host.replaceSelection(text)
        }
        @JavascriptInterface fun getCurrentFilePath(): String? = host.currentFilePath()
        @JavascriptInterface fun showNotification(message: String) = host.showNotification(message)
        @JavascriptInterface fun getSetting(key: String): String? = host.getSetting(extension.manifest.id, key)
        @JavascriptInterface fun setSetting(key: String, value: String): Boolean = host.setSetting(extension.manifest.id, key, value).isSuccess
        @JavascriptInterface fun registerCommand(id: String, title: String, callbackId: String): Boolean {
            if (!extension.manifest.requires(ExtensionPermissions.COMMANDS_REGISTER)) return false
            return host.registerCommand(extension.manifest.id, id, title) {
                val safeId = callbackId.replace("[^A-Za-z0-9_]".toRegex(), "")
                webViews[extension.manifest.id]?.post {
                    webViews[extension.manifest.id]?.evaluateJavascript("window['$safeId'] && window['$safeId']();", null)
                }
            }.isSuccess
        }
    }
}
