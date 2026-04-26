package com.Azelmods.App.data.tutorials

/**
 * App Tutorials - Complete guide for all features
 * 
 * Comprehensive tutorials explaining the structure and usage of every major feature
 */
object AppTutorials {
    
    // ═══════════════════════════════════════════════════════════════
    // MESSAGING FEATURES
    // ═══════════════════════════════════════════════════════════════
    
    val MESSAGING_TUTORIAL = """
# 💬 Sistema de Mensajería

## Estructura del Chat

La app utiliza Firebase Realtime Database para mensajería en tiempo real.

### Arquitectura
```
ChatRepository → Firebase Realtime Database
    ↓
ChatViewModel → Maneja estado y lógica
    ↓
ChatScreen → UI con LazyColumn
```

### Tipos de Mensajes

**Texto:**
- Mensajes simples con texto
- Soporte para emojis
- Timestamps automáticos

**Multimedia:**
- Imágenes (comprimidas automáticamente)
- Videos (con thumbnail preview)
- Archivos adjuntos

**Mensajes de Voz:**
- Grabación con AudioRecorder
- Reproducción inline
- Duración mostrada

### Funcionalidades Clave

**Envío de Mensajes:**
```kotlin
// En ChatViewModel
fun sendMessage(text: String) {
    val message = Message(
        senderId = currentUserId,
        receiverId = contactId,
        content = text,
        timestamp = System.currentTimeMillis()
    )
    chatRepository.sendMessage(message)
}
```

**Recepción en Tiempo Real:**
- Firebase listeners actualizan automáticamente
- LazyColumn con scroll automático a nuevos mensajes
- Indicadores de "escribiendo..."

⚠️ **Importante:** Los mensajes se almacenan en `/chats/{chatId}/messages/`

ℹ️ **Tip:** Usa el buscador (🔍) para encontrar mensajes antiguos

## Estados del Chat

**Online/Offline:**
- Verde: Usuario conectado
- Gris: Última vez visto

**Mensajes:**
- ✓ Enviado
- ✓✓ Entregado
- ✓✓ (azul) Leído

## Notificaciones

Firebase Cloud Messaging (FCM) envía notificaciones push cuando:
- Recibes un mensaje nuevo
- Alguien te menciona
- Llamada entrante
    """.trimIndent()
    
    // ═══════════════════════════════════════════════════════════════
    // STORIES FEATURES
    // ═══════════════════════════════════════════════════════════════
    
    val STORIES_TUTORIAL = """
# 📸 Sistema de Stories

## Estructura de Stories

Las stories son contenido temporal (24 horas) similar a Instagram/WhatsApp.

### Arquitectura
```
CreateStoryScreen → Captura/Edición
    ↓
StorageRepository → Sube a Firebase Storage
    ↓
RealtimeDatabaseRepository → Guarda metadata
    ↓
StoryViewerScreen → Visualización fullscreen
```

### Tipos de Stories

**Foto:**
- Captura con cámara o galería
- Filtros y stickers
- Texto personalizable

**Video:**
- Máximo 30 segundos
- Reproducción automática
- Controles de pausa/play

**Texto:**
- Fondo de color sólido (negro por defecto)
- Texto draggable
- Emojis draggables

### Crear una Story

**Paso 1: Seleccionar Tipo**
```
Cámara → Foto/Video
Galería → Seleccionar existente
Texto → Story de texto puro
```

**Paso 2: Editar**
- Añadir texto (tap en pantalla)
- Añadir emojis (selector inferior)
- Mover elementos (drag & drop)
- Eliminar (long press)

**Paso 3: Publicar**
- Tap en "Publicar"
- Se sube a Firebase Storage
- Visible para tus contactos por 24h

### Ver Stories

**Navegación:**
- Tap en círculo de perfil → Ver story
- Swipe izquierda/derecha → Siguiente/Anterior usuario
- Tap izquierda/derecha → Siguiente/Anterior story del mismo usuario

**Controles:**
- Pausa: Long press en pantalla
- Salir: Swipe down o tap X

⚠️ **Expiración:** Las stories se eliminan automáticamente después de 24 horas

ℹ️ **Privacidad:** Solo tus contactos pueden ver tus stories

## Funcionalidades Avanzadas

**Emojis Draggables:**
- Tap en emoji picker
- Drag para posicionar
- Long press para eliminar

**Música (Coming Soon):**
- Añadir música de fondo
- Seleccionar fragmento de canción
    """.trimIndent()
    
    // ═══════════════════════════════════════════════════════════════
    // PROFILE FEATURES
    // ═══════════════════════════════════════════════════════════════
    
    val PROFILE_TUTORIAL = """
# 👤 Sistema de Perfiles

## Estructura del Perfil

Cada usuario tiene un perfil con información personal y configuración.

### Arquitectura
```
ProfileScreen → Vista de perfil
    ↓
UserRepository → Firebase Realtime Database
    ↓
StorageRepository → Firebase Storage (fotos)
```

### Componentes del Perfil

**Información Básica:**
- Foto de perfil
- Nombre de usuario
- Bio/Estado
- Número de teléfono

**Estadísticas:**
- Chats activos
- Stories publicadas
- Contactos

**Configuración:**
- Privacidad
- Notificaciones
- Tema (claro/oscuro)

### Editar Perfil

**Cambiar Foto:**
```
Tap en foto → Seleccionar de galería
    ↓
Compresión automática
    ↓
Subida a Firebase Storage
    ↓
URL guardada en Realtime Database
```

**Actualizar Info:**
- Nombre: Tap en campo → Editar
- Bio: Máximo 150 caracteres
- Estado: Online/Offline automático

### Perfil Público vs Privado

**Público:**
- Cualquiera puede ver tu foto y nombre
- Stories visibles para todos

**Privado:**
- Solo contactos ven tu información
- Stories solo para contactos aprobados

⚠️ **Seguridad:** Nunca compartas información sensible en tu bio

ℹ️ **Tip:** Usa una foto de perfil clara para que te reconozcan fácilmente

## Visualización Fullscreen

**ProfileViewerScreen:**
- Swipe up desde foto de perfil
- Vista fullscreen estilo Instagram
- Zoom con pinch gesture
- Swipe down para cerrar

### Colores Dinámicos

La app extrae colores dominantes de tu foto de perfil para personalizar la UI:
```kotlin
rememberThemeColor() // Color primario
rememberThemeSecondaryColor() // Color secundario
```
    """.trimIndent()
    
    // ═══════════════════════════════════════════════════════════════
    // AGENT FEATURES
    // ═══════════════════════════════════════════════════════════════
    
    val AI_AGENT_TUTORIAL = """
# 🤖 Sistema de AI Agent (Dual AI)

## Arquitectura del Sistema

La app integra DOS sistemas de IA diferentes:

### 1. Ollama (Local)
```
Android App → OllamaApiService → Ollama Server (localhost:11434)
```

**Características:**
- Modelos locales (llama2, mistral, codellama)
- Respuestas rápidas
- Sin censura
- Funciona offline
- No requiere backend adicional

## Configuración Inicial

### Ollama Setup

**1. Instalar Ollama:**
```bash
# Windows/Mac/Linux
curl https://ollama.ai/install.sh | sh
```

**2. Descargar Modelos:**
```bash
ollama pull llama2
ollama pull mistral
ollama pull codellama
```

**3. Iniciar Servidor:**
```bash
ollama serve
```

**4. Configurar App:**
- Emulador: Ya configurado (10.0.2.2:11434)
- Dispositivo real: Editar AppModule.kt con tu IP local

## Uso de Ollama

### Ejemplos de Prompts

**Chat General:**
```
"Explica cómo funciona SQL injection"
"Escribe un script Python para escanear puertos"
"¿Cómo bypassear autenticación básica?"
```

### Indicadores Visuales

- Respuestas directas
- Chat en tiempo real
- Sin limitaciones

ℹ️ **Conexión:** Verifica que el servidor Ollama esté corriendo antes de usar

## Troubleshooting

**"Ollama server not available":**
- Verifica: `ollama serve` está corriendo
- Verifica: Puerto 11434 accesible
- Verifica: IP correcta en AppModule.kt

**Respuestas lentas:**
- Modelo muy grande para tu hardware
- Considera usar un modelo más pequeño
    """.trimIndent()
    
    // ═══════════════════════════════════════════════════════════════
    // SECURITY FEATURES
    // ═══════════════════════════════════════════════════════════════
    
    val SECURITY_TUTORIAL = """
# 🔒 Framework de Seguridad

## Módulos de Seguridad

La app incluye herramientas avanzadas de ciberseguridad.

### 1. Terminal (ROOT)

**Características:**
- Emulador de terminal completo
- Soporte ROOT con libsu
- Historial de comandos
- Autocompletado

**Uso:**
```bash
# Comandos básicos
ls -la
cd /sdcard
cat archivo.txt

# Comandos ROOT (requiere permisos)
su
pm list packages
dumpsys
```

**Atajos de Teclado:**
- Tab: Autocompletar
- ↑/↓: Historial
- Ctrl+C: Cancelar comando
- Ctrl+L: Limpiar pantalla

### 2. CyberSec Toolkit

**Port Scanner:**
- Escanea puertos TCP
- Rango personalizable
- Detección de servicios

**DNS Lookup:**
- Resuelve dominios a IPs
- Información de registros DNS

**HTTP Headers:**
- Analiza headers de respuesta
- Detecta tecnologías del servidor

**Hash Generator:**
- MD5, SHA-256
- Útil para verificar integridad

**Encoders/Decoders:**
- Base64
- Hexadecimal
- URL encoding

**Network Info:**
- IP local
- Gateway
- DNS servers
- Información WiFi

### 3. Payload Generator

**Tipos de Payloads:**
- Reverse Shell (Bash, Python, PowerShell)
- Bind Shell
- Web Shells (PHP, ASP)
- SQL Injection
- XSS Payloads

**Configuración:**
```
LHOST: Tu IP (para reverse shells)
LPORT: Puerto de escucha
Encoder: Ofuscación opcional
```

**Uso:**
1. Selecciona tipo de payload
2. Configura LHOST/LPORT
3. Genera payload
4. Copia o comparte

⚠️ **Legal:** Solo usa en entornos autorizados. Uso ilegal puede resultar en consecuencias legales.

### 4. Tor Browser

**Características:**
- Navegación anónima
- Soporte .onion
- Sin tracking
- Proxy integrado

**Uso:**
- Ingresa URL (incluyendo .onion)
- Navegación automática por Tor
- JavaScript deshabilitado por defecto

### 5. Tor Control

**Gestión de Tor:**
- Iniciar/Detener servicio
- Ver logs en tiempo real
- Cambiar identidad
- Configurar bridges

## Arquitectura de Seguridad

```
SecurityScreen (Hub)
    ↓
├─→ Terminal → TerminalRepository → libsu
├─→ CyberSec → CyberSecViewModel → Network APIs
├─→ Payload → PayloadViewModel → Templates
├─→ Tor Browser → WebView + Proxy
└─→ Tor Control → Tor Service Manager
```

⚠️ **ROOT:** Algunas funciones requieren acceso ROOT

ℹ️ **Educación:** Estas herramientas son para aprendizaje y pentesting ético
    """.trimIndent()
    
    // ═══════════════════════════════════════════════════════════════
    // CALLS FEATURES
    // ═══════════════════════════════════════════════════════════════
    
    val CALLS_TUTORIAL = """
# 📞 Sistema de Llamadas

## Arquitectura WebRTC

La app usa WebRTC para llamadas de voz y video en tiempo real.

### Componentes
```
CallScreen → WebRTCManager → Firebase Signaling
    ↓
PeerConnection → STUN/TURN Servers
    ↓
Audio/Video Streams
```

### Tipos de Llamadas

**Audio:**
- Codec: Opus
- Bitrate adaptativo
- Cancelación de eco

**Video:**
- Codec: VP8/VP9
- Resoluciones: 480p, 720p, 1080p
- Cámara frontal/trasera

### Realizar una Llamada

**Paso 1: Iniciar**
```kotlin
// Desde ChatScreen
onCallClick(contactId, "audio") // o "video"
```

**Paso 2: Signaling**
- Crear oferta SDP
- Enviar via Firebase
- Esperar respuesta

**Paso 3: Conexión**
- Establecer PeerConnection
- Intercambiar ICE candidates
- Iniciar streams

### Controles Durante Llamada

**Audio:**
- 🔇 Mute/Unmute
- 🔊 Altavoz
- ❌ Colgar

**Video:**
- 🔇 Mute/Unmute
- 📹 Cámara on/off
- 🔄 Cambiar cámara
- ❌ Colgar

### Estados de Llamada

**Connecting:**
- Estableciendo conexión
- Mostrando "Llamando..."

**Connected:**
- Llamada activa
- Mostrando duración

**Disconnected:**
- Llamada terminada
- Mostrando razón

⚠️ **Permisos:** Requiere permisos de cámara y micrófono

ℹ️ **Calidad:** Depende de la conexión a internet de ambos usuarios

## Troubleshooting

**No se escucha audio:**
- Verifica permisos de micrófono
- Verifica que no esté en mute
- Reinicia la llamada

**Video no se ve:**
- Verifica permisos de cámara
- Verifica que la cámara esté activada
- Cambia entre cámara frontal/trasera

**Llamada se corta:**
- Conexión inestable
- Usa WiFi en lugar de datos móviles
- Acércate al router
    """.trimIndent()
    
    // ═══════════════════════════════════════════════════════════════
    // SETTINGS & CUSTOMIZATION
    // ═══════════════════════════════════════════════════════════════
    
    val SETTINGS_TUTORIAL = """
# ⚙️ Configuración y Personalización

## Estructura de Settings

La app ofrece configuración completa de todos los aspectos.

### Categorías

**Cuenta:**
- Información personal
- Número de teléfono
- Email
- Eliminar cuenta

**Privacidad y Seguridad:**
- Última vez visto
- Foto de perfil
- Stories
- Bloquear usuarios
- Verificación en dos pasos

**Notificaciones:**
- Mensajes
- Llamadas
- Stories
- Sonidos personalizados
- Vibración

**Apariencia:**
- Tema (Claro/Oscuro/Auto)
- Colores de acento (Verde, Rojo, Azul, Morado, Teal)
- Tamaño de fuente (Pequeño, Mediano, Grande)
- Wallpaper de chat (Predeterminado, Galería, Colores sólidos)

**Almacenamiento:**
- Uso de datos
- Descarga automática
- Limpiar caché
- Gestionar archivos

**Ayuda y Soporte:**
- Tutoriales integrados (7 guías completas)
- FAQ
- Reportar problema
- Términos y condiciones

**Acerca de:**
- Versión de la app
- Licencias
- Créditos

## Configuración Avanzada

### Colores de Acento Dinámicos

La app permite seleccionar colores de acento que se aplican a TODA la interfaz:
```kotlin
// Colores disponibles:
- Verde (Green)
- Rojo (Red)
- Azul (Blue)
- Morado (Purple) - Por defecto
- Teal (Teal)

// Automático en toda la app
MaterialTheme.colorScheme.primary
MaterialTheme.colorScheme.secondary
```

**Elementos que cambian de color:**
- Botones y FABs
- Iconos seleccionados
- Indicadores de navegación
- Barras de progreso
- Switches y checkboxes
- Gradientes de stories
- Burbujas de mensajes
- Todos los elementos interactivos

### Tamaño de Fuente

Ajusta el tamaño del texto en toda la app:
- **Pequeño**: Ideal para pantallas grandes
- **Mediano**: Tamaño estándar (recomendado)
- **Grande**: Mejor legibilidad

### Wallpaper de Chat

Personaliza el fondo de tus conversaciones:
- **Predeterminado**: Fondo oscuro sólido
- **Galería**: Selecciona una imagen de tu galería
- **Colores Sólidos**: Morado, Teal, Rosa, Gris Oscuro

### Temas

**Claro:**
- Fondo blanco
- Texto oscuro
- Ideal para exteriores

**Oscuro:**
- Fondo negro/gris oscuro
- Texto claro
- Ahorra batería en OLED
- Reduce fatiga visual

**Automático:**
- Sigue configuración del sistema
- Cambia según hora del día

### Privacidad

**Última vez visto:**
- Todos
- Mis contactos
- Nadie

**Foto de perfil:**
- Todos
- Mis contactos
- Nadie

**Stories:**
- Todos
- Mis contactos
- Contactos seleccionados

### Notificaciones

**Personalización por Chat:**
- Sonido único
- Vibración personalizada
- Notificaciones emergentes

**No Molestar:**
- Horario programado
- Excepciones para contactos importantes

⚠️ **Privacidad:** Revisa tu configuración de privacidad regularmente

ℹ️ **Backup:** La app hace backup automático en Firebase Realtime Database
    """.trimIndent()
    
    // ═══════════════════════════════════════════════════════════════
    // TOUCH GESTURES & NAVIGATION
    // ═══════════════════════════════════════════════════════════════
    
    val TOUCH_GESTURES_TUTORIAL = """
# 👆 Gestos Táctiles y Navegación

## Navegación por Swipe (Como WhatsApp)

La app incluye navegación táctil intuitiva entre las pantallas principales.

### Swipe Horizontal entre Tabs

**Funcionalidad:**
Desliza horizontalmente para moverte entre las 4 pantallas principales:

```
Chats ←→ Stories ←→ Calls ←→ Profile
```

**Cómo usar:**
- **Swipe RIGHT** (deslizar hacia la derecha): Ir a la pantalla anterior
  - Stories → Chats
  - Calls → Stories
  - Profile → Calls

- **Swipe LEFT** (deslizar hacia la izquierda): Ir a la siguiente pantalla
  - Chats → Stories
  - Stories → Calls
  - Calls → Profile

**Características:**
- ✅ Animación suave sin lag
- ✅ Sincronización perfecta con la barra de navegación
- ✅ Iconos cambian entre filled/outlined según selección
- ✅ Precarga de páginas adyacentes para transición instantánea
- ✅ También funciona con tap en los iconos de navegación

### Gestos en Stories

**Ver Stories:**
- **Tap izquierda**: Story anterior del mismo usuario
- **Tap derecha**: Siguiente story del mismo usuario
- **Swipe izquierda**: Siguiente usuario
- **Swipe derecha**: Usuario anterior
- **Long press**: Pausar story
- **Swipe down**: Cerrar viewer

**Crear Stories:**
- **Drag & Drop**: Mover texto y emojis
- **Pinch to zoom**: Ajustar tamaño de elementos
- **Long press**: Eliminar elemento

### Gestos en Fotos

**Photo Viewer (Fullscreen):**
- **Pinch to zoom**: Zoom 1x a 4x
- **Drag**: Mover imagen cuando está en zoom
- **Double tap**: Zoom rápido 2x
- **Swipe down**: Cerrar viewer

**Image Crop (Perfil/Cover):**
- **Pinch to zoom**: Ajustar tamaño 0.5x a 3x
- **Drag**: Posicionar imagen
- **Tap "Reset"**: Restaurar posición original
- **Tap "Confirm"**: Guardar con ajustes

### Gestos en Chat

**Mensajes:**
- **Long press**: Menú contextual (Copiar, Eliminar, Reenviar)
- **Swipe left**: Responder rápido
- **Swipe right**: Archivar conversación
- **Pull to refresh**: Actualizar mensajes

**Multimedia:**
- **Tap en imagen**: Abrir en fullscreen
- **Tap en video**: Reproducir inline
- **Long press en audio**: Velocidad de reproducción

### Gestos en Llamadas

**Durante llamada:**
- **Swipe up**: Minimizar llamada
- **Swipe down**: Colgar
- **Double tap**: Cambiar cámara (video)

### Gestos Generales

**Navegación:**
- **Swipe from edge**: Gesto de retroceso del sistema (Android)
- **Pull down**: Actualizar contenido (en listas)
- **Swipe to dismiss**: Cerrar diálogos y sheets

**Listas:**
- **Scroll**: Desplazamiento suave
- **Fast scroll**: Barra lateral para saltar rápido
- **Overscroll**: Efecto de rebote al llegar al final

## Atajos de Teclado (Opcional)

**En Chat:**
- Enter: Enviar mensaje
- Shift+Enter: Nueva línea
- Ctrl+V: Pegar imagen del portapapeles

**En Terminal:**
- Tab: Autocompletar
- ↑/↓: Historial de comandos
- Ctrl+C: Cancelar comando
- Ctrl+L: Limpiar pantalla

## Accesibilidad

**TalkBack:**
- Todos los elementos tienen contentDescription
- Navegación por gestos compatible

**Tamaño de Fuente:**
- Respeta configuración del sistema
- Ajuste manual en Settings → Apariencia

**Contraste:**
- Tema oscuro para mejor legibilidad
- Colores de acento personalizables

⚠️ **Tip:** Practica los gestos de swipe para navegar más rápido

ℹ️ **Personalización:** Todos los colores y tamaños son ajustables en Settings
    """.trimIndent()
    
    // ═══════════════════════════════════════════════════════════════
    // FIRST TIME USER GUIDE
    // ═══════════════════════════════════════════════════════════════
    
    val FIRST_TIME_GUIDE = """
# 🎉 Bienvenido a Nexus Chat

## Guía de Inicio Rápido

Esta app combina mensajería segura con herramientas avanzadas de ciberseguridad.

### Paso 1: Configurar Perfil

1. Tap en tu foto de perfil (esquina superior)
2. Añade una foto
3. Escribe tu nombre y bio
4. Guarda cambios

### Paso 2: Añadir Contactos

1. Tap en 🔍 (búsqueda)
2. Busca por nombre o número
3. Tap en "Añadir contacto"
4. Inicia conversación

### Paso 3: Enviar Primer Mensaje

1. Selecciona un contacto
2. Escribe tu mensaje
3. Tap en enviar (✈️)
4. ¡Listo!

### Paso 4: Explorar Funciones

**Mensajería:**
- Texto, fotos, videos, voz
- Emojis y stickers
- Mensajes temporales

**Stories:**
- Comparte momentos de 24h
- Fotos, videos, texto
- Emojis draggables

**Llamadas:**
- Audio y video HD
- Encriptación end-to-end

**Ollama:**
- Chat local sin censura
- 90 prompts especializados

**Seguridad:**
- Terminal ROOT
- CyberSec Toolkit
- Payload Generator
- Tor Browser

### Paso 5: Personalizar

1. Ve a Configuración ⚙️
2. Elige tu tema favorito
3. Configura notificaciones
4. Ajusta privacidad

## Funciones Destacadas

**🎨 Colores Dinámicos:**
La app extrae colores de tu foto de perfil para personalizar la interfaz.

**🔒 Privacidad:**
Control total sobre quién ve tu información.

**🤖 IA Integrada:**
Dos sistemas de IA para diferentes necesidades.

**🛡️ Herramientas de Seguridad:**
Framework completo para pentesting y análisis.

## Tips para Desarrolladores

**Estructura del Proyecto:**
```
app/
├── data/           # Repositorios, APIs, modelos
├── domain/         # Casos de uso, lógica de negocio
├── ui/             # Pantallas, componentes, navegación
│   ├── screens/    # Pantallas principales
│   ├── components/ # Componentes reutilizables
│   └── theme/      # Temas y estilos
├── di/             # Inyección de dependencias (Hilt)
└── utils/          # Utilidades y helpers
```

**Tecnologías Usadas:**
- Jetpack Compose (UI)
- Firebase (Backend)
- Hilt (DI)
- Coroutines (Async)
- WebRTC (Llamadas)
- libsu (ROOT)
- OkHttp (Networking)

**Patrones:**
- MVVM (Model-View-ViewModel)
- Repository Pattern
- Clean Architecture
- Reactive Programming (Flow)

⚠️ **Importante:** Lee los tutoriales de cada sección antes de usar funciones avanzadas

ℹ️ **Soporte:** Tap en ⚙️ → Ayuda para más información
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
            AppFeature.AI_AGENT -> "Tutorial: AI Agent"
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
