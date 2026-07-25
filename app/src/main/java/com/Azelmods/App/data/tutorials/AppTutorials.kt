package com.Azelmods.App.data.tutorials

/**
 * App Tutorials - Complete guide for all features (2026 Edition)
 *
 * Comprehensive tutorials explaining every major feature of Nexus Chat
 */
object AppTutorials {

    // ═══════════════════════════════════════════════════════════════
    // MESSAGING FEATURES
    // ═══════════════════════════════════════════════════════════════

    val MESSAGING_TUTORIAL = """
# 💬 Sistema de Mensajería

## Arquitectura en Tiempo Real

Nexus Chat usa Firebase Realtime Database y cifra los chats de dos personas en el
dispositivo con ECDH P-256 + AES-256-GCM.

```
ChatRepository → Firebase Realtime Database + E2EECryptoService (ECDH + AES-GCM)
    ↓
ChatViewModel → Maneja estado con StateFlow
    ↓
ChatScreen → UI con LazyColumn optimizada + backgrounds dinámicos
```

## Tipos de Mensajes

**Texto:** Mensajes con emojis, timestamps automáticos y confirmaciones de lectura

**Multimedia:** Imágenes comprimidas, videos con thumbnail, archivos adjuntos

**Voz:** Grabación inline con AudioRecorder, reproducción con seekbar

**Cifrados (E2EE):** En chats de dos personas, el texto se cifra en el dispositivo antes de salir

## Funcionalidades Clave

**Estados del mensaje:**
• ✓ Enviado → ✓✓ Entregado → ✓✓ (azul) Leído

**Notificaciones push:** Firebase Cloud Messaging con canales personalizados

**Wallpaper personalizado:** Fondos sólidos, degradados, imágenes o video por chat

**Responder a un mensaje:** mantén pulsado el mensaje y quedará citado sobre la barra de escritura

**Mensajes temporales:** de una sola visualización o con autodestrucción programada

---

⚠️ Los mensajes se almacenan en `/chats/{chatId}/messages/`; en chats de dos personas
el servidor solo guarda el texto cifrado.

ℹ️ El cifrado E2EE necesita que ambas cuentas hayan publicado su clave pública, cosa
que ocurre sola al abrir la app por primera vez.
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

ℹ️ Cualquier usuario con cuenta puede ver tus historias mientras estén activas: aún
no hay control de audiencia por historia. Publica pensando en eso.
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

## El proveedor lo eliges tú

La app no está atada a ninguna IA concreta. Tú pones el proveedor, el modelo y la
clave, y esa clave se guarda cifrada en el dispositivo.

```
App → AzelAIApiService → el proveedor que hayas configurado
```

Compatibles hoy: Google Gemini, OpenAI, OpenRouter, DeepSeek, Qwen, GLM (Z.ai),
Kimi (Moonshot), Mistral, Groq, Ollama y cualquier servidor que hable el dialecto
de OpenAI (LM Studio, vLLM, llama.cpp).

## Configurarlo

1. Ajustes → IA
2. Elige proveedor y revisa la URL base
3. Pega tu API key (los servidores locales no la necesitan)
4. Elige modelo entre los sugeridos o pulsa **Buscar modelos**
5. Abre "Acceder a Azel IA" y empieza a escribir

## Modelos siempre al día

**Buscar modelos** pregunta al proveedor qué modelos tienes disponibles ahora
mismo. La lista no está grabada dentro de la app, así que un modelo recién
publicado aparece solo, sin actualizar nada.

## Modelos locales

Con Ollama corriendo en tu equipo, la conversación no sale de tu red:

```
adb reverse tcp:11434 tcp:11434
```

En el emulador, usa 10.0.2.2 como host.

---

ℹ️ Usa la IA de forma responsable y conforme a las leyes aplicables.

ℹ️ El comportamiento y los filtros los define el modelo que elijas, no la app.
    """.trimIndent()

    // ═══════════════════════════════════════════════════════════════
    // SECURITY & PRIVACY 2026
    // ═══════════════════════════════════════════════════════════════

    val SECURITY_TUTORIAL = """
# 🔒 Seguridad y Privacidad — 2026 Edition

## Cifrado Extremo a Extremo (E2EE)

Los mensajes de los chats de dos personas se cifran en tu dispositivo con
**ECDH P-256 + AES-256-GCM** antes de salir.

```
Mensaje original → Cifrado AES-GCM → Firebase (solo texto cifrado) → Descifrado → Mensaje original
```

• Cada cuenta genera un par de claves de identidad en el propio dispositivo
• La clave privada NUNCA sale del teléfono; en Firebase solo se publica la pública
• El secreto compartido se deriva con ECDH entre las dos identidades
• AES-256-GCM aporta cifrado autenticado: detecta manipulaciones del mensaje

Qué NO ofrece hoy, dicho claramente:
• No hay secreto hacia adelante (Perfect Forward Secrecy): las claves de identidad
  son estables, no efímeras por sesión
• No implementa X3DH ni doble ratchet
• Los grupos aún no van cifrados de extremo a extremo
• Los metadatos (quién habla con quién y cuándo) siguen siendo visibles en el servidor

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

## Ajustes de Privacidad

En Privacidad y seguridad controlas qué muestras a los demás:
• Última conexión
• Foto de perfil
• Confirmaciones de lectura (el doble check azul)

## Backup Cifrado

Realiza copias de seguridad cifradas con AES-256:

1. Ajustes → Almacenamiento → Crear Backup
2. Establece una contraseña de respaldo
3. El backup se almacena cifrado en Firebase Storage
4. Restaura en cualquier dispositivo con la misma contraseña

Sin esa contraseña la copia no se puede restaurar. No hay forma de recuperarla.

⚠️ El cifrado E2EE está activo por defecto en los chats de dos personas.
Los grupos NO lo usan todavía.

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
• Tema: interruptor de modo oscuro (el oscuro es el predeterminado)
• **25 colores de acento:** de Púrpura y Violeta a Esmeralda, Ámbar, Coral, Carmesí, Magenta, Pizarra o Luna de Sangre
• Tamaño de fuente: Pequeño / Normal / Grande / Muy Grande (se aplica a toda la app)
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

• Long press en mensaje → Responder (queda citado sobre la barra de escritura)
• Menú del mensaje → Traducir, Editar o Eliminar
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
# 🎉 Bienvenido a Nexus Chat

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
**🤖 Azel IA:** Asistente con el proveedor y modelo que tú elijas (incluidos locales)
**🔒 Seguridad:** Cifrado E2EE (ECDH + AES-256-GCM), bloqueo biométrico, Tor/Orbot
**🎨 Personalización:** 25 colores de acento, fondos de imagen/vídeo, tamaño de texto

## Tecnologías Clave

• **UI:** Jetpack Compose + Material 3
• **Backend:** Firebase (Auth, Realtime Database, Storage, FCM)
• **DI:** Hilt
• **Async:** Coroutines + Flow
• **Llamadas:** WebRTC
• **Cifrado:** ECDH P-256 + AES-256-GCM
• **IA:** proveedor y modelo a elección del usuario (incluidos locales)
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
