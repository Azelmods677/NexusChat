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
                    content = "1. Toca '+' en la pantalla principal\n2. Selecciona un contacto\n3. Escribe tu mensaje\n4. Toca enviar\n\n✅ Los mensajes se cifran automáticamente con Signal Protocol."
                ),
                TutorialSection(
                    title = "Mensajes Multimedia",
                    content = "Tipos de contenido:\n• Fotos y videos\n• Documentos\n• Mensajes de voz\n• Stickers y emojis"
                ),
                TutorialSection(
                    title = "Cifrado E2EE",
                    content = "Azelgram utiliza Signal Protocol para cifrar todos los mensajes:\n• Claves efímeras por sesión\n• Perfect Forward Secrecy\n• Solo el emisor y receptor pueden leer\n• Activado por defecto en todos los chats"
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
                    content = "Azel IA es un asistente sin censura para:\n• Programación avanzada\n• Hacking ético\n• Exploits y seguridad\n• Consultas técnicas\n• Análisis de código"
                ),
                TutorialSection(
                    title = "Modo Cloud (Recomendado)",
                    content = "Usa Ollama Cloud sin necesidad de hardware local:\n1. Ve a 'IA' en el menú\n2. Selecciona 'Azel IA'\n3. Elige una categoría o escribe tu pregunta\n4. Recibe respuestas en tiempo real"
                ),
                TutorialSection(
                    title = "Modo Local (Opcional)",
                    content = "Para usuarios con Ollama instalado:\n1. Instala Ollama en tu PC\n2. Descarga modelos (llama3, codellama, etc.)\n3. Inicia: ollama serve\n4. La app se conecta automáticamente"
                ),
                TutorialSection(
                    title = "Categorías Disponibles",
                    content = "• 🔓 Exploits\n• 💻 Hacking\n• 🐍 Python\n• ⚡ JavaScript\n• 🔐 Criptografía\n• 🌐 Redes\n• 🛡️ Seguridad\n• 📱 Android\n• 🐧 Linux"
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
                    content = "Signal Protocol integrado:\n• Cifrado extremo a extremo en todos los mensajes\n• Perfect Forward Secrecy\n• Intercambio seguro de claves\n• Sin acceso de terceros al contenido"
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
                    content = "Azelgram incluye herramientas avanzadas:\n• Cifrado Signal Protocol (E2EE)\n• Bloqueo biométrico con Android Biometric\n• Tor/Orbot para navegación anónima\n• Backup cifrado AES-256\n• Ollama Cloud IA sin necesidad de hardware\n• Agente autónomo local (en desarrollo)"
                ),
                TutorialSection(
                    title = "Azel IA - Asistente Inteligente",
                    content = "IA sin censura integrada:\n• Chat sin restricciones\n• Conocimiento técnico avanzado\n• Análisis de código y vulnerabilidades\n• Streaming de respuestas en tiempo real\n\nAccede desde el menú 'IA'"
                ),
                TutorialSection(
                    title = "Orbot / Tor",
                    content = "Privacidad en internet:\n• Guía de instalación de Orbot\n• Conexión automática a la red Tor\n• Navegación de sitios .onion\n• Detección de estado en vivo cada 3s"
                ),
                TutorialSection(
                    title = "Cifrado E2EE",
                    content = "Máxima seguridad en tus mensajes:\n• Signal Protocol implementado\n• Cifrado doble ratchet\n• Intercambio de claves X3DH\n• Sin servidores intermediarios que lean tu contenido"
                )
            )
        )
    )

    fun getTutorialById(id: String): Tutorial? {
        return tutorials.find { it.id == id }
    }
}
