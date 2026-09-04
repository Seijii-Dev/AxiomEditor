package auto.axiom.editor.lsp

import androidx.compose.runtime.mutableStateMapOf
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.Diagnostic
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates document lifecycle for offline language intelligence and optional
 * external language servers. The external provider can be supplied by a future
 * local/remote LSP transport without changing editor integrations.
 */
object LanguageServiceManager {
    private val documents = ConcurrentHashMap<String, String>()
    private val analyses = mutableStateMapOf<String, LanguageAnalysis>()
    private val externalProviders = ConcurrentHashMap<SupportedLanguage, ExternalLanguageProvider>()
    private val tcpClients = ConcurrentHashMap<SupportedLanguage, TcpLspClient>()
    private val processClients = ConcurrentHashMap<SupportedLanguage, ProcessLspClient>()

    fun open(path: String, text: String) = update(path, text)

    fun update(path: String, text: String): LanguageAnalysis? {
        val language = SupportedLanguage.fromPath(path) ?: return null
        documents[path] = text
        val external = externalProviders[language]
        val analysis = external?.analyze(path, text) ?: OfflineLanguageService.analyze(language, text)
        analyses[path] = analysis
        return analysis
    }

    fun close(path: String) {
        documents.remove(path)
        analyses.remove(path)
    }

    fun text(path: String): String? = documents[path]
    fun diagnostics(path: String): List<Diagnostic> = analyses[path]?.diagnostics.orEmpty()
    fun completions(path: String): List<CompletionItem> = analyses[path]?.completions.orEmpty()
    fun analysis(path: String): LanguageAnalysis? = analyses[path]

    fun configureTcpServers(enabled: Boolean, host: String, port: Int) {
        if (!enabled) {
            tcpClients.values.forEach { it.close() }
            tcpClients.clear()
            processClients.values.forEach { it.close() }
            processClients.clear()
            return
        }
        SupportedLanguage.values().forEach { language ->
            if (!tcpClients.containsKey(language)) {
                runCatching { TcpLspClient(host, port, language) }
                    .onSuccess { tcpClients[language] = it }
            }
        }
    }

    fun semanticCompletions(
        path: String,
        text: String,
        line: Int,
        character: Int,
        prefix: String
    ): List<JSONObject> {
        val language = SupportedLanguage.fromPath(path) ?: return emptyList()
        val client = tcpClients[language] ?: return emptyList()
        return runCatching { client.completion("file://$path", text, line, character) }
            .getOrDefault(emptyList())
            .filter { it.optString("label").startsWith(prefix, ignoreCase = true) }
    }

    fun semanticCompletions(
        language: SupportedLanguage,
        uri: String,
        text: String,
        line: Int,
        character: Int,
        prefix: String
    ): List<JSONObject> = runCatching {
        val values = tcpClients[language]?.completion(uri, text, line, character)
            ?: processClients[language]?.completion(uri, text, line, character)
            ?: startLocal(language)?.let { client ->
                processClients[language] = client
                client.completion(uri, text, line, character)
            }
            ?: emptyList()
        values
            .filter { it.optString("label").startsWith(prefix, ignoreCase = true) }
    }.getOrDefault(emptyList())

    private fun startLocal(language: SupportedLanguage): ProcessLspClient? {
        val command = when (language) {
            SupportedLanguage.PYTHON -> listOf("pyright-langserver", "--stdio")
            SupportedLanguage.JAVASCRIPT, SupportedLanguage.TYPESCRIPT -> listOf("typescript-language-server", "--stdio")
            SupportedLanguage.JAVA -> listOf("jdtls")
            SupportedLanguage.KOTLIN -> listOf("kotlin-language-server")
            else -> return null
        }
        return LspRuntimeBridge.startLocal(
            LspRuntimeBridge.ServerSpec(language, command)
        )
    }

    fun registerExternalProvider(language: SupportedLanguage, provider: ExternalLanguageProvider) {
        externalProviders[language] = provider
    }

    fun unregisterExternalProvider(language: SupportedLanguage) {
        externalProviders.remove(language)
    }
}

fun interface ExternalLanguageProvider {
    fun analyze(path: String, text: String): LanguageAnalysis
}
