# NexusChat (Azelgram Messenger)

Aplicación de mensajería premium para Android construida con **Kotlin + Jetpack Compose** y **Firebase**, con temas personalizables, llamadas y videollamadas WebRTC, Stories con edición multimedia, navegador con soporte Tor (Orbot) y un asistente de IA basado en Gemini que usa la API key del propio usuario.

> **Paquete:** `com.Azelmods.App`

---

## ✨ Características

- **Mensajería en tiempo real** (Firebase Realtime Database) con arquitectura optimizada usando índices y mapas para acceso eficiente, multimedia (imagen, **video**, audio, documento, ubicación, contacto, stickers) y mensajes efímeros.
- **Llamadas y videollamadas** P2P con WebRTC (señalización vía Firebase + STUN).
- **Stories** con editor (texto, stickers, emojis arrastrables) que ahora se **renderizan en el archivo final** tanto en foto (Canvas/GraphicsLayer) como en video (Media3 Transformer).
- **Sistema de temas**: **25 acentos** curados que se aplican en toda la app en tiempo real vía `MaterialTheme.colorScheme`.
- **Privacidad**: navegador Tor con enrutado por Orbot (HTTP 8118 con *fallback* a SOCKS5 9050), pantalla de control y guía de Orbot.
- **Asistente de IA (AzelAI)**: integración con Gemini usando la **API key del usuario** (almacenada cifrada), selección de modelo, streaming, cola con *backoff* y *rate-limit*.
- **Demo mode**: Sistema de cuentas demo con datos de prueba para testing sin Firebase.

---

## 🏗️ Arquitectura

- **UI:** Jetpack Compose, Material 3, navegación con Navigation-Compose.
- **Patrón:** MVVM (ViewModels + `StateFlow`).
- **DI:** Hilt.
- **Backend:** Firebase Auth, Realtime Database, Storage, Cloud Messaging, Cloud Functions (`functions/`).
- **Multimedia:** Coil (imágenes), Media3/ExoPlayer (video) y Media3 Transformer (composición de overlays en Stories).
- **Tema dinámico:** `ui/theme/DynamicTheme.kt` (`AppTheme.ACCENT_SWATCHES`) es la fuente única de los 25 acentos; `NexusChatTheme` alimenta `MaterialTheme.colorScheme.primary` desde las preferencias del usuario.

---

## 🚀 Puesta en marcha

### Requisitos
- Android Studio (Ladybug o superior)
- JDK 17
- Un proyecto de Firebase

### 1. Clonar
```bash
git clone https://github.com/AzelMods677/NexusChat.git
cd NexusChat
```

### 2. Firebase
Coloca tu `google-services.json` en `app/`. Habilita en la consola de Firebase: Authentication, Realtime Database, Storage y Cloud Messaging.

#### Reglas de Realtime Database
El proyecto incluye `database.rules.json` con reglas optimizadas:

- **Estructura basada en mapas**: `members` y `participants` usan `{uid: true}` en lugar de listas, permitiendo validación eficiente con `.child(auth.uid).exists()`
- **Índice `userChats`**: Acceso O(1) a los chats del usuario en lugar de escanear toda la base de datos
- **Permisos granulares**: Validación de membresía, autoría de mensajes y protección de datos de usuario

Despliega las reglas desde la consola de Firebase o con Firebase CLI:
```bash
firebase deploy --only database
```

### 3. API key de la IA (Gemini)
La app **no** trae ninguna API key embebida. Hay dos formas de configurarla:

- **Recomendado (en la app):** Ajustes › IA › *API Key de Gemini* → pega tu key. Se guarda **cifrada** (EncryptedSharedPreferences) en el dispositivo.
- **Para desarrollo:** añade en `local.properties` (no se versiona):
  ```properties
  GEMINI_API_KEY=tu_api_key_aqui
  ```
  Se expone como `BuildConfig.GEMINI_API_KEY` y se usa como *fallback* si el usuario no configuró una.

### 4. Compilar
```bash
./gradlew assembleDebug
```

---

## 🔐 Privacidad / Tor (Orbot)
El navegador integrado enruta el tráfico a través de Orbot:
1. Instala **Orbot** (`org.torproject.android`) y pulsa *Iniciar*.
2. La app detecta el proxy HTTP (`127.0.0.1:8118`) y, si no está disponible, usa **SOCKS5** (`127.0.0.1:9050`).
3. Los enlaces `.onion` se permiten cuando Tor está activo; el proxy se limpia al salir del navegador.

> Nota: WebView enruta de forma fiable por el proxy **HTTP** de Orbot. Para conectividad robusta en redes con NAT estricto en llamadas WebRTC se recomienda un servidor **TURN** (la app solo configura STUN).

---

## 🤖 Asistente de IA
- Usa la **API key del usuario** (o `BuildConfig.GEMINI_API_KEY` en dev).
- Selección de modelo, respuestas en *streaming*, cola de peticiones con *backoff* exponencial y *rate-limiter* para el free tier.
- El asistente opera con un *prompt* de sistema neutro y profesional y respeta las políticas de seguridad del proveedor (Gemini).

---

## 🔒 Seguridad y secretos
- **No** subas tu API key de Gemini al repositorio: va en `local.properties` (ya en `.gitignore`) o se introduce en la app.
- `google-services.json` es configuración de cliente de Firebase; protégela con reglas de seguridad y App Check.
- `local.properties`, `*.keystore`, `*.jks` y la carpeta `.kiro/` están excluidos del control de versiones.

---

## 📂 Estructura (resumen)
```
app/src/main/java/com/Azelmods/App/
├── data/            # repositorios, API, IA (AiKeyStore, GeminiRequestQueue), seguridad (Tor)
├── ui/
│   ├── screens/     # chat, calls, stories, security, settings, azelai, ...
│   ├── components/  # MessageBubble, AttachmentBottomSheet, ...
│   └── theme/       # DynamicTheme (25 acentos), Theme, Color
├── webrtc/          # WebRTCManager (llamadas/videollamadas)
└── services/        # CallService, FCM
functions/           # Cloud Functions (push de llamada entrante, etc.)
```

---

## 📝 Licencia
Proyecto privado. Todos los derechos reservados salvo indicación contraria del propietario.
