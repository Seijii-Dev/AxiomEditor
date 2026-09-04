package auto.axiom.editor.lsp

import androidx.compose.runtime.mutableStateMapOf
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.Diagnostic
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
