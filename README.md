# NexusChat

<div align="center">

![Android](https://img.shields.io/badge/Android-API%2026%2B-brightgreen?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.04.01-4285F4?logo=jetpackcompose)
![Firebase](https://img.shields.io/badge/Firebase-BOM%2033.7.0-FFCA28?logo=firebase)
![WebRTC](https://img.shields.io/badge/WebRTC-1.1.3-333333?logo=webrtc)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

**Aplicación de mensajería instantánea con IA integrada y navegación anónima**  
Construida con Kotlin y Jetpack Compose

[Características](#caracteristicas) | [Arquitectura](#arquitectura) | [Instalación](#instalacion) | [Seguridad](#seguridad-y-privacidad)

</div>

---

## Descripción

NexusChat es una aplicación de mensajería instantánea avanzada desarrollada nativamente para Android utilizando Kotlin y Jetpack Compose. Implementa Clean Architecture con el patrón MVVM para garantizar escalabilidad y mantenibilidad del código.

La aplicación combina funcionalidades completas de mensajería en tiempo real mediante Firebase Realtime Database, llamadas de voz y video con WebRTC, historias efímeras con duración de 24 horas, **asistente de IA integrado (AzelAI)**, **navegador Tor privado**, y un sistema de personalización que incluye temas, fondos personalizados y configuraciones de interfaz.

**Compatibilidad:** Android 12 (API 31) hasta Android 16 (API 36)

---

## Características

### Mensajería
- Chat en tiempo real con sincronización instantánea
- Mensajes de texto, voz, imágenes, videos y archivos
- Indicadores de estado: enviado, entregado, leído
- Indicador de escritura en tiempo real
- Respuestas rápidas deslizando mensajes
- Reacciones con emojis
- Chats grupales con gestión de miembros
- Búsqueda de mensajes y conversaciones
- Fijar, silenciar y archivar conversaciones

### Historias
- Publicación de fotos, videos y texto
- Caducidad automática a las 24 horas
- Visualización con barra de progreso
- Reacciones y respuestas directas
- Lista de visualizaciones con timestamps
- Editor con emojis y texto posicionable
- Ajuste bidimensional de imágenes

### Llamadas
- Llamadas de voz y videollamadas
- Comunicación P2P mediante WebRTC
- Señalización a través de Firebase
- Calidad adaptativa según conexión
- Controles: silenciar, altavoz, cambiar cámara

### 🤖 AzelAI - Asistente de IA Integrado
- **Múltiples proveedores de IA**: OpenAI, Anthropic, Google Gemini, Groq, Ollama local
- **Modelos avanzados**: GPT-4, Claude 3.5 Sonnet, Gemini 2.0 Flash, Llama 3.3, y más
- **Modos de conversación**:
  - Modo Normal: Conversaciones generales con filtros de contenido
  - Modo Agente: Ejecución de herramientas y comandos locales
- **Herramientas de agente**:
  - Ejecución de comandos shell
  - Lectura y escritura de archivos
  - Búsqueda web
  - Gestión del sistema
- **Personalización completa**:
  - Configuración de temperatura, top_p, max_tokens
  - System prompts personalizados
  - Historial de conversaciones
  - Exportación de chats
- **Streaming en tiempo real**: Respuestas progresivas para mejor experiencia

### 🔒 Seguridad y Privacidad

#### Navegador Tor Privado
- **Integración con Orbot**: Navegación anónima a través de la red Tor
- **Soporte para sitios .onion**: Acceso a la dark web
- **Modo dual**: Navegación directa o a través de Tor
- **Detección automática**: Verifica y configura Orbot automáticamente
- **Proxy HTTP/SOCKS5**: Configuración transparente del proxy
- **Manejo de errores**: Páginas de error personalizadas y sugerencias contextuales
- **DuckDuckGo integrado**: Motor de búsqueda privado por defecto

#### Herramientas de Seguridad
- **Backup y Restauración Encriptada**:
  - Encriptación AES-256-GCM
  - Backup de mensajes, configuraciones y preferencias
  - Protección con contraseña
  - Exportación/importación segura
- **Terminal Emulator**: Acceso a shell local para usuarios avanzados
- **Gestión de permisos**: Control granular de permisos de la app

### Personalización
- 15 temas de color predefinidos
- Fondos personalizados (imagen, video, color, degradado)
- Fondos independientes por conversación
- Configuración de tamaños de fuente
- Modo oscuro
- Navegación por gestos

### Notificaciones
- Notificaciones push con Firebase Cloud Messaging
- Agrupación por conversación
- Respuesta rápida desde notificaciones
- Marcar como leído sin abrir la app

---

## Arquitectura

NexusChat implementa **Clean Architecture** dividida en tres capas principales con separación clara de responsabilidades.

### Diagrama de Arquitectura General

```mermaid
graph TB
    subgraph "Capa de Presentacion"
        UI[Jetpack Compose UI<br/>Screens & Components]
        VM[ViewModels<br/>State Management]
        NAV[Navigation<br/>NavGraph & Routes]
    end
    
    subgraph "Capa de Dominio"
        UC[Use Cases<br/>Business Logic]
        REPO_INT[Repository Interfaces<br/>Contracts]
        MODELS[Domain Models<br/>Entities]
    end
    
    subgraph "Capa de Datos"
        REPO[Repository Implementations<br/>Data Operations]
        DS_REMOTE[Remote DataSource<br/>Firebase]
        DS_LOCAL[Local DataSource<br/>Room & DataStore]
    end
    
    subgraph "Fuentes de Datos"
        FB_DB[(Firebase<br/>Realtime DB)]
        FB_STORAGE[(Firebase<br/>Storage)]
        FB_AUTH[Firebase<br/>Auth]
        ROOM[(Room<br/>Database)]
        DATASTORE[(DataStore<br/>Preferences)]
    end
    
    UI --> VM
    VM --> UC
    UC --> REPO_INT
    REPO_INT --> REPO
    REPO --> DS_REMOTE
    REPO --> DS_LOCAL
    DS_REMOTE --> FB_DB
    DS_REMOTE --> FB_STORAGE
    DS_REMOTE --> FB_AUTH
    DS_LOCAL --> ROOM
    DS_LOCAL --> DATASTORE
    
    classDef presentacion fill:#1e88e5,stroke:#0d47a1,stroke-width:2px,color:#fff
    classDef dominio fill:#43a047,stroke:#2e7d32,stroke-width:2px,color:#fff
    classDef datos fill:#fb8c00,stroke:#e65100,stroke-width:2px,color:#fff
    classDef fuentes fill:#7b1fa2,stroke:#4a148c,stroke-width:2px,color:#fff
    
    class UI,VM,NAV presentacion
    class UC,REPO_INT,MODELS dominio
    class REPO,DS_REMOTE,DS_LOCAL datos
    class FB_DB,FB_STORAGE,FB_AUTH,ROOM,DATASTORE fuentes
```

### Capas de la Arquitectura

#### 1. Capa de Presentacion
- **Jetpack Compose**: Interfaz de usuario declarativa
- **ViewModels**: Gestion de estado con StateFlow
- **Navigation Component**: Navegacion entre pantallas
- **Theme System**: Sistema de temas y estilos

#### 2. Capa de Dominio
- **Use Cases**: Logica de negocio encapsulada
- **Repository Interfaces**: Contratos de acceso a datos
- **Domain Models**: Entidades del dominio

#### 3. Capa de Datos
- **Repositories**: Implementacion de acceso a datos
- **Remote DataSource**: Integracion con Firebase
- **Local DataSource**: Cache local con Room y DataStore

---

## Flujos de la Aplicacion

### Flujo de Autenticacion

```mermaid
flowchart TD
    START([Iniciar App]) --> CHECK{Usuario<br/>autenticado?}
    
    CHECK -->|Si| LOAD_USER[Cargar datos<br/>del usuario]
    CHECK -->|No| LOGIN[LoginScreen]
    
    LOAD_USER --> HOME([HomeScreen])
    
    LOGIN --> METHOD{Metodo de acceso}
    
    METHOD -->|Email| EMAIL_FORM[Formulario Email]
    METHOD -->|Google| GOOGLE_FLOW[Google Sign-In Flow]
    
    EMAIL_FORM --> VALIDATE{Validar<br/>credenciales}
    VALIDATE -->|Error| ERROR1[Mostrar error]
    ERROR1 --> EMAIL_FORM
    VALIDATE -->|OK| FB_AUTH[Firebase Auth]
    
    GOOGLE_FLOW --> GOOGLE_API[Google API]
    GOOGLE_API --> VALIDATE_GOOGLE{Token<br/>valido?}
    VALIDATE_GOOGLE -->|Error| ERROR2[Mostrar error]
    ERROR2 --> LOGIN
    VALIDATE_GOOGLE -->|OK| FB_AUTH
    
    FB_AUTH --> CREATE_USER{Usuario<br/>existe?}
    CREATE_USER -->|No| CREATE[Crear perfil<br/>en Realtime DB]
    CREATE_USER -->|Si| LOAD_PROFILE[Cargar perfil]
    
    CREATE --> SAVE_LOCAL[Guardar en<br/>DataStore]
    LOAD_PROFILE --> SAVE_LOCAL
    SAVE_LOCAL --> HOME
    
    classDef success fill:#43a047,stroke:#2e7d32,stroke-width:2px,color:#fff
    classDef error fill:#e53935,stroke:#c62828,stroke-width:2px,color:#fff
    classDef process fill:#1e88e5,stroke:#0d47a1,stroke-width:2px,color:#fff
    classDef decision fill:#fb8c00,stroke:#e65100,stroke-width:2px,color:#fff
    classDef firebase fill:#ff6f00,stroke:#e65100,stroke-width:2px,color:#fff
    
    class START,HOME success
    class ERROR1,ERROR2 error
    class LOAD_USER,EMAIL_FORM,GOOGLE_FLOW,GOOGLE_API,CREATE,LOAD_PROFILE,SAVE_LOCAL process
    class CHECK,METHOD,VALIDATE,VALIDATE_GOOGLE,CREATE_USER decision
    class FB_AUTH firebase
```

### Flujo de Mensajeria en Tiempo Real

```mermaid
sequenceDiagram
    autonumber
    actor Usuario
    participant UI as ChatScreen
    participant VM as ChatViewModel
    participant UC as SendMessageUseCase
    participant REPO as ChatRepository
    participant FB as Firebase Realtime DB
    participant FCM as Cloud Messaging

    rect rgb(30, 136, 229)
    Note over Usuario,UI: Envio de Mensaje
    Usuario->>+UI: Escribe y envia mensaje
    UI->>+VM: sendMessage(text, chatId)
    VM->>+UC: execute(message)
    UC->>+REPO: sendMessage(message)
    REPO->>+FB: push("/chats/{chatId}/messages")
    FB-->>-REPO: onSuccess
    FB->>FCM: Trigger notification
    FCM-->>Usuario: Push notification al receptor
    end
    
    rect rgb(67, 160, 71)
    Note over FB,UI: Sincronizacion en Tiempo Real
    FB-->>REPO: onDataChange (listener activo)
    REPO-->>-VM: Flow<List<Message>>
    VM-->>-UI: StateFlow actualiza
    UI-->>-Usuario: Mensaje visible en UI
    end
    
    Note right of Usuario: Todos los dispositivos<br/>sincronizados
```

### Flujo de Llamadas WebRTC

```mermaid
sequenceDiagram
    autonumber
    actor Usuario A
    participant UI_A as UI A
    participant VM_A as CallViewModel A
    participant FB as Firebase Signaling
    participant VM_B as CallViewModel B
    participant UI_B as UI B
    actor Usuario B
    
    rect rgb(30, 136, 229)
    Note over Usuario A,FB: Iniciar Llamada
    Usuario A->>+UI_A: Iniciar llamada
    UI_A->>+VM_A: startCall(userId)
    VM_A->>VM_A: Crear PeerConnection
    VM_A->>VM_A: Generar Offer SDP
    VM_A->>+FB: Enviar offer a /calls/{callId}
    VM_A->>FB: Enviar ICE candidates
    end
    
    rect rgb(251, 140, 0)
    Note over FB,Usuario B: Recibir Llamada
    FB-->>+VM_B: onCallReceived (listener)
    VM_B-->>+UI_B: Mostrar pantalla de llamada entrante
    UI_B-->>Usuario B: Notificacion de llamada
    
    Usuario B->>UI_B: Aceptar llamada
    UI_B->>VM_B: acceptCall()
    VM_B->>VM_B: Crear PeerConnection
    VM_B->>VM_B: Generar Answer SDP
    VM_B->>FB: Enviar answer a /calls/{callId}
    VM_B->>FB: Enviar ICE candidates
    end
    
    rect rgb(67, 160, 71)
    Note over VM_A,VM_B: Conexion P2P Establecida
    FB-->>VM_A: onAnswerReceived
    VM_A->>VM_A: setRemoteDescription(answer)
    VM_A->>VM_B: Stream de audio/video directo
    VM_B->>VM_A: Stream de audio/video directo
    end
    
    Note over Usuario A,Usuario B: Durante la llamada...
    
    rect rgb(229, 57, 53)
    Note over Usuario A,UI_B: Finalizar Llamada
    Usuario A->>UI_A: Colgar
    UI_A->>VM_A: endCall()
    VM_A->>FB: Actualizar estado a "ended"
    VM_A->>VM_A: Cerrar PeerConnection
    FB-->>VM_B: onCallEnded
    VM_B->>VM_B: Cerrar PeerConnection
    VM_B-->>-UI_B: Volver a HomeScreen
    deactivate VM_A
    deactivate UI_A
    deactivate FB
    end
```

### Flujo de Historias

```mermaid
flowchart TD
    START([Usuario crea historia]) --> TYPE_SELECT{Tipo de historia}
    
    TYPE_SELECT -->|Foto| SELECT_IMAGE[Seleccionar imagen]
    TYPE_SELECT -->|Video| SELECT_VIDEO[Seleccionar video]
    TYPE_SELECT -->|Texto| TEXT_STORY[Historia de texto<br/>Fondo negro]
    
    SELECT_IMAGE --> PREVIEW[Vista previa]
    SELECT_VIDEO --> PREVIEW
    TEXT_STORY --> TEXT_INPUT[Agregar texto posicionable]
    
    TEXT_INPUT --> PREVIEW
    PREVIEW --> EDIT{Editar?}
    
    EDIT -->|Si| TOOLS[Herramientas de edicion]
    EDIT -->|No| CONFIRM
    
    TOOLS --> TEXT_TOOL[Texto posicionable]
    TOOLS --> EMOJI_TOOL[Emojis posicionables<br/>4 categorias]
    TOOLS --> ADJUST_TOOL[Ajustar foto X+Y]
    
    TEXT_TOOL --> CONFIRM{Publicar}
    EMOJI_TOOL --> CONFIRM
    ADJUST_TOOL --> CONFIRM
    
    CONFIRM -->|Cancelar| START
    CONFIRM -->|Publicar| UPLOAD[Subir a<br/>Firebase Storage]
    
    UPLOAD --> GET_URL[Obtener URL]
    GET_URL --> CREATE_STORY[Crear documento<br/>en Realtime DB]
    
    CREATE_STORY --> SET_EXPIRY[Configurar<br/>expiracion 24h]
    SET_EXPIRY --> NOTIFY[Notificar<br/>a seguidores]
    
    NOTIFY --> PUBLISHED([Historia publicada])
    
    PUBLISHED --> WAIT[Esperar 24 horas]
    WAIT --> AUTO_DELETE[Eliminacion automatica]
    
    AUTO_DELETE --> DELETE_STORAGE[Eliminar de Storage]
    DELETE_STORAGE --> DELETE_DB[Eliminar de Realtime DB]
    DELETE_DB --> END([Historia eliminada])
    
    classDef success fill:#43a047,stroke:#2e7d32,stroke-width:2px,color:#fff
    classDef process fill:#1e88e5,stroke:#0d47a1,stroke-width:2px,color:#fff
    classDef decision fill:#fb8c00,stroke:#e65100,stroke-width:2px,color:#fff
    classDef firebase fill:#ff6f00,stroke:#e65100,stroke-width:2px,color:#fff
    classDef delete fill:#757575,stroke:#424242,stroke-width:2px,color:#fff
    
    class START,PUBLISHED success
    class END delete
    class SELECT_IMAGE,SELECT_VIDEO,TEXT_STORY,PREVIEW,TOOLS,GET_URL,CREATE_STORY,SET_EXPIRY,NOTIFY,WAIT process
    class TYPE_SELECT,EDIT,CONFIRM decision
    class UPLOAD,DELETE_STORAGE,DELETE_DB,AUTO_DELETE firebase
```

### Flujo de Personalizacion de Fondos

```mermaid
flowchart TD
    START([Usuario abre<br/>BackgroundPicker]) --> SCOPE{Ambito}
    
    SCOPE -->|Global| GLOBAL[Fondo para toda la app]
    SCOPE -->|Chat| CHAT[Fondo para chat especifico]
    
    GLOBAL --> TYPE{Tipo de fondo}
    CHAT --> TYPE
    
    TYPE -->|Color| COLOR[ColorPicker]
    TYPE -->|Degradado| GRADIENT[GradientPicker]
    TYPE -->|Imagen| IMAGE[Selector de galeria]
    TYPE -->|Video| VIDEO[Selector de video]
    TYPE -->|Ninguno| NONE[Eliminar fondo]
    
    COLOR --> PREVIEW[Vista previa]
    GRADIENT --> PREVIEW
    IMAGE --> CROP[Recortar imagen]
    VIDEO --> VALIDATE{Validar<br/>tamano y formato}
    NONE --> SAVE
    
    CROP --> PREVIEW
    VALIDATE -->|Error| ERROR[Mostrar error]
    ERROR --> VIDEO
    VALIDATE -->|OK| PREVIEW
    
    PREVIEW --> CONFIRM{Confirmar}
    CONFIRM -->|Cancelar| START
    CONFIRM -->|Guardar| SAVE[Guardar configuracion]
    
    SAVE --> STORAGE_CHECK{Archivo<br/>multimedia?}
    STORAGE_CHECK -->|Si| UPLOAD[Subir a<br/>Firebase Storage]
    STORAGE_CHECK -->|No| DATASTORE_SAVE
    
    UPLOAD --> GET_URL[Obtener URL]
    GET_URL --> DATASTORE_SAVE[Guardar en DataStore]
    
    DATASTORE_SAVE --> FIREBASE_SAVE{Fondo<br/>de chat?}
    FIREBASE_SAVE -->|Si| FB_UPDATE[Actualizar en<br/>Realtime DB]
    FIREBASE_SAVE -->|No| APPLY
    
    FB_UPDATE --> APPLY[Aplicar en tiempo real]
    APPLY --> END([Fondo actualizado])
    
    classDef success fill:#43a047,stroke:#2e7d32,stroke-width:2px,color:#fff
    classDef error fill:#e53935,stroke:#c62828,stroke-width:2px,color:#fff
    classDef process fill:#1e88e5,stroke:#0d47a1,stroke-width:2px,color:#fff
    classDef decision fill:#fb8c00,stroke:#e65100,stroke-width:2px,color:#fff
    classDef firebase fill:#ff6f00,stroke:#e65100,stroke-width:2px,color:#fff
    
    class START,END success
    class ERROR error
    class GLOBAL,CHAT,COLOR,GRADIENT,IMAGE,VIDEO,NONE,CROP,PREVIEW,SAVE,GET_URL,DATASTORE_SAVE,APPLY process
    class SCOPE,TYPE,VALIDATE,CONFIRM,STORAGE_CHECK,FIREBASE_SAVE decision
    class UPLOAD,FB_UPDATE firebase
```

---

## Stack Tecnologico

| Componente | Tecnologia | Version |
|-----------|-----------|---------|
| UI Framework | Jetpack Compose BOM | 2025.04.01 |
| Lenguaje | Kotlin | 100% |
| Arquitectura | Clean Architecture + MVVM | - |
| Base de datos | Firebase Realtime Database | BOM 33.7.0 |
| Almacenamiento | Firebase Storage | BOM 33.7.0 |
| Autenticacion | Firebase Auth | BOM 33.7.0 |
| Mensajeria push | Firebase Cloud Messaging | BOM 33.7.0 |
| IA | OpenAI, Anthropic, Google Gemini, Groq, Ollama | - |
| Navegación privada | Orbot/Tor | - |
| Encriptación | AES-256-GCM | - |
| Inyeccion de dependencias | Hilt | 2.52 |
| Carga de imagenes | Coil | 3.1.0 |
| Reproductor de video | ExoPlayer media3 | 1.3.1 |
| Llamadas | Stream WebRTC Android | 1.1.3 |
| HTTP Client | OkHttp + Retrofit | 4.12.0 / 2.11.0 |
| Cache local | Room | - |
| Preferencias | DataStore | - |
| Corrutinas | Kotlin Coroutines + Flow | 1.9.0 |
| SDK minimo | Android 12 | API 31 |
| SDK objetivo | Android 16 | API 36 |

---

## Estructura del Proyecto

```
app/src/main/java/com/Azelmods/App/
│
├── data/                           # Capa de Datos
│   ├── api/                        # Servicios API
│   │   └── AzelAIApiService.kt
│   ├── repository/                 # Implementaciones de repositorios
│   │   ├── ChatRepository.kt
│   │   ├── UserRepository.kt
│   │   ├── StoryRepository.kt
│   │   ├── CallRepository.kt
│   │   └── ChatBackgroundRepository.kt
│   ├── remote/                     # Fuentes de datos remotas
│   │   └── FirebaseDataSource.kt
│   ├── local/                      # Fuentes de datos locales
│   │   ├── RoomDatabase.kt
│   │   └── DataStoreManager.kt
│   ├── preferences/                # Preferencias persistentes
│   │   ├── ThemePreferences.kt
│   │   └── UserPreferences.kt
│   └── model/                      # Modelos de datos
│       ├── User.kt
│       ├── Message.kt
│       ├── Story.kt
│       └── Call.kt
│
├── domain/                         # Capa de Dominio
│   ├── repository/                 # Interfaces de repositorios
│   │   ├── IChatRepository.kt
│   │   ├── IUserRepository.kt
│   │   └── IStoryRepository.kt
│   └── usecase/                    # Casos de uso
│       ├── SendMessageUseCase.kt
│       ├── CreateStoryUseCase.kt
│       └── StartCallUseCase.kt
│
├── ui/                             # Capa de Presentacion
│   ├── screens/                    # Pantallas
│   │   ├── chat/
│   │   │   ├── ChatScreen.kt
│   │   │   └── ChatViewModel.kt
│   │   ├── stories/
│   │   │   ├── StoriesScreen.kt
│   │   │   └── StoriesViewModel.kt
│   │   ├── calls/
│   │   │   ├── CallsScreen.kt
│   │   │   └── ActiveCallScreen.kt
│   │   └── settings/
│   │       └── SettingsScreen.kt
│   ├── components/                 # Componentes reutilizables
│   │   ├── AppBackground.kt
│   │   └── VoiceRecorder.kt
│   ├── theme/                      # Sistema de temas
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   └── navigation/                 # Navegacion
│       ├── NavGraph.kt
│       └── Screen.kt
│
├── di/                             # Inyeccion de Dependencias
│   ├── AppModule.kt
│   └── RepositoryModule.kt
│
├── services/                       # Servicios Android
│   ├── CallService.kt
│   └── NexusFirebaseMessagingService.kt
│
└── MainActivity.kt
```

---

## Instalacion

### Requisitos Previos

- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17 o superior
- Android SDK API 36
- Cuenta de Firebase (gratuita)

### Clonar y Compilar

```bash
# Clonar el repositorio
git clone https://github.com/AzelMods677/NexusChat.git
cd NexusChat

# Compilar APK de depuracion
./gradlew assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug

# Compilar APK de lanzamiento
./gradlew assembleRelease
```

**Salida:** `app/build/outputs/apk/debug/app-debug.apk`

---

## Configuración de Orbot para Navegación Anónima

### Instalación de Orbot

1. **Descarga Orbot** desde una de estas fuentes:
   - [Google Play Store](https://play.google.com/store/apps/details?id=org.torproject.android)
   - [F-Droid](https://f-droid.org/packages/org.torproject.android)
   - [Sitio oficial](https://orbot.app)

2. **Configuración inicial**:
   - Abre Orbot
   - Presiona el botón "Iniciar" (ícono de cebolla)
   - Espera a que se conecte a la red Tor (puede tardar 1-2 minutos)
   - Verás "Conectado a la red Tor" cuando esté listo

3. **Uso en NexusChat**:
   - Abre NexusChat
   - Ve a **Seguridad** → **Navegador Tor**
   - La app detectará automáticamente si Orbot está activo
   - Podrás navegar por sitios .onion y navegar anónimamente

### Características del Navegador Tor

- **Detección automática**: La app verifica si Orbot está instalado y activo
- **Modo dual**: Navega directamente o a través de Tor según disponibilidad
- **Proxy transparente**: Configuración automática del proxy HTTP (8118) y SOCKS5 (9050)
- **Sitios .onion**: Acceso completo a servicios ocultos de Tor
- **Indicadores visuales**: Estado de conexión claramente visible
- **Manejo de errores**: Mensajes de ayuda cuando Tor no está disponible

---

## Configuración de Firebase

### Paso 1: Crear Proyecto

1. Accede a [Firebase Console](https://console.firebase.google.com/)
2. Crea un nuevo proyecto y agrega una aplicacion Android
3. Nombre del paquete: `com.Azelmods.App`
4. Descarga `google-services.json` y colocalo en el directorio `app/`

### Paso 2: Habilitar Servicios

- **Realtime Database**: Modo de prueba o produccion
- **Storage**: Modo de prueba o produccion
- **Authentication**: Email/Contrasena + Google Sign-In
- **Cloud Messaging**: Se habilita automaticamente

### Paso 3: Reglas de Base de Datos

```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null",
    "users": {
      "$uid": {
        ".read": "auth != null",
        ".write": "auth.uid === $uid"
      }
    },
    "chats": {
      "$chatId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    "stories": {
      ".read": "auth != null",
      "$storyId": {
        ".write": "auth != null && (!data.exists() || data.child('userId').val() === auth.uid)"
      }
    }
  }
}
```

### Paso 4: Reglas de Storage

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null
                   && request.resource.size < 10 * 1024 * 1024;
    }
  }
}
```

### Paso 5: Google Sign-In

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Agrega el SHA-1 resultante en **Firebase Console > Configuracion del proyecto > Huella digital**.

---

## Registro de Cambios

### v2.0.0 - Actualización Mayor (2025)

#### 🤖 AzelAI - Asistente de IA
- Integración de múltiples proveedores de IA (OpenAI, Anthropic, Google, Groq, Ollama)
- Modo Agente con herramientas locales (shell, archivos, búsqueda web)
- Streaming en tiempo real de respuestas
- Configuración avanzada de parámetros de IA
- Historial de conversaciones y exportación

#### 🔒 Seguridad y Privacidad
- Navegador Tor integrado con soporte para Orbot
- Acceso a sitios .onion
- Backup encriptado con AES-256-GCM
- Terminal emulator para usuarios avanzados
- Gestión avanzada de permisos

#### 🐛 Correcciones
- **CRÍTICO**: Corregidos enlaces rotos a guardianproject.info (404)
- Actualizados todos los enlaces de descarga de Orbot a fuentes oficiales
- Mejorada detección y configuración del proxy Tor
- Eliminada race condition en inicialización del navegador
- Manejo completo de errores HTTP (404, 500, etc.)

### v1.0.0 - Lanzamiento Inicial (2024)

#### Funcionalidades Principales
- Mensajeria en tiempo real con Firebase Realtime Database
- Historias con caducidad de 24 horas
- Llamadas de voz y video con WebRTC
- Autenticacion Firebase (Email + Google Sign-In)
- Notificaciones push con Firebase Cloud Messaging

#### Sistema de Personalizacion
- 15 temas de color predefinidos
- Fondos personalizados (imagen, video, color, degradado)
- Fondos independientes por conversacion
- Configuracion de tamanos de fuente
- Modo oscuro

#### Historias
- Selector de emojis con 4 categorias
- Emojis y texto posicionables
- Ajuste bidimensional de fotos
- Grid de 8 columnas para emojis

#### Llamadas
- Permisos actualizados para Android 12+
- Verificacion de permisos en tiempo real
- Comunicacion P2P con WebRTC

#### Mensajeria
- Envio de videos optimizado
- Mensajes de voz con visualizacion de forma de onda
- Soporte mejorado para teclado

#### Mejoras Tecnicas
- Arquitectura Clean con MVVM
- Material Design 3
- Visor de fotos con zoom
- Recorte de imagenes

---

## Roadmap

### Próximas Funciones

- Mejoras en el modo Agente de AzelAI
- Más herramientas locales para el agente
- Integración de más modelos de IA
- VPN integrada
- Mensajería encriptada end-to-end
- Gestion avanzada de grupos
- Creador de stickers personalizado
- Sistema de respaldo automático en la nube

---

## Contribuir

1. Haz fork del proyecto
2. Crea una rama: `git checkout -b feature/nueva-funcionalidad`
3. Realiza tus cambios y haz commit: `git commit -m 'feat: descripcion del cambio'`
4. Sube los cambios: `git push origin feature/nueva-funcionalidad`
5. Abre un Pull Request

### Guias de Contribucion

- Kotlin 100%, Jetpack Compose para toda la UI
- Seguir Clean Architecture en todos los cambios
- Documentar funciones publicas con KDoc
- Verificar compilacion sin errores antes de hacer commit

---

## Licencia

**MIT License** - Copyright (c) 2025 AzelMods677

Se concede permiso, de forma gratuita, a cualquier persona que obtenga una copia de este software para utilizarlo sin restriccion, incluyendo los derechos a usar, copiar, modificar, fusionar, publicar, distribuir, sublicenciar y/o vender copias, sujeto a que el aviso de copyright anterior se incluya en todas las copias.

EL SOFTWARE SE PROPORCIONA "TAL CUAL", SIN GARANTIA DE NINGUN TIPO.

---

## Contacto

- **YouTube:** [@AzelModsx677](https://youtube.com/@AzelModsx677)
- **TikTok:** [@azelmodsx677](https://tiktok.com/@azelmodsx677)
- **Telegram:** [@AzelModsx67779](https://t.me/AzelModsx67779)

---

<div align="center">

**Si te gusta el proyecto, dale una estrella en GitHub ⭐**

**Desarrollado por AzelMods677 - 2026**

</div>
