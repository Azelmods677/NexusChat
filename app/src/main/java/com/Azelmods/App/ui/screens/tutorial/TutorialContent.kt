package com.Azelmods.App.ui.screens.tutorial

data class TutorialSection(
    val title: String,
    val content: String
)

data class Tutorial(
    val id: String,
    val title: String,
    val icon: String,
    val sections: List<TutorialSection>
)

/**
 * Contenido de las guías de la app.
 *
 * REGLA DE ORO: aquí solo se documenta lo que la app HACE de verdad, hoy.
 * Un tutorial que promete una función inexistente es peor que no tener tutorial:
 * el usuario la busca, no la encuentra y concluye que la app está rota.
 *
 * Revisión completa en la v5. Se corrigieron:
 *  - La marca: los textos arrastraban un nombre antiguo; la app se llama **NexusChat**.
 *  - "15 colores de acento" → son 25 (`AppTheme.ACCENT_SWATCHES`).
 *  - "3 tamaños de fuente" → son 4 (Pequeño, Normal, Grande, Muy Grande).
 *  - "Tema automático según el sistema": no existe, solo hay interruptor de tema oscuro.
 *  - "Privacidad de Stories: Todos / Solo contactos / Contactos excepto…": nunca se
 *    implementó; no hay control de audiencia por historia.
 *  - "Bloquear contactos" desde el perfil: la pantalla de privacidad lista bloqueados
 *    pero no hay acción para bloquear, así que se retiró la promesa.
 *  - "Swipe left para responder": no existe ese gesto en el chat.
 *  - "Categorías" de Azel IA (Python, Linux, Criptografía…): la pantalla no tiene
 *    categorías, es un chat con historial.
 *  - Azel IA descrito como "usa Gemini": desde la v5 el proveedor y el modelo los
 *    elige el usuario, incluidos modelos locales.
 */
object TutorialContent {
    val tutorials = listOf(
        Tutorial(
            id = "getting_started",
            title = "Primeros Pasos",
            icon = "🚀",
            sections = listOf(
                TutorialSection(
                    title = "Bienvenido a Nexus Chat",
                    content = "Mensajería con la privacidad como punto de partida: cifrado de " +
                        "extremo a extremo en los chats, llamadas de voz y vídeo P2P, historias " +
                        "de 24 h, navegación por Tor y un asistente de IA con el proveedor que tú elijas."
                ),
                TutorialSection(
                    title = "Crear tu cuenta",
                    content = "1. Abre la aplicación\n" +
                        "2. Regístrate con correo y contraseña, o entra con Google\n" +
                        "3. Completa tu perfil: nombre, usuario y foto\n\n" +
                        "Tu nombre de usuario es la forma en que los demás te encuentran."
                ),
                TutorialSection(
                    title = "El chat de bienvenida",
                    content = "En la lista de chats verás a **Azel Assistant**, marcado como DEMO.\n\n" +
                        "No es una demo estática: escríbele y te responde de verdad, mensaje a " +
                        "mensaje, guiándote por las funciones. Pregúntale por *cifrado*, *Tor*, " +
                        "*IA*, *historias*, *llamadas* o *código*."
                ),
                TutorialSection(
                    title = "Ajustes recomendados al empezar",
                    content = "• Bloqueo de la app con huella o rostro (Privacidad y seguridad)\n" +
                        "• Color de acento y fondo a tu gusto (Apariencia)\n" +
                        "• Tu API key de IA si vas a usar el asistente (Ajustes → IA)"
                )
            )
        ),
        Tutorial(
            id = "messaging",
            title = "Mensajería",
            icon = "💬",
            sections = listOf(
                TutorialSection(
                    title = "Empezar una conversación",
                    content = "1. Pulsa **New Chat** en la pantalla principal\n" +
                        "2. Busca a la persona por su nombre de usuario\n" +
                        "3. Escribe y envía\n\n" +
                        "Ambos necesitáis cuenta en la app para poder hablar."
                ),
                TutorialSection(
                    title = "Cifrado de extremo a extremo",
                    content = "Los chats individuales se cifran en tu dispositivo:\n" +
                        "• Intercambio de claves ECDH sobre la curva P-256\n" +
                        "• Cifrado autenticado AES-256-GCM\n" +
                        "• El servidor solo almacena el texto cifrado\n\n" +
                        "El candado 🔒 en la lista de chats indica que la conversación está cifrada."
                ),
                TutorialSection(
                    title = "Contenido multimedia",
                    content = "Puedes enviar:\n" +
                        "• Fotos y vídeos\n" +
                        "• Documentos y archivos\n" +
                        "• Notas de voz\n" +
                        "• Stickers y emojis"
                ),
                TutorialSection(
                    title = "Mensajes temporales",
                    content = "En la barra de escritura puedes activar el modo efímero:\n" +
                        "• Ver una sola vez\n" +
                        "• Autodestrucción pasado un tiempo\n\n" +
                        "El mensaje desaparece del chat al cumplirse la condición."
                ),
                TutorialSection(
                    title = "Grupos",
                    content = "Desde **New Chat** puedes crear un grupo, ponerle nombre y elegir " +
                        "miembros.\n\nAviso honesto: los grupos NO usan cifrado de extremo a extremo; " +
                        "esa protección está activa en los chats de dos personas."
                ),
                TutorialSection(
                    title = "Estados del mensaje",
                    content = "Los checks bajo cada mensaje que envías:\n" +
                        "• Reloj: enviándose\n" +
                        "• Un check: enviado al servidor\n" +
                        "• Doble check gris: entregado\n" +
                        "• Doble check azul: leído"
                )
            )
        ),
        Tutorial(
            id = "stories",
            title = "Historias",
            icon = "📸",
            sections = listOf(
                TutorialSection(
                    title = "Crear una historia",
                    content = "1. Ve a la pestaña **Stories**\n" +
                        "2. Pulsa el botón de crear\n" +
                        "3. Haz una foto, graba un vídeo o elige de la galería\n" +
                        "4. Añade texto, emojis, música o dibuja encima\n" +
                        "5. Publica"
                ),
                TutorialSection(
                    title = "Música y dibujo",
                    content = "• **Música**: la pista se sube junto a la historia y suena en bucle " +
                        "en el visor\n" +
                        "• **Dibujo**: los trazos quedan grabados en la imagen publicada\n" +
                        "• **Texto y emojis**: se arrastran para colocarlos donde quieras"
                ),
                TutorialSection(
                    title = "Ver historias",
                    content = "• Toca el círculo de un contacto para abrir sus historias\n" +
                        "• Toca los lados para ir a la anterior o la siguiente\n" +
                        "• Mantén pulsado para pausar\n" +
                        "• Desliza hacia abajo para cerrar\n\n" +
                        "Las historias caducan a las 24 horas y se borran solas."
                )
            )
        ),
        Tutorial(
            id = "calls",
            title = "Llamadas",
            icon = "📞",
            sections = listOf(
                TutorialSection(
                    title = "Voz y vídeo P2P",
                    content = "Las llamadas usan WebRTC y viajan directamente entre dispositivos " +
                        "siempre que la red lo permite:\n" +
                        "• Audio con códec Opus\n" +
                        "• Vídeo VP8/VP9\n" +
                        "• Cifrado de transporte propio de WebRTC (DTLS-SRTP)"
                ),
                TutorialSection(
                    title = "Iniciar una llamada",
                    content = "1. Abre un chat, o ve a la pestaña **Calls** y pulsa +\n" +
                        "2. Toca el icono de teléfono (voz) o cámara (vídeo)\n" +
                        "3. Concede los permisos de micrófono y cámara\n" +
                        "4. Espera a que la otra persona acepte"
                ),
                TutorialSection(
                    title = "Durante la llamada",
                    content = "• Silenciar el micrófono\n" +
                        "• Altavoz\n" +
                        "• Encender o apagar la cámara\n" +
                        "• Cambiar entre cámara frontal y trasera\n" +
                        "• Colgar"
                ),
                TutorialSection(
                    title = "Si no suenan las llamadas",
                    content = "El aviso de llamada entrante lo envían las Cloud Functions del " +
                        "proyecto Firebase. Si has clonado el repositorio y las llamadas no " +
                        "suenan, es casi seguro que faltan por desplegar:\n\n" +
                        "firebase deploy --only functions"
                )
            )
        ),
        Tutorial(
            id = "ai_features",
            title = "Azel IA",
            icon = "🤖",
            sections = listOf(
                TutorialSection(
                    title = "Tú eliges la inteligencia",
                    content = "Azel IA no está atado a un proveedor. Puedes usar Gemini, OpenAI, " +
                        "OpenRouter, DeepSeek, Qwen, GLM (Z.ai), Kimi (Moonshot), Mistral, Groq " +
                        "o cualquier servidor compatible con OpenAI.\n\n" +
                        "Tu clave se guarda cifrada en el dispositivo y nunca se comparte."
                ),
                TutorialSection(
                    title = "Configurarlo",
                    content = "1. Ajustes → **IA**\n" +
                        "2. Elige proveedor en el desplegable\n" +
                        "3. Revisa la URL base y pega tu API key\n" +
                        "4. Elige modelo entre los sugeridos, o pulsa **Buscar modelos** para " +
                        "que la app pregunte al proveedor cuáles tienes disponibles\n" +
                        "5. Guarda y abre 'Acceder a Azel IA'"
                ),
                TutorialSection(
                    title = "Modelos siempre al día",
                    content = "La lista de modelos no está grabada dentro de la app: **Buscar " +
                        "modelos** consulta el endpoint del proveedor y muestra lo que hay en tu " +
                        "cuenta ahora mismo.\n\nPor eso, cuando un proveedor publica un modelo " +
                        "nuevo, aparece solo sin actualizar la app. El campo de modelo también " +
                        "acepta que escribas el identificador a mano."
                ),
                TutorialSection(
                    title = "Modelos locales",
                    content = "Con Ollama (o LM Studio, vLLM, llama.cpp) corriendo en tu equipo, " +
                        "las conversaciones no salen de tu red.\n\n" +
                        "En un móvil físico, redirige el puerto:\n" +
                        "adb reverse tcp:11434 tcp:11434\n\n" +
                        "En el emulador, usa 10.0.2.2 como host. Para una IP de tu LAN hay que " +
                        "añadirla a network_security_config.xml."
                ),
                TutorialSection(
                    title = "Qué esperar",
                    content = "El comportamiento y los filtros los define el modelo que elijas, " +
                        "no la app: Nexus Chat solo enruta la petición a donde tú le indicas.\n\n" +
                        "El historial de tus conversaciones con la IA se guarda en tu cuenta y " +
                        "puedes borrarlo cuando quieras."
                )
            )
        ),
        Tutorial(
            id = "privacy",
            title = "Privacidad",
            icon = "🔒",
            sections = listOf(
                TutorialSection(
                    title = "Qué protege el cifrado",
                    content = "En los chats de dos personas, el contenido se cifra en tu " +
                        "dispositivo con ECDH P-256 + AES-256-GCM y el servidor solo ve texto " +
                        "cifrado.\n\n" +
                        "Qué NO oculta: que existe una conversación entre dos cuentas y cuándo " +
                        "ocurre. Los metadatos siguen en el servidor."
                ),
                TutorialSection(
                    title = "Bloqueo de la app",
                    content = "Protege la app con la biometría de tu dispositivo:\n" +
                        "• Huella digital o reconocimiento facial\n" +
                        "• Bloqueo inmediato o tras un tiempo de inactividad\n\n" +
                        "Se apoya en el hardware de seguridad de Android."
                ),
                TutorialSection(
                    title = "Ajustes de privacidad",
                    content = "En Privacidad y seguridad puedes controlar:\n" +
                        "• Última conexión visible\n" +
                        "• Foto de perfil visible\n" +
                        "• Confirmaciones de lectura\n\n" +
                        "Si desactivas las confirmaciones de lectura, dejas de enviar el doble " +
                        "check azul."
                ),
                TutorialSection(
                    title = "Copia de seguridad cifrada",
                    content = "Ajustes → Almacenamiento → Crear copia:\n" +
                        "1. Elige una contraseña\n" +
                        "2. La copia se cifra con AES-256 antes de subirse\n" +
                        "3. Sin esa contraseña la copia no se puede restaurar\n\n" +
                        "Guárdala bien: no hay forma de recuperarla."
                )
            )
        ),
        Tutorial(
            id = "tor",
            title = "Tor y Orbot",
            icon = "🧅",
            sections = listOf(
                TutorialSection(
                    title = "Qué necesitas",
                    content = "La navegación por Tor se apoya en **Orbot**, la app oficial del " +
                        "Proyecto Tor. Nexus Chat no incluye un demonio Tor propio: habla con el " +
                        "que Orbot expone en tu dispositivo.\n\n" +
                        "Instálalo desde Google Play o F-Droid: org.torproject.android"
                ),
                TutorialSection(
                    title = "Activarlo",
                    content = "1. Abre Orbot y pulsa **Iniciar**\n" +
                        "2. Espera a que confirme la conexión a la red Tor\n" +
                        "3. Vuelve a Nexus Chat\n\n" +
                        "La app detecta Orbot sola: comprueba si su proxy responde y se conecta " +
                        "sin que tengas que reiniciar nada."
                ),
                TutorialSection(
                    title = "Navegar sitios .onion",
                    content = "Con Orbot activo, abre el navegador integrado y escribe la " +
                        "dirección .onion.\n\n" +
                        "Una dirección válida de hoy (v3) tiene 56 caracteres antes de .onion. " +
                        "Las antiguas de 16 caracteres (v2) ya no funcionan: la red Tor las " +
                        "desconectó en 2021, así que no cargan por muy bien que funcione Orbot."
                ),
                TutorialSection(
                    title = "Modo anónimo",
                    content = "El interruptor de modo anónimo enruta el tráfico de la app a " +
                        "través de Orbot.\n\n" +
                        "Ten en cuenta que Tor añade latencia: las llamadas y la carga de " +
                        "multimedia irán más lentas mientras esté activo."
                )
            )
        ),
        Tutorial(
            id = "appearance",
            title = "Apariencia",
            icon = "🎨",
            sections = listOf(
                TutorialSection(
                    title = "Tema",
                    content = "Interruptor de tema oscuro en Apariencia. El oscuro es el " +
                        "predeterminado y es donde la interfaz está más cuidada."
                ),
                TutorialSection(
                    title = "25 colores de acento",
                    content = "Desde el violeta de marca hasta Luna de Sangre, pasando por " +
                        "esmeralda, ámbar, magenta o pizarra.\n\n" +
                        "El color elegido se aplica a toda la interfaz: botones, acentos, " +
                        "insignias y detalles de las tarjetas de chat."
                ),
                TutorialSection(
                    title = "Tamaño de texto",
                    content = "Cuatro tamaños: Pequeño, Normal, Grande y Muy Grande.\n\n" +
                        "El cambio se aplica al instante en toda la app, no solo en los chats."
                ),
                TutorialSection(
                    title = "Fondos",
                    content = "Puedes personalizar el fondo de la app y de cada chat:\n" +
                        "• Imagen de tu galería\n" +
                        "• Color sólido o degradado\n" +
                        "• Vídeo como fondo animado\n\n" +
                        "La lista de chats es translúcida a propósito, para que el fondo se vea."
                )
            )
        ),
        Tutorial(
            id = "tools",
            title = "Editor y terminal",
            icon = "⚙️",
            sections = listOf(
                TutorialSection(
                    title = "Editor de código",
                    content = "Un editor real dentro de la app, con resaltado de sintaxis para " +
                        "HTML, CSS, JavaScript, TypeScript, JSX, TSX, JSON, Python, Kotlin, " +
                        "Bash y C.\n\n" +
                        "Tus archivos se guardan en tu cuenta y solo tú puedes leerlos."
                ),
                TutorialSection(
                    title = "Vista previa y validación",
                    content = "• Vista previa real de HTML y CSS en un WebView\n" +
                        "• Ejecución de JavaScript\n" +
                        "• Validación y formateo de JSON\n\n" +
                        "Todo se ejecuta en el dispositivo, sin enviar tu código a ningún servidor."
                ),
                TutorialSection(
                    title = "Terminal",
                    content = "Un emulador de terminal que ejecuta los comandos disponibles " +
                        "dentro del entorno de la aplicación en Android.\n\n" +
                        "No es una shell de sistema con privilegios: Android aísla cada app, " +
                        "así que solo alcanza lo que la propia app puede ver. Escribe 'help' " +
                        "para ver lo que admite de verdad."
                )
            )
        ),
        Tutorial(
            id = "touch_gestures",
            title = "Gestos",
            icon = "👆",
            sections = listOf(
                TutorialSection(
                    title = "En la lista de chats",
                    content = "• Toca una tarjeta para abrir la conversación\n" +
                        "• **Mantén pulsada** una tarjeta para fijar, silenciar, archivar o " +
                        "eliminar el chat\n" +
                        "• Toca el avatar para ver el perfil de la persona"
                ),
                TutorialSection(
                    title = "En el chat",
                    content = "• **Mantén pulsado** un mensaje para responder a él: queda citado " +
                        "sobre la barra de escritura\n" +
                        "• Toca un emoji de la fila de reacciones para reaccionar\n" +
                        "• Usa el menú del mensaje para traducirlo, editarlo o eliminarlo\n" +
                        "• Tira hacia abajo para recargar los mensajes"
                ),
                TutorialSection(
                    title = "En las historias",
                    content = "• Toca a izquierda o derecha: anterior o siguiente\n" +
                        "• Mantén pulsado: pausar\n" +
                        "• Desliza hacia abajo: cerrar\n" +
                        "• Al crearlas, arrastra el texto y los emojis para colocarlos"
                ),
                TutorialSection(
                    title = "En las fotos",
                    content = "• Pellizca para hacer zoom\n" +
                        "• Arrastra para desplazarte con el zoom activo\n" +
                        "• Doble toque: zoom rápido\n" +
                        "• Desliza hacia abajo: cerrar"
                )
            )
        )
    )

    fun getTutorialById(id: String): Tutorial? {
        return tutorials.find { it.id == id }
    }
}
