package com.Azelmods.App.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
// withStyle es una funcion de extension de AnnotatedString.Builder y vive en el
// paquete, no en la clase: sin este import no se resuelve dentro del bloque
// buildAnnotatedString.
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Azelmods.App.ui.theme.NexusTokens

/**
 * Renderiza markdown en Compose sin dependencias externas.
 *
 * Las respuestas de la IA llegan en markdown —encabezados, negritas, listas y
 * bloques de código— pero se pintaban con un `Text` plano, así que el usuario
 * leía los asteriscos, las almohadillas y las comillas invertidas tal cual. Lo
 * único que hacía la pantalla era cambiar toda la burbuja a monoespaciada si el
 * texto contenía ```, con lo que un mensaje con un solo fragmento de código
 * convertía también la explicación en código.
 *
 * Se soporta el subconjunto que un asistente usa de verdad:
 * - encabezados `#`, `##`, `###`
 * - **negrita**, *cursiva*, `código`, ~~tachado~~ y enlaces `[texto](url)`
 * - bloques de código con lenguaje, scroll horizontal y botón de copiar
 * - listas con viñetas y numeradas, citas `>` y separadores `---`
 *
 * No se usa una librería de markdown a propósito: las disponibles para Compose
 * arrastran WebView o un parser completo de CommonMark, y aquí sólo hace falta
 * esto.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = NexusTokens.Color.TextPrimary,
    fontSize: TextUnit = 15.sp,
    lineHeight: TextUnit = 24.sp,
    accent: Color = NexusTokens.Color.Primary
) {
    val blocks = remember(markdown) { parseBlocks(markdown) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(NexusTokens.Space.sm)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> Text(
                    text = inlineMarkdown(block.text, accent),
                    color = color,
                    fontSize = when (block.level) {
                        1 -> fontSize * 1.5f
                        2 -> fontSize * 1.3f
                        else -> fontSize * 1.15f
                    },
                    lineHeight = lineHeight * 1.2f,
                    fontWeight = FontWeight.Bold
                )

                is MarkdownBlock.Paragraph -> Text(
                    text = inlineMarkdown(block.text, accent),
                    color = color,
                    fontSize = fontSize,
                    lineHeight = lineHeight
                )

                is MarkdownBlock.Bullet -> MarkdownListRow(
                    marker = "•",
                    text = block.text,
                    indent = block.indent,
                    color = color,
                    accent = accent,
                    fontSize = fontSize,
                    lineHeight = lineHeight
                )

                is MarkdownBlock.Numbered -> MarkdownListRow(
                    marker = "${block.number}.",
                    text = block.text,
                    indent = block.indent,
                    color = color,
                    accent = accent,
                    fontSize = fontSize,
                    lineHeight = lineHeight
                )

                is MarkdownBlock.Quote -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NexusTokens.Space.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .heightIn(min = lineHeight.value.dp)
                            .clip(RoundedCornerShape(NexusTokens.Radius.xs))
                            .background(accent.copy(alpha = 0.6f))
                    )
                    Text(
                        text = inlineMarkdown(block.text, accent),
                        color = color.copy(alpha = 0.8f),
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        fontStyle = FontStyle.Italic
                    )
                }

                MarkdownBlock.Divider -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(NexusTokens.Color.GlassBorder)
                )

                is MarkdownBlock.Code -> MarkdownCodeBlock(
                    code = block.code,
                    language = block.language,
                    accent = accent
                )
            }
        }
    }
}

@Composable
private fun MarkdownListRow(
    marker: String,
    text: String,
    indent: Int,
    color: Color,
    accent: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 16).dp),
        horizontalArrangement = Arrangement.spacedBy(NexusTokens.Space.sm)
    ) {
        Text(
            text = marker,
            color = accent,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = inlineMarkdown(text, accent),
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight
        )
    }
}

/**
 * Bloque de código con etiqueta de lenguaje y botón de copiar.
 *
 * El scroll horizontal es deliberado: partir una línea de código por la mitad la
 * vuelve ilegible, y en un móvil casi cualquier línea real se sale del ancho.
 */
@Composable
private fun MarkdownCodeBlock(
    code: String,
    language: String?,
    accent: Color
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(code) { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1_800)
            copied = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusTokens.Radius.md))
            .background(NexusTokens.Color.BgDeep)
            .border(1.dp, NexusTokens.Color.GlassBorder, RoundedCornerShape(NexusTokens.Radius.md))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NexusTokens.Color.GlassFill)
                .padding(start = NexusTokens.Space.md, end = NexusTokens.Space.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language?.takeIf { it.isNotBlank() } ?: "código",
                color = accent,
                fontSize = NexusTokens.FontSize.xs,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(code))
                    copied = true
                }
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = if (copied) "Copiado" else "Copiar código",
                    tint = if (copied) NexusTokens.Color.Success else NexusTokens.Color.TextSecondary,
                    modifier = Modifier.size(NexusTokens.IconSize.sm)
                )
            }
        }

        // Sin SelectionContainer propio: quien llama envuelve todo el markdown en
        // uno, y anidarlos lanza IllegalStateException en Compose.
        Text(
            text = code,
            color = NexusTokens.Color.TextPrimary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(NexusTokens.Space.md)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Análisis
// ─────────────────────────────────────────────────────────────────────────────

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Bullet(val text: String, val indent: Int) : MarkdownBlock
    data class Numbered(val text: String, val number: Int, val indent: Int) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class Code(val code: String, val language: String?) : MarkdownBlock
    data object Divider : MarkdownBlock
}

private val BULLET_REGEX = Regex("""^(\s*)[-*•]\s+(.*)$""")
private val NUMBERED_REGEX = Regex("""^(\s*)(\d{1,3})[.)]\s+(.*)$""")
private val HEADING_REGEX = Regex("""^(#{1,6})\s+(.*)$""")
private val FENCE_REGEX = Regex("""^\s*```\s*([A-Za-z0-9+#_-]*)\s*$""")

/**
 * Divide el texto en bloques. Los párrafos consecutivos se unen para que un
 * salto de línea simple no rompa la frase, igual que en markdown estándar.
 */
private fun parseBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.replace("\r\n", "\n").split("\n")
    val paragraph = StringBuilder()

    fun flushParagraph() {
        if (paragraph.isNotBlank()) blocks += MarkdownBlock.Paragraph(paragraph.toString().trim())
        paragraph.clear()
    }

    var index = 0
    while (index < lines.size) {
        val line = lines[index]

        val fence = FENCE_REGEX.find(line)
        if (fence != null) {
            flushParagraph()
            val language = fence.groupValues[1].takeIf { it.isNotBlank() }
            val code = StringBuilder()
            index++
            while (index < lines.size && FENCE_REGEX.find(lines[index]) == null) {
                code.appendLine(lines[index])
                index++
            }
            // Si el modelo corta la respuesta a medias puede faltar el cierre;
            // se acepta igual en vez de tragarse el resto del mensaje.
            index++
            blocks += MarkdownBlock.Code(code.toString().trimEnd('\n'), language)
            continue
        }

        when {
            line.isBlank() -> flushParagraph()

            line.trim().matches(Regex("""^([-*_])\1{2,}$""")) -> {
                flushParagraph()
                blocks += MarkdownBlock.Divider
            }

            HEADING_REGEX.matches(line) -> {
                flushParagraph()
                val (hashes, text) = HEADING_REGEX.find(line)!!.destructured
                blocks += MarkdownBlock.Heading(hashes.length, text.trim())
            }

            line.trimStart().startsWith("> ") -> {
                flushParagraph()
                blocks += MarkdownBlock.Quote(line.trimStart().removePrefix("> ").trim())
            }

            BULLET_REGEX.matches(line) -> {
                flushParagraph()
                val (spaces, text) = BULLET_REGEX.find(line)!!.destructured
                blocks += MarkdownBlock.Bullet(text.trim(), spaces.length / 2)
            }

            NUMBERED_REGEX.matches(line) -> {
                flushParagraph()
                val (spaces, number, text) = NUMBERED_REGEX.find(line)!!.destructured
                blocks += MarkdownBlock.Numbered(text.trim(), number.toIntOrNull() ?: 1, spaces.length / 2)
            }

            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(line.trim())
            }
        }
        index++
    }
    flushParagraph()
    return blocks
}

/**
 * Aplica los estilos de línea (negrita, cursiva, código, tachado, enlaces).
 *
 * Se recorre el texto una sola vez en lugar de encadenar reemplazos con regex:
 * los reemplazos encadenados se pisan entre sí cuando un estilo contiene a otro
 * (`**texto con `código` dentro**`) y descuadran los índices.
 */
private fun inlineMarkdown(text: String, accent: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end == -1) { append(text[i]); i++ }
                else {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                }
            }

            text.startsWith("~~", i) -> {
                val end = text.indexOf("~~", i + 2)
                if (end == -1) { append(text[i]); i++ }
                else {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                }
            }

            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end == -1) { append(text[i]); i++ }
                else {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = NexusTokens.Color.GlassFill,
                            color = accent
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                }
            }

            // Cursiva. Se exige que el carácter siguiente no sea un espacio para
            // no convertir en cursiva una multiplicación ("2 * 3 * 4") ni el
            // guion bajo de un identificador (snake_case).
            (text[i] == '*' || text[i] == '_') &&
                i + 1 < text.length && !text[i + 1].isWhitespace() -> {
                val delimiter = text[i]
                val end = text.indexOf(delimiter, i + 1)
                if (end == -1 || text[end - 1].isWhitespace()) { append(text[i]); i++ }
                else {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                }
            }

            text[i] == '[' -> {
                val closeText = text.indexOf(']', i)
                val openUrl = if (closeText == -1) -1 else closeText + 1
                if (closeText == -1 || openUrl >= text.length || text[openUrl] != '(') {
                    append(text[i]); i++
                } else {
                    val closeUrl = text.indexOf(')', openUrl)
                    if (closeUrl == -1) { append(text[i]); i++ }
                    else {
                        val label = text.substring(i + 1, closeText)
                        val url = text.substring(openUrl + 1, closeUrl)
                        pushStringAnnotation("URL", url)
                        withStyle(
                            SpanStyle(color = accent, textDecoration = TextDecoration.Underline)
                        ) {
                            append(label)
                        }
                        pop()
                        i = closeUrl + 1
                    }
                }
            }

            else -> { append(text[i]); i++ }
        }
    }
}
