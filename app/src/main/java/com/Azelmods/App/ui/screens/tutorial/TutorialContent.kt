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
                    title = "Bienvenido a Nexus Chat",
                    content = "Nexus Chat es una aplicación de mensajería segura con funciones avanzadas de IA, llamadas y privacidad."
                ),
                TutorialSection(
                    title = "Crear tu Cuenta",
                    content = "1. Abre la aplicación\n2. Toca 'Registrarse'\n3. Ingresa tu correo y contraseña\n4. Completa tu perfil con nombre y foto"
                ),
                TutorialSection(
                    title = "Configuración Inicial",
                    content = "Después de registrarte, configura:\n• Foto de perfil\n• Nombre de usuario\n• Biografía\n• Preferencias de privacidad"
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
                    content = "1. Toca el ícono '+' en la pantalla principal\n2. Selecciona un contacto\n3. Escribe tu mensaje\n4. Toca enviar"
                ),
                TutorialSection(
                    title = "Mensajes Multimedia",
                    content = "Puedes enviar:\n• Fotos y videos\n• Documentos\n• Ubicación\n• Mensajes de voz\n• Stickers"
                ),
                TutorialSection(
                    title = "Chats Grupales",
                    content = "Crea grupos para chatear con múltiples personas:\n1. Toca 'Nuevo Grupo'\n2. Selecciona contactos\n3. Asigna nombre y foto al grupo"
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
                    content = "1. Toca el ícono de cámara en Historias\n2. Toma una foto o selecciona de galería\n3. Ajusta la posición con gestos\n4. Toca 'Publicar'"
                ),
                TutorialSection(
                    title = "Ver Historias",
                    content = "• Toca el círculo de un contacto para ver su historia\n• Desliza para ver la siguiente\n• Mantén presionado para pausar"
                ),
                TutorialSection(
                    title = "Privacidad de Historias",
                    content = "Controla quién puede ver tus historias:\n• Todos\n• Solo contactos\n• Contactos excepto...\n• Solo compartir con..."
                )
            )
        ),
        Tutorial(
            id = "ai_features",
            title = "Funciones de IA",
            icon = "🤖",
            sections = listOf(
                TutorialSection(
                    title = "Azel IA",
                    content = "Asistente de IA sin censura para:\n• Programación avanzada\n• Hacking ético\n• Exploits y seguridad\n• Consultas técnicas"
                ),
                TutorialSection(
                    title = "Usar Azel IA",
                    content = "1. Ve a 'IA' en el menú\n2. Selecciona 'Azel IA'\n3. Elige una categoría o escribe tu pregunta\n4. Recibe respuestas sin censura"
                ),
                TutorialSection(
                    title = "Categorías Disponibles",
                    content = "• 🔓 Exploits\n• 💻 Hacking\n• 🐍 Python\n• ⚡ JavaScript\n• 🔐 Criptografía\n• 🌐 Redes\n• 🛡️ Seguridad"
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
                    content = "Personaliza la apariencia:\n• Tema oscuro (predeterminado)\n• Tema claro\n• Tema automático (según hora del día)"
                ),
                TutorialSection(
                    title = "Colores de Acento",
                    content = "Elige tu color favorito:\n• Verde\n• Rojo\n• Azul\n• Morado (predeterminado)\n• Teal\n\nEl color se aplica a toda la interfaz"
                ),
                TutorialSection(
                    title = "Tamaño de Fuente",
                    content = "Ajusta el tamaño del texto:\n• Pequeño\n• Mediano (predeterminado)\n• Grande"
                ),
                TutorialSection(
                    title = "Fondo de Chat",
                    content = "Personaliza el fondo de tus chats:\n• Predeterminado\n• Imagen de galería\n• Colores sólidos"
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
                    content = "Desliza horizontalmente entre pantallas:\n• Chats ↔ Stories ↔ Calls ↔ Profile\n• Swipe LEFT: Siguiente pantalla\n• Swipe RIGHT: Pantalla anterior\n• Animación suave como WhatsApp"
                ),
                TutorialSection(
                    title = "Gestos en Stories",
                    content = "• Tap izquierda/derecha: Story anterior/siguiente\n• Swipe izquierda/derecha: Usuario anterior/siguiente\n• Long press: Pausar story\n• Swipe down: Cerrar viewer"
                ),
                TutorialSection(
                    title = "Gestos en Fotos",
                    content = "• Pinch to zoom: Zoom 1x a 4x\n• Drag: Mover imagen en zoom\n• Double tap: Zoom rápido 2x\n• Swipe down: Cerrar viewer"
                ),
                TutorialSection(
                    title = "Gestos en Chat",
                    content = "• Long press: Menú contextual\n• Swipe left: Responder rápido\n• Swipe right: Archivar\n• Pull to refresh: Actualizar"
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
                    content = "Controla tu privacidad:\n• Última vez en línea\n• Foto de perfil\n• Información personal\n• Estados"
                ),
                TutorialSection(
                    title = "Bloquear Contactos",
                    content = "Bloquea usuarios no deseados:\n1. Abre el perfil del contacto\n2. Toca 'Más opciones'\n3. Selecciona 'Bloquear'\n4. Confirma la acción"
                ),
                TutorialSection(
                    title = "Seguridad Avanzada",
                    content = "Funciones de seguridad:\n• Cifrado de extremo a extremo\n• Verificación en dos pasos\n• Bloqueo de aplicación\n• Tor Browser integrado"
                )
            )
        ),
        Tutorial(
            id = "framework",
            title = "Herramientas Avanzadas",
            icon = "⚙️",
            sections = listOf(
                TutorialSection(
                    title = "Funciones Avanzadas",
                    content = "Nexus Chat incluye herramientas avanzadas:\n• Automatización de tareas\n• Terminal integrado\n• Herramientas de ciberseguridad\n• Generación de payloads"
                ),
                TutorialSection(
                    title = "AI Ollama",
                    content = "IA local integrada:\n• Chat sin restricciones\n• 90 prompts especializados\n• 9 categorías técnicas\n\nAccede desde el menú 'IA'"
                ),
                TutorialSection(
                    title = "Terminal",
                    content = "Terminal integrado con:\n• Comandos Linux/Unix\n• Acceso al sistema\n• Scripts personalizados\n• Integración con herramientas"
                ),
                TutorialSection(
                    title = "CyberSec Toolkit",
                    content = "Herramientas de seguridad:\n• Escaneo de puertos\n• Análisis de vulnerabilidades\n• Generación de payloads\n• Exploits y pruebas de penetración"
                )
            )
        ),
        Tutorial(
            id = "gestures",
            title = "Gestos Táctiles",
            icon = "👆",
            sections = listOf(
                TutorialSection(
                    title = "Navegación por Gestos",
                    content = "Desliza entre pantallas:\n• Swipe derecha: Siguiente tab\n• Swipe izquierda: Tab anterior\n• Tabs: Chats ↔ Stories ↔ Llamadas ↔ Perfil"
                ),
                TutorialSection(
                    title = "Gestos en Historias",
                    content = "Controles táctiles:\n• Tap: Siguiente historia\n• Long press: Pausar\n• Swipe up: Ver perfil\n• Swipe down: Cerrar"
                ),
                TutorialSection(
                    title = "Visor de Fotos",
                    content = "Gestos de zoom:\n• Pinch: Zoom 1x-4x\n• Drag: Mover cuando está ampliado\n• Double tap: Zoom rápido\n• Swipe down: Cerrar"
                )
            )
        )
    )
    
    fun getTutorialById(id: String): Tutorial? {
        return tutorials.find { it.id == id }
    }
}
