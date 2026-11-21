package com.Azelmods.App.ui.screens.welcome

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.Azelmods.App.BuildConfig
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.NexusTokens
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

/**
 * Un paso de la visita: la pantalla a la que lleva (null = se queda en Inicio), el ánimo de
 * Nexa y lo que cuenta. [target] es el botón real que ilumina, al que camina y que señala
 * (ver [nexaTarget]); sin él, se coloca a un lado según [left]. [pose] fuerza una postura.
 */
private data class TourStep(
    val route: String?,
    val mood: NexaMood,
    val title: String,
    val text: String,
    val left: Boolean,
    val target: String? = null,
    val pose: NexaPose? = null
)

private fun steps() = listOf(
    TourStep(null, NexaMood.HAPPY, tr("¡Hola! Soy Nexa"),
        tr("Acabas de crear tu cuenta y quiero enseñarte tu nueva casa. Te llevo a cada sitio y te enseño los botones de verdad. Puedes tocarme cuando quieras."), left = true, pose = NexaPose.WAVE),
    TourStep(Screen.Home.route, NexaMood.EXCITED, tr("Tu primera conversación"),
        tr("Empieza aquí. Con este botón buscas a alguien por su usuario o escaneas su código QR. Cuantos más amigos traigas, mejor."), left = false, target = "home_new_chat"),
    TourStep(Screen.Home.route, NexaMood.SMILE, tr("Ordena tus chats"),
        tr("Estos filtros enseñan solo los no leídos, los grupos o los archivados. Mantén pulsado un chat para fijarlo, silenciarlo u ocultarlo."), left = true, target = "home_filters"),
    TourStep(Screen.Home.route, NexaMood.THINK, tr("Busca lo que sea"),
        tr("La lupa busca personas, chats y mensajes. El robot de al lado abre a Azel, la inteligencia artificial."), left = false, target = "home_search"),
    TourStep(Screen.Home.route, NexaMood.WINK, tr("El menú escondido"),
        tr("Aquí están los canales, el modo fantasma, los chats ocultos y secretos, los mensajes guardados y el descargador de vídeos."), left = false, target = "home_menu"),
    TourStep(Screen.Home.route, NexaMood.SURPRISE, tr("Muévete por la app"),
        tr("Abajo cambias entre tus chats, las historias que duran un día, las llamadas cifradas y tu perfil."), left = true, target = "home_tabs"),
    TourStep(Screen.Calls.route, NexaMood.HAPPY, tr("Llamadas"),
        tr("Llamadas de voz y vídeo, cifradas de extremo a extremo. Aquí tienes tu historial: entrantes, salientes y perdidas, y vuelves a llamar con un toque."), left = false),
    TourStep(Screen.Home.route, NexaMood.LOVE, tr("Historias"),
        tr("En la pestaña Historias compartes fotos y vídeos que desaparecen en un día, y puedes ver quién los ha visto."), left = true),
    TourStep(Screen.Home.route, NexaMood.WINK, tr("Dentro de un chat"),
        tr("Desliza un mensaje para responderlo y mantenlo pulsado para reaccionar, reenviarlo o borrarlo para todos. Puedes mandar hasta treinta archivos a la vez, notas de voz, GIF y mensajes que se borran solos."), left = false),
    TourStep(Screen.Home.route, NexaMood.SMUG, tr("Aquí nadie te lee"),
        tr("Cada mensaje se cifra en tu teléfono antes de salir. Ni siquiera nosotros podemos leerlo. Maquiavelo escribió que todos ven lo que aparentas, pero pocos saben lo que eres. Aquí, lo que eres y lo que escribes es solo tuyo."), left = false),
    TourStep("channels", NexaMood.SURPRISE, tr("Canales y difusiones"),
        tr("Sigue canales o crea el tuyo con este botón. Desde el menú también puedes enviar una difusión: un mensaje a muchos contactos, cada uno en su chat privado."), left = true, target = "channels_create"),
    TourStep("broadcast", NexaMood.EXCITED, tr("Difusiones"),
        tr("Escribe una vez y elige a quién: a cada contacto le llega el mensaje por separado, en su chat privado, sin crear un grupo."), left = false),
    TourStep("saved_messages", NexaMood.SMILE, tr("Mensajes guardados"),
        tr("Tu rincón para lo importante: guarda aquí mensajes, enlaces y archivos para tenerlos siempre a mano."), left = true),
    TourStep(Screen.Settings.route, NexaMood.WINK, tr("Tus ajustes"),
        tr("En Privacidad decides quién ve tu foto, tu última conexión y si mandas confirmaciones de lectura. Epicteto enseñaba que hay cosas que dependen de nosotros y otras que no. Esto depende de ti."), left = false, target = "settings_Privacidad"),
    TourStep(Screen.SettingsNotifications.route, NexaMood.SMILE, tr("Notificaciones"),
        tr("Elige el sonido, la vibración, si se ve el texto del mensaje en el aviso y si quieres avisos de los grupos."), left = true),
    TourStep(Screen.SettingsStorage.route, NexaMood.THINK, tr("Almacenamiento y datos"),
        tr("Decide qué se descarga solo, fotos, vídeos o documentos, y vacía la caché cuando el teléfono pida espacio."), left = false),
    TourStep(Screen.ProfileCustomize.route, NexaMood.LOVE, tr("Tu perfil, a tu estilo"),
        tr("Aquí eliges tu color, tu foto y tu portada. Hasta pueden ser un vídeo corto: tu perfil se mueve."), left = false),
    TourStep(Screen.SettingsAppearance.route, NexaMood.HAPPY, tr("Color y fondo"),
        tr("Cambia el color de toda la app, el fondo, el tipo de letra y la animación de entrada. Se aplica al momento."), left = true),
    TourStep("chat_studio", NexaMood.LOVE, tr("El Estudio de diseño"),
        tr("Mi sitio favorito. En estas pestañas cambias filas, burbujas, la barra de escribir y el inicio. Hay chicas y chicos en pixel art que despiertan cuando leen tus mensajes."), left = true, target = "studio_tabs"),
    TourStep("secret_chats", NexaMood.SMUG, tr("Chats secretos"),
        tr("Para lo más privado. En un chat secreto cada mensaje se cifra con una clave nueva."), left = false),
    TourStep(Screen.Security.route, NexaMood.THINK, tr("Modo anónimo y Tor"),
        tr("Toca aquí para activar Tor y nadie sabrá desde dónde escribes. Nietzsche decía que quien tiene un porqué para vivir soporta casi cualquier cómo. Tu porqué aquí es tu libertad."), left = false, target = "security_tor"),
    TourStep(Screen.TorBrowser.route, NexaMood.SMUG, tr("Navegador privado"),
        tr("Navega a través de Tor sin dejar rastro y entra en sitios .onion, sin salir de NexusChat."), left = true),
    TourStep("video_download", NexaMood.EXCITED, tr("Descarga vídeos"),
        tr("Pega el enlace de un vídeo de YouTube, X, Instagram u otras webs y guárdalo en tu teléfono para verlo o compartirlo."), left = true),
    TourStep("terminal", NexaMood.EXCITED, tr("Terminal con Kali Linux"),
        tr("Una terminal de verdad: la del sistema o Kali Linux completo con apt, que solo se instala si tú lo pides."), left = false),
    TourStep("code_editor", NexaMood.THINK, tr("Editor de código"),
        tr("Escribe y edita código con colores para cada lenguaje, directamente en el teléfono."), left = true),
    TourStep(Screen.AzelAI.route, NexaMood.THINK, tr("Azel, la inteligencia artificial"),
        tr("Escríbele aquí. Azel te ayuda a escribir, traducir e investigar con agentes. Funciona con tu propia clave, así que lo que le cuentas no pasa por nuestros servidores."), left = true, target = "azel_input"),
    TourStep(Screen.Home.route, NexaMood.HAPPY, tr("¡Todo listo para empezar!"),
        tr("Si alguna vez dudas, escríbele a Azel Assistant en tus chats o vuelve a llamarme desde Ajustes. Y recuerda: hablar con libertad también es cuidarte. ¡Disfruta NexusChat!"), left = false, pose = NexaPose.WAVE)
)

/** Lo que Nexa puede presentar: la visita de bienvenida o las novedades de la versión. */
enum class NexaShow { TOUR, NEWS }

/** Para llamar a Nexa desde cualquier pantalla (Ajustes): sale en cuanto se vuelve a Inicio. */
object NexaHost {
    val request = kotlinx.coroutines.flow.MutableStateFlow<NexaShow?>(null)
}

/**
 * Centro de novedades: lo que Nexa cuenta tras actualizar, por versionCode. Al publicar una
 * versión con cambios que merezcan contarse, se añade aquí su entrada.
 */
private val NEWS: Map<Int, () -> List<TourStep>> = mapOf(
    860 to {
        listOf(
            TourStep(null, NexaMood.HAPPY, tr("¡Hay novedades!"),
                tr("Soy Nexa. Desde ahora te cuento lo nuevo de cada versión. Esta es la 8.6.0, y viene cargada."), left = true, pose = NexaPose.WAVE),
            TourStep("chat_studio", NexaMood.LOVE, tr("Marcas con personajes"),
                tr("Ocho personajes en pixel art, chicas y chicos, que duermen al enviar, despiertan al entregar y ponen ojos de corazón al leer. En Mis marcas puedes poner tus propias imágenes o GIF."), left = false, target = "studio_tabs"),
            TourStep(Screen.Home.route, NexaMood.SURPRISE, tr("Leído de verdad"),
                tr("La lista de chats ya muestra el estado real: una marca al enviar, dos grises al entregar y azules cuando lo leen."), left = true),
            TourStep(Screen.Home.route, NexaMood.WINK, tr("Actualizar sin sustos"),
                tr("Si alguna vez una actualización no encaja con tu versión, te explico por qué y te ayudo a resolverlo. Puedes volver a verme en Ajustes, en Información."), left = false, pose = NexaPose.WAVE)
        )
    }
)

private fun latestNewsKey(version: Int): Int? = NEWS.keys.filter { it <= version }.maxOrNull()
private fun newsSteps(version: Int): List<TourStep>? = latestNewsKey(version)?.let { NEWS[it]?.invoke() }

/** Frases de Nexa guardadas en la app (assets/nexa/frases.json): [{"m": ánimo, "t": texto}]. */
private fun loadNexaPhrases(context: android.content.Context): List<Pair<NexaMood, String>> = runCatching {
    val json = context.assets.open("nexa/frases.json").bufferedReader().use { it.readText() }
    val arr = org.json.JSONArray(json)
    (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        val text = o.optString("t").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val mood = runCatching { NexaMood.valueOf(o.optString("m", "SMILE")) }.getOrDefault(NexaMood.SMILE)
        mood to text
    }
}.getOrDefault(emptyList())

/** Lo que dice Nexa cuando la tocas si no se pudiera leer el archivo de frases. */
private fun pokes() = listOf(
    // Ella misma.
    NexaMood.SURPRISE to tr("¡Eh! Que me haces cosquillas."),
    NexaMood.LOVE to tr("Me alegra que estés aquí."),
    NexaMood.SMUG to tr("Sí, soy pixel art. Y a mucha honra."),
    NexaMood.WINK to tr("Un secreto: en el Estudio de diseño puedes cambiarlo casi todo."),
    NexaMood.HAPPY to tr("¡Sigamos, que aún queda lo mejor!"),
    NexaMood.LOVE to tr("Si me sigues tocando me voy a sonrojar, ¿eh?"),
    // Historia de los mensajes y del cifrado.
    NexaMood.THINK to tr("Julio César ya cifraba sus cartas: cambiaba cada letra por la que estaba tres puestos más adelante. Hoy lo hacemos con matemáticas mucho más difíciles."),
    NexaMood.SURPRISE to tr("En la Segunda Guerra Mundial, en Bletchley Park, Alan Turing y su equipo rompieron Enigma, la máquina de cifrar de los alemanes. Por eso hoy el cifrado se toma tan en serio."),
    NexaMood.THINK to tr("En 1950 Turing se hizo una pregunta: ¿pueden pensar las máquinas? Yo todavía me lo pregunto."),
    NexaMood.EXCITED to tr("Ada Lovelace escribió en 1843 el primer algoritmo pensado para una máquina. La primera programadora de la historia fue una mujer."),
    NexaMood.SURPRISE to tr("El primer mensaje de texto se envió en 1992 y decía: Merry Christmas. Felices fiestas, nada más."),
    NexaMood.WINK to tr("Ray Tomlinson envió el primer correo electrónico en 1971 y eligió la arroba para separar el nombre del ordenador. Por eso está en todas partes."),
    NexaMood.THINK to tr("En 1844 Samuel Morse envió por telégrafo su primer mensaje oficial: ¿Qué ha hecho Dios? Desde entonces no hemos dejado de escribirnos."),
    NexaMood.SMUG to tr("Tor nació en los años noventa en un laboratorio de la Marina de Estados Unidos. La idea: esconder quién habla con quién envolviéndolo en capas, como una cebolla."),
    NexaMood.HAPPY to tr("Los primeros emojis los dibujó Shigetaka Kurita en Japón en 1999: eran 176, de solo doce por doce píxeles. Como yo, más o menos."),
    // Filosofía.
    NexaMood.THINK to tr("A Sócrates se le atribuye aquello de: solo sé que no sé nada. Yo, al menos, sé usar esta app."),
    NexaMood.THINK to tr("Séneca escribió que no es que tengamos poco tiempo, sino que perdemos mucho. Así que vamos al grano."),
    NexaMood.SMILE to tr("Marco Aurelio, emperador de Roma, escribía cada noche sus pensamientos en un cuaderno solo para él. El primer chat privado de la historia."),
    NexaMood.SMUG to tr("Nietzsche escribió: lo que no me mata me hace más fuerte. Aplica también a las actualizaciones."),
    NexaMood.THINK to tr("Camus decía que hay que imaginarse a Sísifo feliz. Subir la piedra cada día también puede tener sentido."),
    NexaMood.WINK to tr("Sun Tzu decía: conoce a tu enemigo y conócete a ti mismo. En internet, empieza por saber quién puede leerte."),
    NexaMood.SMUG to tr("Maquiavelo escribió que es más seguro ser temido que amado, si no se puede ser ambas cosas. Yo prefiero que me quieras."),
    NexaMood.HAPPY to tr("Cuentan que Alejandro Magno le preguntó a Diógenes qué deseaba, y Diógenes le respondió: que te apartes, me tapas el sol."),
    NexaMood.THINK to tr("Los espartanos cifraban con la escítala: enrollaban una tira de cuero en un bastón y escribían a lo largo. Sin un bastón igual, el mensaje no se entendía."),
    NexaMood.SURPRISE to tr("En 1586 María Estuardo fue condenada porque descifraron las cartas secretas en las que conspiraba. Un cifrado débil puede costar muy caro."),
    NexaMood.SURPRISE to tr("Enigma tenía más de 150 trillones de configuraciones posibles cada día. Aun así, la rompieron."),
    NexaMood.HAPPY to tr("La primera llamada de teléfono, en 1876, fue de Graham Bell a su ayudante: señor Watson, venga aquí, quiero verle."),
    NexaMood.SURPRISE to tr("El primer mensaje de internet, en 1969, iba a ser LOGIN. El sistema se colgó después de la L y la O. Así empezó todo: con un LO."),
    NexaMood.WINK to tr("En 1947 el equipo de Grace Hopper encontró una polilla de verdad atascada en un ordenador. Desde entonces a los fallos se les llama bugs."),
    NexaMood.LOVE to tr("En la Primera Guerra Mundial una paloma mensajera, Cher Ami, llevó el mensaje que salvó a cerca de doscientos soldados. Los mensajes siempre han importado."),
    NexaMood.THINK to tr("La palabra robot viene de una obra de teatro checa de 1920, R.U.R., de Karel Čapek. Yo soy más de pixel art que de metal."),
    NexaMood.SMILE to tr("Leonardo da Vinci escribía sus notas al revés, de derecha a izquierda, como en un espejo. Una forma muy suya de proteger lo que pensaba."),
    NexaMood.EXCITED to tr("Hipatia de Alejandría enseñaba matemáticas y astronomía en el siglo cuarto. Una mente brillante en una época difícil."),
    NexaMood.THINK to tr("Descartes escribió: pienso, luego existo. Yo hablo, luego existo."),
    NexaMood.THINK to tr("A Heráclito se le atribuye: nadie se baña dos veces en el mismo río. Ni abre dos veces la misma app: siempre hay algo nuevo."),
    NexaMood.SMUG to tr("En el mito de la caverna de Platón, la gente solo ve sombras en la pared y cree que es la realidad. Por eso conviene mirar quién está detrás de tus datos."),
    NexaMood.EXCITED to tr("El lema de la Ilustración, según Kant: atrévete a saber. Sapere aude."),
    NexaMood.HAPPY to tr("Me llamo Nexa por Nexus. Somos familia, como quien dice."),
    NexaMood.WINK to tr("Puedes silenciarme con el altavoz de abajo. Pero no lo hagas, porfa."),
    NexaMood.LOVE to tr("Mis cascos llevan la N porque soy de aquí. Y tú ahora también."),
    NexaMood.EXCITED to tr("¿Más historias? Tócame otra vez, que tengo muchas.")
)

/**
 * Presentación con Nexa. Sale UNA sola vez y solo a quien acaba de crear su cuenta: quien ya
 * usaba la app y actualiza no la ve. Nexa camina por las pantallas reales, cambia de ánimo en
 * cada una, escribe lo que dice mientras habla con la voz del teléfono y reacciona si la tocas.
 * Atrás vuelve al paso anterior; se puede saltar o silenciar en cualquier momento.
 */
@Composable
fun WelcomeTourOverlay(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("welcome_tour", android.content.Context.MODE_PRIVATE) }
    val version = BuildConfig.VERSION_CODE
    val requested by NexaHost.request.collectAsState()
    // Lo que Nexa está presentando ahora (TOUR o NEWS), o null si no sale.
    var active by rememberSaveable { mutableStateOf<String?>(null) }

    val entry by navController.currentBackStackEntryAsState()
    // La sesión, como estado: Nexa se entera en cuanto alguien entra o sale.
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    // Se leen ANTES de cualquier salida anticipada: si no, al abrir la app sin sesión Compose
    // dejaba de escuchar la navegación y Nexa no volvía a comprobar nada al iniciar sesión.
    val route = entry?.destination?.route
    val pending = requested
    if (active == null) {
        val u = user
        if (u == null) { android.util.Log.w("Nexa", "sin sesión (ruta=$route)"); return }
        val newAccount = u.metadata?.creationTimestamp?.let { System.currentTimeMillis() - it < 24 * 60 * 60 * 1000L } == true
        // La presentación se recuerda por cuenta: otra cuenta en el mismo teléfono también la ve.
        val seenKey = "seen_${u.uid}"
        val seenTour = prefs.getBoolean(seenKey, false)
        val newsKey = latestNewsKey(version)
        val newsSeen = prefs.getInt("news_version", 0)
        // Presentación: solo a quien acaba de crear la cuenta. Novedades: una vez por versión
        // a quien ya usaba la app. Desde Ajustes se puede pedir cualquiera de las dos.
        val wanted: NexaShow? = pending ?: when {
            !seenTour && newAccount -> NexaShow.TOUR
            newsKey != null && newsSeen < newsKey -> NexaShow.NEWS
            else -> null
        }
        android.util.Log.w("Nexa", "ruta=$route pedido=$pending nueva=$newAccount vista=$seenTour novedades=$newsSeen/$newsKey -> $wanted")
        LaunchedEffect(u.uid, wanted) {
            val edit = prefs.edit()
            // Quien ya tenía cuenta no ve nunca la presentación automática.
            if (!newAccount && !seenTour) edit.putBoolean(seenKey, true)
            if (wanted == null) edit.putInt("news_version", version)
            edit.apply()
        }
        if (wanted == null || route != Screen.Home.route) return
        LaunchedEffect(wanted) {
            delay(900)
            active = wanted.name
            NexaHost.request.value = null
        }
        return
    }
    val show = runCatching { NexaShow.valueOf(active!!) }.getOrDefault(NexaShow.TOUR)

    val voice = remember { NexaVoice(context) }
    DisposableEffect(Unit) { onDispose { voice.shutdown() } }
    val speaking by voice.speaking.collectAsState()
    val wordTick by voice.wordTick.collectAsState()
    var muted by remember { mutableStateOf(prefs.getBoolean("muted", false)) }
    val tour = remember(show) { if (show == NexaShow.NEWS) newsSteps(version) ?: steps() else steps() }
    // Barajadas: cada toque dice una distinta y no se repiten hasta agotarlas.
    // Frases al tocarla: las del archivo de la app más las que se añadan en Firebase
    // (config/nexa/frases), barajadas y sin repetirse hasta agotarlas.
    var pokeLines by remember { mutableStateOf(loadNexaPhrases(context).ifEmpty { pokes() }.shuffled()) }
    LaunchedEffect(Unit) {
        runCatching {
            val snap = com.google.firebase.database.FirebaseDatabase.getInstance().reference
                .child("config").child("nexa").child("frases").get().await()
            val extra = snap.children.mapNotNull { c ->
                val text = c.child("t").getValue(String::class.java) ?: c.getValue(String::class.java) ?: return@mapNotNull null
                val mood = runCatching { NexaMood.valueOf(c.child("m").getValue(String::class.java) ?: "SMILE") }.getOrDefault(NexaMood.SMILE)
                mood to text
            }
            if (extra.isNotEmpty()) pokeLines = pokeLines + extra.shuffled()
        }
    }
    // Sobrevive a girar la pantalla o a que el sistema recree la actividad.
    var index by rememberSaveable { mutableStateOf(0) }
    val step = tour[index.coerceIn(0, tour.lastIndex)]

    // Reacción al tocarla: cambia de cara y dice otra cosa unos segundos.
    var pokeCount by rememberSaveable { mutableStateOf(0) }
    var reaction by remember { mutableStateOf<Pair<NexaMood, String>?>(null) }

    // Con la app en segundo plano se calla; al volver repite el paso.
    var foreground by remember { mutableStateOf(true) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> { foreground = false; voice.stop() }
                Lifecycle.Event.ON_START -> foreground = true
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    fun finish() {
        voice.stop()
        // Tras la presentación tampoco se le cuentan las novedades de esta misma versión.
        val edit = prefs.edit().putInt("news_version", version)
        user?.uid?.let { edit.putBoolean("seen_$it", true) }
        edit.apply()
        runCatching { navController.popBackStack(Screen.Home.route, inclusive = false) }
        active = null
    }

    // Lleva a la pantalla del paso. Los pasos sin pantalla propia vuelven a Inicio, también
    // al retroceder con Atrás.
    LaunchedEffect(index) {
        reaction = null
        runCatching {
            val route = step.route
            if (route == null || route == Screen.Home.route) navController.popBackStack(Screen.Home.route, inclusive = false)
            else navController.navigate(route) {
                popUpTo(Screen.Home.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    // Voz del paso: al llegar a cada pantalla (y al volver a la app o quitar el silencio).
    LaunchedEffect(index, muted, foreground) {
        if (muted || !foreground) voice.stop() else { delay(1000); voice.speak(step.title + ". " + step.text) }
    }
    // Voz de la reacción al toque; al acabar, el bocadillo vuelve al paso sin repetirlo.
    LaunchedEffect(reaction) {
        val r = reaction ?: return@LaunchedEffect
        if (!muted && foreground) voice.speak(r.second)
        delay((2600L + r.second.length * 55L).coerceAtMost(14000L))
        reaction = null
    }

    // Texto que se va escribiendo, como en los juegos de anime. Tocar el bocadillo lo completa.
    suspend fun typeOut(text: String, onChar: (Int) -> Unit) {
        for (i in 1..text.length) {
            onChar(i)
            delay(if (text[i - 1] in ".,;:!?") 140L else 24L)
        }
    }
    var stepShown by remember(index) { mutableStateOf(0) }
    LaunchedEffect(index) { delay(350); typeOut(step.text) { if (it > stepShown) stepShown = it } }
    var reactionShown by remember(reaction) { mutableStateOf(0) }
    LaunchedEffect(reaction) { reaction?.let { r -> typeOut(r.second) { reactionShown = it } } }

    val fullText = reaction?.second ?: step.text
    val shown = if (reaction != null) reactionShown else stepShown
    val typing = shown < fullText.length

    fun next() {
        when {
            reaction != null -> reaction = null
            typing -> stepShown = step.text.length
            index < tour.lastIndex -> index++
            else -> finish()
        }
    }

    // Atrás: paso anterior; en el primero, saltar la visita.
    BackHandler { if (index > 0) index-- else finish() }

    // ── Dónde está el botón de este paso y hacia dónde camina Nexa ──────────────────────────
    val density = LocalDensity.current
    val conf = LocalConfiguration.current
    val screenW = conf.screenWidthDp.dp
    val screenH = conf.screenHeightDp.dp
    // Nexa chibi (sprite de 56×84): alto según la pantalla, ancho según su proporción.
    val nexaH = (screenH * 0.27f).coerceIn(190.dp, 250.dp)
    val nexaW = nexaH * (56f / 84f)
    val targetRect: Rect? = step.target?.let { NexaTargets.bounds[it] }
    val targetCenterX: Dp? = targetRect?.let { with(density) { it.center.x.toDp() } }
    // Si el botón está en la mitad de abajo, el panel sube para no taparlo.
    val panelOnTop = targetRect != null && with(density) { targetRect.center.y.toDp() } > screenH * 0.5f
    // Camina hasta el borde del lado donde está el botón; el bocadillo queda al otro lado.
    val nexaOnLeft = when {
        targetCenterX != null -> targetCenterX < screenW / 2
        else -> step.left
    }
    val destX: Dp = if (nexaOnLeft) 2.dp else screenW - nexaW - 2.dp
    // Camina a su ritmo: más lejos, más tiempo.
    val walkX = remember { Animatable(destX, Dp.VectorConverter) }
    var walking by remember { mutableStateOf(false) }
    var goingRight by remember { mutableStateOf(true) }
    LaunchedEffect(destX) {
        val distance = (destX - walkX.value).value
        if (kotlin.math.abs(distance) < 2f) { walkX.snapTo(destX); return@LaunchedEffect }
        goingRight = distance > 0
        walking = true
        val millis = (kotlin.math.abs(distance) / screenW.value * 2200).toInt().coerceIn(600, 2200)
        walkX.animateTo(destX, tween(millis, easing = LinearEasing))
        walking = false
    }
    val x = walkX.value
    // Hacia dónde mira: al andar, hacia donde va; al llegar, hacia el botón; sin botón, al centro.
    val towardTarget: Float = targetCenterX?.let { if (it > x + nexaW / 2) 1f else -1f } ?: 0f
    val facingRight = when {
        walking -> goingRight
        targetRect != null -> towardTarget > 0f
        else -> nexaOnLeft
    }
    val pose = when {
        walking || reaction != null -> NexaPose.IDLE
        step.pose != null -> step.pose
        // Con el panel arriba, el botón queda por debajo: lo señala hacia abajo.
        targetRect != null -> if (panelOnTop) NexaPose.DOWN else NexaPose.POINT
        else -> NexaPose.IDLE
    }
    var nexaBounds by remember { mutableStateOf<Rect?>(null) }

    val stride = rememberInfiniteTransition(label = "stride")
    val pulse by stride.animateFloat(0.45f, 1f, infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse), label = "pulse")
    // Al llegar: un destello sale de su mano hasta el botón y lo "toca" con dos ondas.
    val beam = remember { Animatable(0f) }
    val tap = remember { Animatable(0f) }
    LaunchedEffect(index, walking, targetRect != null) {
        beam.snapTo(0f); tap.snapTo(0f)
        if (!walking && targetRect != null) {
            delay(350)
            beam.animateTo(1f, tween(520, easing = EaseOutCubic))
            repeat(2) { tap.snapTo(0f); tap.animateTo(1f, tween(650, easing = EaseOutCubic)) }
            tap.snapTo(0f)
        }
    }
    val primary = MaterialTheme.colorScheme.primary

    Box(
        Modifier
            .fillMaxSize()
            // Mientras dura la visita no se tocan las pantallas de debajo.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        // Oscurece todo menos el botón del paso, con un anillo que late y las ondas del toque.
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color(0x9907080A))
            targetRect?.let { r ->
                val pad = 8.dp.toPx()
                val hole = Rect(r.left - pad, r.top - pad, r.right + pad, r.bottom + pad)
                val radius = CornerRadius(16.dp.toPx())
                drawRoundRect(Color.Black, hole.topLeft, hole.size, radius, blendMode = BlendMode.Clear)
                drawRoundRect(primary.copy(alpha = pulse), hole.topLeft, hole.size, radius, style = Stroke(3.dp.toPx()))
                if (tap.value > 0f) {
                    val reach = maxOf(hole.width, hole.height) / 2f
                    drawCircle(
                        primary.copy(alpha = (1f - tap.value) * 0.8f),
                        radius = 14.dp.toPx() + reach * tap.value,
                        center = r.center,
                        style = Stroke(4.dp.toPx() * (1f - tap.value) + 1f)
                    )
                    drawCircle(Color.White.copy(alpha = (1f - tap.value) * 0.5f), radius = 10.dp.toPx() * (1f - tap.value * 0.5f), center = r.center)
                }
            }
        }

        val panelColors = listOf(Color.Transparent, Color(0xB307080A), Color(0xF207080A), Color(0xFA07080A))
        val topColors = listOf(Color(0xFA07080A), Color(0xF507080A), Color(0xE607080A), Color(0x8007080A), Color.Transparent)
        Column(
            Modifier
                .align(if (panelOnTop) Alignment.TopStart else Alignment.BottomStart)
                .fillMaxWidth()
                // Degradado que tapa lo que queda detrás del panel.
                .background(Brush.verticalGradient(if (panelOnTop) topColors else panelColors))
                .then(if (panelOnTop) Modifier.statusBarsPadding() else Modifier.navigationBarsPadding())
                .padding(top = if (panelOnTop) 6.dp else 0.dp, bottom = if (panelOnTop) 0.dp else 8.dp)
        ) {
        // Escena: Nexa de pie y el bocadillo al lado de su cabeza, como en un cómic.
        val sceneBlock: @Composable () -> Unit = {
            Box(Modifier.fillMaxWidth().heightIn(min = nexaH)) {
                AnimatedContent(
                    targetState = index,
                    transitionSpec = { (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 5 }) togetherWith fadeOut(tween(150)) },
                    label = "tour_step",
                    modifier = Modifier
                        .align(if (nexaOnLeft) Alignment.TopEnd else Alignment.TopStart)
                        .padding(
                            start = if (nexaOnLeft) nexaW + 4.dp else 12.dp,
                            end = if (nexaOnLeft) 12.dp else nexaW + 4.dp,
                            top = 8.dp
                        )
                ) { i ->
                    val s = tour[i]
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(NexusTokens.Color.BgSurface)
                            .border(1.dp, primary.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                            .clickable { next() }
                            .padding(14.dp)
                            // Los lectores de pantalla leen el texto completo, no a medias.
                            .semantics { contentDescription = "Nexa. ${s.title}. ${reaction?.second ?: s.text}" }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Nexa", style = MaterialTheme.typography.labelMedium, color = primary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${i + 1}/${tour.size}", style = MaterialTheme.typography.labelSmall, color = NexusTokens.Color.TextMuted)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(s.title, style = MaterialTheme.typography.titleMedium, color = NexusTokens.Color.TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Box {
                            // El texto completo, invisible, reserva el alto para que el bocadillo no salte.
                            Text(fullText, style = MaterialTheme.typography.bodyMedium, color = Color.Transparent)
                            Text(
                                fullText.take(shown),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (reaction != null) primary else NexusTokens.Color.TextSecondary
                            )
                        }
                    }
                }
                // Nexa. Al tocarla, reacciona.
                NexaCharacter(
                    mood = reaction?.first ?: if (walking) NexaMood.SMILE else step.mood,
                    talking = speaking && !muted && !walking,
                    wordTick = wordTick,
                    pose = pose,
                    walking = walking,
                    facingRight = facingRight,
                    modifier = Modifier
                        .offset(x = x)
                        .align(Alignment.BottomStart)
                        .size(width = nexaW, height = nexaH)
                        .onGloballyPositioned { nexaBounds = it.boundsInRoot() }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (!walking) {
                                reaction = pokeLines[pokeCount % pokeLines.size]
                                pokeCount++
                            }
                        }
                )
            }
        }
        val controlsBlock: @Composable () -> Unit = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = ::finish) { Text(tr("Saltar"), color = NexusTokens.Color.TextMuted) }
                IconButton(onClick = {
                    muted = !muted
                    prefs.edit().putBoolean("muted", muted).apply()
                }) {
                    Icon(
                        if (muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                        if (muted) tr("Activar voz") else tr("Silenciar voz"),
                        tint = NexusTokens.Color.TextSecondary
                    )
                }
                // Barra de progreso fina: con muchas paradas, los puntitos empujaban el botón fuera.
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(NexusTokens.Color.BgHigh)
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((index + 1f) / tour.size)
                            .clip(CircleShape)
                            .background(primary)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Button(onClick = ::next) {
                    Text(
                        when {
                            index < tour.lastIndex -> tr("Siguiente")
                            show == NexaShow.NEWS -> tr("¡Entendido!")
                            else -> tr("¡Empezar!")
                        }
                    )
                }
            }
        }
            // Arriba: controles y luego la escena, para que Nexa quede junto al botón de abajo.
            if (panelOnTop) { controlsBlock(); sceneBlock() } else { sceneBlock(); controlsBlock() }
        }

        // Destello mágico: de la mano de Nexa al botón, en arco, con chispas.
        val nb = nexaBounds
        if (targetRect != null && nb != null && beam.value > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                // La mano que señala (el sprite la tiene a la derecha; si mira a la izquierda, se refleja).
                val f = nexaHandFraction(pose)
                val fx = if (facingRight) f.x else 1f - f.x
                val hand = Offset(nb.left + nb.width * fx, nb.top + nb.height * f.y)
                val end = targetRect.center
                val ctrl = Offset((hand.x + end.x) / 2f, minOf(hand.y, end.y) - 90.dp.toPx())
                fun at(t: Float) = Offset(
                    (1 - t) * (1 - t) * hand.x + 2 * (1 - t) * t * ctrl.x + t * t * end.x,
                    (1 - t) * (1 - t) * hand.y + 2 * (1 - t) * t * ctrl.y + t * t * end.y
                )
                val upTo = beam.value
                val fade = 1f - tap.value * 0.6f
                val steps = 26
                for (n in 0..steps) {
                    val t = n / steps.toFloat() * upTo
                    val p = at(t)
                    val r = (2.5f + 2.5f * kotlin.math.sin((t * 18f) + n)) .dp.toPx() * 0.6f
                    drawCircle(primary.copy(alpha = 0.55f * fade * (0.4f + 0.6f * t / maxOf(upTo, 0.01f))), r, p)
                }
                val head = at(upTo)
                drawCircle(Color.White.copy(alpha = 0.9f * fade), 4.dp.toPx(), head)
                drawCircle(primary.copy(alpha = 0.35f * fade), 10.dp.toPx(), head)
                nexaSparkle(hand, 9.dp.toPx() * (1f - tap.value * 0.5f), Color(0xFFD9C2FF))
            }
        }
    }
}
