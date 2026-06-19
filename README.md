


<img width="1254" height="1254" alt="icono de la aplicacion" src="https://github.com/user-attachments/assets/6fac646f-da64-41e7-a9e9-73c0c96abfff" />



<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:7F52FF,100:4285F4&height=200&section=header&text=NexusChat&fontSize=70&fontColor=ffffff&animation=fadeIn&fontAlignY=35&desc=Plantilla%20Open%20Source%20de%20Mensajería%20Android%20%7C%20Enterprise%20Grade&descAlignY=55&descSize=18" />

<br>

<p align="center">
  <a href="https://github.com/Azelmods677/NexusChat/stargazers">
    <img src="https://img.shields.io/github/stars/Azelmods677/NexusChat?style=flat-square&logo=github&color=FFD700&labelColor=0d1117" alt="Stars" />
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white&labelColor=0d1117" alt="Kotlin" />
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/Compose-BOM%202025.04.01-4285F4?style=flat-square&logo=jetpack-compose&logoColor=white&labelColor=0d1117" alt="Compose" />
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/Firebase-BOM%2033.7.0-FFCA28?style=flat-square&logo=firebase&logoColor=white&labelColor=0d1117" alt="Firebase" />
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/Min%20SDK-31%20(Android%2012)-3DDC84?style=flat-square&logo=android&logoColor=white&labelColor=0d1117" alt="Min SDK" />
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/Target%20SDK-36%20(Android%2016)-3DDC84?style=flat-square&logo=android&logoColor=white&labelColor=0d1117" alt="Target SDK" />
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-00C853?style=flat-square&logo=opensourceinitiative&logoColor=white&labelColor=0d1117" alt="License" />
  </a>
</p>

<p align="center">
  <a href="#-qué-es-nexuschat">✨ Qué es</a> ·
  <a href="#-arquitectura">🏗️ Arquitectura</a> ·
  <a href="#-features">🚀 Features</a> ·
  <a href="#-stack-tecnológico">🛠️ Stack</a> ·
  <a href="#-seguridad">🔐 Seguridad</a> ·
  <a href="#-quick-start">⚡ Quick Start</a> ·
  <a href="#-redes">🌐 Redes</a>
</p>

</div>

---

<br>

## ✨ ¿Qué es NexusChat?

**NexusChat** es una plantilla de mensajería **open source** para Android, construida con arquitectura **enterprise-grade**. Diseñada para desarrolladores que necesitan un boilerplate sólido: chat en tiempo real, llamadas WebRTC, cifrado end-to-end, inteligencia artificial integrada, y un sistema de seguridad multi-capa — todo en **100% Kotlin** con Jetpack Compose.

**Público objetivo:**
- 🧑‍💻 **Devs junior** → Aprende Clean Architecture, Hilt, Firebase, Compose y Flow en un proyecto real.
- 🧙‍♂️ **Devs senior** → Base de código con WebRTC, E2EE, Media3 Transformer, AI Streaming y Tor.
- 🎓 **Estudiantes** → Observa cómo se conectan features complejos de producción en una sola app.

<br>

<div align="center">

| 🏗️ Arquitectura | 🔐 Seguridad | 🤖 IA | 📞 WebRTC | 🎨 UX |
|:---:|:---:|:---:|:---:|:---:|
| MVVM + Clean | E2EE + AppLock | Gemini + Ollama | P2P + TURN | Material 3 + Animaciones |

</div>

<br>

---

<br>

## 🏗️ Arquitectura

<div align="center">

```
┌────────────────────────────────────────────────────────────┐
│                      UI Layer (Compose)                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ 72+      │ │ 24+      │ │ StateFlow│ │ Events   │       │
│  │ Screens  │ │ ViewModels│ │ States   │ │ Actions  │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
├────────────────────────────────────────────────────────────┤
│                     Domain Layer                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                    │
│  │ 19+      │ │ Repository│ │ Data     │                    │
│  │ Use Cases│ │ Interfaces│ │ Models   │                    │
│  └──────────┘ └──────────┘ └──────────┘                    │
├────────────────────────────────────────────────────────────┤
│                     Data Layer                               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │
│  │ Firebase │ │ Room     │ │ Storage  │ │ APIs     │     │
│  │ RTDB     │ │ Cache    │ │ Cloud    │ │ AI/WebRTC│     │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘     │
├────────────────────────────────────────────────────────────┤
│              Dependency Injection (Hilt)                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                     │
│  │ AppModule│ │ Firebase │ │ Security │                     │
│  └──────────┘ └──────────┘ └──────────┘                     │
└────────────────────────────────────────────────────────────┘
```

</div>

**Patrón:** MVVM + Clean Architecture  
**Flujo:** `UI → ViewModel → UseCase → Repository → DataSource`  
**Estado:** Unidireccional con `StateFlow` + `collectAsStateWithLifecycle()`

<br>

---

<br>

## 🚀 Features

<div align="center">

### 💬 Mensajería en Tiempo Real

</div>

- **Firebase Realtime Database** con arquitectura optimizada (mapas e índices para acceso O(1))
- **Multimedia completo**: imágenes, videos, audio, documentos
- **Mensajes efímeros** con auto-destrucción configurable
- **Reacciones con emojis**, respuestas, reenvíos, edición y borrado
- **Indicadores de lectura** y estado de conexión en línea
- **Pull-to-refresh** y **swipe-to-delete** en lista de chats
- **Skeleton shimmer** durante carga

<div align="center">

### 📞 Llamadas WebRTC P2P

</div>

- **Llamadas de audio y video** en alta calidad (1280x720 @ 30fps)
- **Señalización vía Firebase** Realtime Database
- **STUN + TURN servers** integrados (incluye OpenRelay para NAT estricto)
- **Notificaciones de llamada** con Firebase Cloud Messaging
- **Controles**: mute, video on/off, cambio de cámara
- **Echo cancellation, noise suppression, auto gain**

<div align="center">

### 📸 Stories Multimedia (24h)

</div>

- **Editor avanzado** con overlays en tiempo real
- **Stickers y emojis** arrastrables
- **Texto personalizable** con fuentes y colores
- **Renderizado final**: Canvas para fotos, **Media3 Transformer** para videos
- **Auto-eliminación** después de 24 horas

<div align="center">

### 🤖 Asistente IA — Azel AI

</div>

- **Integración con Gemini** (Google Generative AI)
- **API key cifrada** con AES-256 en EncryptedSharedPreferences
- **Streaming en tiempo real** con SSE (Server-Sent Events)
- **Cola con backoff exponencial** y rate limiting inteligente
- **Gestión de contexto** (últimos 8 mensajes)
- **Ollama configurable** para modelo local

<div align="center">

### 🌐 Traducción de Mensajes

</div>

- **12 idiomas** disponibles (Español, English, Français, Deutsch, Português, Italiano, 日本語, 中文, 한국어, Русский, العربية)
- **MyMemory API** con rate limiting
- **Spinner de carga** y feedback visual en cada mensaje
- **Toggle**: traducir / eliminar traducción con un toque
- **Preferencia persistente** en DataStore

<div align="center">

### 💻 Code Editor + Terminal

</div>

- **Editor de código** con lista de archivos, syntax highlighting y panel de output
- **Terminal integrado** con Sora Editor (0.23.5)
- Soporte para ejecutar código en múltiples lenguajes

<div align="center">

### 🧅 Navegador Tor Integrado

</div>

- **Enrutado automático** por Orbot (HTTP 8118 + SOCKS5 9050 fallback)
- **Soporte para sitios .onion**
- **Detección automática** de Orbot instalado
- **WebView** con NetCipher para navegación anónima

<div align="center">

### 🎨 Sistema de Temas Dinámicos

</div>

- **25 colores de acento** curados profesionalmente
- **Material 3** Design System
- **Cambio en tiempo real** sin reiniciar la app
- **Fondos personalizables**: color sólido, gradiente, video
- **Modo oscuro** optimizado

<br>

---

<br>

## 🔐 Seguridad

<div align="center">

| Capa | Tecnología | Descripción |
|:---|:---|:---|
| 🔒 **App Lock** | PIN + Biometría | SHA-256 + EncryptedSharedPreferences |
| 🔐 **E2EE** | ECDH + AES-256-GCM | Cifrado end-to-end en mensajes |
| 💾 **Backups** | AES-256-GCM + PBKDF2 | 100k iteraciones, formato .azelback |
| 🧅 **Tor** | Orbot + NetCipher | Proxy HTTP/SOCKS5 para navegación anónima |
| 🛡️ **Root Detection** | libsu | Bloqueo en dispositivos rooteados |
| 🛡️ **Tamper Detection** | Security-Crypto | Detección de manipulación de APK |
| 🚫 **Screen Recording** | FLAG_SECURE | Bloqueo en pantallas sensibles |

</div>

<br>

---

<br>

## 🛠️ Stack Tecnológico

<div align="center">

### Core

| Kotlin | Compose | Material 3 | Hilt | Coroutines |
|:---:|:---:|:---:|:---:|:---:|
| 2.0.21 | BOM 2025.04.01 | Compose BOM | 2.52 | 1.9.0 |

### Firebase

| Auth | RTDB | Storage | FCM | Crashlytics |
|:---:|:---:|:---:|:---:|:---:|
| Email/Google | Real-time | Multimedia | Push | Errors |

### Multimedia

| WebRTC | ExoPlayer | Transformer | Coil |
|:---:|:---:|:---:|:---:|
| P2P Calls | Video | Stories | Images/GIF |

### Seguridad

| E2EE | AES-256-GCM | PBKDF2 | Biometric | libsu |
|:---:|:---:|:---:|:---:|:---:|
| Messages | Backups | Key Derivation | AppLock | Root Detection |

### IA

| Gemini | SSE | Rate Limiter | Ollama |
|:---:|:---:|:---:|:---:|
| Streaming | Real-time | Queue | Local Model |

</div>

<br>

---

<br>

## ⚡ Quick Start

```bash
# 1. Clonar
git clone https://github.com/Azelmods677/NexusChat.git
cd NexusChat

# 2. Crear local.properties
cat > local.properties << 'EOF'
sdk.dir=/path/to/android/sdk
GEMINI_API_KEY=tu_key
FCM_SERVER_KEY=tu_key
EOF

# 3. Descargar google-services.json en app/
#    (desde Firebase Console)

# 4. Compilar
./gradlew assembleDebug
```

### Requisitos
- Android Studio Koala+ · JDK 17 · Gradle 8.14
- Firebase project configurado (Auth, RTDB, Storage, FCM)

### 🔗 Deep Links

```bash
nexuschat://chat/{chatId}
nexuschat://profile/{userId}
```

<br>

---

<br>

## 📈 Métricas del Proyecto

<div align="center">

| 📊 **58,242** líneas Kotlin | 📄 **253** archivos | 🏗️ **157** clases | 🎨 **88** Composables |
|:---:|:---:|:---:|:---:|
| 🧠 **24** ViewModels | 💾 **10** Repositories | ⚙️ **19** Use Cases | 🔌 **4** Hilt Modules |

</div>

<br>

---

<br>

## 🌐 Redes Oficiales

<div align="center">

| 🎬 YouTube | 🎵 TikTok | ✈️ Telegram | 🐙 GitHub |
|:---:|:---:|:---:|:---:|
| [@AzelModsx677](https://www.youtube.com/@AzelModsx677) | [@azelmodsx677](https://www.tiktok.com/@azelmodsx677?lang=es) | [@AzelModsx67779](https://t.me/AzelModsx67779) | [@Azelmods677](https://github.com/Azelmods677) |

</div>

<br>

---

<br>

<div align="center">

**⭐ Si te sirvió esta plantilla, dale una estrella en GitHub ⭐**

<br>

*Construido con ❤️ por **Azel Mods** para la comunidad de desarrolladores Android*

<br>

<a href="https://github.com/Azelmods677/NexusChat">
  <img src="https://img.shields.io/github/stars/Azelmods677/NexusChat?style=social&logo=github" alt="GitHub Stars" />
</a>

</div>

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:4285F4,100:7F52FF&height=100&section=footer" />
