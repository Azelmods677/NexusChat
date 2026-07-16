package com.Azelmods.App.data.tutorials

/**
 * App Tutorials - Complete guide for all features (2026 Edition)
 *
 * Comprehensive tutorials explaining every major feature of Azelgram Messenger
 */
object AppTutorials {

    // ═══════════════════════════════════════════════════════════════
    // MESSAGING FEATURES
    // ═══════════════════════════════════════════════════════════════

    val MESSAGING_TUTORIAL = """
# 💬 Sistema de Mensajería

## Arquitectura en Tiempo Real

Azelgram utiliza Firebase Realtime Database con cifrado Signal Protocol (E2EE).

```
ChatRepository → Firebase Realtime Database + Signal Protocol
    ↓
ChatViewModel → Maneja estado con StateFlow
    ↓
ChatScreen → UI con LazyColumn optimizada + backgrounds dinámicos
```

## Tipos de Mensajes

**Texto:** Mensajes con emojis, timestamps automáticos y confirmaciones de lectura

**Multimedia:** Imágenes comprimidas, videos con thumbnail, archivos adjuntos

**Voz:** Grabación inline con AudioRecorder, reproducción con seekbar

**Cifrados (E2EE):** Todo el contenido se cifra con Signal Protocol antes de enviarse

## Funcionalidades Clave

**Estados del mensaje:**
• ✓ Enviado → ✓✓ Entregado → ✓✓ (azul) Leído

**Notificaciones push:** Firebase Cloud Messaging con canales personalizados

**Wallpaper personalizado:** Fondos sólidos, degradados, imágenes o video por chat

**Respuesta rápida:** Swipe left en mensaje para responder

---

⚠️ Los mensajes se almacenan en `/chats/{chatId}/messages/` con cifrado extremo a extremo.

ℹ️ El cifrado E2EE requiere que ambos usuarios tengan la versión 2.0+ de Azelgram.
    """.trimIndent()

    // ═══════════════════════════════════════════════════════════════
    // STORIES FEATURES
    // ═══════════════════════════════════════════════════════════════

    val STORIES_TUTORIAL = """
# 📸 Sistema de Stories

## Contenido Temporal de 24h

Similar a Instagram/WhatsApp, las stories expiran automáticamente.

## Tipos de Stories

**Foto:** Captura con cámara o galería + ajuste bidimensional (drag X+Y) + texto/emojis draggables

**Video:** Máximo 30 segundos con reproducción automática y controles

**Texto:** Fondo oscuro con texto y emojis de posicionamiento libre

## Cómo Crear

1. Abre la pestaña Stories → ícono cámara
2. Captura o selecciona contenido
3. Añade texto (tap en "Text") o emojis (tap en "Sticker")
4. Ajusta posición con drag & drop
5. Long press para eliminar elementos
6. Tap en "Share" para publicar

## Selector de Emojis

4 categorías completas en ModalBottomSheet:
• 😀 Caritas • 🐶 Animales • 🍎 Comida • ⚽ Deportes

Grid de 8 columnas con scrollable tabs.

## Ver Stories

• Tap en círculo de perfil → Ver story
• Swipe izquierda/derecha → Siguiente/Anterior usuario
• Tap izquierda/derecha → Siguiente/Anterior story
• Long press → Pausar
• Swipe down → Cerrar

---

⚠️ Las stories se eliminan automáticamente después de 24 horas.

ℹ️ Solo tus contactos pueden ver tus stories (configurable en privacidad).
    """.trimIndent()

    // ═══════════════════════════════════════════════════════════════
    // PROFILE FEATURES
    // ═══════════════════════════════════════════════════════════════

    val PROFILE_TUTORIAL = """
# 👤 Sistema de Perfiles

## Información Personal

Cada usuario tiene un perfil con foto, nombre, bio y estadísticas.

**Componentes:**
• Foto de perfil (zoomable, fullscreen viewer)
• Nombre de usuario
• Bio (máx. 150 caracteres)
• Estadísticas: chats activos, stories, contactos
• Colores dinámicos extraídos de tu foto

## Editar Perfil

1. Tap en tu foto o nombre
2. Cambia foto desde galería (compresión + subida automática)
3. Actualiza nombre y bio
4. Los cambios se guardan en Firebase Realtime Database

## Colores Dinámicos (2026)

La app extrae colores dominantes de tu foto de perfil:

• `rememberThemeColor()` → Color primario
• `rememberThemeSecondaryColor()` → Color secundario

Estos colores se aplican a toda la interfaz automáticamente.

---

⚠️ Nunca compartas información sensible en tu bio.

ℹ️ Usa una foto clara para mejor reconocimiento.
    """.trimIndent()

    // ═══════════════════════════════════════════════════════════════
    // AI AGENT FEATURES 2026
    // ═══════════════════════════════════════════════════════════════

    val AI_AGENT_TUTORIAL = """
# 🤖 Sistema de IA — Azel IA

## Arquitectura 2026

Dos motores de IA disponibles:

**Ollama Cloud (recomendado):**
```
App → AzelAIApiService → Ollama Cloud API (servidores remotos)
```

Sin necesidad de hardware local. Modelos disponibles:
• deepseek-r1:70b — Razonamiento profundo
• gpt-oss:120b-cloud — Modelo insignia
• llama3:70b — Propósito general
• codellama:70b — Programación

**Ollama Local (alternativa):**
```
App → OllamaApiService → Ollama Server (localhost:11434)
```

Para usuarios con hardware potente que prefieren privacidad total.

## Cómo Usar Azel IA

1. Ve a la pestaña "IA" desde el menú principal
2. Selecciona modo Cloud (automático) o Local (requiere Ollama instalado)
3. Elige un prompt predefinido o escribe tu consulta
4. Recibe respuestas en tiempo real con streaming

## Categorías de Prompts

• 🐍 Python • ⚡ JavaScript • 📱 Android • 🐧 Linux
• 🔐 Criptografía • 🌐 Redes • 🛡️ Seguridad defensiva
• ✍️ Redacción • 🌎 Traducción • 🤖 Prompts de IA

## Configuración Local (Opcional)

Solo si quieres usar Ollama local en lugar de la nube:

1. Instala Ollama: `curl https://ollama.ai/install.sh | sh`
2. Descarga modelos: `ollama pull llama3`
3. Inicia servidor: `ollama serve`
4. La app se conecta automáticamente a localhost:11434

---

ℹ️ Usa la IA de forma responsable y conforme a las leyes aplicables.

ℹ️ El modo Cloud está configurado por defecto y no requiere instalación adicional.
    """.trimIndent()

    // ═══════════════════════════════════════════════════════════════
    // SECURITY & PRIVACY 2026
    // ═══════════════════════════════════════════════════════════════

    val SECURITY_TUTORIAL = """
# 🔒 Seguridad y Privacidad — 2026 Edition

## Cifrado Extremo a Extremo (E2EE)

Todos los mensajes se cifran con **Signal Protocol** antes de salir del dispositivo.

```
Mensaje original → Cifrado Signal → Firebase → Descifrado Signal → Mensaje original
```

• Claves efímeras por sesión (Perfect Forward Secrecy)
• Intercambio de claves mediante el protocolo X3DH
• Cifrado doble ratchet para mensajes continuos
• Solo el emisor y receptor pueden leer el contenido

## Bloqueo Biométrico

Protege la app con huella digital o reconocimiento facial:

1. Ve a Ajustes → Privacidad → Bloqueo de App
2. Activa "Bloqueo biométrico"
3. Configura tiempo de bloqueo (inmediato / 1 min / 5 min)

## Tor / Orbot

Navegación anónima integrada:

• **Orbot Setup:** Guía para instalar y configurar Tor en tu dispositivo
• **Navegación .onion:** Acceso a sitios onion desde la app
• **Proxy automático:** Redirección de tráfico a través de Tor

## Backup Cifrado

Realiza copias de seguridad cifradas con AES-256:

1. Ajustes → Almacenamiento → Crear Backup
2. Establece una contraseña de respaldo
3. El backup se almacena cifrado en Firebase Storage
4. Restaura en cualquier dispositivo con la misma contraseña

## Bloqueo de Contactos

1. Abre el perfil del contacto
2. Tap en "Más opciones" → Bloquear
3. El contacto no podrá enviarte mensajes ni ver tu perfil

## Detección de Root / Tampering

La app detecta automáticamente:
• Dispositivos rooteados
• Modificaciones en el APK
• Entornos de depuración no autorizados

⚠️ El cifrado E2EE está activo por defecto en todos los mensajes.

ℹ️ El bloqueo biométrico usa el hardware de tu dispositivo (no almacena huellas).
    """.trimIndent()

    // ═══════════════════════════════════════════════════════════════
    // CALLS FEATURES 2026
    // ═══════════════════════════════════════════════════════════════

    val CALLS_TUTORIAL = """
# 📞 Llamadas WebRTC

## Arquitectura

```
CallScreen → WebRTCManager → Firebase Signaling → STUN/TURN Servers
    ↓
PeerConnection → Audio/Video streams cifrados
```

## Tipos de Llamadas

**Audio:** Codec Opus, bitrate adaptativo, cancelación de eco

**Video:** Codec VP8/VP9, resoluciones 480p/720p/1080p, cámara frontal/trasera

## Cómo Llamar

1. Abre un chat o la pestaña Llamadas
2. Tap en ícono de teléfono (audio) o cámara (video)
3. La app verifica permisos automáticamente
4. Espera a que el contacto acepte

**Permisos requeridos:**
• `RECORD_AUDIO` — Para capturar audio
• `CAMERA` — Para videollamadas
• `FOREGROUND_SERVICE_PHONE_CALL` — Llamadas en primer plano
• `MANAGE_OWN_CALLS` — Gestión de llamadas

## Controles Durante Llamada

• 🔇 Mute/Unmute • 🔊 Altavoz • 📹 Cámara on/off
• 🔄 Cambiar cámara • ❌ Colgar

## Estados

• **Connecting:** Estableciendo conexión
• **Connected:** Llamada activa con duración
• **Disconnected:** Llamada terminada

---

⚠️ Requiere conexión a internet activa de ambos usuarios.

ℹ️ La calidad depende del ancho de banda disponible.
    """.trimIndent()

    // ═══════════════════════════════════════════════════════════════
    // SETTINGS & CUSTOMIZATION 2026
    // ═══════════════════════════════════════════════════════════════

    val SETTINGS_TUTORIAL = """
# ⚙️ Configuración y Personalización

## Categorías

**Cuenta:** Información personal, email, eliminar cuenta

**Privacidad y Seguridad:**
• Última vez visto (Todos / Contactos / Nadie)
• Foto de perfil (Todos / Contactos / Nadie)
• Stories (Todos / Contactos / Seleccionados)
• Bloqueo biométrico
• Cifrado E2EE
• Bloqueo de contactos

**Notificaciones:**
• Mensajes, llamadas, stories
• Sonidos personalizados por chat
• Vibración
• Canales FCM configurados

**Apariencia:**
• Tema: Claro / Oscuro / Automático
• **15 colores de acento:** Verde, Rojo, Azul, Morado, Teal, Rosa, Naranja, Amarillo, Cian, Índigo, Lima, Ámbar, Marrón, Gris, Azul Gris
• Tamaño de fuente: Pequeño / Mediano / Grande
• Wallpaper de chat: Predeterminado / Galería / Colores sólidos / Degradados / Video

**Almacenamiento:**
• Uso de datos
• Descarga automática
• Limpiar caché
• Backup cifrado
• Restaurar backup

## Temas

• **Claro:** Fondo blanco, texto oscuro. Ideal para exteriores
• **Oscuro:** Fondo negro, texto claro. Ahorra batería OLED
• **Automático:** Sigue la configuración del sistema

## Wallpaper de Chat

1. Abre un chat → Tap en ⋮ → Fondo de chat
2. Elige entre: Predeterminado / Galería / Colores sólidos / Degradados / Video
3. El cambio se aplica al instante

---

⚠️ Revisa tu configuración de privacidad regularmente.

ℹ️ Los wallpapers de video y degradados tienen efecto en todos los chats.
    """.trimIndent()

    // ═══════════════════════════════════════════════════════════════
    // TOUCH GESTURES & NAVIGATION
    // ═══════════════════════════════════════════════════════════════

    val TOUCH_GESTURES_TUTORIAL = """
# 👆 Gestos Táctiles y Navegación

## Swipe Horizontal entre Tabs

Desliza para moverte entre las 4 pantallas principales:

```
Chats ←→ Stories ←→ Llamadas ←→ Perfil
```

• **Swipe LEFT:** Siguiente pantalla
• **Swipe RIGHT:** Pantalla anterior
• **Tap en ícono:** También funciona

## Gestos en Stories

• Tap izquierda/derecha → Story anterior/siguiente del mismo usuario
• Swipe izquierda/derecha → Usuario anterior/siguiente
• Long press → Pausar
• Swipe down → Cerrar viewer
• Drag & drop → Mover texto/emojis

## Gestos en Fotos (Fullscreen)

• Pinch to zoom: 1x a 4x
• Drag: Mover imagen en zoom
• Double tap: Zoom rápido 2x
• Swipe down: Cerrar

## Gestos en Chat

• Long press en mensaje → Menú contextual (Copiar, Eliminar, Reenviar)
• Swipe left → Responder rápido
• Pull to refresh → Actualizar mensajes

## Accesibilidad

• TalkBack: Todos los elementos tienen contentDescription
• Tamaño de fuente respeta configuración del sistema
• Contraste optimizado para tema oscuro

---

⚠️ Practica los gestos de swipe para navegar más rápido.

ℹ️ Todos los colores y tamaños son ajustables en Settings.
    """.trimIndent()

    // ═══════════════════════════════════════════════════════════════
    // FIRST TIME USER GUIDE
    // ═══════════════════════════════════════════════════════════════

    val FIRST_TIME_GUIDE = """
# 🎉 Bienvenido a Azelgram Messenger

## Guía de Inicio Rápido

### Paso 1: Crear tu Cuenta
1. Abre la aplicación
2. Inicia sesión con Google
3. Completa tu perfil (foto, nombre, bio)

### Paso 2: Añadir Contactos
1. Tap en 🔍 (búsqueda) o ➕ (nuevo chat)
2. Busca por nombre o escanea QR
3. Inicia conversación

### Paso 3: Enviar Primer Mensaje
1. Selecciona un contacto
2. Escribe tu mensaje
3. Tap en enviar (✈️)
4. ¡Los mensajes se cifran automáticamente!

### Paso 4: Explorar Funciones

**💬 Mensajería:** Texto, fotos, videos, voz, E2EE
**📸 Stories:** Contenido temporal de 24h con emojis draggables
**📞 Llamadas:** Audio y video HD con WebRTC
**🤖 Azel IA:** Asistente inteligente para programación, redacción y análisis
**🔒 Seguridad:** Cifrado Signal, bloqueo biométrico, Tor/Orbot
**🎨 Personalización:** 15 colores de acento, wallpapers, temas

## Tecnologías Clave

• **UI:** Jetpack Compose + Material 3
• **Backend:** Firebase (Auth, Realtime Database, Storage, FCM)
• **DI:** Hilt
• **Async:** Coroutines + Flow
• **Llamadas:** WebRTC
• **Cifrado:** Signal Protocol
• **IA:** Ollama Cloud API
• **Anonimato:** Tor / Orbot

⚠️ Activa el bloqueo biométrico en Ajustes → Privacidad para proteger tu app.

ℹ️ Para ayuda, ve a Ajustes → Ayuda o consulta los tutoriales individuales.
    """.trimIndent()

    // Helper function to get tutorial by feature
    fun getTutorial(feature: AppFeature): String {
        return when (feature) {
            AppFeature.MESSAGING -> MESSAGING_TUTORIAL
            AppFeature.STORIES -> STORIES_TUTORIAL
            AppFeature.PROFILE -> PROFILE_TUTORIAL
            AppFeature.AI_AGENT -> AI_AGENT_TUTORIAL
            AppFeature.SECURITY -> SECURITY_TUTORIAL
            AppFeature.CALLS -> CALLS_TUTORIAL
            AppFeature.SETTINGS -> SETTINGS_TUTORIAL
            AppFeature.TOUCH_GESTURES -> TOUCH_GESTURES_TUTORIAL
            AppFeature.FIRST_TIME -> FIRST_TIME_GUIDE
        }
    }

    fun getTutorialTitle(feature: AppFeature): String {
        return when (feature) {
            AppFeature.MESSAGING -> "Tutorial: Mensajería"
            AppFeature.STORIES -> "Tutorial: Stories"
            AppFeature.PROFILE -> "Tutorial: Perfiles"
            AppFeature.AI_AGENT -> "Tutorial: Azel IA"
            AppFeature.SECURITY -> "Tutorial: Seguridad"
            AppFeature.CALLS -> "Tutorial: Llamadas"
            AppFeature.SETTINGS -> "Tutorial: Configuración"
            AppFeature.TOUCH_GESTURES -> "Tutorial: Gestos Táctiles"
            AppFeature.FIRST_TIME -> "Guía de Inicio"
        }
    }
}

enum class AppFeature {
    MESSAGING,
    STORIES,
    PROFILE,
    AI_AGENT,
    SECURITY,
    CALLS,
    SETTINGS,
    TOUCH_GESTURES,
    FIRST_TIME
}
