# 🚀 NexusChat

<div align="center">

![Version](https://img.shields.io/badge/version-3.0.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-purple.svg)
![Compose](https://img.shields.io/badge/compose-2025.04.01-orange.svg)
![MinSDK](https://img.shields.io/badge/minSdk-31_(Android_12)-orange.svg)
![TargetSDK](https://img.shields.io/badge/targetSdk-36_(Android_16)-green.svg)

**Aplicación de mensajería avanzada para Android** con características empresariales: llamadas WebRTC, Stories multimedia, navegación Tor, asistente IA con Gemini, terminal integrado, editor de código y seguridad de nivel militar.

[Características](#-características-principales) •
[Requisitos](#-requisitos-del-sistema) •
[Instalación](#-instalación-y-configuración) •
[Documentación](#-documentación-técnica) •
[Soporte](#-soporte-y-contacto)

</div>

---

## 📋 Tabla de Contenidos

- [Características Principales](#-características-principales)
- [Requisitos del Sistema](#-requisitos-del-sistema)
- [Instalación y Configuración](#-instalación-y-configuración)
- [Stack Tecnológico](#-stack-tecnológico)
- [Arquitectura](#-arquitectura)
- [Características Técnicas Detalladas](#-características-técnicas-detalladas)
- [Seguridad](#-seguridad)
- [Soporte y Contacto](#-soporte-y-contacto)

---

## ✨ Características Principales

### 💬 Mensajería en Tiempo Real
- **Firebase Realtime Database** con arquitectura optimizada (mapas e índices para acceso O(1))
- **Multimedia completo**: imágenes, videos, audio, documentos, ubicación, contactos
- **Mensajes efímeros** con auto-destrucción configurable
- **Encriptación E2E** con Signal Protocol (en desarrollo)
- **Respuestas y reenvíos**
- **Indicadores de lectura** y estado en línea

### 📞 Llamadas y Videollamadas WebRTC
- **Llamadas P2P** de alta calidad con WebRTC
- **Señalización vía Firebase** Realtime Database
- **STUN servers** integrados (Google)
- **Notificaciones de llamada** con Firebase Cloud Messaging
- **Controles**: mute, video on/off, cambio de cámara
- **Historial de llamadas**

### 📸 Stories Multimedia (24h)
- **Editor avanzado** con overlays en tiempo real
- **Stickers y emojis arrastrables**
- **Texto personalizable** (fuentes y colores)
- **Renderizado en archivo final**: Canvas para fotos, Media3 Transformer para videos
- **Auto-eliminación** después de 24 horas
- **Indicadores de visualización**

### 🤖 Asistente IA (Azel IA)
- **Integración con Gemini** (Google AI)
- **API key del usuario** (almacenada cifrada con AES-256)
- **5 modelos disponibles**:
  - Gemini 3.1 Pro Preview (Inteligencia Suprema)
  - Gemini 2.5 Flash (Recomendado - Rápido y Eficiente)
  - Gemini 2.0 Flash (Rápido y capaz)
  - Gemini 1.5 Pro (Modelo clásico avanzado)
  - Gemini 1.5 Flash (Tareas rápidas)
- **Streaming en tiempo real** con SSE (Server-Sent Events)
- **Cola con backoff exponencial** y rate limiting
- **Gestión inteligente de contexto** (últimos 8 mensajes)

### 🎨 Sistema de Temas Dinámicos
- **25 colores de acento** curados profesionalmente
- **Material 3** Design System
- **Cambio en tiempo real** sin reiniciar
- **Fondos personalizables** (color sólido, gradiente, video)
- **Modo oscuro** optimizado

### 🔒 Seguridad de Nivel Militar

#### 1. Bloqueo de Aplicación (App Lock)
- **PIN de 4-6 dígitos** con hash SHA-256
- **Autenticación biométrica** (huella digital / Face ID)
- **Auto-bloqueo configurable**: inmediato, 1, 5 o 30 minutos
- **Teclado numérico personalizado** con animaciones
- **Interceptor en lifecycle** para bloqueo consistente

#### 2. Navegador Tor Integrado
- **Enrutado automático por Orbot** (HTTP 8118 + SOCKS5 9050 fallback)
- **Soporte para sitios .onion** (Dark Web)
- **Detección automática** de Orbot instalado (2 paquetes soportados)
- **Proxy para Firebase** (opcional)
- **Estado visual** del proxy Tor
- **Motor de navegación WebView** con NetCipher

#### 3. Encriptación E2E (Signal Protocol)
- **Signal Protocol** (libsignal-android 0.40.1)
- **Pre-keys y session keys** gestionados automáticamente
- **Cifrado de mensajes** de extremo a extremo
- **Verificación de identidad** (en desarrollo)

#### 4. Copias de Seguridad Cifradas
- **AES-256-GCM** para encriptación autenticada
- **PBKDF2** (100,000 iteraciones) para derivación de claves
- **HMAC-SHA256** para verificación de integridad
- **Almacenamiento dual**: Firebase Storage + local
- **Formato propietario** .azelback

### 💻 Herramientas de Desarrollo

#### Terminal Integrado
- **Emulador de terminal real** en la aplicación
- **Soporte para comandos shell**
- **Editor Sora** (0.23.5) para código
- **Resaltado de sintaxis** para Java y más lenguajes

#### Editor de Código
- **Sora Editor** profesional integrado
- **Syntax highlighting** (Java, TextMate grammar)
- **Editor de archivos** dentro de la app

### 🛡️ Características de Seguridad Avanzadas
- **Root detection** con libsu (5.2.2)
- **Tamper detection** con Security-Crypto
- **Payload Generator** (APK injection, ZIP manipulation)
- **Cryptografía avanzada** con Bouncy Castle (1.78.1)

---
## 📱 Requisitos del Sistema

### Requisitos Mínimos
- **Android**: 12.0 (API 31) o superior
- **RAM**: 2 GB
- **Almacenamiento**: 100 MB libres
- **Procesador**: ARM (armeabi-v7a, arm64-v8a, x86, x86_64)

### Requisitos Recomendados
- **Android**: 12.0 (API 31) o superior (optimizado para Android 16 API 36)
- **RAM**: 4 GB o más
- **Almacenamiento**: 500 MB libres
- **Procesador**: Octa-core 2.0 GHz o superior
- **Conexión**: Wi-Fi o datos móviles 4G/5G

### Dependencias Externas (Opcionales)
- **Orbot** (para navegación Tor): [Google Play](https://play.google.com/store/apps/details?id=org.torproject.android)
- **API Key de Gemini** (para IA): [Google AI Studio](https://makersuite.google.com/app/apikey)

---

## 🏗️ Arquitectura

NexusChat sigue una arquitectura **MVVM (Model-View-ViewModel)** limpia con:
- **Inyección de dependencias**: Hilt
- **Programación reactiva**: Kotlin Flow y StateFlow
- **UI moderna**: Jetpack Compose con Material 3

```
UI Layer (Compose) ↔ ViewModel Layer ↔ Repository Layer ↔ Data Sources (Firebase, WebRTC, Gemini AI)
```

### Principios de Diseño
- **Single Responsibility**: Cada clase tiene una única responsabilidad
- **Dependency Inversion**: Las capas superiores no dependen de las inferiores
- **Clean Architecture**: Separación clara de responsabilidades

---


## 🛠️ Stack Tecnológico

### Core
- **Kotlin** 2.1.0
- **Jetpack Compose** BOM 2025.04.01
- **Material 3** Design System
- **Coroutines** 1.9.0
- **Kotlin Flow**

### Android Jetpack
- **Lifecycle** 2.8.7
- **Navigation Compose** 2.8.5
- **DataStore** 1.1.1
- **WorkManager** 2.10.0
- **BiometricPrompt** 1.1.0
- **Security-Crypto** 1.1.0-alpha07 (EncryptedSharedPreferences)
- **Room** 2.6.1 (Offline Cache)
- **CameraX** 1.3.1 + ML Kit Barcode Scanning

### Inyección de Dependencias
- **Hilt** 2.52

### Firebase (BOM 33.7.0)
- **Firebase Auth** (Email/Password + Google)
- **Firebase Realtime Database**
- **Firebase Storage**
- **Firebase Cloud Messaging**
- **Firebase Crashlytics**

### Multimedia
- **WebRTC** (Stream WebRTC Android 1.1.3)
- **Media3 ExoPlayer** 1.5.1
- **Media3 Transformer** 1.5.1 (Story video composition)
- **Coil 3.x** (Image loading)

### IA
- **Gemini API** (Google Generative AI)
- **OkHttp SSE** 4.12.0 (Server-Sent Events para streaming)

### Seguridad
- **Signal Protocol** (libsignal-android 0.40.1)
- **Bouncy Castle** 1.78.1 (Cryptography)
- **NetCipher WebKit** 2.1.0 (Tor integration)
- **libsu** 5.2.2 (Root detection)

### Herramientas de Desarrollo
- **Sora Editor** 0.23.5 (Code editor)
- **Smali/DEX** 2.5.2 (APK manipulation)
- **ZIP4j** 2.11.5 (Payload generation)

### Testing
- **JUnit 5** (Kotest 5.8.0)
- **Mockk** 1.13.9
- **Turbine** 1.2.0 (Flow testing)

### Build
- **Gradle** 8.12
- **KSP** 2.1.0-1.0.29
- **Android Gradle Plugin** 8.8.0
- **Min SDK**: 31 (Android 12)
- **Target SDK**: 36 (Android 16)
- **Compile SDK**: 36

---


## 🚀 Instalación y Configuración

### Requisitos Previos
- **Android Studio** Ladybug (2024.2.1) o superior
- **JDK** 17
- **Android SDK** 31+ (compileSdk 36)
- **Proyecto Firebase** configurado

### 1. Clonar el Repositorio

```bash
git clone https://github.com/AzelMods677/NexusChat-Messenger.git
cd NexusChat-Messenger
```

### 2. Configurar Firebase

#### a) Crear Proyecto Firebase
1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Crea un nuevo proyecto
3. Añade una aplicación Android con el paquete: `com.Azelmods.App`

#### b) Descargar google-services.json
1. Descarga el archivo `google-services.json` de tu proyecto Firebase
2. Colócalo en `app/google-services.json`

#### c) Habilitar Servicios Firebase
En la consola de Firebase, habilita:
- ✅ **Authentication** (Email/Password y Google Sign-In)
- ✅ **Realtime Database**
- ✅ **Storage**
- ✅ **Cloud Messaging** (FCM)
- ✅ **Crashlytics** (opcional)

#### d) Configurar Reglas de Seguridad

**Realtime Database** (`database.rules.json`):
```json
{
  "rules": {
    "chats": {
      "$chatId": {
        ".read": "auth != null && (data.child('members').child(auth.uid).exists() || data.child('participants').child(auth.uid).exists())",
        ".write": "auth != null && (!data.exists() || data.child('members').child(auth.uid).exists() || data.child('participants').child(auth.uid).exists())"
      }
    },
    "userChats": {
      "$userId": {
        ".read": "auth != null && auth.uid == $userId",
        ".write": "auth != null && auth.uid == $userId"
      }
    },
    "users": {
      "$userId": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == $userId"
      }
    },
    "stories": {
      ".read": "auth != null",
      "$storyId": {
        ".write": "auth != null"
      }
    }
  }
}
```

Despliega las reglas con Firebase CLI:
```bash
firebase deploy --only database
```

### 3. Configurar API Key de Gemini (Opcional para IA)

La aplicación **NO incluye API key embebida**. Para usar el asistente IA:

#### Opción A: Desde la Aplicación (Recomendado)
1. Abre la app
2. Ve a **Ajustes** → **Funciones IA** → **Configurar API Key**
3. Pega tu API key de Gemini
4. Se guardará cifrada con AES-256 en el dispositivo

#### Opción B: Para Desarrollo (local.properties)
Crea `local.properties` en la raíz del proyecto:
```properties
GEMINI_API_KEY=tu_api_key_aqui
```

Obtén tu API key gratuita en: [Google AI Studio](https://makersuite.google.com/app/apikey)

### 4. Compilar y Ejecutar

```bash
# Limpiar y compilar Debug
./gradlew clean assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug

# Ejecutar tests
./gradlew test

# Generar APK Release (con splits por ABI)
./gradlew assembleRelease
```

### 5. Configuración Opcional

#### Navegación Tor (Orbot)
Para habilitar navegación anónima:

1. Instala **Orbot** desde:
   - [Google Play](https://play.google.com/store/apps/details?id=org.torproject.android)
   - [F-Droid](https://guardianproject.info/fdroid/)
   
2. Abre Orbot y pulsa **Iniciar**

3. En NexusChat:
   - Ve a **Seguridad** → **Control Tor**
   - La app detectará automáticamente Orbot
   - Activa el navegador Tor integrado

#### Servidor TURN para WebRTC (Producción)
Para mejorar conectividad en llamadas detrás de NAT estricto, configura un servidor TURN:

1. Instala [coturn](https://github.com/coturn/coturn)
2. Modifica `WebRTCManager.kt`:

```kotlin
val iceServers = listOf(
    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
    PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
    // Añade tu servidor TURN
    PeerConnection.IceServer.builder("turn:tu-servidor.com:3478")
        .setUsername("usuario")
        .setPassword("contraseña")
        .createIceServer()
)
```

---


## � Características Técnicas Detalladas

### Firebase Realtime Database - Arquitectura Optimizada

#### Estructura con Mapas e Índices (O(1) Access)

```javascript
{
  "chats": {
    "chatId123": {
      "members": {              // Map (no array) para O(1) lookup
        "uid1": true,
        "uid2": true
      },
      "participants": {          // Map (no array)
        "uid1": true,
        "uid2": true
      },
      "messages": {
        "msgId1": {
          "senderId": "uid1",
          "content": "Hola",
          "timestamp": 1234567890,
          "type": "text"
        }
      },
      "lastMessage": "Hola",
      "lastMessageTimestamp": 1234567890
    }
  },
  "userChats": {                 // Índice para acceso O(1)
    "uid1": {
      "chatId123": true,
      "chatId456": true
    }
  },
  "users": {
    "uid1": {
      "displayName": "Usuario",
      "email": "user@example.com",
      "photoUrl": "https://...",
      "status": "online",
      "lastSeen": 1234567890
    }
  },
  "stories": {
    "storyId1": {
      "userId": "uid1",
      "mediaUrl": "https://...",
      "type": "image",
      "timestamp": 1234567890,
      "expiresAt": 1234654290
    }
  }
}
```

**Ventajas**:
- `.child(auth.uid).exists()` es O(1) vs O(n) con arrays
- `userChats/{uid}` lista chats sin escanear toda la DB
- Escalable independiente del número de miembros
- Reglas de seguridad simples y eficientes

### WebRTC - Llamadas P2P

**Flujo de conexión**:
1. Caller crea oferta (SDP) → guarda en Firebase
2. Callee escucha oferta → crea respuesta (SDP)
3. Intercambio de ICE candidates vía Firebase
4. Establecimiento de conexión P2P directa

**ICE Candidate Buffering**:
Los candidatos ICE que llegan ANTES de setRemoteDescription se almacenan en buffer para evitar que WebRTC los descarte.

**Características implementadas**:
- ✅ Audio y video simultáneos (1280x720 @ 30fps)
- ✅ Cambio de cámara frontal/trasera
- ✅ Mute audio / Video on-off
- ✅ Echo cancellation, noise suppression, auto gain
- ✅ Notificaciones FCM de llamada entrante
- ✅ Foreground Service (Android 14+)

### Asistente IA - Gemini Integration

**Arquitectura del sistema IA**:

```
User Input → AzelAIViewModel → GeminiRequestQueue → GeminiRateLimiter → Gemini API (SSE Stream)
                    ↓                     ↓
              AiKeyStore         GeminiContextManager
           (EncryptedPrefs)      (últimos 8 msgs)
```

**Rate Limiting y Retry**:
- `GeminiRateLimiter`: Espaciado mínimo entre requests (429 prevention)
- `GeminiRequestQueue`: Backoff exponencial automático ante errores 429/quota
- Streaming con SSE (Server-Sent Events) para respuestas en tiempo real

**API Key Resolution**:
1. Primero: Clave del usuario (EncryptedSharedPreferences)
2. Fallback: BuildConfig.GEMINI_API_KEY (local.properties)

### Signal Protocol - E2E Encryption

**Componentes**:
- `SignalProtocolManager`: Gestión de sesiones Signal
- `SignalKeyStore`: Almacenamiento de claves (identity, pre-keys, session keys)
- `E2EECryptoService`: Encriptación/desencriptación de mensajes
- `PreKeyManager`: Generación y rotación de pre-keys

**Estado**: En desarrollo (infraestructura lista, integración pendiente)

### Tor Integration

**Proxy Detection**:
```kotlin
// Soporta 2 paquetes de Orbot
val ORBOT_PACKAGES = listOf(
    "org.torproject.android",           // Google Play / F-Droid
    "org.torproject.android.debug"      // Debug builds
)
```

**Proxy Cascade**:
1. HTTP Proxy: `localhost:8118` (Polipo)
2. Fallback SOCKS5: `localhost:9050` (Tor)

**Features**:
- WebView routing automático con NetCipher
- Firebase RTDB via Tor (opcional, configurable)
- Estado reactivo con StateFlow

---raphy,
        content = content
    )
}
```

### Cambio de Tema

El usuario puede cambiar el tema en **Ajustes → Apariencia → Color de acento**, y el cambio se aplica **instantáneamente** sin reiniciar la aplicación gracias a la observación reactiva con Flow.

---


## 🔐 Seguridad

### 1. App Lock - Bloqueo de Aplicación

Sistema completo de bloqueo con PIN y biometría:

#### Características
- **PIN de 4-6 dígitos** hasheado con SHA-256
- **Autenticación biométrica** (huella digital / Face ID)
- **Auto-bloqueo configurable**: 0 (inmediato), 1, 5, 30 minutos
- **Interceptor en lifecycle** (onResume/onPause)
- **UI profesional** con animaciones
- **Verificación automática** al completar PIN

#### Flujo de Bloqueo

```kotlin
// MainActivity verifica en cada onResume
override fun onResume() {
    super.onResume()
    lifecycleScope.launch {
        if (appLockManager.shouldLockOnResume()) {
            appLockManager.lock()  // Activa el bloqueo
        }
    }
}

// Se muestra AppLockScreen hasta que el usuario se autentique
if (showLockScreen.value) {
    AppLockScreen(
        onUnlocked = {
            showLockScreen.value = false
            appLockManager.unlock()
        }
    )
}
```

#### Configuración

```kotlin
// AppLockPreferences usa DataStore con encriptación
class AppLockPreferences(context: Context) {
    private val dataStore: DataStore<Preferences> = context.dataStore
    
    suspend fun setPin(pin: String) {
        dataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = hashPin(pin)  // SHA-256
        }
    }
    
    suspend fun verifyPin(pin: String): Boolean {
        val stored = dataStore.data.first()[KEY_PIN_HASH] ?: return false
        return stored == hashPin(pin)
    }
}
```

---

### 2. Copias de Seguridad Cifradas

Sistema robusto de backups con encriptación militar:

#### Características de Seguridad
- **AES-256-GCM**: Encriptación autenticada
- **PBKDF2** con 100,000 iteraciones para derivar claves
- **HMAC-SHA256** para verificación de integridad
- **Salt y IV únicos** por cada backup
- **Compresión GZIP** antes de encriptar

#### Formato de Archivo (.azelback)

```
[MAGIC_BYTES: 8 bytes]   "AZELBACK"
[VERSION: 1 byte]         0x01
[SALT: 32 bytes]          Random
[IV: 12 bytes]            Random
[HMAC: 32 bytes]          SHA-256
[ENCRYPTED_DATA: var]     AES-256-GCM
```

#### Proceso de Encriptación

```kotlin
fun encryptBackup(inputFile: File, outputFile: File, password: String): Boolean {
    // 1. Generar salt y IV aleatorios
    val salt = ByteArray(32).apply { SecureRandom().nextBytes(this) }
    val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
    
    // 2. Derivar clave de 256 bits con PBKDF2
    val key = deriveKey(password, salt)
    
    // 3. Comprimir datos con GZIP
    val compressed = compress(inputFile.readBytes())
    
    // 4. Encriptar con AES-256-GCM
    val encrypted = encrypt(compressed, key, iv)
    
    // 5. Calcular HMAC para integridad
    val hmac = calculateHMAC(encrypted, key)
    
    // 6. Escribir archivo con header
    writeBackupFile(outputFile, salt, iv, hmac, encrypted)
}
```

#### Almacenamiento Dual

Los backups se pueden guardar en:
- **Local**: `getExternalFilesDir("backups")` o `filesDir/backups`
- **Firebase Storage**: `backups/{uid}/{timestamp}_backup.azelback`

Con auto-limpieza que mantiene máximo 5 backups por ubicación.

---


### 3. Navegador Tor con Orbot

Soporte completo para navegación anónima a través de Tor:

#### Características
- **Detección automática** de Orbot instalado
- **Dual proxy support**: HTTP (8118) + SOCKS5 (9050) fallback
- **Soporte .onion** para sitios de la Dark Web
- **Estado visual** del proxy Tor
- **Integración con DuckDuckGo**

#### Implementación

```kotlin
// OrbotDetector verifica instalación y estado
object OrbotDetector {
    fun isOrbotInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("org.torproject.android", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            // Intenta con paquete alternativo
            try {
                context.packageManager.getPackageInfo("org.torproject.orbot", 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
    
    fun isTorAvailable(): Boolean {
        return try {
            // Verifica proxy HTTP
            val socket = Socket()
            socket.connect(InetSocketAddress("127.0.0.1", 8118), 1000)
            socket.close()
            true
        } catch (e: IOException) {
            // Intenta SOCKS5
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress("127.0.0.1", 9050), 1000)
                socket.close()
                true
            } catch (e: IOException) {
                false
            }
        }
    }
}

// TorBrowserScreen configura proxy en WebView
webView.settings.apply {
    // Configura proxy HTTP para Orbot
    if (OrbotDetector.isTorAvailable()) {
        System.setProperty("http.proxyHost", "127.0.0.1")
        System.setProperty("http.proxyPort", "8118")
        System.setProperty("https.proxyHost", "127.0.0.1")
        System.setProperty("https.proxyPort", "8118")
    }
}

// Permitir sitios .onion solo cuando Tor está activo
override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
    val url = request?.url?.toString() ?: return false
    return if (url.contains(".onion")) {
        if (OrbotDetector.isTorAvailable()) {
            view?.loadUrl(url)
            true
        } else {
            // Mostrar mensaje de que Orbot no está activo
            false
        }
    } else {
        view?.loadUrl(url)
        true
    }
}
```

---

### 4. Mensajes Autodestructivos

Los mensajes se eliminan automáticamente de Firebase al expirar:

#### Características
- **Temporizadores configurables**: desde segundos hasta días
- **Vista única** para multimedia sensible
- **Eliminación automática** en segundo plano
- **Countdown visual** en tiempo real

#### Implementación

```kotlin
// Model
data class Message(
    val messageId: String = "",
    val isEphemeral: Boolean = false,
    val isViewOnce: Boolean = false,
    val selfDestructDuration: Long = 0,  // segundos
    val selfDestructAt: Long = 0,         // timestamp
    val viewedBy: List<String> = emptyList()
)

// Cleanup automático en RealtimeDatabaseRepository
fun cleanupExpiredMessages(chatId: String) {
    val now = System.currentTimeMillis()
    messagesRef.child(chatId).get().addOnSuccessListener { snapshot ->
        snapshot.children.forEach { msg ->
            val destructAt = msg.child("selfDestructAt").getValue(Long::class.java) ?: 0L
            if (destructAt > 0 && now > destructAt) {
                msg.ref.removeValue()  // Elimina de Firebase
            }
        }
    }
}

// UI con countdown visual
@Composable
fun MessageBubble(message: Message) {
    var remainingSeconds by remember {
        mutableStateOf(calculateRemainingSeconds(message.selfDestructAt))
    }
    
    LaunchedEffect(message.selfDestructAt) {
        if (message.isEphemeral && message.selfDestructAt > 0) {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds = calculateRemainingSeconds(message.selfDestructAt)
            }
        }
    }
    
    if (remainingSeconds > 0) {
        LinearProgressIndicator(
            progress = remainingSeconds.toFloat() / message.selfDestructDuration.toFloat()
        )
        Text("🕐 ${remainingSeconds}s")
    }
}
```

---


## 🤖 Asistente de IA (AzelAI)

### Integración con Gemini

El asistente usa la **API key del usuario**, almacenada de forma segura:

#### Gestión de API Key

```kotlin
// AiKeyStore - Almacenamiento cifrado con EncryptedSharedPreferences
class AiKeyStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "azel_ai_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_GEMINI_API, apiKey).apply()
    }
    
    fun getApiKey(): String? {
        return encryptedPrefs.getString(KEY_GEMINI_API, null)
            ?: BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
    }
}
```

#### Selección de Modelos

```kotlin
enum class GeminiModel(val modelId: String, val displayName: String) {
    FLASH("gemini-1.5-flash", "Gemini 1.5 Flash (Rápido)"),
    PRO("gemini-1.5-pro", "Gemini 1.5 Pro (Avanzado)"),
    PRO_VISION("gemini-pro-vision", "Gemini Pro Vision (Imágenes)")
}
```

#### Cola con Rate Limiting

```kotlin
// GeminiRequestQueue - Gestión de peticiones con backoff
class GeminiRequestQueue @Inject constructor(
    private val rateLimiter: GeminiRateLimiter
) {
    private val requestQueue = mutableListOf<PendingRequest>()
    private var isProcessing = false
    
    suspend fun enqueue(request: GeminiRequest): Flow<GeminiResponse> = flow {
        if (!rateLimiter.canMakeRequest()) {
            emit(GeminiResponse.Error("Rate limit exceeded. Try again later."))
            return@flow
        }
        
        rateLimiter.recordRequest()
        
        try {
            val response = executeRequest(request)
            emit(response)
        } catch (e: Exception) {
            val backoffDelay = calculateBackoff(request.retryCount)
            delay(backoffDelay)
            
            if (request.retryCount < MAX_RETRIES) {
                enqueue(request.copy(retryCount = request.retryCount + 1))
            } else {
                emit(GeminiResponse.Error("Max retries exceeded"))
            }
        }
    }
    
    private fun calculateBackoff(retryCount: Int): Long {
        return (INITIAL_BACKOFF * (2.0.pow(retryCount))).toLong()
    }
}

// GeminiRateLimiter - Free tier: 15 RPM, 1 RPD
class GeminiRateLimiter {
    private val requestTimestamps = mutableListOf<Long>()
    
    fun canMakeRequest(): Boolean {
        val now = System.currentTimeMillis()
        
        // Limpiar timestamps antiguos (>1 minuto)
        requestTimestamps.removeAll { now - it > 60_000 }
        
        // Verificar límite de 15 por minuto
        return requestTimestamps.size < 15
    }
}
```

#### Streaming de Respuestas

```kotlin
@Composable
fun AzelAIScreen(viewModel: AzelAIViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.streamingResponse.collect { chunk ->
            // Actualiza mensaje en tiempo real
            viewModel.appendToLastMessage(chunk)
        }
    }
    
    // UI muestra respuestas en tiempo real
    LazyColumn {
        items(messages) { message ->
            MessageBubble(message)
        }
        
        if (isStreaming) {
            item {
                TypingIndicator()
            }
        }
    }
}
```

---


## 📸 Stories - Editor Multimedia

### Renderización de Overlays

El sistema de Stories renderiza overlays (texto, stickers, emojis) directamente en el archivo final:

#### Para Fotos (Canvas/GraphicsLayer)

```kotlin
@Composable
fun renderStoryPhoto(
    baseImage: Bitmap,
    overlays: List<StoryOverlay>
): Bitmap {
    return AndroidView(
        factory = { context ->
            ImageView(context).apply {
                setImageBitmap(baseImage)
            }
        },
        modifier = Modifier
            .graphicsLayer {
                // Captura toda la UI incluyendo overlays
                compositingStrategy = CompositingStrategy.Offscreen
                renderEffect = null
            }
            .drawWithContent {
                drawContent()
                
                // Dibuja overlays
                overlays.forEach { overlay ->
                    when (overlay) {
                        is TextOverlay -> drawText(overlay)
                        is StickerOverlay -> drawSticker(overlay)
                        is EmojiOverlay -> drawEmoji(overlay)
                    }
                }
            }
    ).toBitmap()
}
```

#### Para Videos (Media3 Transformer)

```kotlin
// StoryVideoComposer - Composición con Media3 Transformer
class StoryVideoComposer @Inject constructor(
    private val context: Context
) {
    fun composeVideo(
        videoUri: Uri,
        overlays: List<StoryOverlay>,
        outputFile: File
    ): Flow<CompositionProgress> = flow {
        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onTransformationCompleted(
                    composition: MediaItem,
                    result: ExportResult
                ) {
                    emit(CompositionProgress.Complete(outputFile))
                }
                
                override fun onTransformationError(
                    composition: MediaItem,
                    result: ExportResult,
                    exception: ExportException
                ) {
                    emit(CompositionProgress.Error(exception))
                }
            })
            .setVideoEffects(listOf(
                OverlayEffect(overlays)  // Custom effect
            ))
            .build()
        
        transformer.startTransformation(
            MediaItem.fromUri(videoUri),
            outputFile.absolutePath
        )
    }
    
    // Custom effect para dibujar overlays
    class OverlayEffect(
        private val overlays: List<StoryOverlay>
    ) : GlEffect {
        override fun toGlShaderProgram(
            context: Context,
            useHdr: Boolean
        ): GlShaderProgram {
            return object : BaseGlShaderProgram(useHdr) {
                override fun drawFrame(
                    inputTexId: Int,
                    presentationTimeUs: Long
                ) {
                    // Dibuja video base
                    super.drawFrame(inputTexId, presentationTimeUs)
                    
                    // Dibuja overlays en cada frame
                    overlays.forEach { overlay ->
                        drawOverlay(overlay, presentationTimeUs)
                    }
                }
            }
        }
    }
}
```

### Tipos de Overlays

```kotlin
sealed class StoryOverlay {
    abstract val id: String
    abstract val position: Offset
    abstract val rotation: Float
    abstract val scale: Float
    
    data class TextOverlay(
        override val id: String,
        override val position: Offset,
        override val rotation: Float,
        override val scale: Float,
        val text: String,
        val fontSize: Float,
        val color: Color,
        val fontFamily: FontFamily,
        val textAlign: TextAlign
    ) : StoryOverlay()
    
    data class StickerOverlay(
        override val id: String,
        override val position: Offset,
        override val rotation: Float,
        override val scale: Float,
        val stickerRes: Int
    ) : StoryOverlay()
    
    data class EmojiOverlay(
        override val id: String,
        override val position: Offset,
        override val rotation: Float,
        override val scale: Float,
        val emoji: String
    ) : StoryOverlay()
}
```

---


## 🧪 Testing y Calidad

### Estructura de Tests

```
app/src/
├── test/                              # Tests unitarios
│   ├── java/com/Azelmods/App/
│   │   ├── data/
│   │   │   ├── repository/
│   │   │   │   ├── ChatRepositoryTest.kt
│   │   │   │   └── UserRepositoryTest.kt
│   │   │   └── security/
│   │   │       ├── AppLockManagerTest.kt
│   │   │       └── BackupEncryptorTest.kt
│   │   └── ui/
│   │       └── viewmodel/
│   │           ├── ChatViewModelTest.kt
│   │           └── AzelAIViewModelTest.kt
│
└── androidTest/                       # Tests instrumentados
    └── java/com/Azelmods/App/
        ├── data/
        │   └── security/
        │       └── tor/
        │           └── OrbotDetectorTest.kt
        └── ui/
            └── screens/
                └── chat/
                    └── ChatScreenTest.kt
```

### Ejemplo de Test Unitario

```kotlin
@ExperimentalCoroutinesTest
class ChatViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var viewModel: ChatViewModel
    private val chatRepository: ChatRepository = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ChatViewModel(chatRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `sendMessage should update messages state`() = runTest {
        // Given
        val chatId = "chat123"
        val message = "Test message"
        coEvery { 
            chatRepository.sendMessage(chatId, message) 
        } returns Result.Success(Unit)
        
        // When
        viewModel.sendMessage(chatId, message)
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state.messages.any { it.content == message })
        coVerify { chatRepository.sendMessage(chatId, message) }
    }
    
    @Test
    fun `loadMessages should emit loading and success states`() = runTest {
        // Given
        val chatId = "chat123"
        val messages = listOf(
            Message(messageId = "1", content = "Hello"),
            Message(messageId = "2", content = "World")
        )
        coEvery { chatRepository.getMessages(chatId) } returns flowOf(messages)
        
        // When
        viewModel.loadMessages(chatId)
        
        // Then
        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem())
            val successState = awaitItem() as UiState.Success
            assertEquals(messages, successState.data)
        }
    }
}
```

### Tests de Seguridad

```kotlin
class BackupEncryptorTest {
    
    private lateinit var encryptor: BackupEncryptor
    private lateinit var testFile: File
    
    @Before
    fun setup() {
        encryptor = BackupEncryptor()
        testFile = File.createTempFile("test", ".txt")
        testFile.writeText("Sensitive data to encrypt")
    }
    
    @Test
    fun `encryption and decryption should preserve data`() {
        // Given
        val password = "strong_password_123"
        val encryptedFile = File.createTempFile("encrypted", ".azelback")
        val decryptedFile = File.createTempFile("decrypted", ".txt")
        
        // When
        val encrypted = encryptor.encryptBackup(testFile, encryptedFile, password)
        val decrypted = encryptor.decryptBackup(encryptedFile, decryptedFile, password)
        
        // Then
        assertTrue(encrypted)
        assertTrue(decrypted)
        assertEquals(testFile.readText(), decryptedFile.readText())
    }
    
    @Test
    fun `decryption with wrong password should fail`() {
        // Given
        val correctPassword = "correct123"
        val wrongPassword = "wrong456"
        val encryptedFile = File.createTempFile("encrypted", ".azelback")
        val decryptedFile = File.createTempFile("decrypted", ".txt")
        
        encryptor.encryptBackup(testFile, encryptedFile, correctPassword)
        
        // When
        val result = encryptor.decryptBackup(encryptedFile, decryptedFile, wrongPassword)
        
        // Then
        assertFalse(result)
    }
}
```

### Ejecutar Tests

```bash
# Tests unitarios
./gradlew test

# Tests instrumentados (requiere dispositivo/emulador)
./gradlew connectedAndroidTest

# Tests con cobertura
./gradlew testDebugUnitTestCoverage

# Tests específicos
./gradlew test --tests ChatViewModelTest
```

---


## 🔧 Herramientas de Desarrollo

### Build Variants

```kotlin
android {
    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            
            buildConfigField("String", "API_ENV", "\"development\"")
            resValue("string", "app_name", "NexusChat Debug")
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            buildConfigField("String", "API_ENV", "\"production\"")
            resValue("string", "app_name", "NexusChat")
        }
    }
}
```

### ProGuard/R8

Configuración de ofuscación para Release:

```proguard
# app/proguard-rules.pro

# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Data classes
-keep class com.Azelmods.App.data.model.** { *; }

# WebRTC
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
```

### CI/CD con GitHub Actions

```yaml
# .github/workflows/android.yml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Run tests
      run: ./gradlew test
    
    - name: Build Debug APK
      run: ./gradlew assembleDebug
    
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

---


## 📱 Requisitos del Sistema

### Mínimos
- **Android**: 7.0 (API 24) o superior
- **RAM**: 2 GB
- **Almacenamiento**: 100 MB disponibles
- **Permisos**:
  - Internet y estado de red
  - Cámara y micrófono (llamadas)
  - Almacenamiento (multimedia)
  - Notificaciones
  - Biometría (opcional, para App Lock)

### Recomendados
- **Android**: 12.0 (API 31) o superior
- **RAM**: 4 GB o más
- **Almacenamiento**: 500 MB disponibles
- **Procesador**: Octa-core 2.0 GHz+
- **GPU**: Soporte OpenGL ES 3.0+

---

## 🤝 Contribución

### Guía de Contribución

1. **Fork** el repositorio
2. Crea una **rama** para tu feature (`git checkout -b feature/AmazingFeature`)
3. **Commit** tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. **Push** a la rama (`git push origin feature/AmazingFeature`)
5. Abre un **Pull Request**

### Estándares de Código

- **Kotlin Coding Conventions**: Sigue las [convenciones oficiales](https://kotlinlang.org/docs/coding-conventions.html)
- **MVVM Pattern**: Mantén separación de capas
- **Single Responsibility**: Una responsabilidad por clase
- **Naming**: Nombres descriptivos en inglés
- **Comments**: Documenta código complejo
- **Tests**: Añade tests para nuevas funcionalidades

### Commit Messages

Usa formato convencional:

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

**Tipos**:
- `feat`: Nueva característica
- `fix`: Corrección de bug
- `docs`: Documentación
- `style`: Formato de código
- `refactor`: Refactorización
- `test`: Tests
- `chore`: Mantenimiento

**Ejemplo**:
```
feat(chat): add end-to-end encryption support

- Implement E2EE with Signal Protocol
- Add key exchange mechanism
- Update UI to show encrypted status

Closes #123
```

---


## 🐛 Reporte de Bugs

Para reportar un bug, abre un [issue](https://github.com/AzelMods677/NexusChat/issues) con:

1. **Descripción clara** del problema
2. **Pasos para reproducir**
3. **Comportamiento esperado** vs **comportamiento actual**
4. **Screenshots o videos** (si aplica)
5. **Información del dispositivo**:
   - Modelo
   - Versión de Android
   - Versión de la app

**Template de Bug Report**:

```markdown
## Descripción
[Describe el bug brevemente]

## Pasos para Reproducir
1. Ve a '...'
2. Haz clic en '....'
3. Desplázate hasta '....'
4. Observa el error

## Comportamiento Esperado
[Qué esperabas que pasara]

## Comportamiento Actual
[Qué pasó en realidad]

## Screenshots
[Si aplica, añade screenshots]

## Información del Dispositivo
- Dispositivo: [ej. Samsung Galaxy S21]
- OS: [ej. Android 13]
- Versión de la app: [ej. 2.0.0]

## Información Adicional
[Cualquier otra información relevante]
```

---

## 🔒 Seguridad y Privacidad

### Reporte de Vulnerabilidades

Para reportar vulnerabilidades de seguridad, **NO uses los issues públicos**. En su lugar:

1. Envía un email a: **security@azelmods.com**
2. Incluye:
   - Descripción de la vulnerabilidad
   - Pasos para reproducir
   - Impacto potencial
   - Sugerencias de mitigación (opcional)

### Políticas de Seguridad

- Las API keys **NUNCA** deben estar en el código fuente
- Usa `local.properties` para configuración local
- Las contraseñas se hashean con **SHA-256**
- Los backups usan **AES-256-GCM**
- Las comunicaciones con Firebase usan **TLS 1.3**

### Protección de Datos

- Los datos del usuario se almacenan en Firebase con reglas de seguridad
- Las preferencias sensibles usan `EncryptedSharedPreferences`
- Los archivos multimedia se almacenan en Firebase Storage con acceso autenticado
- Las sesiones se refrescan automáticamente cada 50 minutos

---


## 📚 Recursos Adicionales

### Documentación
- **Firebase**: [https://firebase.google.com/docs](https://firebase.google.com/docs)
- **Jetpack Compose**: [https://developer.android.com/jetpack/compose](https://developer.android.com/jetpack/compose)
- **WebRTC**: [https://webrtc.org/getting-started/overview](https://webrtc.org/getting-started/overview)
- **Gemini API**: [https://ai.google.dev/docs](https://ai.google.dev/docs)
- **Material 3**: [https://m3.material.io/](https://m3.material.io/)

### Tutoriales y Guías
- [Implementar WebRTC en Android](https://webrtc.org/getting-started/android)
- [Jetpack Compose Best Practices](https://developer.android.com/jetpack/compose/performance)
- [Firebase Security Rules](https://firebase.google.com/docs/rules)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)

### Comunidad
- **Discord**: [Azel Mods Community](https://discord.gg/azelmods)
- **Telegram**: [@NexusChatDev](https://t.me/NexusChatDev)
- **GitHub Discussions**: [Discusiones](https://github.com/AzelMods677/NexusChat/discussions)

---

## 🎓 Licencia

Este proyecto es **privado y propietario**. Todos los derechos reservados © 2024 Azel Mods.

**Restricciones**:
- ❌ No se permite la redistribución
- ❌ No se permite el uso comercial sin autorización
- ❌ No se permite la modificación sin autorización
- ✅ Se permite el uso con fines educativos (con atribución)

Para solicitar permisos o licencias comerciales, contacta a:
- **Email**: contact@azelmods.com
- **Website**: [https://azelmods.com](https://azelmods.com)

---

## 👨‍💻 Autor y Créditos

### Desarrollador Principal
**Azel Mods**
- GitHub: [@AzelMods677](https://github.com/AzelMods677)
- Email: dev@azelmods.com
- Website: [azelmods.com](https://azelmods.com)

### Agradecimientos

- **Google** por Firebase, Jetpack Compose y Gemini API
- **Tor Project** por Orbot y la red Tor
- **WebRTC** por la tecnología P2P
- **Kotlin Community** por las herramientas y bibliotecas
- **Material Design** por el sistema de diseño

---

## 📊 Estado del Proyecto

| Característica | Estado | Versión |
|---|---|---|
| Mensajería básica | ✅ Completo | 1.0.0 |
| Multimedia | ✅ Completo | 1.0.0 |
| Llamadas WebRTC | ✅ Completo | 1.2.0 |
| Stories | ✅ Completo | 1.3.0 |
| Asistente IA | ✅ Completo | 1.5.0 |
| Sistema de temas | ✅ Completo | 1.7.0 |
| Navegador Tor | ✅ Completo | 1.8.0 |
| App Lock | ✅ Completo | 2.0.0 |
| Backups cifrados | ✅ Completo | 2.0.0 |
| Mensajes autodestructivos | ✅ Completo | 2.0.0 |
| E2E Encryption | 🚧 En desarrollo | 2.1.0 |
| Grupos | 🚧 En desarrollo | 2.2.0 |
| Canales | 📋 Planeado | 3.0.0 |

### Última Actualización
**Versión**: 3.0.0  
**Fecha**: junio 2026  
**Build**: 100

---

## 📞 Contacto y Soporte

### Redes Sociales
- **YouTube**: [@Azelmods677](https://youtube.com/@Azelmods677)
- **TikTok**: [@azelmods677](https://tiktok.com/@azelmods677)
- **Telegram**: [@Azelmods677](https://t.me/Azelmods677)
- **GitHub**: [@Azelmods677](https://github.com/Azelmods677)

### Documentación
- [Changelog](./CHANGELOG.md) - Historial de versiones
- [Contributing](./CONTRIBUTING.md) - Guía de contribución
- [Security](./SECURITY.md) - Políticas de seguridad

---

<div align="center">

**⭐ Si te gusta el proyecto, dale una estrella en GitHub**

**Desarrollado con ❤️ por Azelmods677 · 2026**

[![GitHub](https://img.shields.io/badge/GitHub-Azelmods677-181717?logo=github)](https://github.com/Azelmods677)

</div>

