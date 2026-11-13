package com.Azelmods.App.data.demo

import java.text.Normalizer

/**
 * 🤖 Azel Assistant — guion del chat de bienvenida.
 *
 * El chat demo NO usa un modelo de IA: es un asistente guionizado y determinista
 * cuyo objetivo es dar la bienvenida y explicar **qué es NexusChat, por qué
 * existe y qué hace**, mensaje a mensaje (no un único bloque), para que el
 * usuario pruebe la experiencia de conversación real mientras aprende.
 *
 * ## Por qué se reescribió el emparejado de palabras
 *
 * La versión anterior decidía el tema con `texto.contains(clave)` sobre claves
 * de dos letras. La entrada `"ia"` casaba dentro de **"histor*ia*s"**,
 * **"grac*ia*s"**, **"famil*ia*"** o **"gu*ía*"**, y como esa rama iba antes que
 * la de historias, preguntar por las historias respondía sobre proveedores de
 * IA. Lo mismo ocurría con `"ok"` y `"stor"`. Ahora se compara contra **palabras
 * completas** (o prefijos explícitos) sobre el texto normalizado sin acentos.
 *
 * ## Por qué el tour ya no da la vuelta
 *
 * `tourStep` hacía `steps[turnIndex % steps.size]`: al sexto mensaje el usuario
 * volvía a leer el primer paso, y la sensación era la de un bot que se reinicia
 * solo. Ahora el recorrido es finito y, al terminar, cae en un cierre estable
 * que invita a preguntar por temas concretos.
 */
object DemoAssistant {

    /** Identidad del bot (coincide con [DemoAccountManager]). */
    const val USER_ID = "demo_azel_assistant"

    /**
     * Mensajes que el bot envía al abrirse el chat, antes de que el usuario
     * escriba nada. Responden a las tres preguntas que importan: qué es esto,
     * por qué existe y qué se puede hacer aquí.
     */
    val welcome: List<String> = listOf(
        "¡Hola! 👋 Soy Azel Assistant. Esto es un chat de verdad: lo que escribas viaja por la misma base de datos que cualquier otra conversación de la app.",
        "*¿Qué es NexusChat?* Un mensajero hecho para que puedas hablar sin dejar rastro. Chats, grupos, llamadas e historias, pero pensados desde el primer día para el anonimato.",
        "*¿Por qué existe?* Porque cifrar el mensaje no basta: los demás mensajeros siguen sabiendo con quién hablas, cuándo y desde dónde. Aquí el texto se cifra en tu teléfono, la vista previa también, Tor viene dentro y un solo interruptor te vuelve invisible.",
        "*¿Qué puedo hacer?* Pregúntame por *novedades*, *personalizar*, *cifrado*, *chats secretos*, *grupos*, *canales*, *stickers*, *carpetas*, *agentes*, *Kali* o *Tor*. Escribe *funciones* y te doy el índice completo. ✨"
    )

    /**
     * Novedades de la versión instalada, leídas del CHANGELOG empaquetado en la app.
     * Se cargan al arrancar ([loadReleaseNotes]); si no hay, se usa el resumen de la 8.
     */
    @Volatile
    private var releaseNotes: Pair<String, List<String>>? = null

    /** Lee la primera sección `## vX.Y.Z` de `CHANGELOG.md` (la más reciente). */
    fun loadReleaseNotes(context: android.content.Context) {
        releaseNotes = runCatching {
            context.assets.open("CHANGELOG.md").bufferedReader().useLines { lines ->
                var version: String? = null
                val bullets = mutableListOf<String>()
                for (line in lines) {
                    val t = line.trim()
                    if (t.startsWith("## v")) {
                        if (version != null) break
                        version = t.removePrefix("## v").trim()
                    } else if (version != null && t.startsWith("- ")) {
                        bullets += t.removePrefix("- ").trim()
                    } else if (version != null && t == "---") {
                        break
                    }
                }
                version?.let { it to bullets.take(8) }
            }
        }.getOrNull()
    }

    private fun latestNotes(): List<String>? = releaseNotes?.let { (version, bullets) ->
        if (bullets.isEmpty()) null
        else listOf("🆕 Novedades de NexusChat $version:") + bullets.map { "• $it" }
    }

    /** Lo que se le cuenta una sola vez a quien ya tenía la app al llegar esta versión. */
    val whatsNew: List<String> get() = latestNotes()?.plus("Escríbeme *novedades* cuando quieras volver a verlas.") ?: whatsNew8

    private val whatsNew8: List<String> = listOf(
        "🆕 ¡NexusChat 8 ya está aquí! Te cuento lo que trae:",
        "🎨 *Personaliza todo* en Ajustes → Personalización → Estudio de diseño: filas, burbujas, campo de escribir, inicio y ajustes.",
        "🔐 *Grupos cifrados*, *chats secretos* con una clave nueva en cada mensaje, y *canales*.",
        "💬 Reenviar, seleccionar varios mensajes, encuestas, mensajes programados, carpetas, chats ocultos y stickers propios.",
        "🛠️ *Kali Linux* en la terminal y *descarga de vídeos*. Escríbeme *novedades* cuando quieras el detalle."
    )

    /**
     * Respuestas contextuales según el texto del usuario. Devuelve varias líneas
     * cortas a propósito: el ViewModel las envía por separado para simular una
     * conversación real.
     *
     * @param userText   lo que acaba de escribir el usuario.
     * @param turnIndex  cuántos mensajes ha escrito ya el usuario en este chat
     *                   (0 = primero). Sirve para avanzar el tour sin repetir.
     */
    fun repliesFor(userText: String, turnIndex: Int): List<String> {
        val words = tokenize(userText)

        /** Palabra exacta: "ia" casa con "ia", nunca dentro de "historias". */
        fun word(vararg keys: String) = keys.any { key -> words.any { it == key } }

        /** Prefijo de palabra: "cifr" casa con "cifrado" y "cifrar". */
        fun starts(vararg keys: String) = keys.any { key -> words.any { it.startsWith(key) } }

        return when {
            // ── Índice de funciones ──
            starts("funcion", "menu", "indice") || word("ayuda", "help", "opciones") -> listOf(
                "Esto es lo que hay dentro, por bloques 👇",
                "💬 *Comunicación* — chats, grupos, canales, llamadas de voz y vídeo, historias de 24 h, encuestas y stickers.",
                "🔒 *Privacidad* — cifrado de extremo a extremo, chats secretos, Tor integrado, chats ocultos, bloqueo con huella.",
                "🛠️ *Herramientas* — agentes de IA, editor de código, terminal con Kali Linux, descarga de vídeos, traducción.",
                "🎨 *Personalización* — Estudio de diseño, 36 colores más el que tú quieras, fondos propios y tipos de letra.",
                "🗂️ *Organización* — carpetas, mensajes guardados y programados.",
                "Escríbeme el nombre de cualquiera y te cuento el detalle."
            )

            // ── Novedades de la versión ──
            starts("novedad", "actualiz", "version") || word("nuevo", "nueva", "nuevas", "nuevos") -> (latestNotes() ?: emptyList()) + listOf(
                "📦 Y lo que trajo la 8, la actualización más grande hasta ahora:",
                "🎨 *Estudio de diseño*: filas, burbujas, campo de escribir, inicio y ajustes a tu gusto.",
                "🔐 *Grupos cifrados*, *chats secretos* y *canales*.",
                "💬 *Reenviar*, seleccionar, *encuestas*, *programar*, *carpetas*, *chats ocultos* y *stickers* propios.",
                "🛠️ *Kali Linux* en la terminal, *descarga de vídeos* y agentes que guardan archivos con tu permiso.",
                "Escríbeme el nombre de cualquiera y te lo explico."
            )

            starts("personaliz", "estudio", "fila", "burbuj", "entry", "row") -> listOf(
                "🎨 Ve a Ajustes → Personalización → *Estudio de diseño*.",
                "Filas: tarjeta, barra lateral, línea, relieve 3D, borde, tarjeta 3D, neón, cristal o foto de fondo. También en llamadas, historias y contactos.",
                "Burbujas, marcas de lectura, campo de escribir, inicio y ajustes tienen sus propias opciones, y lo ves al momento arriba."
            )

            starts("secret") -> listOf(
                "🔐 Los chats secretos están en el menú ⋮ → *Chats secretos*.",
                "Cada mensaje usa una clave nueva, no se pueden hacer capturas y el servidor lo borra al entregarlo.",
                "Solo existen en el teléfono donde se crearon."
            )

            starts("canal") -> listOf(
                "📣 Los canales están en el menú ⋮ → *Canales*.",
                "Crea el tuyo y publica para tus seguidores, o sigue los de otros. Solo el dueño publica."
            )

            starts("sticker", "pegatina") -> listOf(
                "😀 En el panel de stickers del chat creas uno desde una foto: quitas el fondo, añades texto y un borde.",
                "Agrúpalos en paquetes y compártelos con un código.",
                "Y si te envían uno que te gusta: mantenlo pulsado → *Añadir a mis stickers*."
            )

            starts("carpeta", "ocult", "archiv") -> listOf(
                "🗂️ Mantén pulsado un chat para añadirlo a una *carpeta*, *ocultarlo* o archivarlo.",
                "Los ocultos se abren desde el menú ⋮ → *Chats ocultos*, con tu huella."
            )

            starts("reenvi", "selecc", "encuest", "programar", "programad", "guardad", "fijad") -> listOf(
                "💬 Mantén pulsado un mensaje para *reenviarlo*, *seleccionar* varios, *fijarlo* o *guardarlo*.",
                "En el botón + del chat tienes *Encuesta*, *Contacto* y *Programar*. Lo programado se ve y se cancela en el menú ⋮ del chat."
            )

            starts("descarg", "youtube", "instagram", "facebook") -> listOf(
                "⬇️ Menú ⋮ → *Descargar vídeos*, o escribe /descargar y el enlace en un chat.",
                "Funciona con YouTube, Facebook, X e Instagram, y se guarda en Descargas."
            )

            starts("agente") -> listOf(
                "🤖 Encarga una tarea a los agentes de Azel IA y míralos trabajar: investigan, analizan, programan, prueban y redactan.",
                "Si crean un archivo o un PDF, te piden permiso y lo guardan en la carpeta que elijas."
            )

            // ── Identidad del proyecto ──
            (starts("que") && (starts("nexus", "aplicacion") || word("es", "esto", "app"))) ||
                (starts("para") && starts("sirve", "vale")) -> listOf(
                "NexusChat es un mensajero anónimo. No es una app de chat con un ajuste de privacidad escondido: el anonimato es la razón por la que existe.",
                "Lo que escribes sale cifrado de tu teléfono, la vista previa del chat también, tu conexión puede ir por Tor y el modo anónimo deja de publicar cuándo estás en línea.",
                "Encima de eso está todo lo que esperas de un mensajero, y un estudio de diseño para que tu app no se parezca a la de nadie. La hizo Azelmodsx677."
            )

            (starts("porque", "porqu") || word("motivo", "razon")) &&
                !starts("cifr", "llam", "histor", "traduc") -> listOf(
                "Porque hablar en privado se ha vuelto un privilegio técnico, y no debería serlo. 🕶️",
                "Los grandes mensajeros cifran el texto, pero guardan quién habla con quién, cuándo y desde qué red. Eso dice tanto de ti como el propio mensaje.",
                "NexusChat existe para que eso no quede registrado: cifrado en tu teléfono, Tor integrado, modo anónimo de un toque, sin anuncios y sin vender datos."
            )

            // ── Seguridad y privacidad ──
            starts("cifr", "encript", "encrypt") || word("e2ee") -> listOf(
                "🔒 Tus chats y tus grupos van cifrados de extremo a extremo con ECDH (P-256) + AES-256-GCM.",
                "La clave se deriva entre tu dispositivo y el del otro; el servidor solo almacena el resultado cifrado.",
                "Si quieres aún más, los *chats secretos* usan una clave nueva en cada mensaje. Lo tienes detallado en Ajustes → Acerca de."
            )

            starts("privac", "segur", "anonim") -> listOf(
                "🕶️ El anonimato aquí son cosas concretas, no un eslogan:",
                "1) Mensajes y vista previa cifrados en tu teléfono: el servidor no puede leerlos.  2) Tor integrado: el servidor ve una salida de Tor, no tu red.",
                "3) Modo anónimo: dejas de publicar tu *en línea* y tu *última vez*, y no se generan vistas previas de enlaces que revelen tu dirección.",
                "4) Chats secretos con clave nueva en cada mensaje, chats ocultos tras tu huella y archivos que solo ven los participantes.",
                "Pregúntame por *cifrado* o *Tor* para el detalle de cada uno."
            )

            starts("tor", "onion", "orbot") -> listOf(
                "🧅 Tor viene dentro de la app: no hace falta instalar nada.",
                "Activa el *Modo anónimo* en Seguridad y Tor y podrás abrir sitios .onion.",
                "Si prefieres Orbot, también puedes abrirlo desde esa misma pantalla."
            )

            // ── Historias ──
            // Antes esta rama iba DESPUÉS de la de IA y era inalcanzable:
            // "historias" contiene "ia".
            starts("histor", "stories", "estado") || word("story") -> listOf(
                "📸 Las Historias duran 24 h y admiten foto, vídeo, texto, música y dibujo.",
                "Ve a la pestaña *Stories* y toca el botón + para crear la tuya.",
                "Puedes ver quién la ha visto y recibir reacciones con emoji."
            )

            starts("llam", "videollam", "call") || word("voz", "video") -> listOf(
                "📞 Las llamadas de voz y vídeo son P2P con WebRTC: el audio y el vídeo van directos entre los dos dispositivos.",
                "Desde un chat, toca el icono de teléfono o el de cámara para iniciarlas.",
                "Si el otro no contesta, la llamada se cierra sola a los 45 segundos y queda el aviso de llamada perdida."
            )

            starts("grupo") -> listOf(
                "👥 Puedes crear grupos desde *Nueva conversación* → *Nuevo grupo*.",
                "Van cifrados de extremo a extremo y tienen administradores.",
                "En la información del grupo creas un *enlace de invitación*. Para entrar con uno: menú ⋮ → *Unirse a un grupo*."
            )

            starts("traduc", "translat", "idioma") -> listOf(
                "🌐 Mantén pulsado cualquier mensaje y elige *Traducir* para verlo en tu idioma.",
                "El idioma de destino se configura en Ajustes → Idioma de traducción.",
                "El original no se toca: la traducción aparece debajo y puedes ocultarla cuando quieras."
            )

            // "ia" y "ai" como PALABRAS, no como trozos de otra palabra.
            word("ia", "ai", "gemini", "chatgpt", "openai", "deepseek", "qwen", "ollama", "openrouter", "mistral", "groq") ||
                starts("inteligencia", "asistent", "modelo") -> listOf(
                "🤖 En Ajustes → IA eliges proveedor, modelo y tu propia clave.",
                "Funciona con Gemini, OpenAI, OpenRouter, DeepSeek, Mistral, Groq… y modelos locales con Ollama.",
                "La clave se guarda cifrada en tu dispositivo y las peticiones salen directas al proveedor: no pasan por ningún servidor intermedio."
            )

            starts("codigo", "editor", "program", "terminal", "consola", "kali", "linux") || word("code") -> listOf(
                "💻 Hay un editor de código con resaltado (Kotlin, JS, React, JSON…) y una terminal real.",
                "En la terminal, el botón *Kali Linux* instala un Kali de verdad, sin root: funcionan apt y los comandos de Linux.",
                "Búscalos en el menú ⋮."
            )

            // "disen" y no "diseñ": tokenize() ya ha quitado la tilde de la ñ.
            starts("disen", "tema", "color", "fondo", "aparienc") -> listOf(
                "🎨 En Ajustes → Personalización tienes el *Estudio de diseño*: filas, burbujas, campo de escribir, inicio y ajustes.",
                "En *Color y fondo* cambias el acento entre 36 colores o eliges cualquier otro, pones tu propio fondo (imagen o vídeo) y ajustas el tamaño de letra.",
                "La pantalla de inicio es translúcida a propósito: el fondo que elijas se ve a través de las tarjetas."
            )

            starts("contact", "amigo", "convers", "escrib", "mensaj") || word("chat") -> listOf(
                "💬 Para hablar con alguien real: pulsa *Nueva conversación* y busca su usuario.",
                "Los dos necesitáis cuenta en la app. También puedes crear grupos o compartir tu QR."
            )

            starts("gracias", "genial", "perfecto", "entendido") || word("vale", "ok", "okey") -> listOf(
                "¡Genial! 🚀 Sigo por aquí para lo que necesites.",
                "Escribe *funciones* cuando quieras volver al índice."
            )

            starts("hola", "buenas", "saludos") || word("hey", "hello", "hi") -> listOf(
                "¡Hola otra vez! 👋 ¿Por dónde quieres seguir?",
                "Prueba con *novedades*, *personalizar*, *cifrado*, *grupos*, *agentes* o *Tor*."
            )

            else -> tourStep(turnIndex)
        }
    }

    /**
     * Recorrido guiado para cuando el mensaje no casa con ningún tema.
     *
     * Es finito: al llegar al final se queda en el cierre en lugar de volver al
     * principio, para que el bot no dé la sensación de reiniciarse.
     */
    private fun tourStep(turnIndex: Int): List<String> {
        val steps = listOf(
            listOf(
                "Te leo. 👀 Te sigo enseñando cosas mientras tanto.",
                "🔒 Tus chats y grupos van cifrados de extremo a extremo. Escríbeme *cifrado* y te cuento con qué."
            ),
            listOf(
                "📞 Las llamadas son P2P: el audio va directo entre los dos móviles, sin pasar por un servidor. Escríbeme *llamadas*."
            ),
            listOf(
                "🤖 Puedes conectar tu propia IA, incluso una local con Ollama, y la clave nunca sale de tu dispositivo. Escríbeme *IA*."
            ),
            listOf(
                "🧅 Y si te va la privacidad, hay Tor integrado. Escríbeme *Tor*."
            ),
            listOf(
                "🎨 Puedes cambiar casi todo el aspecto de la app. Escríbeme *personalizar*."
            ),
            listOf(
                "📸 También hay Historias de 24 h con foto, vídeo, texto y música. Escríbeme *historias*."
            )
        )
        val closing = listOf(
            "Ya te he enseñado lo esencial. 🚀 Escribe *funciones* para ver el índice completo, o el nombre de lo que te interese.",
            "Y cuando quieras hablar con alguien de verdad, usa *Nueva conversación*."
        )
        return steps.getOrElse(turnIndex) { closing }
    }

    /**
     * Divide el texto en palabras comparables: minúsculas, sin acentos y sin
     * signos. Sin quitar acentos, "traducción" y "traduccion" serían temas
     * distintos según cómo escriba cada usuario.
     */
    private fun tokenize(text: String): List<String> {
        val normalized = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return normalized.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
    }
}
