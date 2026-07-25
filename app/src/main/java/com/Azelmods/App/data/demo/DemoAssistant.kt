package com.Azelmods.App.data.demo

/**
 * 🤖 Azel Assistant — guion del chat de bienvenida.
 *
 * El chat demo NO usa un modelo de IA: es un asistente guionizado y determinista
 * cuyo objetivo es dar la bienvenida y hacer un recorrido por las funciones de la
 * app **mensaje a mensaje** (no un único bloque), para que el usuario pruebe la
 * experiencia de conversación real mientras aprende qué hace NexusChat.
 *
 * [repliesFor] devuelve la LISTA de respuestas que el asistente enviará una a una,
 * con una pequeña pausa entre cada una (lo gestiona el ViewModel), en función de
 * palabras clave del mensaje del usuario. Si nada casa, avanza el "tour" por temas.
 */
object DemoAssistant {

    /** Identidad del bot (coincide con [DemoAccountManager]). */
    const val USER_ID = "demo_azel_assistant"

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
        val t = userText.lowercase().trim()

        fun matches(vararg keys: String) = keys.any { t.contains(it) }

        return when {
            matches("hola", "buenas", "hey", "hello", "hi", "saludos") -> listOf(
                "¡Hola! 👋 Soy Azel Assistant, tu guía dentro de NexusChat.",
                "Escríbeme lo que quieras y te iré mostrando funciones una a una.",
                "Prueba a preguntarme por: *cifrado*, *Tor*, *IA*, *historias* o *llamadas*. ✨"
            )

            matches("cifr", "seguridad", "e2ee", "privac", "encrypt") -> listOf(
                "🔒 Tus mensajes van cifrados de extremo a extremo (ECDH + AES-256-GCM).",
                "El servidor solo guarda texto cifrado: ni yo ni nadie más puede leerlo.",
                "La clave se genera en tu dispositivo. Ni siquiera sale de él."
            )

            matches("tor", "onion", "orbot", "anonim", "anónim") -> listOf(
                "🧅 Con Orbot instalado puedes navegar por Tor desde el navegador integrado.",
                "Activa el *Modo anónimo* en Seguridad y podrás abrir sitios .onion.",
                "Si Orbot no está corriendo, te aviso y te dejo el enlace para instalarlo."
            )

            matches("ia", "ai", "gemini", "modelo", "asistente", "deepseek", "qwen", "openrouter", "ollama") -> listOf(
                "🤖 En Ajustes → IA eliges proveedor, modelo y tu propia clave.",
                "Funciona con Gemini, OpenAI, OpenRouter, DeepSeek, Mistral, Groq… y modelos locales con Ollama.",
                "Con OpenRouter tienes de un tirón DeepSeek, Qwen Coder, GLM, Kimi y muchos más."
            )

            matches("histor", "stor", "estado") -> listOf(
                "📸 Las Historias duran 24 h y admiten foto, vídeo, texto, música y dibujo.",
                "Ve a la pestaña *Stories* y toca el botón + para crear la tuya."
            )

            matches("llam", "call", "video", "voz") -> listOf(
                "📞 Las llamadas de voz y vídeo son P2P (WebRTC), directas entre dispositivos.",
                "Desde un chat, toca el icono de teléfono o cámara para iniciar una."
            )

            matches("chat", "mensaje", "escrib", "contacto", "amigo") -> listOf(
                "💬 Para hablar con alguien real: pulsa *Nueva conversación* y busca su usuario.",
                "Los dos necesitáis cuenta en la app. También puedes crear grupos."
            )

            matches("codigo", "código", "editor", "program", "code", "terminal") -> listOf(
                "💻 Hay un editor de código con resaltado (Kotlin, JS, React, JSON…) y una terminal.",
                "Búscalos en el menú: son perfectos para trastear sin salir de la app."
            )

            matches("gracias", "genial", "perfecto", "ok", "vale", "entendido") -> listOf(
                "¡Genial! 🚀 Sigo por aquí para lo que necesites.",
                "Pregúntame por otra función cuando quieras."
            )

            else -> tourStep(turnIndex)
        }
    }

    /**
     * Cuando el mensaje no casa con ninguna palabra clave, se avanza el tour por
     * pasos para que cada respuesta sea distinta y el usuario descubra algo nuevo.
     */
    private fun tourStep(turnIndex: Int): List<String> {
        val steps = listOf(
            listOf(
                "Te leo. 👀 Mientras tanto, un dato: todo esto es un chat real, guardado en la base de datos.",
                "Prueba a escribirme *cifrado*, *Tor*, *IA*, *historias* o *llamadas* y te cuento más."
            ),
            listOf(
                "🔒 ¿Sabías que tus mensajes van cifrados de extremo a extremo? Escríbeme *cifrado* para el detalle."
            ),
            listOf(
                "🤖 Puedes conectar tu propia IA (incluida local con Ollama). Escríbeme *IA* y te guío."
            ),
            listOf(
                "🧅 Y si te va la privacidad, hay navegación por Tor. Escríbeme *Tor*."
            ),
            listOf(
                "Cuando quieras hablar con alguien de verdad, usa *Nueva conversación*. ¡Disfruta NexusChat! 🚀"
            )
        )
        return steps[turnIndex % steps.size]
    }
}
