package com.Azelmods.App.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.Azelmods.App.ui.theme.NexusTokens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Cómo se dibuja cada conversación en la lista. */
enum class ChatRowStyle(val title: String, val hint: String) {
    CLASSIC("Clásica", "Limpia, sin tarjeta"),
    CARD("Tarjeta", "Cada chat en su propia tarjeta con sombra"),
    RAIL("Barra lateral", "Tarjeta elevada sobre una franja de color"),
    LINE("Línea", "Una línea de color entre el nombre y el mensaje"),
    FRAME("Relieve 3D", "Bisel de color y la foto hundida en la tarjeta"),
    OUTLINE("Borde", "Solo un borde de color, sin relleno"),
    DEPTH("Tarjeta 3D", "Con canto real: se inclina al tocarla y al desplazar"),
    NEON("Neón", "Borde luminoso"),
    GLASS("Cristal", "Translúcida: deja ver tu fondo"),
    PHOTO("Foto", "La foto del contacto de fondo")
}

/** De qué color son los detalles de las filas. */
enum class RowTint(val title: String) {
    BLUE("Azul"), APP("Color de la app"), CONTACT("Cada contacto")
}

/** Efecto sobre las fotos de perfil de las listas. */
enum class AvatarEffect(val title: String) {
    AUTO("Según la fila"), NONE("Normal"), INSET("Hundida"), ORB("Esfera")
}

/** Quién habla al otro lado, para la foto dentro de la burbuja. Null donde no se sabe (grupos). */
val LocalBubblePeer = compositionLocalOf<Pair<String, String?>?> { null }

/** En grupos: quién escribió el mensaje (uid, nombre), para ponerlo encima de la burbuja. */
val LocalBubbleSender = compositionLocalOf<Pair<String, String>?> { null }

/** Dentro de una fila de estilo Línea: pinta el trazo entre el nombre y el mensaje. */
val LocalChatRowDivider = compositionLocalOf { false }

/** Cómo se dibuja el campo donde se escribe. */
enum class ChatEntryStyle(val title: String) {
    PILL("Cápsula"),
    DEPTH("3D"),
    OUTLINE("Contorno"),
    LINE("Línea"),
    BLOCK("Bloque"),
    PHOTO("Con tu foto"),
    SOCIAL("Social"),
    FLAT("Plana")
}

/** Qué hay encima de la lista de chats. */
enum class HomeStyle(val title: String, val hint: String) {
    CLASSIC("Clásico", "Título y filtros, sin nada más"),
    PROFILE("Con tu foto", "Tu foto con un aro de color junto al título"),
    NAME("Panel con tu nombre", "Tu foto arriba y un panel redondeado con tu nombre"),
    ARC("Curva", "Una curva con tu foto en el centro"),
    STRIP("Barra de accesos", "Contactos, historias, tu foto y llamadas"),
    CARD("Tarjeta", "Una tarjeta con tu perfil, encima de los chats"),
    COVER("Portada", "Tu portada de fondo con tu foto encima")
}

/** Cómo se presenta la pantalla de Ajustes. */
enum class SettingsStyle(val title: String, val hint: String) {
    PANEL("Panel", "Perfil, batería, dispositivo y hora arriba"),
    GROUPS("Grupos", "Tarjeta de perfil y secciones agrupadas"),
    LIST("Lista", "Filas limpias, sin tarjetas"),
    GRID("Cuadrícula", "Dos columnas de accesos grandes"),
    HEADER("Encabezado", "Título grande, tu foto y una cápsula de accesos"),
    CARDS("Tarjetas grandes", "Tu nombre a la izquierda, la foto a la derecha y accesos grandes")
}

/** Tipografía de toda la app. Son las del sistema: funcionan sin descargar nada. */
enum class AppFontStyle(val title: String, val systemName: String?) {
    SANS("Normal", null),
    LIGHT("Ligera", "sans-serif-light"),
    MEDIUM("Media", "sans-serif-medium"),
    BLACK("Gruesa", "sans-serif-black"),
    CONDENSED("Estrecha", "sans-serif-condensed"),
    SMALLCAPS("Versalitas", "sans-serif-smallcaps"),
    CASUAL("Informal", "casual"),
    CURSIVE("Manuscrita", "cursive"),
    SERIF("Con remates", "serif"),
    MONO("Monoespaciada", "monospace"),
    SERIF_MONO("Máquina de escribir", "serif-monospace")
}

/** Forma de las burbujas de mensaje. */
enum class BubbleStyle(val title: String) {
    SOFT("Suave"), TAIL("Con cola"), PILL("Cápsula"), SQUARE("Recta"),
    OUTLINE("Contorno"), DEPTH("3D"), NEON("Neón"), GLASS("Cristal")
}

/** Cómo se dibujan las marcas de enviado y leído. */
enum class TickStyle(val title: String) {
    CLASSIC("Clásicas"), GLOW("Con brillo"), DOTS("Puntos"), BARS("Barras")
}

/** Barra inferior de la pantalla principal. */
enum class NavBarStyle(val title: String) {
    BAR("Clásica"), FLOATING("Flotante"), ICONS("Solo iconos")
}

/** La elección del usuario. Se aplica al momento en toda la app. */
object ChatLook {
    private const val PREFS = "chat_look"
    private const val KEY_ROW = "row_style"
    private const val KEY_ENTRY = "entry_style"

    private var prefs: SharedPreferences? = null
    private val _rowStyle = MutableStateFlow(ChatRowStyle.CLASSIC)
    private val _entryStyle = MutableStateFlow(ChatEntryStyle.PILL)
    val rowStyle: StateFlow<ChatRowStyle> = _rowStyle.asStateFlow()
    val entryStyle: StateFlow<ChatEntryStyle> = _entryStyle.asStateFlow()

    private val _bubbleStyle = MutableStateFlow(BubbleStyle.SOFT)
    private val _tickStyle = MutableStateFlow(TickStyle.CLASSIC)
    private val _navBarStyle = MutableStateFlow(NavBarStyle.BAR)
    private val _ownBubbleColor = MutableStateFlow<Int?>(null)
    private val _peerBubbleColor = MutableStateFlow<Int?>(null)
    private val _textScale = MutableStateFlow(1f)
    private val _entryHint = MutableStateFlow("")
    private val _homeAvatar = MutableStateFlow(false)
    private val _hideFilters = MutableStateFlow(false)
    private val _wallpaperDim = MutableStateFlow(0.45f)
    private val _homeStyle = MutableStateFlow(HomeStyle.CLASSIC)
    private val _settingsStyle = MutableStateFlow(SettingsStyle.PANEL)
    val homeStyle: StateFlow<HomeStyle> = _homeStyle.asStateFlow()
    val settingsStyle: StateFlow<SettingsStyle> = _settingsStyle.asStateFlow()

    fun setHomeStyle(style: HomeStyle) {
        _homeStyle.value = style
        prefs?.edit()?.putString("home", style.name)?.apply()
    }

    fun setSettingsStyle(style: SettingsStyle) {
        _settingsStyle.value = style
        prefs?.edit()?.putString("settings", style.name)?.apply()
    }

    private val _linkPreviews = MutableStateFlow(true)
    val linkPreviews: StateFlow<Boolean> = _linkPreviews.asStateFlow()

    fun setLinkPreviews(on: Boolean) {
        _linkPreviews.value = on
        prefs?.edit()?.putBoolean("linkpreviews", on)?.apply()
    }

    private val _rowTint = MutableStateFlow(RowTint.BLUE)
    private val _avatarEffect = MutableStateFlow(AvatarEffect.AUTO)
    val rowTint: StateFlow<RowTint> = _rowTint.asStateFlow()
    val avatarEffect: StateFlow<AvatarEffect> = _avatarEffect.asStateFlow()

    fun setRowTint(tint: RowTint) {
        _rowTint.value = tint
        prefs?.edit()?.putString("rowtint", tint.name)?.apply()
    }

    fun setAvatarEffect(effect: AvatarEffect) {
        _avatarEffect.value = effect
        prefs?.edit()?.putString("avatareffect", effect.name)?.apply()
    }

    private val _hideEntryEmoji = MutableStateFlow(false)
    private val _entryTranslate = MutableStateFlow(false)
    private val _bubblePhoto = MutableStateFlow(false)
    val hideEntryEmoji: StateFlow<Boolean> = _hideEntryEmoji.asStateFlow()
    val entryTranslate: StateFlow<Boolean> = _entryTranslate.asStateFlow()
    val bubblePhoto: StateFlow<Boolean> = _bubblePhoto.asStateFlow()

    fun setHideEntryEmoji(on: Boolean) { _hideEntryEmoji.value = on; flag("noemoji", on) }
    fun setEntryTranslate(on: Boolean) { _entryTranslate.value = on; flag("entrytranslate", on) }
    fun setBubblePhoto(on: Boolean) { _bubblePhoto.value = on; flag("bubblephoto", on) }

    private val _hideStoriesTab = MutableStateFlow(false)
    val hideStoriesTab: StateFlow<Boolean> = _hideStoriesTab.asStateFlow()
    fun setHideStoriesTab(on: Boolean) { _hideStoriesTab.value = on; flag("hidestories", on) }

    private val _fontStyle = MutableStateFlow(AppFontStyle.SANS)
    private val _homeSearchBar = MutableStateFlow(false)
    private val _quickBar = MutableStateFlow(false)
    private val _ghostButton = MutableStateFlow(false)
    private val _hideArchived = MutableStateFlow(false)
    private val _confirmSticker = MutableStateFlow(false)
    private val _storyCards = MutableStateFlow(false)
    val fontStyle: StateFlow<AppFontStyle> = _fontStyle.asStateFlow()
    val homeSearchBar: StateFlow<Boolean> = _homeSearchBar.asStateFlow()
    val quickBar: StateFlow<Boolean> = _quickBar.asStateFlow()
    val ghostButton: StateFlow<Boolean> = _ghostButton.asStateFlow()
    val hideArchived: StateFlow<Boolean> = _hideArchived.asStateFlow()
    val confirmSticker: StateFlow<Boolean> = _confirmSticker.asStateFlow()
    val storyCards: StateFlow<Boolean> = _storyCards.asStateFlow()

    private fun flag(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
    }

    fun setFontStyle(style: AppFontStyle) {
        _fontStyle.value = style
        prefs?.edit()?.putString("font", style.name)?.apply()
    }

    fun setHomeSearchBar(on: Boolean) { _homeSearchBar.value = on; flag("searchbar", on) }
    fun setQuickBar(on: Boolean) { _quickBar.value = on; flag("quickbar", on) }
    fun setGhostButton(on: Boolean) { _ghostButton.value = on; flag("ghostbtn", on) }
    fun setHideArchived(on: Boolean) { _hideArchived.value = on; flag("hidearchived", on) }
    fun setConfirmSticker(on: Boolean) { _confirmSticker.value = on; flag("confirmsticker", on) }
    fun setStoryCards(on: Boolean) { _storyCards.value = on; flag("storycards", on) }

    val bubbleStyle: StateFlow<BubbleStyle> = _bubbleStyle.asStateFlow()
    val tickStyle: StateFlow<TickStyle> = _tickStyle.asStateFlow()
    val navBarStyle: StateFlow<NavBarStyle> = _navBarStyle.asStateFlow()
    val ownBubbleColor: StateFlow<Int?> = _ownBubbleColor.asStateFlow()
    val peerBubbleColor: StateFlow<Int?> = _peerBubbleColor.asStateFlow()
    val textScale: StateFlow<Float> = _textScale.asStateFlow()
    val entryHint: StateFlow<String> = _entryHint.asStateFlow()
    val homeAvatar: StateFlow<Boolean> = _homeAvatar.asStateFlow()
    val hideFilters: StateFlow<Boolean> = _hideFilters.asStateFlow()
    val wallpaperDim: StateFlow<Float> = _wallpaperDim.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val stored = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = stored
        _rowStyle.value = ChatRowStyle.entries.firstOrNull { it.name == stored.getString(KEY_ROW, null) } ?: ChatRowStyle.CLASSIC
        _entryStyle.value = ChatEntryStyle.entries.firstOrNull { it.name == stored.getString(KEY_ENTRY, null) } ?: ChatEntryStyle.PILL
        _bubbleStyle.value = BubbleStyle.entries.firstOrNull { it.name == stored.getString("bubble", null) } ?: BubbleStyle.SOFT
        _tickStyle.value = TickStyle.entries.firstOrNull { it.name == stored.getString("tick", null) } ?: TickStyle.CLASSIC
        _navBarStyle.value = NavBarStyle.entries.firstOrNull { it.name == stored.getString("nav", null) } ?: NavBarStyle.BAR
        _ownBubbleColor.value = if (stored.contains("own")) stored.getInt("own", 0) else null
        _peerBubbleColor.value = if (stored.contains("peer")) stored.getInt("peer", 0) else null
        _textScale.value = stored.getFloat("text", 1f)
        _entryHint.value = stored.getString("hint", "").orEmpty()
        _homeAvatar.value = stored.getBoolean("avatar", false)
        _hideFilters.value = stored.getBoolean("filters", false)
        _wallpaperDim.value = stored.getFloat("dim", 0.45f)
        _homeStyle.value = HomeStyle.entries.firstOrNull { it.name == stored.getString("home", null) } ?: HomeStyle.CLASSIC
        _settingsStyle.value = SettingsStyle.entries.firstOrNull { it.name == stored.getString("settings", null) } ?: SettingsStyle.PANEL
        _linkPreviews.value = stored.getBoolean("linkpreviews", true)
        _rowTint.value = RowTint.entries.firstOrNull { it.name == stored.getString("rowtint", null) } ?: RowTint.BLUE
        _avatarEffect.value = AvatarEffect.entries.firstOrNull { it.name == stored.getString("avatareffect", null) } ?: AvatarEffect.AUTO
        _hideEntryEmoji.value = stored.getBoolean("noemoji", false)
        _entryTranslate.value = stored.getBoolean("entrytranslate", false)
        _bubblePhoto.value = stored.getBoolean("bubblephoto", false)
        _hideStoriesTab.value = stored.getBoolean("hidestories", false)
        _fontStyle.value = AppFontStyle.entries.firstOrNull { it.name == stored.getString("font", null) } ?: AppFontStyle.SANS
        _homeSearchBar.value = stored.getBoolean("searchbar", false)
        _quickBar.value = stored.getBoolean("quickbar", false)
        _ghostButton.value = stored.getBoolean("ghostbtn", false)
        _hideArchived.value = stored.getBoolean("hidearchived", false)
        _confirmSticker.value = stored.getBoolean("confirmsticker", false)
        _storyCards.value = stored.getBoolean("storycards", false)
    }

    fun setRowStyle(style: ChatRowStyle) {
        _rowStyle.value = style
        prefs?.edit()?.putString(KEY_ROW, style.name)?.apply()
    }

    fun setBubbleStyle(style: BubbleStyle) {
        _bubbleStyle.value = style
        prefs?.edit()?.putString("bubble", style.name)?.apply()
    }

    fun setTickStyle(style: TickStyle) {
        _tickStyle.value = style
        prefs?.edit()?.putString("tick", style.name)?.apply()
    }

    fun setNavBarStyle(style: NavBarStyle) {
        _navBarStyle.value = style
        prefs?.edit()?.putString("nav", style.name)?.apply()
    }

    fun setOwnBubbleColor(color: Int?) {
        _ownBubbleColor.value = color
        prefs?.edit()?.apply { if (color == null) remove("own") else putInt("own", color) }?.apply()
    }

    fun setPeerBubbleColor(color: Int?) {
        _peerBubbleColor.value = color
        prefs?.edit()?.apply { if (color == null) remove("peer") else putInt("peer", color) }?.apply()
    }

    fun setTextScale(scale: Float) {
        _textScale.value = scale
        prefs?.edit()?.putFloat("text", scale)?.apply()
    }

    fun setEntryHint(hint: String) {
        _entryHint.value = hint
        prefs?.edit()?.putString("hint", hint)?.apply()
    }

    fun setHomeAvatar(show: Boolean) {
        _homeAvatar.value = show
        prefs?.edit()?.putBoolean("avatar", show)?.apply()
    }

    fun setHideFilters(hide: Boolean) {
        _hideFilters.value = hide
        prefs?.edit()?.putBoolean("filters", hide)?.apply()
    }

    fun setWallpaperDim(dim: Float) {
        _wallpaperDim.value = dim
        prefs?.edit()?.putFloat("dim", dim)?.apply()
    }

    fun setEntryStyle(style: ChatEntryStyle) {
        _entryStyle.value = style
        prefs?.edit()?.putString(KEY_ENTRY, style.name)?.apply()
    }
}

/** Azul de acento por defecto de las filas. */
private val RowBlue = Color(0xFF0077FF)
private val RowShape = RoundedCornerShape(10.dp)
private val OutlineRipple = Color(0xFFFF0045)
private val RowMarginH = 10.dp
private val RowMarginV = 5.dp

/** Grosor visible de las piezas en 3D: cuántas capas forman su canto. */
private const val DEPTH_LAYERS = 6
private val DepthLayerStep = 1.dp

/** Marco de una fila. [contactColors] es el color de perfil del contacto. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChatRowFrame(
    style: ChatRowStyle,
    contactColors: List<Color>,
    photoUrl: String?,
    content: @Composable () -> Unit
) {
    val tint by ChatLook.rowTint.collectAsState()
    val appColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val colors = when (tint) {
        RowTint.BLUE -> listOf(RowBlue, RowBlue)
        RowTint.APP -> listOf(appColor, appColor)
        RowTint.CONTACT -> contactColors
    }
    when (style) {
        ChatRowStyle.CLASSIC -> content()

        ChatRowStyle.CARD -> Box(
            margin()
                .shadow(3.dp, RowShape)
                .clip(RowShape)
                .background(NexusTokens.Color.BgSurface)
        ) { content() }

        ChatRowStyle.RAIL -> Box(
            margin()
                // Sombra grande y con el color del contacto: la tarjeta parece flotar.
                .shadow(10.dp, RowShape, ambientColor = colors.first(), spotColor = colors.first())
                .clip(RowShape)
                .background(Brush.verticalGradient(colors))
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
                    .clip(RowShape)
                    .background(NexusTokens.Color.BgSurface)
            ) { content() }
        }

        ChatRowStyle.DEPTH -> DepthCard(colors, content)

        ChatRowStyle.LINE -> Box(
            margin()
                .shadow(3.dp, RowShape)
                .clip(RowShape)
                .background(NexusTokens.Color.BgSurface)
        ) {
            CompositionLocalProvider(LocalChatRowDivider provides true) { content() }
        }

        ChatRowStyle.FRAME -> Box(
            margin()
                .shadow(3.dp, RowShape)
                .clip(RowShape)
                .background(NexusTokens.Color.BgSurface)
                .drawBehind { drawRowBevel(colors.first()) }
        ) { content() }

        ChatRowStyle.OUTLINE -> Box(
            margin()
                .clip(RowShape)
                .border(1.5.dp, Brush.linearGradient(colors), RowShape)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalRippleConfiguration provides
                    androidx.compose.material3.RippleConfiguration(color = OutlineRipple)
            ) { content() }
        }

        ChatRowStyle.NEON -> Box(
            margin()
                .neonGlow(colors.first())
                .clip(RowShape)
                .background(NexusTokens.Color.BgBase)
                .border(1.5.dp, Brush.linearGradient(colors), RowShape)
        ) { content() }

        ChatRowStyle.GLASS -> Box(
            margin()
                .clip(RowShape)
                .background(Color.White.copy(alpha = 0.06f))
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))),
                    RowShape
                )
        ) { content() }

        ChatRowStyle.PHOTO -> Box(
            margin()
                .clip(RowShape)
                .background(Brush.linearGradient(colors.map { it.copy(alpha = 0.45f) }))
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .blur(14.dp)
                )
            }
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.82f), Color.Black.copy(alpha = 0.52f))
                        )
                    )
            )
            content()
        }
    }
}

/** Fila de cualquier otra lista (llamadas, historias, contactos, búsqueda): mismo estilo. */
@Composable
fun ListRowFrame(seed: String, photoUrl: String?, content: @Composable () -> Unit) {
    val style by ChatLook.rowStyle.collectAsState()
    val colors = remember(seed) { ProfileColors.forUser(seed) }
    val tint by ChatLook.rowTint.collectAsState()
    val appColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    ChatRowFrame(style, colors, photoUrl) {
        if (style == ChatRowStyle.LINE) {
            // Estas filas no tienen un hueco propio para la línea: se traza entre sus dos textos.
            val lineColor = when (tint) {
                RowTint.BLUE -> RowBlue
                RowTint.APP -> appColor
                RowTint.CONTACT -> colors.first()
            }
            Box(
                Modifier.drawWithContent {
                    drawContent()
                    val startX = 78.dp.toPx()
                    val y = size.height / 2f
                    drawLine(lineColor, Offset(startX, y), Offset(size.width - 16.dp.toPx(), y), 1.dp.toPx())
                }
            ) { content() }
        } else {
            content()
        }
    }
}

/**
 * Bisel de la fila en relieve: una franja que se apaga hacia fuera en los lados y abajo, y unos
 * trazos en diagonal en dos esquinas. Las medidas son proporcionales a la fila, así que se estira
 * con ella y no deforma nada en filas más altas o más anchas.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRowBevel(tint: Color) {
    val w = size.width
    val h = size.height
    val sideBand = w * 0.0147f
    val topBand = h * 0.107f
    val bottomBand = h * 0.107f

    fun fade(vararg stops: Pair<Float, Float>) = stops.map { (at, alpha) -> at to tint.copy(alpha = alpha) }.toTypedArray()

    drawRect(
        Brush.horizontalGradient(*fade(0f to 0.09f, 0.3f to 0.22f, 0.6f to 0.31f, 1f to 0.49f), startX = 0f, endX = sideBand),
        topLeft = Offset.Zero,
        size = androidx.compose.ui.geometry.Size(sideBand, h)
    )
    drawRect(
        Brush.horizontalGradient(*fade(0f to 0.49f, 0.4f to 0.31f, 0.7f to 0.22f, 1f to 0.09f), startX = w - sideBand, endX = w),
        topLeft = Offset(w - sideBand, 0f),
        size = androidx.compose.ui.geometry.Size(sideBand, h)
    )
    drawRect(
        Brush.verticalGradient(*fade(0f to 0.02f, 0.5f to 0.08f, 1f to 0.15f), startY = 0f, endY = topBand),
        topLeft = Offset.Zero,
        size = androidx.compose.ui.geometry.Size(w, topBand)
    )
    drawRect(
        Brush.verticalGradient(*fade(0f to 0.49f, 0.5f to 0.3f, 1f to 0.1f), startY = h - bottomBand, endY = h),
        topLeft = Offset(0f, h - bottomBand),
        size = androidx.compose.ui.geometry.Size(w, bottomBand)
    )

    // Los trazos se colocan en proporción a la fila, igual que la franja.
    fun at(x: Float, y: Float) = Offset(x * w, y * h)
    val stroke = 1.6.dp.toPx()
    drawLine(tint.copy(alpha = 0.32f), at(0.0137f, 0.227f), at(0.0333f, 0.107f), stroke)
    drawLine(tint.copy(alpha = 0.62f), at(0.0137f, 0.36f), at(0.0529f, 0.107f), stroke)
    drawLine(tint.copy(alpha = 0.72f), at(0.959f, 0.893f), at(0.988f, 0.693f), stroke)
}

private fun margin() = Modifier
    .fillMaxWidth()
    .padding(horizontal = RowMarginH, vertical = RowMarginV)

/**
 * Tarjeta con volumen real: un canto de varias capas con el color del contacto debajo de la
 * cara, y un giro en perspectiva que depende de dónde está en la pantalla y de dónde la toques.
 */
@Composable
private fun DepthCard(colors: List<Color>, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val touchTilt = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var centerY by remember { mutableFloatStateOf(0f) }
    val screenHeight = LocalWindowInfo.current.containerSize.height.toFloat().coerceAtLeast(1f)
    val edge = remember(colors) { colors.map { lerp(it, Color.Black, 0.45f) } }
    val faceTop = remember { lerp(NexusTokens.Color.BgSurface, Color.White, 0.07f) }

    Box(
        margin()
            .padding(bottom = DepthLayerStep * DEPTH_LAYERS)
            .onGloballyPositioned { coordinates ->
                centerY = coordinates.positionInRoot().y + coordinates.size.height / 2f
            }
            // No consume el toque: abrir el chat, mantener pulsado y desplazar siguen funcionando.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val target = Offset(
                        (down.position.x / size.width.coerceAtLeast(1)) * 2f - 1f,
                        (down.position.y / size.height.coerceAtLeast(1)) * 2f - 1f
                    )
                    scope.launch { touchTilt.animateTo(target, spring(stiffness = Spring.StiffnessMedium)) }
                    waitForUpOrCancellation()
                    scope.launch {
                        touchTilt.animateTo(
                            Offset.Zero,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        )
                    }
                }
            }
            .graphicsLayer {
                val fromCenter = (centerY / screenHeight - 0.5f).coerceIn(-0.6f, 0.6f)
                rotationX = fromCenter * -9f + touchTilt.value.y * -7f
                rotationY = touchTilt.value.x * 5f
                cameraDistance = 14f * density
            }
            .drawBehind {
                val radius = CornerRadius(10.dp.toPx())
                val step = DepthLayerStep.toPx()
                for (layer in DEPTH_LAYERS downTo 1) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(edge),
                        topLeft = Offset(0f, layer * step),
                        size = size,
                        cornerRadius = radius,
                        // Las capas de más abajo se apagan, como si la luz viniera de arriba.
                        alpha = 1f - layer * 0.09f
                    )
                }
            }
            .clip(RowShape)
            .background(Brush.verticalGradient(listOf(faceTop, NexusTokens.Color.BgSurface)))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)),
                RowShape
            )
    ) { content() }
}

/** Halo de luz alrededor de una pieza, dibujado con la sombra del lienzo. */
private fun Modifier.neonGlow(color: Color): Modifier = drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        paint.asFrameworkPaint().apply {
            this.color = android.graphics.Color.TRANSPARENT
            setShadowLayer(12.dp.toPx(), 0f, 0f, color.copy(alpha = 0.75f).toArgb())
        }
        val radius = 16.dp.toPx()
        canvas.drawRoundRect(0f, 0f, size.width, size.height, radius, radius, paint)
    }
}

/** Marco del campo donde se escribe. El contenido (iconos y texto) es siempre el mismo. */
@Composable
fun chatEntryModifier(style: ChatEntryStyle, accent: Color): Modifier = when (style) {
    ChatEntryStyle.PILL, ChatEntryStyle.PHOTO, ChatEntryStyle.SOCIAL -> Modifier
        .clip(RoundedCornerShape(25.dp))
        .background(NexusTokens.Color.BgElevated)

    // Sin caja: el campo se funde con la barra y solo destacan sus iconos.
    ChatEntryStyle.FLAT -> Modifier

    ChatEntryStyle.DEPTH -> {
        val shape = RoundedCornerShape(16.dp)
        val edge = lerp(accent, Color.Black, 0.45f)
        val top = lerp(NexusTokens.Color.BgElevated, Color.White, 0.08f)
        Modifier
            .padding(bottom = DepthLayerStep * 4)
            .drawBehind {
                val radius = CornerRadius(16.dp.toPx())
                val step = DepthLayerStep.toPx()
                for (layer in 4 downTo 1) {
                    drawRoundRect(
                        color = edge,
                        topLeft = Offset(0f, layer * step),
                        size = size,
                        cornerRadius = radius,
                        alpha = 1f - layer * 0.12f
                    )
                }
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, NexusTokens.Color.BgElevated)))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)), shape)
    }

    ChatEntryStyle.OUTLINE -> {
        val shape = RoundedCornerShape(22.dp)
        Modifier
            .clip(shape)
            .border(1.5.dp, accent, shape)
    }

    ChatEntryStyle.LINE -> Modifier.drawBehind {
        val stroke = 2.dp.toPx()
        drawLine(accent, Offset(0f, size.height - stroke / 2f), Offset(size.width, size.height - stroke / 2f), stroke)
    }

    ChatEntryStyle.BLOCK -> Modifier
        .clip(RoundedCornerShape(10.dp))
        .background(NexusTokens.Color.BgHigh)
}

/** Forma de una burbuja según el estilo y de quién es. */
fun chatBubbleShape(style: BubbleStyle, isOwn: Boolean): Shape = when (style) {
    BubbleStyle.SOFT, BubbleStyle.DEPTH, BubbleStyle.GLASS ->
        if (isOwn) RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp) else RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp)
    BubbleStyle.TAIL ->
        if (isOwn) RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    BubbleStyle.PILL -> RoundedCornerShape(22.dp)
    BubbleStyle.SQUARE -> RoundedCornerShape(4.dp)
    BubbleStyle.OUTLINE, BubbleStyle.NEON -> RoundedCornerShape(12.dp)
}

/** Superficie de una burbuja: incluye su recorte, por eso va en lugar de `.clip`. */
fun chatBubbleSurface(style: BubbleStyle, shape: Shape, color: Color, accent: Color): Modifier = when (style) {
    BubbleStyle.SOFT, BubbleStyle.TAIL, BubbleStyle.PILL, BubbleStyle.SQUARE -> Modifier
        .clip(shape)
        .background(color)

    BubbleStyle.OUTLINE -> Modifier
        .clip(shape)
        .border(1.5.dp, accent, shape)

    BubbleStyle.GLASS -> Modifier
        .clip(shape)
        .background(color.copy(alpha = 0.35f))
        .border(1.dp, Color.White.copy(alpha = 0.18f), shape)

    BubbleStyle.NEON -> Modifier
        .neonGlow(accent)
        .clip(shape)
        .background(color)
        .border(1.dp, accent, shape)

    BubbleStyle.DEPTH -> Modifier
        .padding(bottom = 4.dp)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            val edge = lerp(accent, Color.Black, 0.45f)
            for (layer in 4 downTo 1) {
                translate(top = layer * 1.dp.toPx()) { drawOutline(outline, edge, alpha = 1f - layer * 0.12f) }
            }
        }
        .clip(shape)
        .background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.08f), color)))
}

/** Marca de enviado o leído en el estilo elegido. */
@Composable
fun ReceiptMark(style: TickStyle, color: Color, double: Boolean, read: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val h = size.height
        val w = size.width
        val stroke = h * 0.16f
        when (style) {
            TickStyle.CLASSIC, TickStyle.GLOW -> {
                val width = if (double) w * 0.62f else w
                fun tick(x: Float) = Path().apply {
                    moveTo(x, h * 0.52f)
                    lineTo(x + width * 0.34f, h * 0.82f)
                    lineTo(x + width * 0.96f, h * 0.18f)
                }
                val line = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                if (style == TickStyle.GLOW) {
                    val halo = Stroke(stroke * 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    if (double) drawPath(tick(w - width), color.copy(alpha = 0.3f), style = halo)
                    drawPath(tick(0f), color.copy(alpha = 0.3f), style = halo)
                }
                if (double) drawPath(tick(w - width), color, style = line)
                drawPath(tick(0f), color, style = line)
            }
            TickStyle.DOTS -> {
                val radius = h * 0.24f
                drawCircle(color, radius, Offset(if (double) radius else w / 2f, h / 2f))
                if (double) drawCircle(color, radius, Offset(w - radius, h / 2f))
            }
            TickStyle.BARS -> {
                val barWidth = h * 0.22f
                val y = h * 0.8f
                drawLine(color, Offset(0f, y), Offset(0f + barWidth, h * 0.3f), stroke, StrokeCap.Round)
                if (double) drawLine(color, Offset(w * 0.5f, y), Offset(w * 0.5f + barWidth, h * 0.3f), stroke, StrokeCap.Round)
            }
        }
        if (read && style != TickStyle.CLASSIC && style != TickStyle.GLOW) {
            drawLine(color, Offset(0f, h * 0.96f), Offset(w, h * 0.96f), stroke * 0.6f, StrokeCap.Round)
        }
    }
}

/** Separador de listas: entre tarjetas sobra, ellas ya se separan solas. */
@Composable
fun RowDivider(modifier: Modifier = Modifier, startInset: androidx.compose.ui.unit.Dp = 0.dp) {
    val style by ChatLook.rowStyle.collectAsState()
    if (style == ChatRowStyle.CLASSIC) NexusDivider(modifier = modifier, startInset = startInset)
}
