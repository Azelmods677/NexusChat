<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=2,22,28,40&height=200&section=header&text=CHANGELOG&fontSize=70&fontColor=fff&animation=twinkling&fontAlignY=35&desc=NexusChat%20—%20Historial%20de%20Cambios&descAlignY=60&descSize=20&descAlign=50" />

<br>

<p align="center">
  <a href="https://github.com/Azelmods677/NexusChat">
    <img src="https://img.shields.io/badge/Version-3.0.1-7F52FF?style=for-the-badge&logo=android&logoColor=white&labelColor=0d1117" alt="Version" />
  </a>
  <a href="https://github.com/Azelmods677/NexusChat/releases">
    <img src="https://img.shields.io/github/v/release/Azelmods677/NexusChat?style=for-the-badge&logo=github&color=00E676&labelColor=0d1117" alt="Latest Release" />
  </a>
  <a href="https://github.com/Azelmods677/NexusChat/commits/main">
    <img src="https://img.shields.io/github/last-commit/Azelmods677/NexusChat?style=for-the-badge&logo=git&color=FFD700&labelColor=0d1117" alt="Last Commit" />
  </a>
</p>

<p align="center">
  <a href="#v301">🆕 v3.0.1</a> ·
  <a href="#v300">🚀 v3.0.0</a> ·
  <a href="#v220">🔧 v2.2.0</a> ·
  <a href="#v210">⚡ v2.1.0</a> ·
  <a href="#v200">🎉 v2.0.0</a> ·
  <a href="#v100">🌱 v1.0.0</a>
</p>

</div>

---

<br>

<div align="center">

<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&size=24&duration=2000&pause=500&color=00E676&center=true&vCenter=true&width=600&lines=🆕+v3.0.1+—+Robustez+y+Estabilidad;2025-06-15" alt="v3.0.1" />

</div>

<br>

> 🎯 **Enfoque**: Corrección de 4 bugs críticos reportados por usuarios. Todos los fixes priorizan **graceful degradation** — nunca crashea, siempre hay fallback.

<br>

### 🐛 Fixed

| Icono | Fix | Descripción | Archivo |
|:---:|:---|:---|:---|
| 🌍 | **Traducción multi-idioma** | MyMemory `Auto\|target` a veces devolvía el mismo texto cuando la auto-detección fallaba. Ahora se reintenta con el idioma **detectado explícitamente** (`detectedLang\|targetLang`) como fallback. | `TranslationService.kt` |
| 💻 | **Code Editor crash** | `executeCode()` usaba `ProcessBuilder("python3")` (no existe en Android) y creaba `WebView` desde `ViewModel`. Reescrito con mensajes informativos seguros para cada lenguaje. | `CodeEditorViewModel.kt` |
| 💻 | **Terminal inaccesible** | `RealTerminalEmulator.init()` llamaba `Shell.Builder.create().build()` sin try-catch. Si `libsu` no puede crear shell, ahora hay **fallback a modo simulado** con mensajes de ayuda. | `RealTerminalEmulator.kt` |
| 🧅 | **Tor Browser pantalla en blanco** | `LaunchedEffect` complejo bloqueaba la carga de URL si la detección de Orbot fallaba. Refactorizado: **URL se carga inmediatamente**, proxy async en background. | `TorBrowserScreenNew.kt` |

<br>

### 🔧 Changed

| Icono | Cambio | Descripción |
|:---:|:---|:---|
| 🛡️ | **Graceful degradation general** | Todos los módulos críticos ahora tienen fallback informativo en lugar de crashear |
| 📝 | **README actualizado** | Sección "Novedades Recientes" agregada con descripción de fixes v3.0.1 |

<br>

---

<br>

<div align="center">

<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&size=24&duration=2000&pause=500&color=4285F4&center=true&vCenter=true&width=600&lines=🚀+v3.0.0+—+Major+Release;2025-06-10" alt="v3.0.0" />

</div>

<br>

> 🎉 **Release más grande hasta la fecha**. Completa funcionalidad de 5 módulos clave + E2EE + WebRTC TURN + UX overhaul.

<br>

### ✨ Added

| Icono | Feature | Descripción |
|:---:|:---|:---|
| 🌍 | **Traductor de Mensajes** | MyMemory API con auto-detección. 12 idiomas: ES, EN, FR, DE, PT, IT, JA, ZH, KO, RU, AR, TR. Preferencia persistente en DataStore. |
| 🔐 | **Privacy & Security Settings** | Pantalla completa con: App Lock (PIN/biometría), E2EE toggle, bloqueo de capturas, notificaciones silenciosas, auto-borrado. |
| 👤 | **Account Settings** | Cambiar contraseña, eliminar cuenta, sesiones activas, exportar datos. |
| 👥 | **Contactos / Nueva Conversación** | Búsqueda de usuarios, QR scanner, lista de contactos, iniciar chat desde perfil. |
| 💻 | **Code Editor** | Editor con syntax highlighting, lista de archivos, ejecución de scripts (Python/JS/Bash/Kotlin). |
| 💻 | **Terminal** | Terminal interactiva usando `libsu` con Sora Editor para syntax highlighting. |
| 🧅 | **Navegador Tor** | WebView con proxy Orbot (HTTP 8118 + SOCKS5 9050). Soporte .onion, detección automática de Orbot. |
| 🔐 | **E2EE** | Cifrado end-to-end con ECDH + AES-256-GCM en `RealtimeDatabaseRepository`. |
| 📡 | **WebRTC TURN** | Servidores TURN de OpenRelay agregados para NAT estricto. |
| 🔗 | **Deep Links** | `nexuschat://chat/{id}` y `nexuschat://profile/{id}` con `IntentFilter` en `AndroidManifest.xml`. |
| ✨ | **UX Polish** | Skeleton shimmer, pull-to-refresh, swipe-to-delete, haptic feedback, mejor keyboard UX. |

<br>

### 🔧 Changed

| Icono | Cambio | Descripción |
|:---:|:---|:---|
| 🎨 | **Offline-first** | `PendingMessageEntity` + `PendingMessageDao` + `SendPendingMessagesWorker` para cola offline. |
| 🎨 | **Translation Settings** | `TranslationLanguageScreen` + `UserPreferences.translationLanguage` con selector de idioma. |

<br>

---

<br>

<div align="center">

<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&size=24&duration=2000&pause=500&color=FFD700&center=true&vCenter=true&width=600&lines=🔧+v2.2.0+—+UX+Improvements;2025-05-20" alt="v2.2.0" />

</div>

<br>

### ✨ Added

| Icono | Feature | Descripción |
|:---:|:---|:---|
| 🎨 | **Dynamic Themes** | 25 colores de acento + Material 3 + cambio en tiempo real. |
| 🎨 | **Custom Backgrounds** | Color sólido, gradiente animado, video de fondo. |
| 🌙 | **Dark Mode** | Modo oscuro optimizado con Material 3 dynamic colors. |
| 📞 | **Call Controls** | Mute, video on/off, cambio de cámara durante llamadas WebRTC. |

<br>

### 🔧 Changed

| Icono | Cambio | Descripción |
|:---:|:---|:---|
| 🎨 | **Stories Editor** | Overlays en tiempo real, stickers arrastrables, texto con fuentes personalizables. |

<br>

---

<br>

<div align="center">

<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&size=24&duration=2000&pause=500&color=FF5252&center=true&vCenter=true&width=600&lines=⚡+v2.1.0+—+AI+Integration;2025-05-01" alt="v2.1.0" />

</div>

<br>

### ✨ Added

| Icono | Feature | Descripción |
|:---:|:---|:---|
| 🤖 | **Azel AI** | Asistente integrado con Gemini (Google Generative AI). |
| 🔐 | **API Key Encryption** | AES-256 en EncryptedSharedPreferences para la key de Gemini. |
| 📡 | **SSE Streaming** | Server-Sent Events para respuestas en tiempo real. |
| ⏱️ | **Rate Limiter** | Cola con backoff exponencial y rate limiting inteligente. |
| 📝 | **Context Management** | Gestión de últimos 8 mensajes para contexto de conversación. |
| 🏠 | **Ollama Support** | Configuración para modelo local vía Ollama. |

<br>

---

<br>

<div align="center">

<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&size=24&duration=2000&pause=500&color=9C27B0&center=true&vCenter=true&width=600&lines=🎉+v2.0.0+—+WebRTC+%26+Stories;2025-04-10" alt="v2.0.0" />

</div>

<br>

### ✨ Added

| Icono | Feature | Descripción |
|:---:|:---|:---|
| 📞 | **WebRTC P2P Calls** | Llamadas de audio y video en alta calidad (1280x720 @ 30fps). |
| 📡 | **Firebase Signaling** | Señalización de llamadas vía Realtime Database. |
| 🔔 | **FCM Call Notifications** | Notificaciones push para llamadas entrantes. |
| 📸 | **Stories Multimedia** | Editor 24h con Canvas + Media3 Transformer. |
| 🎬 | **Media3 Transformer** | Renderizado de videos con overlays y efectos. |

<br>

---

<br>

<div align="center">

<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&size=24&duration=2000&pause=500&color=00E676&center=true&vCenter=true&width=600&lines=🌱+v1.0.0+—+Initial+Release;2025-03-15" alt="v1.0.0" />

</div>

<br>

### ✨ Added

| Icono | Feature | Descripción |
|:---:|:---|:---|
| 💬 | **Chat en Tiempo Real** | Firebase Realtime Database con mensajes de texto. |
| 🔥 | **Firebase Auth** | Email/password + Google Sign-In. |
| 🖼️ | **Media Sharing** | Imágenes, videos, audio, documentos vía Firebase Storage. |
| 🎨 | **Material 3 UI** | Jetpack Compose con Material 3 Design System. |
| 🏗️ | **Clean Architecture** | MVVM + Use Cases + Repositories + Hilt DI. |
| 🎨 | **Custom Themes** | Sistema de temas con colores personalizables. |

<br>

---

<br>

<div align="center">

### 🏷️ Leyenda de Cambios

| Icono | Tipo | Descripción | Color |
|:---:|:---:|:---|:---:|
| ✨ | **Added** | Nueva feature | 🟢 |
| 🐛 | **Fixed** | Bug corregido | 🔴 |
| 🔧 | **Changed** | Mejora o cambio | 🟡 |
| ⚡ | **Performance** | Optimización | 🔵 |
| 🔐 | **Security** | Mejora de seguridad | 🟣 |
| 🗑️ | **Deprecated** | Feature obsoleta | 🟤 |
| 🚫 | **Removed** | Feature eliminada | ⚫ |

</div>

<br>

---

<br>

<div align="center">

*Changelog mantenido por **Azel Mods** · Seguimos el formato [Keep a Changelog](https://keepachangelog.com/)*

<br>

<a href="https://github.com/Azelmods677/NexusChat">
  <img src="https://img.shields.io/github/stars/Azelmods677/NexusChat?style=social&logo=github" alt="GitHub Stars" />
</a>

<br><br>

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=2,22,28,40&height=100&section=footer&text=Azel%20Mods&fontSize=40&fontColor=fff&animation=twinkling&fontAlignY=65" />

</div>
