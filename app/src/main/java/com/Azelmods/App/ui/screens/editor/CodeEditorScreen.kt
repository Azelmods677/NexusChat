package com.Azelmods.App.ui.screens.editor

import com.Azelmods.App.i18n.tr

import android.graphics.Typeface
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.components.NexusDivider
import com.Azelmods.App.ui.components.NexusEmptyState
import com.Azelmods.App.ui.components.NexusListRow
import com.Azelmods.App.ui.components.NexusTopBar
import com.Azelmods.App.ui.theme.NexusTokens
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val EditorSymbols = listOf(
    "Tab", "{", "}", "(", ")", "[", "]", ";", "=", "\"", "'", "<", ">", "/", "\\", ".", ",", ":", "_", "-", "+", "*", "&", "|", "!", "#", "$", "%", "?"
)

/**
 * Editor basado en Sora Editor: numeración de líneas, deshacer ilimitado, autocompletado,
 * buscar y reemplazar, pestañas y guardado automático.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    navController: NavController,
    viewModel: CodeEditorViewModel = hiltViewModel()
) {
    val files by viewModel.files.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val currentFile by viewModel.currentFile.collectAsState()
    val unsaved by viewModel.unsaved.collectAsState()
    val output by viewModel.output.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val jsToExecute by viewModel.jsToExecute.collectAsState()
    val htmlToPreview by viewModel.htmlToPreview.collectAsState()

    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary.toArgb()
    val editorRef = remember { arrayOfNulls<CodeEditor>(1) }

    var showNewFile by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var wordWrap by remember { mutableStateOf(false) }
    var fontSize by remember { mutableFloatStateOf(14f) }
    var cursorLine by remember { mutableIntStateOf(1) }
    var cursorColumn by remember { mutableIntStateOf(1) }
    var lineCount by remember { mutableIntStateOf(1) }
    var outputExpanded by remember { mutableStateOf(true) }
    var confirmDelete by remember { mutableStateOf<CodeFile?>(null) }

    fun currentText(): String? = editorRef[0]?.text?.toString()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importDocument)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val text = currentText()
        if (uri != null && text != null) viewModel.exportTo(uri, text)
    }

    // Motor de JavaScript: un WebView invisible que captura la consola.
    var jsOutput by remember { mutableStateOf("") }
    var jsError by remember { mutableStateOf<String?>(null) }
    val jsEngine = remember { arrayOfNulls<WebView>(1) }
    DisposableEffect(Unit) {
        val web = WebView(context).apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            addJavascriptInterface(object {
                @JavascriptInterface
                fun log(message: String) {
                    jsOutput += message + "\n"
                }

                @JavascriptInterface
                fun error(message: String) {
                    jsError = message
                }
            }, "AndroidBridge")
            loadDataWithBaseURL(
                null,
                """<!DOCTYPE html><html><body><script>
                    const fmt = a => a.map(x => { try { return typeof x === 'object' ? JSON.stringify(x, null, 2) : String(x) } catch (e) { return String(x) } }).join(' ');
                    console.log = (...a) => AndroidBridge.log(fmt(a));
                    console.info = console.log;
                    console.warn = (...a) => AndroidBridge.log('[aviso] ' + fmt(a));
                    console.error = (...a) => AndroidBridge.log('[error] ' + fmt(a));
                    window.onerror = (m, u, l) => { AndroidBridge.error(m + ' (línea ' + l + ')'); return true; };
                </script></body></html>""",
                "text/html",
                "UTF-8",
                null
            )
        }
        jsEngine[0] = web
        onDispose {
            web.destroy()
            jsEngine[0] = null
        }
    }

    LaunchedEffect(jsToExecute) {
        val code = jsToExecute ?: return@LaunchedEffect
        jsOutput = ""
        jsError = null
        delay(150)
        val wrapped = "(function(){try{\n$code\n}catch(e){AndroidBridge.error(e.name+': '+e.message)}})();void 0"
        jsEngine[0]?.evaluateJavascript(wrapped) {
            viewModel.onJsResult(jsOutput, jsError)
        }
    }

    // Vuelve a la lista de archivos con la tecla atrás, guardando antes.
    BackHandler(enabled = currentFile != null) {
        currentFile?.let { file -> currentText()?.let { viewModel.saveFile(file.id, it) } }
        viewModel.closeAllTabs()
    }

    if (currentFile == null) {
        FileList(
            files = files,
            onBack = { navController.popBackStack() },
            onNew = { showNewFile = true },
            onImport = { importLauncher.launch(arrayOf("*/*")) },
            onOpen = viewModel::openFile,
            onDelete = { confirmDelete = it }
        )
    } else {
        val file = currentFile!!
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = (if (file.id in unsaved) "• " else "") + file.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            currentText()?.let { viewModel.saveFile(file.id, it) }
                            viewModel.closeAllTabs()
                        }) {
                            Icon(Icons.Default.FolderOpen, "Archivos")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = !showSearch }) { Icon(Icons.Default.Search, "Buscar") }
                        IconButton(
                            onClick = {
                                if (isRunning) {
                                    viewModel.stopExecution()
                                } else {
                                    outputExpanded = true
                                    currentText()?.let { text ->
                                        viewModel.saveFile(file.id, text)
                                        viewModel.executeCode(text, file.language)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                if (isRunning) tr("Detener") else tr("Ejecutar"),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Más opciones") }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                containerColor = NexusTokens.Color.BgElevated
                            ) {
                                EditorMenuItem("Nuevo archivo", Icons.Default.Add) {
                                    showMenu = false
                                    showNewFile = true
                                }
                                EditorMenuItem("Abrir del teléfono", Icons.Outlined.FileOpen) {
                                    showMenu = false
                                    importLauncher.launch(arrayOf("*/*"))
                                }
                                EditorMenuItem("Guardar en el teléfono", Icons.Outlined.SaveAlt) {
                                    showMenu = false
                                    exportLauncher.launch(file.name)
                                }
                                EditorMenuItem("Renombrar", Icons.Outlined.DriveFileRenameOutline) {
                                    showMenu = false
                                    showRename = true
                                }
                                EditorMenuItem(if (wordWrap) tr("Sin ajuste de línea") else tr("Ajuste de línea"), Icons.AutoMirrored.Filled.WrapText) {
                                    showMenu = false
                                    wordWrap = !wordWrap
                                }
                                EditorMenuItem("Letra más grande", Icons.Outlined.TextIncrease) {
                                    fontSize = (fontSize + 1f).coerceAtMost(28f)
                                }
                                EditorMenuItem("Letra más pequeña", Icons.Outlined.TextDecrease) {
                                    fontSize = (fontSize - 1f).coerceAtLeast(9f)
                                }
                                EditorMenuItem("Eliminar archivo", Icons.Outlined.DeleteOutline) {
                                    showMenu = false
                                    confirmDelete = file
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NexusTokens.Color.BgSurface,
                        titleContentColor = NexusTokens.Color.TextPrimary,
                        navigationIconContentColor = NexusTokens.Color.TextSecondary,
                        actionIconContentColor = NexusTokens.Color.TextSecondary
                    )
                )
            },
            containerColor = NexusTokens.Color.BgBase
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
            ) {
                if (tabs.size > 1) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(NexusTokens.Color.BgSurface)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        tabs.forEach { tab ->
                            val selected = tab.id == file.id
                            Row(
                                modifier = Modifier
                                    .background(if (selected) NexusTokens.Color.BgBase else NexusTokens.Color.BgSurface)
                                    .clickable {
                                        if (!selected) {
                                            currentText()?.let { viewModel.saveFile(file.id, it) }
                                            viewModel.openFile(tab)
                                        }
                                    }
                                    .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    (if (tab.id in unsaved) "• " else "") + tab.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) NexusTokens.Color.TextPrimary else NexusTokens.Color.TextMuted,
                                    maxLines = 1
                                )
                                Icon(
                                    Icons.Default.Close,
                                    "Cerrar pestaña",
                                    tint = NexusTokens.Color.TextMuted,
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .size(16.dp)
                                        .clickable {
                                            viewModel.closeTab(tab.id, if (selected) currentText() else null)
                                        }
                                )
                            }
                        }
                    }
                }

                if (showSearch) {
                    SearchBar(
                        editor = { editorRef[0] },
                        onClose = {
                            editorRef[0]?.searcher?.stopSearch()
                            showSearch = false
                        }
                    )
                }

                key(file.id) {
                    AndroidView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        factory = { ctx ->
                            CodeEditor(ctx).apply {
                                editorRef[0] = this
                                colorScheme = nexusEditorScheme(accent)
                                typefaceText = Typeface.MONOSPACE
                                typefaceLineNumber = Typeface.MONOSPACE
                                setTextSize(fontSize)
                                setTabWidth(4)
                                isLineNumberEnabled = true
                                setPinLineNumber(true)
                                val density = resources.displayMetrics.density
                                lineNumberMarginLeft = 8 * density
                                setDividerMargin(8 * density, 6 * density)
                                setEditorLanguage(NexusCodeLanguage(file.language))
                                setText(file.content)
                                lineCount = text.lineCount
                                subscribeAlways(ContentChangeEvent::class.java) { event ->
                                    lineCount = event.editor.text.lineCount
                                    if (event.action != ContentChangeEvent.ACTION_SET_NEW_TEXT) {
                                        viewModel.onContentChanged(file.id, event.editor.text.toString())
                                    }
                                }
                                subscribeAlways(SelectionChangeEvent::class.java) { event ->
                                    cursorLine = event.left.line + 1
                                    cursorColumn = event.left.column + 1
                                }
                            }
                        },
                        update = { editor ->
                            editor.setTextSize(fontSize)
                            editor.isWordwrap = wordWrap
                        },
                        onRelease = { editor ->
                            // Al cambiar de pestaña el editor nuevo ya se creó: no hay que
                            // borrar su referencia al liberar el anterior.
                            if (editorRef[0] === editor) editorRef[0] = null
                            editor.release()
                        }
                    )
                }

                Text(
                    "Ln $cursorLine, Col $cursorColumn · ${CodeLanguages.label(file.language)} · $lineCount líneas",
                    style = MaterialTheme.typography.labelSmall,
                    color = NexusTokens.Color.TextMuted,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexusTokens.Color.BgSurface)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )

                // Deshacer, rehacer y los símbolos que cuestan en el teclado del móvil.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(NexusTokens.Color.BgSurface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorToolKey(Icons.AutoMirrored.Filled.Undo, "Deshacer") { editorRef[0]?.undo() }
                    EditorToolKey(Icons.AutoMirrored.Filled.Redo, "Rehacer") { editorRef[0]?.redo() }
                    EditorSymbols.forEach { symbol ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(NexusTokens.Radius.xs))
                                .clickable {
                                    val insert = if (symbol == "Tab") "    " else symbol
                                    editorRef[0]?.insertText(insert, insert.length)
                                }
                                .defaultMinSize(minWidth = 34.dp, minHeight = 34.dp)
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                symbol,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = NexusTokens.Color.TextSecondary
                            )
                        }
                    }
                }

                if (output.isNotEmpty() || isRunning) {
                    NexusDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(NexusTokens.Color.BgSurface)
                            .clickable { outputExpanded = !outputExpanded }
                            .padding(start = 12.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isRunning) tr("Ejecutando…") else tr("Salida"),
                            style = MaterialTheme.typography.labelMedium,
                            color = NexusTokens.Color.TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (outputExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            if (outputExpanded) tr("Ocultar salida") else tr("Mostrar salida"),
                            tint = NexusTokens.Color.TextMuted,
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }
                    if (outputExpanded) {
                        SelectionContainer {
                            Text(
                                text = output,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = NexusTokens.Color.TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                                    .background(NexusTokens.Color.BgDeep)
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewFile) {
        NameDialog(
            title = tr("Nuevo archivo"),
            initial = "",
            hint = tr("main.py, index.html, script.sh…"),
            confirm = "Crear",
            onDismiss = { showNewFile = false },
            onConfirm = {
                currentFile?.let { file -> currentText()?.let { text -> viewModel.saveFile(file.id, text) } }
                viewModel.newFile(it)
                showNewFile = false
            }
        )
    }

    if (showRename) {
        currentFile?.let { file ->
            NameDialog(
                title = tr("Renombrar"),
                initial = file.name,
                hint = file.name,
                confirm = "Renombrar",
                onDismiss = { showRename = false },
                onConfirm = {
                    viewModel.renameFile(file.id, it)
                    showRename = false
                    editorRef[0]?.setEditorLanguage(NexusCodeLanguage(CodeLanguages.fromFileName(it)))
                }
            )
        }
    }

    confirmDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(tr("¿Eliminar %s?", target.name)) },
            text = { Text(tr("No se puede deshacer."), color = NexusTokens.Color.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFile(target)
                    confirmDelete = null
                }) { Text(tr("Eliminar"), color = NexusTokens.Color.Error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(tr("Cancelar")) } },
            containerColor = NexusTokens.Color.BgSurface
        )
    }

    htmlToPreview?.let { html ->
        Dialog(
            onDismissRequest = { viewModel.clearHtmlPreview() },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(NexusTokens.Color.BgBase)
                    .statusBarsPadding()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(NexusTokens.Color.BgSurface)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr("Vista previa"),
                        style = MaterialTheme.typography.titleMedium,
                        color = NexusTokens.Color.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearHtmlPreview() }) {
                        Icon(Icons.Default.Close, "Cerrar", tint = NexusTokens.Color.TextPrimary)
                    }
                }
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                        }
                    },
                    update = { it.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun EditorToolKey(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(NexusTokens.Radius.xs))
            .clickable(onClick = onClick)
            .defaultMinSize(minWidth = 38.dp, minHeight = 34.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = NexusTokens.Color.TextSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EditorMenuItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = NexusTokens.Color.TextPrimary) },
        leadingIcon = { Icon(icon, null, tint = NexusTokens.Color.TextSecondary) },
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileList(
    files: List<CodeFile>,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onImport: () -> Unit,
    onOpen: (CodeFile) -> Unit,
    onDelete: (CodeFile) -> Unit
) {
    Scaffold(
        topBar = {
            NexusTopBar(
                title = tr("Editor de código"),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onImport) { Icon(Icons.Outlined.FileOpen, "Abrir del teléfono") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNew,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(2.dp, 2.dp, 2.dp, 2.dp)
            ) {
                Icon(Icons.Default.Add, "Nuevo archivo")
            }
        },
        containerColor = NexusTokens.Color.BgBase
    ) { padding ->
        if (files.isEmpty()) {
            NexusEmptyState(
                icon = Icons.Outlined.Code,
                title = tr("Sin archivos"),
                body = "Crea uno nuevo o abre uno del teléfono. Se guardan en tu cuenta.",
                actionLabel = tr("Nuevo archivo"),
                onAction = onNew,
                modifier = Modifier.padding(padding)
            )
        } else {
            val dateFormat = remember { SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag("es")) }
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(files, key = { it.id }) { file ->
                    NexusListRow(
                        title = file.name,
                        subtitle = "${CodeLanguages.label(file.language)} · ${formatSize(file.size)} · ${dateFormat.format(Date(file.timestamp))}",
                        icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                        singleLineSubtitle = true,
                        onClick = { onOpen(file) },
                        trailing = {
                            IconButton(onClick = { onDelete(file) }) {
                                Icon(Icons.Outlined.DeleteOutline, "Eliminar", tint = NexusTokens.Color.TextMuted)
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }
}

private fun formatSize(bytes: Long): String =
    if (bytes < 1024) "$bytes B" else "%.1f KB".format(bytes / 1024.0)

@Composable
private fun SearchBar(editor: () -> CodeEditor?, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var ignoreCase by remember { mutableStateOf(true) }
    var useRegex by remember { mutableStateOf(false) }
    var showReplace by remember { mutableStateOf(false) }
    var matches by remember { mutableIntStateOf(0) }
    var regexError by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }

    LaunchedEffect(query, ignoreCase, useRegex) {
        val searcher = editor()?.searcher ?: return@LaunchedEffect
        regexError = false
        if (query.isEmpty()) {
            runCatching { searcher.stopSearch() }
            matches = 0
            return@LaunchedEffect
        }
        runCatching {
            val type = if (useRegex) EditorSearcher.SearchOptions.TYPE_REGULAR_EXPRESSION else EditorSearcher.SearchOptions.TYPE_NORMAL
            searcher.search(query, EditorSearcher.SearchOptions(type, ignoreCase))
        }.onFailure { regexError = true }
        // El recuento se calcula en segundo plano.
        delay(200)
        matches = runCatching { searcher.matchedPositionCount }.getOrDefault(0)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(NexusTokens.Color.BgSurface)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EditorField(
                value = query,
                onValueChange = { query = it },
                placeholder = tr("Buscar"),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus)
            )
            Text(
                text = if (regexError) tr("Regex no válida") else if (query.isEmpty()) "" else matches.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (regexError) NexusTokens.Color.Error else NexusTokens.Color.TextMuted,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            IconButton(onClick = { runCatching { editor()?.searcher?.gotoPrevious() } }) {
                Icon(Icons.Default.KeyboardArrowUp, "Anterior", tint = NexusTokens.Color.TextSecondary)
            }
            IconButton(onClick = { runCatching { editor()?.searcher?.gotoNext() } }) {
                Icon(Icons.Default.KeyboardArrowDown, "Siguiente", tint = NexusTokens.Color.TextSecondary)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Cerrar búsqueda", tint = NexusTokens.Color.TextSecondary)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            FilterChip(selected = !ignoreCase, onClick = { ignoreCase = !ignoreCase }, label = { Text(tr("Aa")) })
            FilterChip(selected = useRegex, onClick = { useRegex = !useRegex }, label = { Text(tr("Regex")) })
            FilterChip(selected = showReplace, onClick = { showReplace = !showReplace }, label = { Text(tr("Reemplazar")) })
        }
        if (showReplace) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EditorField(
                    value = replacement,
                    onValueChange = { replacement = it },
                    placeholder = tr("Reemplazar por"),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { runCatching { editor()?.searcher?.replaceThis(replacement) } }) { Text(tr("Uno")) }
                TextButton(onClick = { runCatching { editor()?.searcher?.replaceAll(replacement) } }) { Text(tr("Todos")) }
            }
        }
    }
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = NexusTokens.Color.TextPrimary,
            fontFamily = FontFamily.Monospace
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .clip(RoundedCornerShape(NexusTokens.Radius.sm))
            .background(NexusTokens.Color.BgElevated)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = NexusTokens.Color.TextMuted)
            }
            inner()
        }
    )
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    hint: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(hint) },
                supportingText = { Text(tr("El tipo de archivo se detecta por la extensión.")) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name) }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}

