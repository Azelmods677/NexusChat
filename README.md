<div align="center">

<h1>
  🔐 NexusChat v3.0.0
</h1>

<p align="center">
  <strong>Plantilla Open Source de Mensajería Android · Enterprise Grade</strong>
</p>

<p align="center">
  <a href="#estado-de-completitud">
    <img src="https://img.shields.io/badge/Completitud-97.75%25-success?style=for-the-badge&logo=android&logoColor=white&labelColor=0d1117" alt="Completitud" />
  </a>
  <a href="#stack-tecnol%C3%B3gico">
    <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white&labelColor=0d1117" alt="Kotlin" />
  </a>
  <a href="#stack-tecnol%C3%B3gico">
    <img src="https://img.shields.io/badge/Compose-BOM%202025.04.01-4285F4?style=for-the-badge&logo=jetpack-compose&logoColor=white&labelColor=0d1117" alt="Compose" />
  </a>
  <a href="#stack-tecnol%C3%B3gico">
    <img src="https://img.shields.io/badge/Firebase-BOM%2033.7.0-FFCA28?style=for-the-badge&logo=firebase&logoColor=white&labelColor=0d1117" alt="Firebase" />
  </a>
  <a href="#licencia">
    <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge&logo=opensourceinitiative&logoColor=white&labelColor=0d1117" alt="License" />
  </a>
</p>

<p align="center">
  <a href="#arquitectura">🏗️ Arquitectura</a> ·
  <a href="#estado-de-completitud">📊 Completitud</a> ·
  <a href="#stack-tecnol%C3%B3gico">🛠️ Stack</a> ·
  <a href="#diagramas-de-flujo">🔄 Diagramas</a> ·
  <a href="#estructura-del-proyecto">📁 Estructura</a> ·
  <a href="#configuraci%C3%B3n">⚙️ Config</a> ·
  <a href="#m%C3%A9tricas">📈 Métricas</a>
</p>

</div>

---

## 🚀 ¿Qué es NexusChat?

NexusChat es una **plantilla de mensajería open source** para Android, construida como referencia de arquitectura **enterprise**. Incluye chat en tiempo real, llamadas WebRTC, cifrado end-to-end, IA integrada, y un sistema de seguridad multi-capa.

**Público objetivo:**
- 👶 **Devs junior** → Aprender Clean Architecture, Hilt, Compose, Firebase, Flow
- 🧙 **Devs senior** → Boilerplate con WebRTC, E2EE, Media3, AI streaming
- 🎓 **Estudiantes** → Ver cómo se conectan features complejos en una app real

**Pilares:**
- ✅ **100% Kotlin** — Código moderno, sin legacy Java
- ✅ **Arquitectura limpia** — MVVM + Clean Architecture + Hilt DI
- ✅ **Seguridad real** — E2EE (ECDH + AES-256-GCM), AppLock, Biometría, Root detection
- ✅ **Multimedia avanzada** — WebRTC P2P, Media3 Transformer, Coil 3
- ✅ **IA integrada** — Gemini streaming SSE + Ollama configurable
- ✅ **Offline-first** — Room cache + WorkManager retry
- ✅ **UX profesional** — Skeletons, pull-to-refresh, swipe-to-delete, animations

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                     UI Layer (Compose)                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │
│  │  Screens │ │ ViewModel│ │  State   │ │  Events  │     │
│  │  72+     │ │  24+     │ │  Flow    │ │  Actions │     │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘     │
├─────────────────────────────────────────────────────────────┤
│                    Domain Layer                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                  │
│  │ Use Cases│ │Repository│ │  Models  │                  │
│  │  19+     │ │Interfaces│ │  Data    │                  │
│  └──────────┘ └──────────┘ └──────────┘                  │
├─────────────────────────────────────────────────────────────┤
│                    Data Layer                               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │
│  │Firebase  │ │  Room    │ │ Storage  │ │   APIs   │     │
│  │  RTDB    │ │  Cache   │ │  Cloud   │ │ AI/WebRTC│     │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘     │
├─────────────────────────────────────────────────────────────┤
│              Dependency Injection (Hilt)                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                   │
│  │  AppMod  │ │FirebaseMod│ │SecurityMod│                  │
│  └──────────┘ └──────────┘ └──────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

**Patrón:** MVVM + Clean Architecture  
**Flujo de datos:** `UI → ViewModel → UseCase → Repository → DataSource`  
**Estado:** `StateFlow` unidireccional, `collectAsStateWithLifecycle()`

---

## 📊 Estado de Completitud

### 🟢 100% — Completamente Funcional

| Feature | Descripción | Estado |
|---------|-------------|--------|
| 🔐 **Auth (Email/Password)** | Login + Register + Logout. Firebase Auth. | ✅ |
| 🔐 **Auth (Google Sign-In)** | `play-services-auth` + `credentials-play-services-auth`. Launcher funcional. | ✅ |
| 💬 **Chat en tiempo real** | Firebase RTDB + paginación + reactions + reply + edit + delete + ephemeral. | ✅ |
| 📋 **Lista de chats** | Pull-to-refresh, swipe-to-delete, skeleton shimmer, pin, mute, archive. | ✅ |
| 🎨 **UI/UX general** | Todos los botones, settings, dialogs, snackbars funcionan. | ✅ |
| 🌐 **Traductor de mensajes** | MyMemory API, 12 idiomas, spinner de carga, error feedback, setting persistente. | ✅ |
| 🔒 **App Lock / Biometría** | PIN + fingerprint. EncryptedSharedPreferences. | ✅ |
| 👤 **Settings (Account)** | Change password + delete account dialogs funcionales. | ✅ |
| 🛡️ **Settings (Privacy)** | Blocked users, active sessions, passcode redirect, download data, delete data. | ✅ |
| 💾 **Settings (Storage)** | Data usage dialog + low data mode toggle. | ✅ |
| 🌐 **Settings (Translation)** | Pantalla de selección de idioma con RadioButton. | ✅ |
| 💻 **Code Editor** | Lista de archivos, editor, ejecutar código, output panel. | ✅ |
| 🤖 **AI Assistant (Gemini)** | Streaming SSE, rate limiting, cola de requests. | ✅ |
| 📸 **Stories** | Editor + viewer con Media3 Transformer. | ✅ |
| 📞 **Call Service** | FCM + notificaciones de llamada. Accept/Decline. | ✅ |
| 🔔 **Notifications** | FCM push + notification channel. | ✅ |
| 🖼️ **Image Viewer** | Full-screen con zoom, share, download, close. | ✅ |
| 🧅 **Tor Browser** | WebView + Orbot proxy + .onion sites. | ✅ |
| 🎨 **Theme / Personalización** | Material 3, colores, backgrounds, tipografía. | ✅ |

### 🟡 95% — Funcional con notas

| Feature | Descripción | Notas |
|---------|-------------|-------|
| 📞 **Llamadas WebRTC** | Llamadas P2P audio/video. | ✅ Funcionan. TURN servers gratuitos de OpenRelay agregados. |
| 🔐 **E2EE (ECDH + AES-256-GCM)** | Cifrado end-to-end. | ✅ Funcional en envío/recepción. Integración completa. |
| 📴 **Offline-First** | Room + WorkManager. | ✅ Cola de mensajes offline funcional. Room como cache. |
| 💾 **Backups Cifrados** | AES-256-GCM + PBKDF2. | ✅ Funcionan. Sin task cancellation (esqueleto). |
| 🤖 **AI (Ollama local)** | Configurable via setting. | ✅ Funciona. Sin detección de servidor offline. |
| 👤 **Edit Profile** | Cambiar foto, nombre, etc. | ✅ Redirige a account settings. Upload funciona. |
| ➕ **New Conversation** | Buscar por UID. | ✅ SearchViewModel con filtrado. Sin importación de contactos del teléfono. |
| 💻 **Terminal** | Sora Editor como emulador. | ✅ Comandos básicos funcionan. |
| 🎨 **Chat Backgrounds** | Cache en Room. | ✅ Funcional. Persistencia offline. |
| 🖼️ **Zoomable Cropper** | UI lista. | ⚠️ Crop transformation no aplica (devuelve URI original). |
| 🎬 **Media Gallery** | Lista funcional. | ⚠️ Video player sin zoom. |
| 📞 **Call History** | Lista básica. | ⚠️ Sin búsqueda ni filtros avanzados. |
| 📱 **Swipeable Screen** | Funcional. | ⚠️ Sin indicador visual de swipe. |
| 👥 **Contactos / Sync** | Manual por UID. | ⚠️ Sin importación de agenda telefónica. |

### 🔴 40% — Parcial / No implementado

| Feature | Descripción | Estado |
|---------|-------------|--------|
| 🧪 **Tests** | Unit tests. | ⚠️ 10 tests en ChatState. Falta cobertura de ViewModels. |
| ♿ **Accessibility** | TalkBack. | ⚠️ `contentDescription` en iconos principales. No auditado completo. |

---

## 🔄 Diagramas de Flujo

### 1. Flujo de Envío de Mensaje (con E2EE + Offline)

```
┌─────────┐     ┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│ Usuario │────→│ ChatScreen   │────→│ ChatViewModel│────→│ SendMessage │
│  Toca   │     │ Input Area   │     │              │     │   UseCase   │
│  Send   │     │              │     │              │     │             │
└─────────┘     └─────────────┘     └──────────────┘     └─────────────┘
                                                               │
                         ┌─────────────────────┐              │
                         │  ¿Network OK?       │              │
                         │  Sí → Firebase RTDB│              │
                         │  No → Room Pending │              │
                         └─────────────────────┘              ▼
                                                                
┌─────────────┐     ┌──────────────┐     ┌─────────────┐    ┌────────────┐
│   Room      │←────│  Firebase     │←────│  Repository│←───│  Encrypt   │
│  Pending    │     │   RTDB        │     │  (Data)    │    │ ECDH +     │
│  (Offline)  │     │  (Real-time)  │     │            │    │ AES-256-GCM│
└─────────────┘     └──────────────┘     └─────────────┘    └────────────┘
```

**Pasos:**
1. Usuario escribe y toca Send (haptic feedback)
2. `ChatViewModel` valida texto
3. `SendMessageUseCase` prepara mensaje
4. E2EE: `RealtimeDatabaseRepository` cifra con ECDH + AES-256-GCM
5. Si hay red: escribe en Firebase RTDB + Room cache
6. Si NO hay red: guarda en `PendingMessageEntity` (Room) + WorkManager retry
7. FCM notifica al receptor
8. `MessageBubble` recompone con `StateFlow`

### 2. Flujo de Llamada WebRTC (con TURN)

```
┌──────────┐              ┌──────────┐              ┌──────────┐
│  Caller  │ ──OFFER──→  │  STUN    │ ←──ANSWER──  │  Callee  │
│  (Alice) │              │  Server  │              │  (Bob)   │
└──────────┘              └──────────┘              └──────────┘
     │                         │                         │
     │  ICE Candidates         │                         │
     │────────────────────────→│←─────────────────────────│
     │                         │                         │
     ▼                         ▼                         ▼
┌──────────┐              ┌──────────┐              ┌──────────┐
│  TURN    │ ←─────────── │  P2P     │ ───────────→ │  TURN    │
│  Server  │   Fallback   │  Conn    │   Fallback   │  Server  │
│  (NAT)   │              │          │              │  (NAT)   │
└──────────┘              └──────────┘              └──────────┘
```

**Pasos:**
1. Alice toca llamada → `WebRTCManager.createOffer()`
2. Offer por Firebase RTDB signaling channel
3. Bob recibe offer → `createAnswer()` → answer
4. Intercambio de ICE candidates
5. Si NAT estricto → TURN server relay (OpenRelay)
6. `MediaStream` P2P directa (o relayed)

### 3. Flujo de Traducción

```
┌──────────┐     ┌──────────────┐     ┌────────────────┐
│ LongPress │────→│ translateMsg │────→│ TranslationService│
│  Message  │     │ (ChatViewModel)│    │   (MyMemory API) │
└──────────┘     └──────────────┘     └────────────────┘
                                              │
                    ┌──────────────┐         │
                    │  ChatState    │←────────┘
                    │ translatingIds│
                    │ translatedMsgs│
                    └──────────────┘
```

**Pasos:**
1. Usuario hace long-press en mensaje → haptic feedback → menú contextual
2. Toca "Traducir" → `ChatViewModel.translateMessage()`
3. Se muestra spinner "Traduciendo..." en `MessageBubble`
4. `TranslationService` consulta MyMemory API (GET `api.mymemory.translated.net/get`)
5. Idioma destino: `UserPreferences.translationLanguage` (default = device locale)
6. Si success: texto traducido aparece debajo del original (italic, diferente alpha)
7. Si fail: `translationError` → Snackbar en `ChatScreen`
8. Toggle: si ya existe traducción, tocar de nuevo la elimina

### 4. Flujo de Backup Cifrado

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐     ┌────────────┐
│  User    │────→│  BackupUse   │────→│  AES-256-GCM │────→│  .azelback │
│  Export  │     │  Case        │     │  + PBKDF2    │     │  file      │
└──────────┘     └──────────────┘     └──────────────┘     └────────────┘
```

**Pasos:**
1. Usuario va a Settings → Privacy → Export Data
2. Se genera clave de derivación con PBKDF2 (100k iteraciones, salt aleatorio)
3. Datos de Firebase se serializan a JSON (profile, chats, settings)
4. AES-256-GCM cifra el JSON con clave derivada
5. Archivo `.azelback` se guarda en almacenamiento local compartido
6. Se comparte via `Intent.ACTION_SEND` (email, Drive, etc.)
7. **Import:** mismo flujo inverso → PBKDF2 → AES decrypt → Firebase restore

### 5. Flujo de AI Assistant (Gemini)

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐     ┌────────────┐
│  User    │────→│  AzelAIScreen│────→│  AiManager   │────→│  Gemini API │
│  Prompt  │     │  Input       │     │  (SSE Queue) │     │  (Streaming)│
└──────────┘     └──────────────┘     └──────────────┘     └────────────┘
                                              │                    │
                                              ▼                    ▼
                                       ┌──────────────┐     ┌────────────┐
                                       │  RateLimiter │     │  Response  │
                                       │  (tokens/min)│     │  (SSE)     │
                                       └──────────────┘     └────────────┘
```

**Pasos:**
1. Usuario escribe prompt en `AzelAIScreen`
2. `AiManager` verifica `RateLimiter` (previene costos excesivos)
3. Si dentro del límite: request a Gemini API con `OkHttp + SSE`
4. Si fuera del límite: request se encola en `pendingQueue`
5. Respuesta llega en streaming (SSE chunks) → se muestra palabra por palabra
6. Historia de conversación guardada en `DataStore`
7. **Prompts "hacking":** incluidos en `UncensoredPrompts.kt` (jailbreak, DAN, etc.)

---

## 🛠️ Stack Tecnológico

### Core
| Tecnología | Versión | Uso |
|------------|---------|-----|
| Kotlin | 2.0.21 | 100% del código fuente |
| Jetpack Compose | BOM 2025.04.01 | UI declarativa |
| Material 3 | Compose BOM | Design system |
| Hilt | 2.52 | Inyección de dependencias |
| KSP | 2.0.21-1.0.28 | Procesamiento de anotaciones |
| Coroutines | 1.9.0 | Concurrencia + Flow |
| Java 17 | 17 | sourceCompatibility / targetCompatibility |

### Firebase
| Servicio | Uso |
|----------|-----|
| Authentication | Email/Password + Google Sign-In |
| Realtime Database | Mensajes, presencia, typing, signaling WebRTC |
| Cloud Storage | Imágenes, audio, video, documentos |
| Cloud Messaging | Notificaciones push |
| Crashlytics | Reporte de crashes |

### Multimedia
| Tecnología | Uso |
|------------|-----|
| WebRTC | Llamadas P2P (audio/video) |
| Media3 ExoPlayer | Reproducción de video/stories |
| Media3 Transformer | Export de stories con overlays |
| Coil 3 | Imágenes, GIF, video thumbnails |

### Seguridad
| Tecnología | Uso |
|------------|-----|
| E2EE (ECDH + AES-256-GCM) | Cifrado end-to-end en mensajes |
| AES-256-GCM | Backups cifrados |
| PBKDF2 | Derivación de clave de backups (100k iteraciones) |
| EncryptedSharedPreferences | API keys, PIN hash, tokens |
| Biometric | AppLock + desbloqueo |
| libsu | Root detection |
| NetCipher | Tor proxy integration |

### IA
| Tecnología | Uso |
|------------|-----|
| Gemini API | Streaming SSE, rate limiting, cola de requests |
| Ollama API | Modelo local configurable |
| Uncensored Prompts | Jailbreak, DAN, prompts de "hacking" |

### Testing
| Tecnología | Uso |
|------------|-----|
| JUnit 4 | Tests unitarios (base) |
| Kotest 5.8.0 | BDD testing |
| Mockk 1.13.9 | Mocking |
| Turbine 1.2.0 | Testing de Flows |
| Espresso | Tests de UI (base) |

---

## 📁 Estructura del Proyecto

```
com.Azelmods.App
├── MainActivity.kt              # Entry point, NavHost, deep links
├── NexusChatApplication.kt    # Hilt Application, notif channels, Coil
│
├── data/                        # Data Layer (21.2% del código)
│   ├── ai/                      # Gemini, Ollama, prompts, rate limiting
│   ├── api/                     # Retrofit/OkHttp services
│   ├── backup/                  # Encrypted .azelback (AES-256-GCM + PBKDF2)
│   ├── chat/                    # ChatId helpers, ChatManager
│   ├── demo/                    # Demo accounts
│   ├── encryption/              # E2EE (ECDH + AES-256-GCM), Signal Protocol
│   ├── file/                    # Cache, SecureFileManager
│   ├── firebase/                # FirebaseManager, FirebaseAuth helpers
│   ├── local/                   # Room: DB, DAOs, Entities
│   │   ├── dao/                 # MessageDao, ChatDao, UserDao, PendingMessageDao
│   │   ├── entity/              # CachedMessage, CachedChat, CachedUser, PendingMessage
│   │   └── AppDatabase.kt       # Room Database v2
│   ├── manager/                 # AiManager, BackgroundManager
│   ├── model/                   # User, Chat, Message, Story...
│   ├── preferences/             # DataStore wrappers (UserPreferences)
│   ├── repository/              # 10 Repositories (Auth, Chat, RTDB, Storage...)
│   ├── security/                # AppLock, Tor, NetCipher, AppLockManager
│   ├── session/                 # SessionManager
│   ├── translation/             # MyMemory API, TranslationService
│   └── work/                    # SendPendingMessagesWorker (WorkManager + HiltWorker)
│
├── domain/                      # Domain Layer (1.5%)
│   ├── repository/              # Repository interfaces
│   └── usecase/                 # AuthUseCase, ChatUseCase, etc.
│
├── di/                          # Hilt Modules (0.4%)
│   ├── AppModule.kt
│   ├── FirebaseModule.kt
│   ├── RepositoryModule.kt
│   └── SecurityModule.kt
│
├── ui/                          # UI Layer (67.7%)
│   ├── components/              # 26 reusable components
│   │   ├── chat/                # MessageBubble, TypingIndicator, ChatInputBar
│   │   ├── UserAvatar.kt
│   │   ├── FullScreenImageViewer.kt
│   │   └── ZoomableCropper.kt
│   ├── screens/                 # 72+ screens by feature
│   │   ├── auth/                # Login, Register, Splash
│   │   ├── chat/                # Chat, ChatViewModel, ChatState
│   │   ├── home/                # Home, HomeViewModel, ChatListScreen
│   │   ├── settings/            # All settings (Account, Privacy, Storage, Appearance...)
│   │   ├── call/                # ActiveCall, IncomingCall, CallViewModel
│   │   ├── stories/             # CreateStory, StoryViewer, StoryEditor
│   │   ├── editor/              # CodeEditorScreen, CodeEditorViewModel, CodeFile
│   │   ├── terminal/            # TerminalScreen, TerminalViewModel, RealTerminalEmulator
│   │   ├── profile/             # ProfileScreen, EditProfileScreen
│   │   └── about/               # AboutScreen, AboutScreenRedesigned
│   ├── theme/                   # Material 3 theme, colors, typography, dark mode
│   └── utils/                   # UI helpers, navigation extensions
│
├── service/                     # FCM, FirebaseMessagingService
├── services/                    # CallService (foreground service for calls)
├── startup/                     # FirebaseInitializer (App Startup library)
├── security/                    # RootDetection, TamperDetection
├── util/ & utils/               # General helpers, extensions
├── webrtc/                      # WebRTCManager (call lifecycle, peer connection, TURN)
└── navigation/                  # NavGraph, Screen routes, type-safe navigation, deep links
```

---

## 🔐 Seguridad

| Amenaza | Mitigación | Estado |
|---------|------------|--------|
| 🕵️ Firebase admin lee mensajes | E2EE (ECDH + AES-256-GCM) | ✅ Funcional en envío/recepción |
| 📱 Dispositivo robado | AppLock + Biometría + EncryptedSharedPreferences | ✅ |
| 🔓 Root / jailbreak | libsu detection + Tamper detection | ✅ |
| 🔑 API keys expuestas | EncryptedSharedPreferences + fallback mínimo | ⚠️ Ollama key hardcoded en build.gradle (fallback dev) |
| 🕵️ Man-in-the-middle | TLS 1.3 + Firebase cert pinning | ✅ |
| 💾 Backups no autorizados | AES-256-GCM + PBKDF2 + password derivada | ✅ |
| 📹 Screen recording | FLAG_SECURE | ✅ Aplicado en AuthScreen |
| 📸 Screenshot | FLAG_SECURE | ✅ Aplicado en AuthScreen |
| 📋 Clipboard | Clear clipboard en logout | ✅ |

---

## ⚙️ Configuración

### Requisitos
- Android Studio Koala o superior
- JDK 17
- Gradle 8.14

### Setup paso a paso
1. **Clona el repo**
   ```bash
   git clone https://github.com/Azelmods677/NexusChat.git
   cd NexusChat
   ```

2. **Crea `local.properties`** en la raíz:
   ```properties
   sdk.dir=/path/to/android/sdk
   OLLAMA_API_KEY=tu_key_aqui
   OLLAMA_BASE_URL=https://tu-ollama.com/v1
   GEMINI_API_KEY=tu_gemini_key
   FCM_SERVER_KEY=tu_fcm_key
   ```

3. **Conecta Firebase:**
   - Crea proyecto en [Firebase Console](https://console.firebase.google.com)
   - Descarga `google-services.json` → coloca en `app/`
   - Agrega SHA-1 de debug: `./gradlew signingReport`
   - Habilita Authentication, Realtime Database, Storage, FCM

4. **Google Sign-In:**
   - Agrega SHA-1 en Firebase Console → Project Settings → Add fingerprint
   - Verifica `default_web_client_id` en `app/src/main/res/values/strings.xml`
   - Asegúrate que el package name coincide

5. **Sync y run:**
   ```bash
   ./gradlew assembleDebug
   ```

### 🔗 Deep Links

La app soporta deep links para abrir chats y perfiles directamente:

```bash
# Abrir un chat
nexuschat://chat/{chatId}

# Abrir un perfil
nexuschat://profile/{userId}
```

Ejemplo desde terminal ADB:
```bash
adb shell am start -a android.intent.action.VIEW -d "nexuschat://chat/userA_userB"
```

---

## 📈 Métricas

| Métrica | Valor |
|---------|-------|
| 📝 Líneas de código Kotlin | 58,242 |
| 📄 Archivos Kotlin | 253 |
| 📄 Archivos Java | 0 (100% Kotlin) |
| 🏗️ Clases / Objetos | ~157 |
| 🎨 Composables @Composable | 88 |
| 🧠 ViewModels @HiltViewModel | 24 |
| 🖼️ Pantallas (Screens) | 72+ |
| 💾 Repositories | 10 |
| ⚙️ Use Cases | 19 |
| 🔌 Hilt Modules | 4 |
| 🛡️ Bloques try-catch | 401 |
| ⚠️ Operadores `!!` | 4 (solo en UI) |
| 📭 Estados vacíos implementados | 84 |
| ⏳ Loading states | 35 |
| 🔔 Snackbars | 59 |
| ✨ Animaciones | 33 |
| 🧪 Tests unitarios | 10 (ChatStateTest) |
| 📱 Min SDK | 31 (Android 12) |
| 🎯 Target SDK | 36 (Android 16) |
| 🔧 Compile SDK | 36 |
| 💜 Kotlin | 2.0.21 |
| 🏗️ AGP | 8.7.3 |
| ⚙️ Gradle | 8.14 |

---

## 🤝 Contribución

Las contribuciones son bienvenidas. Antes de hacer un PR:
1. 🔀 Fork el repo
2. 🌿 Crea una branch (`git checkout -b feature/nueva-feature`)
3. 💾 Commit tus cambios (`git commit -m 'Agrega nueva feature'`)
4. 📤 Push a la branch (`git push origin feature/nueva-feature`)
5. 📋 Abre un Pull Request

**Reglas:**
- 💜 100% Kotlin
- 🏗️ Seguir Clean Architecture
- 🧪 Agregar tests para nuevos ViewModels
- 📖 Documentar features en el README

---

## 📄 Licencia

MIT License — Libre para uso personal, comercial, y educativo.  
Crédito a **Azel Mods** como autor original.

---

## 🌐 Redes Oficiales

| Plataforma | Enlace |
|------------|--------|
| 🎬 **YouTube** | [youtube.com/@AzelModsx677](https://www.youtube.com/@AzelModsx677) |
| 🎵 **TikTok** | [tiktok.com/@azelmodsx677](https://www.tiktok.com/@azelmodsx677?lang=es) |
| ✈️ **Telegram** | [t.me/AzelModsx67779](https://t.me/AzelModsx67779) |
| 🐙 **GitHub** | [github.com/Azelmods677](https://github.com/Azelmods677) |

---

<p align="center">
  <strong>⭐ Si te sirvió esta plantilla, dale una estrella en GitHub ⭐</strong>
</p>

<p align="center">
  <em>Construido con ❤️ por <strong>Azel Mods</strong> para la comunidad de desarrolladores Android</em>
</p>
