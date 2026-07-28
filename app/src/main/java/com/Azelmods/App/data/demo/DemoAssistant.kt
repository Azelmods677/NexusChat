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
        "*¿Qué es NexusChat?* Un mensajero completo —chats, grupos, llamadas e historias— que además trae dentro herramientas que normalmente viven en otras apps: navegador Tor, editor de código, terminal y un asistente de IA con tu propia clave.",
        "*¿Por qué existe?* Porque casi todos los mensajeros te piden confiar en su palabra. Aquí el cifrado ocurre en tu dispositivo, la clave privada no sale de él, y el código está publicado para que lo compruebes en vez de creértelo.",
        "*¿Qué puedo hacer?* Pregúntame por *cifrado*, *llamadas*, *historias*, *grupos*, *IA*, *Tor*, *traducción*, *privacidad*, *código* o *diseño*. Escribe *funciones* y te doy el índice completo. ✨"
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
                "💬 *Comunicación* — chats 1:1, grupos, llamadas de voz y vídeo, historias de 24 h.",
                "🔒 *Privacidad* — cifrado de extremo a extremo, navegación Tor, bloqueo con huella, bloqueo de contactos.",
                "🛠️ *Herramientas* — asistente de IA, editor de código, terminal, traducción de mensajes.",
                "🎨 *Personalización* — fondos propios, 25 acentos de color, tamaño de letra.",
                "Escríbeme el nombre de cualquiera y te cuento el detalle."
            )

            // ── Identidad del proyecto ──
            (starts("que") && (starts("nexus", "aplicacion") || word("es", "esto", "app"))) ||
                (starts("para") && starts("sirve", "vale")) -> listOf(
                "NexusChat es un mensajero con cifrado de extremo a extremo que además trae navegador Tor, editor de código, terminal y un asistente de IA.",
                "La idea no es competir en número de usuarios, sino enseñar cómo se construye una app así de completa: también sirve de plantilla para desarrolladores.",
                "Todo lo que ves —incluida esta conversación— funciona de verdad contra Firebase. Nada es una maqueta."
            )

            (starts("porque", "porqu") || word("motivo", "razon")) &&
                !starts("cifr", "llam", "histor", "traduc") -> listOf(
                "Porque un mensajero que te pide confiar en su palabra no es privacidad, es marketing. 🙂",
                "Aquí la clave privada se genera en tu dispositivo y no sale de él: el servidor guarda texto cifrado que no puede leer.",
                "Y porque el código está publicado, así que cualquiera puede comprobar que hace lo que dice."
            )

            // ── Seguridad y privacidad ──
            starts("cifr", "encript", "encrypt") || word("e2ee") -> listOf(
                "🔒 Tus mensajes 1:1 van cifrados de extremo a extremo con ECDH (P-256) + AES-256-GCM.",
                "La clave se deriva entre tu dispositivo y el del otro; el servidor solo almacena el resultado cifrado.",
                "Aviso honesto: los *grupos* todavía NO van cifrados de extremo a extremo, y no hay secreto hacia adelante. Está escrito tal cual en Ajustes → Acerca de."
            )

            starts("privac", "segur", "anonim") -> listOf(
                "🛡️ La privacidad aquí son cuatro cosas concretas, no un eslogan:",
                "1) Cifrado de extremo a extremo en los chats 1:1.  2) Navegación por Tor con Orbot.",
                "3) Bloqueo de la app con huella o PIN.  4) Bloqueo de contactos que aplican las reglas del servidor, no solo la interfaz.",
                "Pregúntame por *cifrado* o *Tor* para el detalle de cada uno."
            )

            starts("tor", "onion", "orbot") -> listOf(
                "🧅 Con Orbot instalado puedes navegar por Tor desde el navegador integrado.",
                "Activa el *Modo anónimo* en Seguridad y podrás abrir sitios .onion.",
                "Si Orbot no está corriendo, te aviso y te dejo el enlace para instalarlo."
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
                "Tienen administradores, permisos y ajustes propios.",
                "Aviso honesto: los grupos NO usan cifrado de extremo a extremo todavía. Los mensajes 1:1 sí."
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

            starts("codigo", "editor", "program", "terminal", "consola") || word("code") -> listOf(
                "💻 Hay un editor de código con resaltado (Kotlin, JS, React, JSON…) y una terminal real.",
                "Búscalos en el menú: son perfectos para trastear sin salir de la app."
            )

            // "disen" y no "diseñ": tokenize() ya ha quitado la tilde de la ñ.
            starts("disen", "tema", "color", "fondo", "aparienc") -> listOf(
                "🎨 La app usa el *Nexus Design System*: un solo violeta de marca, una escalera de superficies y tokens compartidos por todas las pantallas.",
                "En Ajustes → Apariencia cambias el acento entre 25 colores, pones tu propio fondo (imagen o vídeo) y ajustas el tamaño de letra.",
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
                "Prueba con *cifrado*, *llamadas*, *historias*, *IA* o *Tor*."
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
                "🔒 Los mensajes 1:1 van cifrados de extremo a extremo. Escríbeme *cifrado* y te cuento con qué."
            ),
            listOf(
                "📞 Las llamadas son P2P: el audio va directo entre los dos móviles, sin pasar por un servidor. Escríbeme *llamadas*."
            ),
            listOf(
                "🤖 Puedes conectar tu propia IA, incluso una local con Ollama, y la clave nunca sale de tu dispositivo. Escríbeme *IA*."
            ),
            listOf(
                "🧅 Y si te va la privacidad, hay navegación por Tor integrada. Escríbeme *Tor*."
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
