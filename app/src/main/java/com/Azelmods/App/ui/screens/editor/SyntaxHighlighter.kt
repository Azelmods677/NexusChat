package com.Azelmods.App.ui.screens.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Resaltado de sintaxis para el editor de código.
 *
 * Se implementa como [VisualTransformation] porque SOLO añade estilos: no inserta ni
 * borra un solo carácter. Por eso [OffsetMapping.Identity] es correcto y el cursor
 * nunca se desalinea del texto — que es el fallo clásico de los editores que
 * reescriben el contenido para colorearlo.
 */

/** Paleta de tokens, en el registro oscuro del Nexus Design System. */
object CodeColors {
    val Plain = Color(0xFFD6DEEB)
    val Keyword = Color(0xFFC792EA)
    val StringLit = Color(0xFFC3E88D)
    val Comment = Color(0xFF637777)
    val Number = Color(0xFFF78C6C)
    val Tag = Color(0xFF82AAFF)
    val Attribute = Color(0xFFFFCB6B)
    val Property = Color(0xFF80CBC4)
}

private data class Span(val start: Int, val end: Int, val color: Color, val italic: Boolean = false)

// Palabras clave por familia de lenguaje.
private val JS_TS_KEYWORDS = setOf(
    "const", "let", "var", "function", "return", "if", "else", "for", "while", "class",
    "extends", "new", "async", "await", "import", "export", "from", "default", "type",
    "interface", "enum", "implements", "public", "private", "protected", "readonly",
    "as", "typeof", "instanceof", "null", "undefined", "true", "false", "this", "super",
    "try", "catch", "finally", "throw", "switch", "case", "break", "continue", "do",
    "delete", "in", "of", "void", "yield", "static", "get", "set", "declare", "namespace",
    "satisfies", "keyof", "infer", "abstract", "override"
)

private val PYTHON_KEYWORDS = setOf(
    "def", "class", "return", "if", "elif", "else", "for", "while", "import", "from",
    "as", "pass", "break", "continue", "with", "try", "except", "finally", "raise",
    "lambda", "None", "True", "False", "and", "or", "not", "in", "is", "global",
    "nonlocal", "yield", "async", "await", "del", "assert", "self"
)

private val KOTLIN_KEYWORDS = setOf(
    "fun", "val", "var", "class", "object", "interface", "return", "if", "else", "for",
    "while", "when", "import", "package", "as", "is", "in", "out", "null", "true",
    "false", "this", "super", "try", "catch", "finally", "throw", "override", "private",
    "public", "internal", "protected", "suspend", "data", "sealed", "enum", "companion",
    "init", "constructor", "by", "lateinit", "typealias", "operator", "inline", "reified"
)

private val C_KEYWORDS = setOf(
    "int", "char", "float", "double", "void", "long", "short", "unsigned", "signed",
    "struct", "union", "enum", "typedef", "static", "extern", "const", "volatile",
    "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue",
    "sizeof", "goto", "default", "NULL"
)

private val BASH_KEYWORDS = setOf(
    "if", "then", "else", "elif", "fi", "for", "while", "do", "done", "case", "esac",
    "function", "return", "in", "echo", "export", "local", "read", "cd", "exit"
)

private fun keywordsFor(lang: String): Set<String> = when (lang) {
    "js", "jsx", "ts", "tsx", "javascript", "typescript", "json" -> JS_TS_KEYWORDS
    "python", "py" -> PYTHON_KEYWORDS
    "kotlin", "kt" -> KOTLIN_KEYWORDS
    "c", "cpp", "c++" -> C_KEYWORDS
    "bash", "sh", "shell" -> BASH_KEYWORDS
    else -> emptySet()
}

private val WORD = Regex("[A-Za-z_$][A-Za-z0-9_$]*")
private val NUMBER = Regex("\\b\\d+(\\.\\d+)?\\b")
private val DQ_STRING = Regex("\"(\\\\.|[^\"\\\\\\n])*\"?")
private val SQ_STRING = Regex("'(\\\\.|[^'\\\\\\n])*'?")
private val TEMPLATE = Regex("`(\\\\.|[^`\\\\])*`?")
private val HTML_TAG = Regex("</?\\s*[A-Za-z][\\w:-]*")
private val HTML_ATTR = Regex("[A-Za-z_:][\\w:.-]*(?=\\s*=)")
private val CSS_PROPERTY = Regex("[-A-Za-z]+(?=\\s*:)")
private val CSS_AT_RULE = Regex("@[A-Za-z-]+")

/**
 * Construye el texto coloreado. Las cadenas y los comentarios se detectan PRIMERO y
 * bloquean el resto de reglas: si no, un `// const x` se pintaría con la palabra
 * clave resaltada dentro del comentario.
 */
fun highlightCode(code: String, language: String): AnnotatedString {
    if (code.isEmpty()) return AnnotatedString("")

    val lang = language.lowercase()
    val protectedRanges = mutableListOf<Span>()

    val commentPatterns: List<Regex> = when (lang) {
        "python", "py", "bash", "sh", "shell", "yaml", "yml" -> listOf(Regex("#[^\\n]*"))
        "html", "xml" -> listOf(Regex("<!--[\\s\\S]*?-->"))
        "css" -> listOf(Regex("/\\*[\\s\\S]*?\\*/"))
        "json" -> emptyList()
        else -> listOf(Regex("//[^\\n]*"), Regex("/\\*[\\s\\S]*?\\*/"))
    }
    commentPatterns.forEach { rx ->
        rx.findAll(code).forEach {
            protectedRanges += Span(it.range.first, it.range.last + 1, CodeColors.Comment, italic = true)
        }
    }

    val stringPatterns = buildList {
        add(DQ_STRING)
        if (lang != "json") add(SQ_STRING)
        if (lang in setOf("js", "jsx", "ts", "tsx", "javascript", "typescript")) add(TEMPLATE)
    }
    stringPatterns.forEach { rx ->
        rx.findAll(code).forEach { m ->
            val s = m.range.first
            val e = m.range.last + 1
            if (protectedRanges.none { s < it.end && it.start < e }) {
                protectedRanges += Span(s, e, CodeColors.StringLit)
            }
        }
    }

    fun free(s: Int, e: Int) = protectedRanges.none { s < it.end && it.start < e }

    val spans = mutableListOf<Span>()
    spans += protectedRanges

    when (lang) {
        "html", "xml" -> {
            HTML_TAG.findAll(code).forEach {
                if (free(it.range.first, it.range.last + 1)) {
                    spans += Span(it.range.first, it.range.last + 1, CodeColors.Tag)
                }
            }
            HTML_ATTR.findAll(code).forEach {
                if (free(it.range.first, it.range.last + 1)) {
                    spans += Span(it.range.first, it.range.last + 1, CodeColors.Attribute)
                }
            }
        }
        "css" -> {
            CSS_AT_RULE.findAll(code).forEach {
                if (free(it.range.first, it.range.last + 1)) {
                    spans += Span(it.range.first, it.range.last + 1, CodeColors.Keyword)
                }
            }
            CSS_PROPERTY.findAll(code).forEach {
                if (free(it.range.first, it.range.last + 1)) {
                    spans += Span(it.range.first, it.range.last + 1, CodeColors.Property)
                }
            }
        }
        else -> {
            val keywords = keywordsFor(lang)
            if (keywords.isNotEmpty()) {
                WORD.findAll(code).forEach { m ->
                    if (m.value in keywords && free(m.range.first, m.range.last + 1)) {
                        spans += Span(m.range.first, m.range.last + 1, CodeColors.Keyword)
                    }
                }
            }
            // JSX/TSX: los componentes en PascalCase se leen como etiquetas.
            if (lang in setOf("jsx", "tsx")) {
                Regex("<\\s*/?\\s*[A-Z][A-Za-z0-9_]*").findAll(code).forEach {
                    if (free(it.range.first, it.range.last + 1)) {
                        spans += Span(it.range.first, it.range.last + 1, CodeColors.Tag)
                    }
                }
            }
        }
    }

    NUMBER.findAll(code).forEach {
        if (free(it.range.first, it.range.last + 1)) {
            spans += Span(it.range.first, it.range.last + 1, CodeColors.Number)
        }
    }

    return AnnotatedString.Builder(code).apply {
        addStyle(SpanStyle(color = CodeColors.Plain), 0, code.length)
        spans.forEach { span ->
            val end = span.end.coerceAtMost(code.length)
            if (span.start in 0 until end) {
                addStyle(
                    SpanStyle(
                        color = span.color,
                        fontStyle = if (span.italic) FontStyle.Italic else null
                    ),
                    span.start,
                    end
                )
            }
        }
    }.toAnnotatedString()
}

/**
 * [VisualTransformation] lista para pasar a un `BasicTextField`.
 * Recuerda: sin cambios de longitud → [OffsetMapping.Identity] es seguro.
 */
class CodeSyntaxTransformation(private val language: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(highlightCode(text.text, language), OffsetMapping.Identity)
}
