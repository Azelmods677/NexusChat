package com.Azelmods.App.ui.screens.editor

import com.Azelmods.App.i18n.tr

import android.graphics.Color
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.SimpleAnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionHelper
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.lang.styling.MappedSpans
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.SymbolPairMatch
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/** Idiomas que el editor reconoce por la extensión del archivo. */
object CodeLanguages {
    private val BY_EXTENSION = mapOf(
        "js" to "js", "mjs" to "js", "cjs" to "js", "ts" to "ts", "jsx" to "jsx", "tsx" to "tsx",
        "py" to "python", "kt" to "kotlin", "kts" to "kotlin", "java" to "java",
        "c" to "c", "h" to "c", "cpp" to "c", "cc" to "c", "hpp" to "c", "cs" to "c", "go" to "c",
        "rs" to "c", "swift" to "c", "dart" to "c", "php" to "js", "rb" to "python",
        "sh" to "bash", "bash" to "bash", "zsh" to "bash",
        "html" to "html", "htm" to "html", "xml" to "html", "svg" to "html",
        "css" to "css", "scss" to "css", "json" to "json",
        "yml" to "python", "yaml" to "python", "toml" to "python", "ini" to "python",
        "md" to "text", "txt" to "text", "gradle" to "kotlin", "sql" to "text"
    )

    fun fromFileName(name: String): String =
        BY_EXTENSION[name.substringAfterLast('.', "").lowercase()] ?: "text"

    fun label(language: String): String = when (language) {
        "js" -> tr("JavaScript")
        "ts" -> tr("TypeScript")
        "jsx" -> "JSX"
        "tsx" -> "TSX"
        "python" -> tr("Python")
        "kotlin" -> tr("Kotlin")
        "java" -> tr("Java")
        "c" -> "C"
        "bash" -> tr("Shell")
        "html" -> "HTML"
        "css" -> "CSS"
        "json" -> "JSON"
        else -> tr("Texto")
    }
}

/** Colores del editor, sobre la misma superficie que el resto de la app. */
fun nexusEditorScheme(accent: Int): EditorColorScheme = EditorColorScheme().apply {
    setColor(EditorColorScheme.WHOLE_BACKGROUND, Color.parseColor("#101214"))
    setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, Color.parseColor("#101214"))
    setColor(EditorColorScheme.LINE_NUMBER, Color.parseColor("#4F545A"))
    setColor(EditorColorScheme.LINE_NUMBER_CURRENT, Color.parseColor("#A6ACB3"))
    setColor(EditorColorScheme.LINE_DIVIDER, Color.parseColor("#24282C"))
    setColor(EditorColorScheme.CURRENT_LINE, Color.parseColor("#171A1D"))
    setColor(EditorColorScheme.TEXT_NORMAL, Color.parseColor("#D6DADF"))
    setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, (accent and 0x00FFFFFF) or 0x55000000)
    setColor(EditorColorScheme.SELECTION_HANDLE, accent)
    setColor(EditorColorScheme.SELECTION_INSERT, accent)
    setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND, Color.parseColor("#5C4A1E"))
    setColor(EditorColorScheme.BLOCK_LINE, Color.parseColor("#24282C"))
    setColor(EditorColorScheme.BLOCK_LINE_CURRENT, Color.parseColor("#3A4046"))
    setColor(EditorColorScheme.KEYWORD, Color.parseColor("#C49BD6"))
    setColor(EditorColorScheme.COMMENT, Color.parseColor("#6B737B"))
    setColor(EditorColorScheme.LITERAL, Color.parseColor("#9CCB8E"))
    setColor(EditorColorScheme.OPERATOR, Color.parseColor("#A6ACB3"))
    setColor(EditorColorScheme.FUNCTION_NAME, Color.parseColor("#7FB0DE"))
    setColor(EditorColorScheme.IDENTIFIER_NAME, Color.parseColor("#D6DADF"))
    setColor(EditorColorScheme.IDENTIFIER_VAR, Color.parseColor("#D6DADF"))
    setColor(EditorColorScheme.ANNOTATION, Color.parseColor("#D9A441"))
    setColor(EditorColorScheme.HTML_TAG, Color.parseColor("#7FB0DE"))
    setColor(EditorColorScheme.ATTRIBUTE_NAME, Color.parseColor("#D9A441"))
    setColor(EditorColorScheme.ATTRIBUTE_VALUE, Color.parseColor("#9CCB8E"))
}

/**
 * Lenguaje para Sora Editor: resaltado propio, pares de símbolos y autocompletado.
 *
 * El resaltado es un analizador de un solo pasaje en segundo plano, así que los archivos
 * grandes no traban la escritura. Sustituye a la versión anterior, que volvía a colorear
 * el documento entero con expresiones regulares en cada pulsación.
 */
class NexusCodeLanguage(private val language: String) : EmptyLanguage() {

    private val keywords: Set<String> = keywordsFor(language)
    private val manager = CodeAnalyzer(language, keywords)

    override fun getAnalyzeManager(): AnalyzeManager = manager

    override fun getInterruptionLevel(): Int = 0

    override fun getSymbolPairs(): SymbolPairMatch = PAIRS

    override fun useTab(): Boolean = false

    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int {
        val text = content.getLine(line).substring(0, column.coerceAtMost(content.getColumnCount(line)))
        val trimmed = text.trimEnd()
        return when {
            trimmed.endsWith("{") || trimmed.endsWith("(") || trimmed.endsWith("[") -> 4
            language == "python" && trimmed.endsWith(":") -> 4
            else -> 0
        }
    }

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: android.os.Bundle
    ) {
        val prefix = CompletionHelper.computePrefix(content, position) { Character.isJavaIdentifierPart(it) }
        if (prefix.length < 2) return
        val inDocument = Regex("[A-Za-z_][A-Za-z0-9_]{2,}")
            .findAll(content.toString().take(MAX_COMPLETION_SCAN))
            .map { it.value }
            .toSet()
        val candidates = (keywords + inDocument)
            .filter { it.startsWith(prefix) && it != prefix }
            .sorted()
            .take(40)
        publisher.addItems(
            candidates.map { word ->
                SimpleCompletionItem(word, if (word in keywords) tr("palabra clave") else tr("en este archivo"), prefix.length, word)
            }
        )
    }

    private class CodeAnalyzer(
        private val language: String,
        private val keywords: Set<String>
    ) : SimpleAnalyzeManager<Unit>() {

        private val normal = TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL)
        private val keyword = TextStyle.makeStyle(EditorColorScheme.KEYWORD, 0, true, false, false)
        private val comment = TextStyle.makeStyle(EditorColorScheme.COMMENT, 0, false, true, false)
        private val literal = TextStyle.makeStyle(EditorColorScheme.LITERAL)
        private val function = TextStyle.makeStyle(EditorColorScheme.FUNCTION_NAME)
        private val annotation = TextStyle.makeStyle(EditorColorScheme.ANNOTATION)
        private val tag = TextStyle.makeStyle(EditorColorScheme.HTML_TAG)
        private val attribute = TextStyle.makeStyle(EditorColorScheme.ATTRIBUTE_NAME)

        private val hashComments = language in setOf("python", "bash")
        private val isMarkup = language == "html"
        private val isCss = language == "css"
        private val hasCode = language !in setOf("text")

        override fun analyze(text: StringBuilder, delegate: Delegate<Unit>): Styles {
            val spans = MappedSpans.Builder(2048)
            var i = 0
            var line = 0
            var col = 0
            val n = text.length

            fun advance(style: Long, count: Int) {
                // Un token que cruza líneas debe reabrir su estilo en cada línea nueva.
                var left = count
                while (left > 0 && i < n) {
                    if (text[i] == '\n') {
                        line++
                        col = 0
                        spans.addIfNeeded(line, 0, style)
                    } else {
                        col++
                    }
                    i++
                    left--
                }
            }

            fun emit(style: Long, count: Int) {
                spans.addIfNeeded(line, col, style)
                advance(style, count)
            }

            spans.addNormalIfNull()
            if (!hasCode) {
                spans.determine(0)
                return Styles(spans.build())
            }

            var inTag = false
            while (i < n && !delegate.isCancelled) {
                val c = text[i]
                val next = if (i + 1 < n) text[i + 1] else '\u0000'

                when {
                    c == '\n' -> advance(normal, 1)

                    // ── Comentarios ──
                    isMarkup && text.startsWith("<!--", i) -> {
                        val end = text.indexOf("-->", i + 4).let { if (it < 0) n else it + 3 }
                        emit(comment, end - i)
                    }
                    !isMarkup && hashComments && c == '#' -> {
                        val end = text.indexOf('\n', i).let { if (it < 0) n else it }
                        emit(comment, end - i)
                    }
                    !isMarkup && !hashComments && c == '/' && next == '*' -> {
                        val end = text.indexOf("*/", i + 2).let { if (it < 0) n else it + 2 }
                        emit(comment, end - i)
                    }
                    !isMarkup && !isCss && !hashComments && language != "json" && c == '/' && next == '/' -> {
                        val end = text.indexOf('\n', i).let { if (it < 0) n else it }
                        emit(comment, end - i)
                    }

                    // ── Cadenas ──
                    c == '"' || c == '\'' || c == '`' -> {
                        var j = i + 1
                        while (j < n) {
                            val d = text[j]
                            if (d == '\\') {
                                j += 2
                                continue
                            }
                            if (d == c) {
                                j++
                                break
                            }
                            // Solo las plantillas de JS cruzan de línea.
                            if (d == '\n' && c != '`') break
                            j++
                        }
                        emit(literal, j.coerceAtMost(n) - i)
                    }

                    // ── HTML ──
                    isMarkup && c == '<' -> {
                        var j = i + 1
                        if (j < n && (text[j] == '/' || text[j] == '!' || text[j] == '?')) j++
                        while (j < n && (text[j].isLetterOrDigit() || text[j] == '-' || text[j] == ':')) j++
                        emit(tag, j - i)
                        inTag = true
                    }
                    isMarkup && c == '>' -> {
                        emit(tag, 1)
                        inTag = false
                    }
                    isMarkup && inTag && (c.isLetter() || c == '-') -> {
                        var j = i
                        while (j < n && (text[j].isLetterOrDigit() || text[j] == '-' || text[j] == ':')) j++
                        emit(attribute, j - i)
                    }
                    isMarkup && !inTag -> emit(normal, 1)

                    // ── Números ──
                    c.isDigit() -> {
                        var j = i
                        while (j < n && (text[j].isLetterOrDigit() || text[j] == '.' || text[j] == '_')) j++
                        emit(literal, j - i)
                    }

                    // ── Anotaciones y reglas @ ──
                    c == '@' -> {
                        var j = i + 1
                        while (j < n && (text[j].isLetterOrDigit() || text[j] == '_' || text[j] == '-')) j++
                        emit(annotation, j - i)
                    }

                    // ── Palabras ──
                    c.isLetter() || c == '_' || c == '$' -> {
                        var j = i
                        while (j < n && (text[j].isLetterOrDigit() || text[j] == '_' || text[j] == '$' || (isCss && text[j] == '-'))) j++
                        val word = text.substring(i, j)
                        var k = j
                        while (k < n && (text[k] == ' ' || text[k] == '\t')) k++
                        val style = when {
                            word in keywords -> keyword
                            isCss && k < n && text[k] == ':' -> attribute
                            k < n && text[k] == '(' -> function
                            else -> normal
                        }
                        emit(style, j - i)
                    }

                    else -> emit(normal, 1)
                }
            }

            spans.determine(line)
            return Styles(spans.build())
        }
    }

    private companion object {
        const val MAX_COMPLETION_SCAN = 200_000

        val PAIRS = SymbolPairMatch().apply {
            putPair('{', SymbolPairMatch.SymbolPair("{", "}"))
            putPair('(', SymbolPairMatch.SymbolPair("(", ")"))
            putPair('[', SymbolPairMatch.SymbolPair("[", "]"))
            putPair('"', SymbolPairMatch.SymbolPair("\"", "\""))
            putPair('\'', SymbolPairMatch.SymbolPair("'", "'"))
            putPair('`', SymbolPairMatch.SymbolPair("`", "`"))
        }

        fun keywordsFor(language: String): Set<String> = when (language) {
            "js", "jsx", "ts", "tsx" -> setOf(
                "const", "let", "var", "function", "return", "if", "else", "for", "while", "class",
                "extends", "new", "async", "await", "import", "export", "from", "default", "type",
                "interface", "enum", "implements", "public", "private", "protected", "readonly",
                "as", "typeof", "instanceof", "null", "undefined", "true", "false", "this", "super",
                "try", "catch", "finally", "throw", "switch", "case", "break", "continue", "do",
                "delete", "in", "of", "void", "yield", "static", "get", "set", "declare", "namespace",
                "satisfies", "keyof", "infer", "abstract", "override"
            )
            "python" -> setOf(
                "def", "class", "return", "if", "elif", "else", "for", "while", "import", "from",
                "as", "pass", "break", "continue", "with", "try", "except", "finally", "raise",
                "lambda", "None", "True", "False", "and", "or", "not", "in", "is", "global",
                "nonlocal", "yield", "async", "await", "del", "assert", "self", "print"
            )
            "kotlin" -> setOf(
                "fun", "val", "var", "class", "object", "interface", "return", "if", "else", "for",
                "while", "when", "import", "package", "as", "is", "in", "out", "null", "true",
                "false", "this", "super", "try", "catch", "finally", "throw", "override", "private",
                "public", "internal", "protected", "suspend", "data", "sealed", "enum", "companion",
                "init", "constructor", "by", "lateinit", "typealias", "operator", "inline", "reified"
            )
            "java" -> setOf(
                "public", "private", "protected", "class", "interface", "extends", "implements",
                "static", "final", "void", "int", "long", "float", "double", "boolean", "char",
                "byte", "short", "new", "return", "if", "else", "for", "while", "do", "switch",
                "case", "break", "continue", "try", "catch", "finally", "throw", "throws", "import",
                "package", "this", "super", "null", "true", "false", "abstract", "synchronized"
            )
            "c" -> setOf(
                "int", "char", "float", "double", "void", "long", "short", "unsigned", "signed",
                "struct", "union", "enum", "typedef", "static", "extern", "const", "volatile",
                "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue",
                "sizeof", "goto", "default", "NULL", "include", "define", "class", "public", "private",
                "namespace", "template", "fn", "let", "mut", "func", "package", "import"
            )
            "bash" -> setOf(
                "if", "then", "else", "elif", "fi", "for", "while", "do", "done", "case", "esac",
                "function", "return", "in", "echo", "export", "local", "read", "cd", "exit", "set",
                "unset", "shift", "source", "ls", "cat", "grep", "sed", "awk"
            )
            "css" -> setOf("important", "inherit", "initial", "none", "auto", "solid", "flex", "grid", "block", "inline")
            "json" -> setOf("true", "false", "null")
            else -> emptySet()
        }
    }
}
