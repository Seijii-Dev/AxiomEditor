package auto.axiom.editor.lsp

import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

/** Languages supported by the hybrid language-service bridge. */
enum class SupportedLanguage(
    val id: String,
    val extensions: Set<String>,
    val displayName: String
) {
    HTML("html", setOf("html", "htm"), "HTML"),
    CSS("css", setOf("css", "scss", "less"), "CSS"),
    JAVASCRIPT("javascript", setOf("js", "jsx", "mjs", "cjs"), "JavaScript"),
    TYPESCRIPT("typescript", setOf("ts", "tsx"), "TypeScript"),
    PYTHON("python", setOf("py", "pyw"), "Python"),
    JAVA("java", setOf("java"), "Java"),
    KOTLIN("kotlin", setOf("kt", "kts"), "Kotlin"),
    JSON("json", setOf("json", "jsonc"), "JSON"),
    XML("xml", setOf("xml", "svg"), "XML");

    companion object {
        fun fromPath(path: String): SupportedLanguage? = values().firstOrNull {
            path.substringAfterLast('.', "").lowercase() in it.extensions
        }
    }
}

data class LanguageAnalysis(
    val language: SupportedLanguage,
    val diagnostics: List<Diagnostic>,
    val completions: List<CompletionItem>
)

/** Fast, offline fallback used when no external language server is configured. */
object OfflineLanguageService {
    fun analyze(language: SupportedLanguage, text: String): LanguageAnalysis {
        val diagnostics = when (language) {
            SupportedLanguage.HTML, SupportedLanguage.XML -> balancedDiagnostics(text, '<', '>')
            SupportedLanguage.JSON -> balancedDiagnostics(text, '{', '}') + balancedDiagnostics(text, '[', ']')
            else -> balancedDiagnostics(text, '{', '}') + balancedDiagnostics(text, '(', ')')
        }
        return LanguageAnalysis(language, diagnostics, completionItems(language))
    }

    private fun balancedDiagnostics(text: String, opening: Char, closing: Char): List<Diagnostic> {
        var depth = 0
        var line = 0
        val result = mutableListOf<Diagnostic>()
        text.forEach { char ->
            when (char) {
                opening -> depth++
                closing -> if (depth > 0) depth-- else result += diagnostic("Unexpected '$closing'", line)
                '\n' -> line++
            }
        }
        if (depth > 0) result += diagnostic("Unclosed '$opening'", line.coerceAtLeast(0))
        return result
    }

    private fun diagnostic(message: String, line: Int) = Diagnostic(
        Range(Position(line, 0), Position(line, 1)),
        message,
        DiagnosticSeverity.Warning,
        "axiom-offline"
    )

    private fun completionItems(language: SupportedLanguage): List<CompletionItem> {
        val words = when (language) {
            SupportedLanguage.HTML -> listOf("html", "head", "body", "div", "main", "section", "button", "input", "script", "style")
            SupportedLanguage.CSS -> listOf("display", "position", "color", "background", "margin", "padding", "grid", "flex", "font-size")
            SupportedLanguage.JAVASCRIPT, SupportedLanguage.TYPESCRIPT -> listOf("const", "let", "function", "import", "export", "async", "await", "interface", "type", "class")
            SupportedLanguage.PYTHON -> listOf("def", "class", "import", "from", "async", "await", "return", "yield", "with", "match")
            SupportedLanguage.JAVA, SupportedLanguage.KOTLIN -> listOf("class", "interface", "fun", "public", "private", "override", "import", "return", "when", "data")
            SupportedLanguage.JSON -> listOf("true", "false", "null")
            SupportedLanguage.XML -> listOf("layout", "resources", "string", "color", "item", "manifest")
        }
        return words.map {
            CompletionItem(it).apply {
                kind = CompletionItemKind.Keyword
                detail = "Offline ${language.displayName} completion"
            }
        }
    }
}
