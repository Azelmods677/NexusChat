# Guía de Instalación — NexusChat

Esta guía explica cómo compilar y ejecutar NexusChat localmente.

## Requisitos

- Android Studio (Ladybug o superior)
- JDK 17
- Android SDK con API 31–36
- Una cuenta de Firebase

## 1. Clonar el repositorio

```bash
git clone https://github.com/AzelMods677/NexusChat.git
cd NexusChat
```

## 2. Configurar Firebase

1. Crea un proyecto en la [consola de Firebase](https://console.firebase.google.com/).
2. Añade una app Android con el package `com.Azelmods.App`.
3. Descarga `google-services.json` y colócalo en `app/google-services.json`.
4. Habilita en la consola:
   - **Authentication** (Email/Password y Google Sign-In)
   - **Realtime Database**
   - **Storage**

## 3. Configurar la API de IA (Gemini)

La función AzelAI usa la API de Google Gemini.

1. Obtén una API key gratuita en [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Coloca tu key donde el servicio de IA la consume (no la subas al repositorio público).

> La capa de rate-limiting y reintentos ya está incluida para el plan gratuito (15 RPM).

## 4. Compilar

```bash
./gradlew :app:compileDebugKotlin   # verificación rápida
./gradlew :app:assembleDebug        # genera el APK de debug
```

El APK queda en `app/build/outputs/apk/debug/`.

## 5. Ejecutar

Conecta un dispositivo (Android 12–16) o usa un emulador y ejecuta desde Android Studio, o:

```bash
./gradlew :app:installDebug
```

## Solución de problemas

- **El build falla por `google-services.json`:** asegúrate de que el archivo exista en `app/`.
- **La IA no responde:** verifica tu API key y la conexión a internet.
- **Errores de SDK:** instala las plataformas API 31–36 desde el SDK Manager.
