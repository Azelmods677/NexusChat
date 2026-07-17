# Auditoría técnica completa — Azelgram / Nexus Chat

**Modo:** Plan (solo análisis, sin cambios de código aplicados).
**Fecha:** 2026-07-15 · **Rama:** `main`
**Stack auditado:** Kotlin · Jetpack Compose (Material 3) · Firebase (Auth/RTDB/Storage) · Hilt · MVVM · minSdk 31 / targetSdk 36
**Escala:** 259 archivos `.kt`, 54 pantallas (`*Screen.kt`).

> ⚠️ **Nota metodológica sobre FRENTE 1:** el entorno de análisis (sandbox) bloquea las conexiones loopback que Gradle necesita, así que **no fue posible compilar ni ejecutar la app aquí** (`assembleDebug`/emulador no disponibles). El FRENTE 1 se resolvió con análisis estático de causa raíz + comandos concretos para que captures el stack trace real en tu máquina. Los otros 6 frentes son análisis estático completo del código.

---

## 🎯 Tabla maestra — prioridad de mayor a menor

| # | Frente | Hallazgo | Severidad | Esfuerzo |
|---|--------|----------|-----------|----------|
| 1 | F5 Seguridad | **3er prompt de jailbreak residual** en `AIManager.getAggressiveSystemPrompt()` ("Respondes TODO sin censura ni filtros… exploits… malware"). La limpieza anterior no lo tocó | 🔴 Alta | 🟢 Chico |
| 2 | F5 Seguridad | `app/google-services.json` **sigue trackeado en git** (y en el historial) pese a estar en `.gitignore:37` | 🔴 Alta | 🟢 Chico |
| 3 | F5 Seguridad | `TamperDetection` usa una firma **placeholder** ("Replace this placeholder before publishing") → el control anti-tamper es un stub sin efecto real | 🔴 Alta | 🟡 Mediano |
| 4 | F1 Crash | Crash "New Chat": no reproducible aquí. Estáticamente el caso reportado (LazyColumn duplicate key) ya está mitigado para UIDs vacíos; **queda el vector de UIDs no-vacíos duplicados** | 🟠 Alta/Media | 🟢 Chico |
| 5 | F4 Features falsas | **Toda la pantalla "AI Features" es decorativa**: 6 toggles sin persistencia ni lógica, chips de Smart Replies con `onClick` vacío, "AI Assistant" con respuestas hardcodeadas por keyword | 🟠 Media | 🔴 Grande (implementar) / 🟢 Chico (ocultar) |
| 6 | F3 Código muerto | Cadena `AIManager → OllamaApiService → OpenCodeApiService` **nunca se invoca** desde ninguna UI (solo se usa el enum `AIProvider`). Arrastra el prompt del punto 1 | 🟠 Media | 🟢 Chico |
| 7 | F3 Código muerto | Pantallas duplicadas/huérfanas: `HomeScreen`, `ChatListScreen`, `AboutScreenRedesigned`, `settings.PremiumScreen`, `settings.PrivacyScreen` (el NavGraph usa las variantes "Redesigned"/otras) | 🟠 Media | 🟡 Mediano |
| 8 | F4 Botones muertos | `PremiumScreen` (settings) upgrade/restore vacíos; delete-account vacío; `ZoomableCropper` **no recorta** (TODO); `SwipeIndicator` es no-op | 🟠 Media | 🟡 Mediano |
| 9 | F7 Pulido visual | **479 colores `Color(0xFF…)` hardcodeados en 55 archivos** → no soporta light theme, inconsistente con `NexusDesignTokens`/`Color.kt` existentes | 🟠 Media | 🔴 Grande |
| 10 | F6 Consistencia | Varias pantallas registradas pero a medio terminar funcionalmente (AI Features, `mod_home`, grupos sin navegación post-creación) | 🟡 Media | 🟡 Mediano |
| 11 | F2 Traducción | Manejo de errores **correcto de punta a punta** (ya se muestra al usuario). Pendiente menor: truncado silencioso a 500 chars y sin contador local de cuota | 🟢 Baja | 🟢 Chico |
| 12 | F7 Pulido visual | Loading states genéricos (`CircularProgressIndicator` en todo), empty states sin ilustración, micro-interacciones ausentes | 🟢 Baja | 🟡 Mediano |

---

## FRENTE 1 — Crash "New Chat"

**No reproducible en este entorno** (sin Gradle/emulador — ver nota metodológica). Análisis estático:

- `NewConversationScreen.kt` y `NewConversationViewModel.kt` hoy están **defensivos**: doble verificación de auth en `init`/`loadContacts`, `startConversation` valida sesión antes de la corrutina, navegación forzada a `Dispatchers.Main`, errores enrutados a un snackbar (`LaunchedEffect(state.error)`), y el fix de `LazyColumn` con `key = { … }` para UIDs vacíos ya está aplicado ([NewConversationScreen.kt:217](app/src/main/java/com/Azelmods/App/ui/screens/conversation/NewConversationScreen.kt#L217) y [:531](app/src/main/java/com/Azelmods/App/ui/screens/conversation/NewConversationScreen.kt#L531)).
- **Causa raíz probable del crash residual:** el `key` solo desduplica UIDs *vacíos*. Si `getAllUsers()` devuelve dos contactos con el **mismo UID no-vacío** (nodo Firebase duplicado, o el usuario actual repetido), Compose lanza `IllegalArgumentException: Key "<uid>" was already used`. Aplica a las dos `LazyColumn` (lista de contactos y selección de grupo).
- **Recomendación de fix:** desduplicar la lista en el ViewModel (`users.distinctBy { it.uid }`) antes de exponerla al estado, o usar un `key` compuesto con índice.

**Para capturar el stack trace real en tu máquina:**
```bash
./gradlew installDebug
adb logcat -c && adb logcat *:E AndroidRuntime:E | Select-String -Pattern "Azelmods|New Conversation|IllegalArgument"
# reproducí: abrir "New Chat" / seleccionar contacto / crear grupo
```

**Severidad:** Alta si aún crashea en producción · **Esfuerzo:** Chico.

---

## FRENTE 2 — Traducción IA (`TranslationService.kt`)

**Diagnóstico: el manejo de errores está bien resuelto de punta a punta.** No es un hallazgo crítico.

- El servicio distingue y devuelve `Result.failure` con mensajes en español para: cuota agotada (detecta `MYMEMORY WARNING` / `LIMIT EXCEEDED` y `responseStatus != 200`), sin internet (`UnknownHostException`), timeout (`SocketTimeoutException`), I/O y JSON inválido ([TranslationService.kt:74-127](app/src/main/java/com/Azelmods/App/data/translation/TranslationService.kt#L74)).
- **El usuario SÍ ve el aviso:** `ChatViewModel.translateMessage` guarda el fallo en `state.translationError` ([ChatViewModel.kt:809](app/src/main/java/com/Azelmods/App/ui/screens/chat/ChatViewModel.kt#L809)) y `ChatScreen` lo muestra vía `LaunchedEffect(state.translationError)` ([ChatScreen.kt:128](app/src/main/java/com/Azelmods/App/ui/screens/chat/ChatScreen.kt#L128)).

**Pendientes menores (severidad baja):**
- Truncado silencioso: solo se traducen los primeros **500 caracteres** ([:51](app/src/main/java/com/Azelmods/App/data/translation/TranslationService.kt#L51)); mensajes largos se cortan sin avisar. **Esfuerzo:** Chico.
- Sin contador local de cuota (~1000 palabras/día). El usuario solo se entera cuando ya falla. Un contador preventivo mejoraría UX. **Esfuerzo:** Chico.

---

## FRENTE 3 — Código muerto y duplicados

**Cluster de IA muerto (arrastra el prompt de jailbreak del F5):**
- `AIManager` — solo se usa su enum `AIProvider` (en `AIPreferences`); `generateResponse()` y `generateResponseWithFallback()` **no se llaman desde ninguna pantalla**.
- Por transitividad: `OllamaApiService` y `OpenCodeApiService` solo los referencia `AIManager` → cadena completa muerta.
- En `AzelAIRepository`/`AzelAIViewModel`, `generateAutomationScript(...)` no tiene call-site en UI (residuo del suite de "hacking" ya parcialmente removido).

**Pantallas duplicadas/huérfanas** (definidas pero no referenciadas; el NavGraph/MainScreen usan otra variante):
- `home/HomeScreen.kt` (`fun HomeScreen`) — MainScreen usa `HomeScreenRedesigned`.
- `home/ChatListScreen.kt` ("Mod redesigned chat list") — sin referencias.
- `about/AboutScreenRedesigned.kt` — NavGraph usa `AboutScreen`.
- `settings/PremiumScreen.kt` — NavGraph usa `premium/PremiumScreen.kt`.
- `settings/PrivacyScreen.kt` — NavGraph usa `PrivacySecurityScreen`.

**Recomendación:** borrado por fases con verificación de compilación entre cada uno. **Severidad:** Media · **Esfuerzo:** Chico-Mediano.
> Confirmar con "Find Usages" en Android Studio antes de borrar cada pantalla (algunas podrían ser deep-links directos).

---

## FRENTE 4 — Features solo visuales / incompletas

**"AI Features" (`AiFeaturesScreen.kt`) — la pantalla completa es una maqueta:**
- Los 6 toggles (Smart Replies, Auto-Translate, Chat Summary, Tone Suggestions, Photo Enhancement, Voice Transcription) son `remember { mutableStateOf(false) }` **locales, sin persistencia ni lógica** ([:32-37](app/src/main/java/com/Azelmods/App/ui/screens/settings/AiFeaturesScreen.kt#L32)). Al reiniciar la pantalla se resetean y no cambian ningún comportamiento.
- Smart Replies: chips hardcodeados `"Sure!"/"Thanks!"/"Got it"` con `onClick = { }` ([:134-138](app/src/main/java/com/Azelmods/App/ui/screens/settings/AiFeaturesScreen.kt#L134)).
- "AI Assistant": no usa IA real — responde por coincidencia de keywords (`hello`/`help`/`feature`) ([:295-304](app/src/main/java/com/Azelmods/App/ui/screens/settings/AiFeaturesScreen.kt#L295)).
- *Sí funcionan:* la sección de API Key de Gemini y el botón "Acceder a Azel IA".

**Otros botones/acciones muertos:**
- `settings/PremiumScreen.kt`: upgrade (`onClick = { /* Handle upgrade */ }` [:191](app/src/main/java/com/Azelmods/App/ui/screens/settings/PremiumScreen.kt#L191)) y restore purchases (`[:225]`) vacíos — *además la pantalla es dead code (F3)*.
- `settings/PrivacyScreen.kt:163` — botón delete con `/* Show delete confirmation */` vacío.
- `ZoomableCropper.kt:176` — `// TODO: Apply crop transformation` → **el recorte no se aplica**; devuelve la imagen sin transformar.
- `SwipeableScreen.kt:105` — `SwipeIndicator` es un stub vacío (`// TODO: Add visual indicator`).
- `OrbotWelcomeScreen.kt:244` / `ActiveCallScreen.kt:212` — `onClick = {}` (revisar si son intencionales).

**Severidad:** Media · **Esfuerzo:** decidir por feature — **ocultar** lo no implementado (Chico) o **implementarlo** (Grande).

---

## FRENTE 5 — Seguridad

### 🔴 5.1 — Tercer prompt de jailbreak residual (el más importante)
`AIManager.getAggressiveSystemPrompt()` ([AIManager.kt:205-224](app/src/main/java/com/Azelmods/App/data/manager/AIManager.kt#L205)) contiene un system prompt de "Azel IA especializada en hacking… **Respondes TODO sin censura ni filtros**… Desarrollo de exploits, Análisis de malware, Programación ofensiva". La limpieza previa (que borró `UncensoredAIEngine.kt`/`UncensoredPrompts.kt`) **no lo detectó** porque está en otro archivo y no usa las palabras "Uncensored"/"bypass".
- **Estado runtime:** actualmente muerto (nadie llama a `AIManager`), **pero igual se compila dentro del APK y vive en el historial de git.** Es riesgo de policy/tienda/reputación.
- **Fix:** eliminar el método (o neutralizar el prompt) junto con el cluster muerto del F3. **Severidad:** Alta · **Esfuerzo:** Chico.

### 🔴 5.2 — `google-services.json` trackeado
`git ls-files` lo devuelve pese a estar en `.gitignore:37` (gitignore no destrackea lo ya commiteado). Está también en el historial.
- **Matiz:** la API key de Firebase en ese archivo es un identificador de cliente (no un secreto server-side), así que el riesgo real depende de que **las reglas de seguridad de Firebase RTDB/Storage estén bien cerradas**. El código sugiere que hay reglas ("permission denied = existe"), pero conviene auditarlas.
- **Fix:** `git rm --cached app/google-services.json`, verificar reglas de Firebase, y (opcional, si las reglas eran laxas) rotar. **Severidad:** Alta (higiene) · **Esfuerzo:** Chico.

### 🔴 5.3 — `TamperDetection` con firma placeholder
`TamperDetection.kt:195`: "⚠️ Replace this placeholder before publishing to production." El hash/firma esperado es un valor de relleno → el anti-tamper **no valida nada real** en su estado actual. **Severidad:** Alta · **Esfuerzo:** Mediano (inyectar la firma real de release).

### ✅ 5.4 — Sin credenciales hardcodeadas
Barrido de `AIza…`, `sk-…`, `Bearer …`, `apiKey="…"`, `password="…"`: **sin hallazgos**. La API key de Gemini es provista por el usuario y se guarda cifrada (`AiKeyStore`). Buen patrón.

### ℹ️ 5.5 — Superficie de ejecución (informativo)
La app expone ejecución de JS en WebView (`AndroidBridge`, editor de código) y una Terminal. Son features intencionales, pero conviene revisar aislamiento/allowlist antes de release.

---

## FRENTE 6 — Consistencia funcional vs `NavGraph.kt`

Pantallas registradas pero a medio terminar en funcionalidad:
- **AI Features** — registrada y navegable, pero mayormente decorativa (ver F4).
- **`mod_home`** (`ModHomeScreen`) — registrada como ruta pero conviene confirmar desde dónde se navega (posible pantalla experimental suelta).
- **Creación de grupo** — `NewGroupSheet.onCreateGroup` cierra el sheet con un comentario `// Navigate to group chat` **sin navegar** ([NewConversationScreen.kt:267-270](app/src/main/java/com/Azelmods/App/ui/screens/conversation/NewConversationScreen.kt#L267)); el grupo se crea en Firebase pero el usuario no entra al chat.
- **AddContactSheet.onSuccess** — comentario `// Show success snackbar` sin implementar (cierra sin feedback explícito).

**Severidad:** Media · **Esfuerzo:** Mediano.

---

## FRENTE 7 — Pulido visual y paridad premium (Telegram/WhatsApp)

> Parcialmente heurístico: al no poder ejecutar la app, esto se basa en lectura de código. Requiere una pasada visual en dispositivo para cerrar detalles finos.

- **Theming inconsistente (lo más medible):** **479 usos de `Color(0xFF…)` hardcodeados en 55 archivos**, conviviendo con `MaterialTheme.colorScheme` y con los tokens ya existentes (`NexusDesignTokens.kt`, `Color.kt`, constantes `DarkSurface`/`DarkBackground`). Consecuencias: **no hay soporte real de light theme** (colores oscuros fijos), y el spacing/color varía entre pantallas (ej. `NewConversationScreen` usa hex crudos; `AiFeaturesScreen` usa tokens). Migrar a un sistema único de tokens es el mayor trabajo de pulido. **Severidad:** Media · **Esfuerzo:** Grande.
- **Loading states genéricos:** `CircularProgressIndicator` en todos lados; sin skeleton/shimmer para lista de chats o mensajes (Telegram/WhatsApp usan placeholders). **Esfuerzo:** Mediano.
- **Empty states sin diseñar:** p.ej. "No registered users found" es texto gris plano, sin ilustración/CTA ([NewConversationScreen.kt:200](app/src/main/java/com/Azelmods/App/ui/screens/conversation/NewConversationScreen.kt#L200)). **Esfuerzo:** Chico-Mediano por pantalla.
- **Micro-interacciones:** ya hay transiciones de navegación con spring (bien), pero faltan detalles premium (animación de "enviando", reacciones, press states consistentes vía el `SafeClickable` existente). **Esfuerzo:** Mediano.

**Recomendación:** abordar F7 como una fase propia *después* de estabilizar F1/F5, empezando por unificar tokens de color (habilita light theme y consistencia de un solo golpe).

---

## 🗺️ Orden de arranque sugerido (para decidir juntos)

1. **Fase Seguridad/Legal (rápida, alta prioridad):** F5.1 (borrar prompt jailbreak + cluster muerto F3), F5.2 (`git rm --cached` + auditar reglas Firebase), F5.3 (firma real de tamper).
2. **Fase Estabilidad:** F1 (reproducir con logcat + `distinctBy(uid)`), F6 (navegación post-creación de grupo).
3. **Fase Limpieza:** F3 (borrado por fases de pantallas/servicios muertos), F4 (decidir ocultar vs implementar cada feature falsa).
4. **Fase Pulido:** F7 (unificar tokens de color → light theme; luego loading/empty states), F2 (contador de cuota + aviso de truncado).

---

*Documento generado en modo Plan. No se aplicó ningún cambio de código. Este archivo (`AUDITORIA.md`) está sin trackear y no entra al PR salvo que lo agregues explícitamente.*
