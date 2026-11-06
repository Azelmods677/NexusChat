package com.Azelmods.App.ui.screens.tutorial

import com.Azelmods.App.i18n.tr

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
            id = "whats_new",
            title = tr("Novedades de la 8.0"),
            icon = "🆕",
            sections = listOf(
                TutorialSection(
                    title = tr("Personalización completa"),
                    content = "En Ajustes → Personalización → Estudio de diseño cambias las filas de los chats, las burbujas, las marcas de lectura, el campo de escribir, el inicio y los ajustes. Lo que elijas en filas se aplica también a llamadas, historias, contactos y búsqueda."
                ),
                TutorialSection(
                    title = tr("Más formas de hablar"),
                    content = "Grupos cifrados con administradores y enlace de invitación, chats secretos con una clave nueva en cada mensaje y canales para publicar a tus seguidores."
                ),
                TutorialSection(
                    title = tr("En cada chat"),
                    content = "Reenviar y seleccionar varios mensajes, encuestas, mensajes fijados, guardados y programados, compartir contactos, vista previa de enlaces y stickers propios."
                ),
                TutorialSection(
                    title = tr("Tu lista de chats"),
                    content = "Carpetas, chats ocultos con tu huella y marcar como no leído. Mantén pulsado un chat para verlo."
                ),
                TutorialSection(
                    title = tr("Herramientas"),
                    content = "Kali Linux de verdad en la terminal, descarga de vídeos y agentes de IA que guardan sus archivos en la carpeta que elijas."
                ),
                TutorialSection(
                    title = tr("Y además"),
                    content = "Avisos con el mensaje real, aviso de actualización al momento, recuperación de contraseña, vídeos de perfil de hasta 1 minuto y 30 segundos y nueva animación de entrada."
                )
            )
        ),
        Tutorial(
            id = "studio",
            title = tr("Estudio de diseño"),
            icon = "🎨",
            sections = listOf(
                TutorialSection(
                    title = tr("Dónde está"),
                    content = "Ajustes → Personalización → Estudio de diseño. Arriba ves el resultado al momento, y cada pestaña cambia una parte de la app."
                ),
                TutorialSection(
                    title = tr("Filas"),
                    content = "Tarjeta, barra lateral, línea, relieve 3D, borde, tarjeta 3D, neón, cristal o la foto de fondo. También eliges el color de las filas y el efecto de las fotos de perfil: hundida o en esfera."
                ),
                TutorialSection(
                    title = tr("Burbujas"),
                    content = "Forma, marcas de lectura, colores, tamaño de letra y una foto pequeña dentro de la burbuja."
                ),
                TutorialSection(
                    title = tr("Campo de escribir"),
                    content = "Cápsula, con tu foto, social, plano, 3D, contorno, línea o bloque. Puedes cambiar el texto del campo, quitar el emoji y añadir un botón para traducir lo que escribes."
                ),
                TutorialSection(
                    title = tr("Inicio"),
                    content = "Clásico, con tu perfil, tarjeta, portada o barra de accesos. Además: barra de búsqueda, accesos rápidos, botón de modo fantasma, historias en tarjetas y tipo de letra."
                ),
                TutorialSection(
                    title = tr("Ajustes"),
                    content = "Panel, grupos, lista, cuadrícula o encabezado. La barra de búsqueda de Ajustes encuentra cualquier opción."
                )
            )
        ),
        Tutorial(
            id = "groups",
            title = tr("Grupos, canales y chats secretos"),
            icon = "👥",
            sections = listOf(
                TutorialSection(
                    title = tr("Grupos"),
                    content = "Créalos desde Nueva conversación. Los mensajes van cifrados de extremo a extremo: cuando alguien entra o sale se crea una clave nueva, así que quien sale no lee lo que venga después."
                ),
                TutorialSection(
                    title = tr("Administrar un grupo"),
                    content = "Toca el nombre del grupo para ver sus miembros. Un administrador puede cambiar el nombre, añadir o quitar personas y nombrar a otros administradores."
                ),
                TutorialSection(
                    title = tr("Enlace de invitación"),
                    content = "En la información del grupo creas un enlace para compartir. Cámbialo cuando quieras: el anterior deja de servir. Para entrar con un enlace o un código: menú ⋮ → Unirse a un grupo."
                ),
                TutorialSection(
                    title = tr("Chats secretos"),
                    content = "Menú ⋮ → Chats secretos. Cada mensaje usa una clave nueva, no se pueden hacer capturas y el servidor borra el mensaje al entregarlo. Solo existen en el teléfono donde se crearon."
                ),
                TutorialSection(
                    title = tr("Canales"),
                    content = "Menú ⋮ → Canales. Publica para quien te siga o sigue los que te interesen. Solo el dueño publica."
                )
            )
        ),
        Tutorial(
            id = "organize",
            title = tr("Organizar tus chats"),
            icon = "🗂️",
            sections = listOf(
                TutorialSection(
                    title = tr("Carpetas"),
                    content = "Mantén pulsado un chat → Carpetas. Crea las que quieras: aparecen junto a los filtros de arriba. Se guardan solo en tu teléfono."
                ),
                TutorialSection(
                    title = tr("Chats ocultos"),
                    content = "Mantén pulsado un chat → Ocultar chat. Para verlos: menú ⋮ → Chats ocultos, con tu huella o tu bloqueo de pantalla. No salen en la búsqueda y sus avisos no dicen de quién son."
                ),
                TutorialSection(
                    title = tr("No leídos y fijados"),
                    content = "Mantén pulsado un chat para marcarlo como no leído, fijarlo, silenciarlo o archivarlo."
                ),
                TutorialSection(
                    title = tr("Seleccionar y reenviar"),
                    content = "Mantén pulsado un mensaje → Seleccionar, y toca los demás. Arriba puedes copiarlos, reenviarlos o eliminarlos."
                ),
                TutorialSection(
                    title = tr("Mensajes programados"),
                    content = "En el botón + del chat, escribe tu mensaje y elige Programar. Para ver o cancelar los pendientes: menú ⋮ del chat → Mensajes programados."
                ),
                TutorialSection(
                    title = tr("Mensajes guardados"),
                    content = "Mantén pulsado un mensaje → Guardar. Los encuentras en el menú ⋮ → Mensajes guardados."
                )
            )
        ),
        Tutorial(
            id = "stickers",
            title = tr("Stickers"),
            icon = "😀",
            sections = listOf(
                TutorialSection(
                    title = tr("Crear un sticker"),
                    content = "Abre el panel de stickers del chat y crea uno desde una foto: puedes quitar el fondo, añadir texto y un borde blanco."
                ),
                TutorialSection(
                    title = tr("Paquetes"),
                    content = "Agrupa tus stickers en paquetes y compártelos con un código. Con el código de otra persona instalas el suyo."
                ),
                TutorialSection(
                    title = tr("Stickers recibidos"),
                    content = "Mantén pulsado un sticker → Añadir a mis stickers."
                ),
                TutorialSection(
                    title = tr("Emojis animados"),
                    content = "Un mensaje con hasta tres emojis se muestra animado. En modo anónimo no se descargan las animaciones."
                )
            )
        ),
        Tutorial(
            id = "downloads",
            title = tr("Descargar vídeos"),
            icon = "⬇️",
            sections = listOf(
                TutorialSection(
                    title = tr("Cómo se usa"),
                    content = "Menú ⋮ → Descargar vídeos y pega el enlace. También puedes escribir /descargar en un chat, seguido del enlace."
                ),
                TutorialSection(
                    title = tr("De dónde"),
                    content = "YouTube, Facebook, X e Instagram. Se guarda en la carpeta Descargas del teléfono."
                )
            )
        ),
        Tutorial(
            id = "getting_started",
            title = tr("Primeros Pasos"),
            icon = "🚀",
            sections = listOf(
                TutorialSection(
                    title = tr("Bienvenido a NexusChat"),
                    content = "Mensajería con la privacidad como punto de partida: cifrado de " +
                        "extremo a extremo en los chats, llamadas de voz y vídeo P2P, historias " +
                        "de 24 h, navegación por Tor y un asistente de IA con el proveedor que tú elijas."
                ),
                TutorialSection(
                    title = tr("Crear tu cuenta"),
                    content = "1. Abre la aplicación\n" +
                        "2. Regístrate con correo y contraseña, o entra con Google\n" +
                        "3. Completa tu perfil: nombre, usuario y foto\n\n" +
                        "Tu nombre de usuario es la forma en que los demás te encuentran."
                ),
                TutorialSection(
                    title = tr("El chat de bienvenida"),
                    content = "En la lista de chats verás a **Azel Assistant**, marcado como DEMO.\n\n" +
                        "No es una demo estática: escríbele y te responde de verdad, mensaje a " +
                        "mensaje, guiándote por las funciones. Pregúntale por *cifrado*, *Tor*, " +
                        "*IA*, *historias*, *llamadas* o *código*."
                ),
                TutorialSection(
                    title = tr("Ajustes recomendados al empezar"),
                    content = "• Bloqueo de la app con huella o rostro (Privacidad y seguridad)\n" +
                        "• Color de acento y fondo a tu gusto (Apariencia)\n" +
                        "• Tu API key de IA si vas a usar el asistente (Ajustes → IA)"
                )
            )
        ),
        Tutorial(
            id = "messaging",
            title = tr("Mensajería"),
            icon = "💬",
            sections = listOf(
                TutorialSection(
                    title = tr("Empezar una conversación"),
                    content = "1. Pulsa **New Chat** en la pantalla principal\n" +
                        "2. Busca a la persona por su nombre de usuario\n" +
                        "3. Escribe y envía\n\n" +
                        "Ambos necesitáis cuenta en la app para poder hablar."
                ),
                TutorialSection(
                    title = tr("Cifrado de extremo a extremo"),
                    content = "Los chats individuales se cifran en tu dispositivo:\n" +
                        "• Intercambio de claves ECDH sobre la curva P-256\n" +
                        "• Cifrado autenticado AES-256-GCM\n" +
                        "• El servidor solo almacena el texto cifrado\n\n" +
                        "El candado 🔒 en la lista de chats indica que la conversación está cifrada."
                ),
                TutorialSection(
                    title = tr("Contenido multimedia"),
                    content = "Puedes enviar:\n" +
                        "• Fotos y vídeos\n" +
                        "• Documentos y archivos\n" +
                        "• Notas de voz\n" +
                        "• Stickers y emojis"
                ),
                TutorialSection(
                    title = tr("Mensajes temporales"),
                    content = "En la barra de escritura puedes activar el modo efímero:\n" +
                        "• Ver una sola vez\n" +
                        "• Autodestrucción pasado un tiempo\n\n" +
                        "El mensaje desaparece del chat al cumplirse la condición."
                ),
                TutorialSection(
                    title = tr("Grupos"),
                    content = "Desde **Nueva conversación** puedes crear un grupo, ponerle nombre y elegir " +
                        "miembros. Van cifrados de extremo a extremo. Mira la guía de grupos para los " +
                        "administradores y el enlace de invitación."
                ),
                TutorialSection(
                    title = tr("Estados del mensaje"),
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
            title = tr("Historias"),
            icon = "📸",
            sections = listOf(
                TutorialSection(
                    title = tr("Crear una historia"),
                    content = "1. Ve a la pestaña **Stories**\n" +
                        "2. Pulsa el botón de crear\n" +
                        "3. Haz una foto, graba un vídeo o elige de la galería\n" +
                        "4. Añade texto, emojis, música o dibuja encima\n" +
                        "5. Publica"
                ),
                TutorialSection(
                    title = tr("Música y dibujo"),
                    content = "• **Música**: la pista se sube junto a la historia y suena en bucle " +
                        "en el visor\n" +
                        "• **Dibujo**: los trazos quedan grabados en la imagen publicada\n" +
                        "• **Texto y emojis**: se arrastran para colocarlos donde quieras"
                ),
                TutorialSection(
                    title = tr("Ver historias"),
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
            title = tr("Llamadas"),
            icon = "📞",
            sections = listOf(
                TutorialSection(
                    title = tr("Voz y vídeo P2P"),
                    content = "Las llamadas usan WebRTC y viajan directamente entre dispositivos " +
                        "siempre que la red lo permite:\n" +
                        "• Audio con códec Opus\n" +
                        "• Vídeo VP8/VP9\n" +
                        "• Cifrado de transporte propio de WebRTC (DTLS-SRTP)"
                ),
                TutorialSection(
                    title = tr("Iniciar una llamada"),
                    content = "1. Abre un chat, o ve a la pestaña **Calls** y pulsa +\n" +
                        "2. Toca el icono de teléfono (voz) o cámara (vídeo)\n" +
                        "3. Concede los permisos de micrófono y cámara\n" +
                        "4. Espera a que la otra persona acepte"
                ),
                TutorialSection(
                    title = tr("Durante la llamada"),
                    content = "• Silenciar el micrófono\n" +
                        "• Altavoz\n" +
                        "• Encender o apagar la cámara\n" +
                        "• Cambiar entre cámara frontal y trasera\n" +
                        "• Colgar"
                ),
                TutorialSection(
                    title = tr("Si no suenan las llamadas"),
                    content = "El aviso de llamada entrante lo envían las Cloud Functions del " +
                        "proyecto Firebase. Si has clonado el repositorio y las llamadas no " +
                        "suenan, es casi seguro que faltan por desplegar:\n\n" +
                        "firebase deploy --only functions"
                )
            )
        ),
        Tutorial(
            id = "ai_features",
            title = tr("Azel IA"),
            icon = "🤖",
            sections = listOf(
                TutorialSection(
                    title = tr("Tú eliges la inteligencia"),
                    content = "Azel IA no está atado a un proveedor. Puedes usar Gemini, OpenAI, " +
                        "OpenRouter, DeepSeek, Qwen, GLM (Z.ai), Kimi (Moonshot), Mistral, Groq " +
                        "o cualquier servidor compatible con OpenAI.\n\n" +
                        "Tu clave se guarda cifrada en el dispositivo y nunca se comparte."
                ),
                TutorialSection(
                    title = tr("Configurarlo"),
                    content = "1. Ajustes → **IA**\n" +
                        "2. Elige proveedor en el desplegable\n" +
                        "3. Revisa la URL base y pega tu API key\n" +
                        "4. Elige modelo entre los sugeridos, o pulsa **Buscar modelos** para " +
                        "que la app pregunte al proveedor cuáles tienes disponibles\n" +
                        "5. Guarda y abre 'Acceder a Azel IA'"
                ),
                TutorialSection(
                    title = tr("Agentes que trabajan por ti"),
                    content = "Encárgales una tarea y mira cómo la resuelven: investigan, analizan, " +
                        "programan, prueban y redactan.\n\n" +
                        "Cuando crean un archivo o un PDF te piden permiso y lo guardan en la " +
                        "carpeta que elijas."
                ),
                TutorialSection(
                    title = tr("Modelos siempre al día"),
                    content = "La lista de modelos no está grabada dentro de la app: **Buscar " +
                        "modelos** consulta el endpoint del proveedor y muestra lo que hay en tu " +
                        "cuenta ahora mismo.\n\nPor eso, cuando un proveedor publica un modelo " +
                        "nuevo, aparece solo sin actualizar la app. El campo de modelo también " +
                        "acepta que escribas el identificador a mano."
                ),
                TutorialSection(
                    title = tr("Modelos locales"),
                    content = "Con Ollama (o LM Studio, vLLM, llama.cpp) corriendo en tu equipo, " +
                        "las conversaciones no salen de tu red.\n\n" +
                        "En un móvil físico, redirige el puerto:\n" +
                        "adb reverse tcp:11434 tcp:11434\n\n" +
                        "En el emulador, usa 10.0.2.2 como host. Para una IP de tu LAN hay que " +
                        "añadirla a network_security_config.xml."
                ),
                TutorialSection(
                    title = tr("Qué esperar"),
                    content = "El comportamiento y los filtros los define el modelo que elijas, " +
                        "no la app: Nexus Chat solo enruta la petición a donde tú le indicas.\n\n" +
                        "El historial de tus conversaciones con la IA se guarda en tu cuenta y " +
                        "puedes borrarlo cuando quieras."
                )
            )
        ),
        Tutorial(
            id = "privacy",
            title = tr("Privacidad"),
            icon = "🔒",
            sections = listOf(
                TutorialSection(
                    title = tr("Qué protege el cifrado"),
                    content = "En los chats y en los grupos, el contenido se cifra en tu " +
                        "dispositivo con ECDH P-256 + AES-256-GCM y el servidor solo ve texto " +
                        "cifrado.\n\n" +
                        "Qué NO oculta: que existe una conversación entre dos cuentas y cuándo " +
                        "ocurre. Los metadatos siguen en el servidor."
                ),
                TutorialSection(
                    title = tr("Bloqueo de la app"),
                    content = "Protege la app con la biometría de tu dispositivo:\n" +
                        "• Huella digital o reconocimiento facial\n" +
                        "• Bloqueo inmediato o tras un tiempo de inactividad\n\n" +
                        "Se apoya en el hardware de seguridad de Android."
                ),
                TutorialSection(
                    title = tr("Ajustes de privacidad"),
                    content = "En Privacidad y seguridad puedes controlar:\n" +
                        "• Última conexión visible\n" +
                        "• Foto de perfil visible\n" +
                        "• Confirmaciones de lectura\n\n" +
                        "Si desactivas las confirmaciones de lectura, dejas de enviar el doble " +
                        "check azul."
                ),
                TutorialSection(
                    title = tr("Copia de seguridad cifrada"),
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
            title = tr("Tor y Orbot"),
            icon = "🧅",
            sections = listOf(
                TutorialSection(
                    title = tr("Qué necesitas"),
                    content = "Tor viene dentro de la app: no hace falta instalar nada. Activa el " +
                        "**Modo anónimo** en Seguridad y Tor y la app arranca Tor por su cuenta.\n\n" +
                        "Si ya usas **Orbot**, también puedes abrirlo desde esa misma pantalla."
                ),
                TutorialSection(
                    title = tr("Activarlo"),
                    content = "1. Ajustes → **Seguridad y Tor**\n" +
                        "2. Activa **Modo anónimo**\n" +
                        "3. Espera a que confirme la conexión\n\n" +
                        "La pantalla muestra el progreso y el circuito real que está usando Tor."
                ),
                TutorialSection(
                    title = tr("Navegar sitios .onion"),
                    content = "Con el modo anónimo activo, abre el navegador integrado y escribe la " +
                        "dirección .onion.\n\n" +
                        "Una dirección válida de hoy (v3) tiene 56 caracteres antes de .onion. " +
                        "Las antiguas de 16 caracteres (v2) ya no funcionan: la red Tor las " +
                        "desconectó en 2021, así que no cargan por muy bien que funcione Orbot."
                ),
                TutorialSection(
                    title = tr("Modo anónimo"),
                    content = "El interruptor de modo anónimo enruta el tráfico de la app a " +
                        "través de Tor.\n\n" +
                        "Ten en cuenta que Tor añade latencia: las llamadas y la carga de " +
                        "multimedia irán más lentas mientras esté activo."
                )
            )
        ),
        Tutorial(
            id = "appearance",
            title = tr("Apariencia"),
            icon = "🎨",
            sections = listOf(
                TutorialSection(
                    title = tr("Tema"),
                    content = "Interruptor de tema oscuro en Apariencia. El oscuro es el " +
                        "predeterminado y es donde la interfaz está más cuidada."
                ),
                TutorialSection(
                    title = tr("Estudio de diseño"),
                    content = "Filas, burbujas, campo de escribir, inicio y ajustes se cambian en " +
                        "Ajustes → Personalización → Estudio de diseño."
                ),
                TutorialSection(
                    title = tr("36 colores y el que quieras"),
                    content = "Desde el jade de marca hasta el azul eléctrico o el sangre. Con el botón " +
                        "de colores del final eliges cualquier otro.\n\n" +
                        "El color elegido se aplica a toda la interfaz: botones, acentos, " +
                        "insignias y detalles de las tarjetas de chat."
                ),
                TutorialSection(
                    title = tr("Tamaño de texto"),
                    content = "Cuatro tamaños: Pequeño, Normal, Grande y Muy Grande.\n\n" +
                        "El cambio se aplica al instante en toda la app, no solo en los chats."
                ),
                TutorialSection(
                    title = tr("Fondos"),
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
            title = tr("Editor y terminal"),
            icon = "⚙️",
            sections = listOf(
                TutorialSection(
                    title = tr("Editor de código"),
                    content = "Un editor real dentro de la app, con resaltado de sintaxis para " +
                        "HTML, CSS, JavaScript, TypeScript, JSX, TSX, JSON, Python, Kotlin, " +
                        "Bash y C.\n\n" +
                        "Tus archivos se guardan en tu cuenta y solo tú puedes leerlos."
                ),
                TutorialSection(
                    title = tr("Vista previa y validación"),
                    content = "• Vista previa real de HTML y CSS en un WebView\n" +
                        "• Ejecución de JavaScript\n" +
                        "• Validación y formateo de JSON\n\n" +
                        "Todo se ejecuta en el dispositivo, sin enviar tu código a ningún servidor."
                ),
                TutorialSection(
                    title = tr("Terminal"),
                    content = "Tiene dos modos. **Shell** ejecuta los comandos del propio teléfono, " +
                        "con los permisos de la app.\n\n" +
                        "**Kali Linux** instala un sistema Kali real, sin root: la primera vez descarga " +
                        "unos 126 MB y ocupa cerca de 860 MB. Dentro funcionan apt y los comandos de Linux."
                )
            )
        ),
        Tutorial(
            id = "touch_gestures",
            title = tr("Gestos"),
            icon = "👆",
            sections = listOf(
                TutorialSection(
                    title = tr("En la lista de chats"),
                    content = "• Toca una tarjeta para abrir la conversación\n" +
                        "• **Mantén pulsada** una tarjeta para fijar, marcar como no leído, " +
                        "añadir a una carpeta, ocultar, silenciar, archivar o eliminar el chat\n" +
                        "• Toca el avatar para ver el perfil de la persona"
                ),
                TutorialSection(
                    title = tr("En el chat"),
                    content = "• **Mantén pulsado** un mensaje para responder a él: queda citado " +
                        "sobre la barra de escritura\n" +
                        "• Toca un emoji de la fila de reacciones para reaccionar\n" +
                        "• Usa el menú del mensaje para reenviarlo, seleccionar varios, fijarlo, " +
                        "guardarlo, traducirlo, editarlo o eliminarlo\n" +
                        "• Tira hacia abajo para recargar los mensajes"
                ),
                TutorialSection(
                    title = tr("En las historias"),
                    content = "• Toca a izquierda o derecha: anterior o siguiente\n" +
                        "• Mantén pulsado: pausar\n" +
                        "• Desliza hacia abajo: cerrar\n" +
                        "• Al crearlas, arrastra el texto y los emojis para colocarlos"
                ),
                TutorialSection(
                    title = tr("En las fotos"),
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
