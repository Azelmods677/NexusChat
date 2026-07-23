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

object TutorialContent {
    val tutorials = listOf(
        Tutorial(
            id = "getting_started",
            title = "Primeros Pasos",
            icon = "🚀",
            sections = listOf(
                TutorialSection(
                    title = "Bienvenido a Azelgram",
                    content = "Azelgram Messenger es una app de mensajería segura con cifrado extremo a extremo, IA integrada, llamadas HD y herramientas de privacidad avanzadas."
                ),
                TutorialSection(
                    title = "Crear tu Cuenta",
                    content = "1. Abre la aplicación\n2. Inicia sesión con Google\n3. Completa tu perfil con nombre y foto\n4. ¡Todo cifrado desde el primer mensaje!"
                ),
                TutorialSection(
                    title = "Configuración Inicial",
                    content = "Después de registrarte:\n• Foto de perfil\n• Nombre de usuario\n• Biografía\n• Preferencias de privacidad\n• Bloqueo biométrico (recomendado)"
                )
            )
        ),
        Tutorial(
            id = "messaging",
            title = "Mensajería",
            icon = "💬",
            sections = listOf(
                TutorialSection(
                    title = "Enviar Mensajes",
                    content = "1. Toca '+' en la pantalla principal\n2. Selecciona un contacto\n3. Escribe tu mensaje\n4. Toca enviar\n\n✅ Los mensajes se cifran de extremo a extremo automáticamente (ECDH P-256 + AES-256-GCM)."
                ),
                TutorialSection(
                    title = "Mensajes Multimedia",
                    content = "Tipos de contenido:\n• Fotos y videos\n• Documentos\n• Mensajes de voz\n• Stickers y emojis"
                ),
                TutorialSection(
                    title = "Cifrado E2EE",
                    content = "Azelgram cifra tus mensajes de extremo a extremo:\n• Intercambio de claves ECDH (curva P-256)\n• Cifrado AES-256-GCM autenticado\n• Solo el emisor y el receptor pueden leer\n• Activado por defecto en todos los chats"
                ),
                TutorialSection(
                    title = "Chats Grupales",
                    content = "1. Toca 'Nuevo Grupo'\n2. Selecciona contactos\n3. Asigna nombre y foto\n4. Los mensajes grupales también se cifran"
                )
            )
        ),
        Tutorial(
            id = "stories",
            title = "Historias",
            icon = "📸",
            sections = listOf(
                TutorialSection(
                    title = "Crear una Historia",
                    content = "1. Ve a la pestaña Historias\n2. Toca el ícono de cámara\n3. Toma foto o selecciona de galería\n4. Añade texto y emojis draggables\n5. Toca 'Publicar'"
                ),
                TutorialSection(
                    title = "Ver Historias",
                    content = "• Toca el círculo de un contacto\n• Desliza para siguiente/anterior\n• Mantén presionado para pausar\n• Swipe down para cerrar\n• Expiran en 24 horas"
                ),
                TutorialSection(
                    title = "Privacidad",
                    content = "Control de quién ve tus stories:\n• Todos\n• Solo contactos\n• Contactos excepto...\n• Solo compartir con..."
                )
            )
        ),
        Tutorial(
            id = "ai_features",
            title = "Azel IA",
            icon = "🤖",
            sections = listOf(
                TutorialSection(
                    title = "Asistente Inteligente",
                    content = "Azel IA es un asistente inteligente para:\n• Programación y revisión de código\n• Redacción, resumen y traducción\n• Explicación de conceptos técnicos\n• Buenas prácticas de seguridad defensiva\n• Consultas y análisis de información"
                ),
                TutorialSection(
                    title = "Modo Cloud (Recomendado)",
                    content = "Azel IA usa Gemini con tu propia API key (sin hardware local):\n1. Añade tu API key en Ajustes → IA\n2. Ve a 'IA' en el menú y abre 'Azel IA'\n3. Elige una categoría o escribe tu pregunta\n4. Recibe respuestas en tiempo real (streaming)"
                ),
                // Sección "Modo Local (Opcional)" eliminada: describía OllamaApiService
                // (localhost:11434), clase que ya no existe en el código. El tutorial
                // prometía una feature que la app no puede cumplir.
                TutorialSection(
                    title = "Categorías Disponibles",
                    content = "• 🐍 Python\n• ⚡ JavaScript\n• 📱 Android\n• 🐧 Linux\n• 🔐 Criptografía\n• 🌐 Redes\n• 🛡️ Seguridad defensiva\n• ✍️ Redacción\n• 🌎 Traducción"
                )
            )
        ),
        Tutorial(
            id = "appearance",
            title = "Apariencia",
            icon = "🎨",
            sections = listOf(
                TutorialSection(
                    title = "Temas",
                    content = "Personaliza la apariencia:\n• Tema oscuro (predeterminado)\n• Tema claro\n• Tema automático (según el sistema)"
                ),
                TutorialSection(
                    title = "15 Colores de Acento",
                    content = "Elige entre 15 colores:\nVerde, Rojo, Azul, Morado, Teal, Rosa, Naranja, Amarillo, Cian, Índigo, Lima, Ámbar, Marrón, Gris, Azul Gris\n\nEl color se aplica a toda la interfaz."
                ),
                TutorialSection(
                    title = "Tamaño de Fuente",
                    content = "Ajustes de texto:\n• Pequeño\n• Mediano (predeterminado)\n• Grande"
                ),
                TutorialSection(
                    title = "Fondo de Chat",
                    content = "Personaliza fondos:\n• Predeterminado\n• Imagen de galería\n• Colores sólidos\n• Degradados\n• Video wallpaper"
                )
            )
        ),
        Tutorial(
            id = "touch_gestures",
            title = "Gestos Táctiles",
            icon = "👆",
            sections = listOf(
                TutorialSection(
                    title = "Navegación por Swipe",
                    content = "Desliza horizontalmente entre pantallas:\n• Chats ↔ Stories ↔ Llamadas ↔ Perfil\n• Swipe LEFT: Siguiente pantalla\n• Swipe RIGHT: Pantalla anterior\n• También funciona con tap en íconos"
                ),
                TutorialSection(
                    title = "Gestos en Stories",
                    content = "• Tap izquierda/derecha: Story anterior/siguiente\n• Swipe izquierda/derecha: Usuario anterior/siguiente\n• Long press: Pausar\n• Swipe down: Cerrar viewer\n• Drag & drop: Mover texto/emojis"
                ),
                TutorialSection(
                    title = "Gestos en Fotos",
                    content = "• Pinch to zoom: 1x a 4x\n• Drag: Mover imagen en zoom\n• Double tap: Zoom rápido 2x\n• Swipe down: Cerrar viewer"
                ),
                TutorialSection(
                    title = "Gestos en Chat",
                    content = "• Long press: Menú contextual (copiar, eliminar, reenviar)\n• Swipe left: Responder rápido\n• Pull to refresh: Actualizar mensajes"
                )
            )
        ),
        Tutorial(
            id = "privacy",
            title = "Privacidad",
            icon = "🔒",
            sections = listOf(
                TutorialSection(
                    title = "Configuración de Privacidad",
                    content = "Controla tu privacidad:\n• Última vez en línea\n• Foto de perfil\n• Información personal\n• Stories\n• Bloqueo biométrico"
                ),
                TutorialSection(
                    title = "Cifrado E2EE",
                    content = "Cifrado E2EE integrado:\n• Cifrado extremo a extremo en todos los mensajes\n• Intercambio de claves ECDH por destinatario\n• AES-256-GCM (cifrado autenticado)\n• Sin acceso de terceros al contenido"
                ),
                TutorialSection(
                    title = "Bloqueo Biométrico",
                    content = "Protege la app con:\n• Huella digital\n• Reconocimiento facial\n• Bloqueo inmediato / 1 min / 5 min\n\nUsa el hardware de seguridad de tu dispositivo."
                ),
                TutorialSection(
                    title = "Tor / Orbot",
                    content = "Navegación anónima:\n• Orbot Setup: guía de instalación paso a paso\n• Navegación .onion integrada\n• Proxy automático a través de Tor\n• Detección de estado de Orbot en tiempo real"
                ),
                TutorialSection(
                    title = "Bloquear Contactos",
                    content = "1. Abre el perfil del contacto\n2. Toca 'Más opciones'\n3. Selecciona 'Bloquear'\n4. El contacto no podrá contactarte"
                ),
                TutorialSection(
                    title = "Backup Cifrado",
                    content = "Realiza backups cifrados con AES-256:\n1. Ajustes → Almacenamiento → Crear Backup\n2. Establece una contraseña\n3. Se almacena cifrado en Firebase Storage\n4. Restaura en cualquier dispositivo"
                )
            )
        ),
        Tutorial(
            id = "calls",
            title = "Llamadas",
            icon = "📞",
            sections = listOf(
                TutorialSection(
                    title = "Llamadas WebRTC",
                    content = "Llamadas de audio y video HD con WebRTC:\n• Codec Opus para audio\n• VP8/VP9 para video\n• Resoluciones hasta 1080p\n• Cifrado integrado"
                ),
                TutorialSection(
                    title = "Iniciar Llamada",
                    content = "1. Abre un chat o ve a Llamadas\n2. Tapa en 📞 (audio) o 📹 (video)\n3. Concede los permisos si se solicita\n4. Espera a que el otro usuario acepte"
                ),
                TutorialSection(
                    title = "Controles",
                    content = "Durante la llamada:\n• 🔇 Mute/Unmute\n• 🔊 Altavoz\n• 📹 Cámara on/off (video)\n• 🔄 Cambiar cámara\n• ❌ Colgar"
                )
            )
        ),
        Tutorial(
            id = "framework",
            title = "Herramientas Avanzadas",
            icon = "⚙️",
            sections = listOf(
                TutorialSection(
                    title = "Funciones Exclusivas 2026",
                    content = "Azelgram incluye herramientas avanzadas:\n• Cifrado E2EE (ECDH + AES-256-GCM)\n• Bloqueo biométrico con Android Biometric\n• Tor/Orbot para navegación anónima\n• Backup cifrado AES-256\n• IA integrada (Gemini) con tu propia API key\n• Asistente Azel IA dentro de la app"
                ),
                TutorialSection(
                    title = "Azel IA - Asistente Inteligente",
                    content = "Asistente de IA integrado:\n• Chat con respuestas en tiempo real\n• Conocimiento técnico general\n• Ayuda con código y buenas prácticas de seguridad\n• Streaming de respuestas en tiempo real\n\nAccede desde el menú 'IA'"
                ),
                TutorialSection(
                    title = "Orbot / Tor",
                    content = "Privacidad en internet:\n• Guía de instalación de Orbot\n• Conexión automática a la red Tor\n• Navegación de sitios .onion\n• Detección de estado en vivo cada 3s"
                ),
                TutorialSection(
                    title = "Cifrado E2EE",
                    content = "Máxima seguridad en tus mensajes:\n• Cifrado extremo a extremo real\n• AES-256-GCM (cifrado autenticado)\n• Intercambio de claves ECDH P-256\n• Sin servidores intermediarios que lean tu contenido"
                )
            )
        )
    )

    fun getTutorialById(id: String): Tutorial? {
        return tutorials.find { it.id == id }
    }
}
