# PORTAFOLIO TÉCNICO — Azelgram Messenger

> Documento de estudio para entrevistas. La idea no es memorizarlo: es entender
> cada decisión lo suficiente como para explicarla con tus palabras y responder
> repreguntas. Cada sección termina con la idea central en una frase, que es lo
> que conviene tener claro antes que el detalle.

---

## 1. Arquitectura general

### Qué patrón usa la app

**MVVM (Model–View–ViewModel) + Repository**, con inyección de dependencias
vía **Hilt** y UI declarativa en **Jetpack Compose (Material 3)**.

```
Firebase (RTDB / Auth / Storage)
        │
        ▼
Repository  (data/repository/*, data/translation/*, webrtc/*)
        │   expone Flow / suspend functions, oculta de dónde vienen los datos
        ▼
ViewModel   (@HiltViewModel, uno por pantalla)
        │   transforma datos en un StateFlow<UiState> inmutable
        ▼
StateFlow<UiState>
        │   collectAsState() en el Composable
        ▼
Compose UI  (pantallas @Composable, sin lógica de negocio)
```

### Por qué tiene sentido para una app de mensajería en tiempo real

1. **Los datos cambian solos.** En un chat, los mensajes llegan sin que el
   usuario toque nada (otro usuario escribe, Firebase notifica). Eso pide una
   arquitectura *reactiva*: el Repository expone un `Flow` que emite cada vez
   que Firebase cambia, el ViewModel lo transforma en estado, y Compose se
   recompone automáticamente. No hay ningún "refresh manual".

2. **El estado sobrevive a la UI.** Rotar la pantalla o navegar destruye y
   recrea los Composables, pero el ViewModel sobrevive (está atado al ciclo de
   vida de la pantalla, no del frame). El chat no se recarga desde cero en cada
   rotación.

3. **Cada capa es reemplazable y testeable.** La UI no sabe que existe
   Firebase: si mañana se migra a otro backend, solo cambian los Repositories.
   Y un ViewModel se puede testear con un Repository falso, sin emulador.

### Cómo fluye un mensaje concreto (ejemplo para contar en entrevista)

Cuando llega un mensaje: Firebase RTDB dispara el listener → el Repository lo
emite por un `Flow` → `ChatViewModel` lo desencripta si es E2EE
(`DecryptMessageUseCase`), lo agrega a la lista y publica un nuevo
`ChatState` (data class inmutable) por su `StateFlow` → `ChatScreen` está
suscrito con `collectAsState()` → Compose detecta que `state.messages` cambió
y recompone solo la `LazyColumn` de mensajes.

> **Idea central:** los datos fluyen en una sola dirección (unidirectional
> data flow) y la UI es una función del estado: `UI = f(state)`. Nadie muta
> vistas a mano; se publica un estado nuevo y Compose reacciona.

---

## 2. Los bugs más interesantes que se arreglaron

### Bug 1 — Crash de "New Chat": keys duplicadas en LazyColumn

- **Síntoma:** la app crasheaba al abrir la pantalla de nueva conversación.
- **Causa técnica:** `LazyColumn` acepta un parámetro `key` por ítem para
  identificar cada fila de forma estable (mejora animaciones y recomposición).
  La key era el `uid` del contacto. Si Firebase devolvía el mismo usuario dos
  veces (nodo duplicado, o el usuario actual repetido en la lista), Compose
  lanzaba `IllegalArgumentException: Key "<uid>" was already used`.
- **Por qué es un error común:** todo el mundo asume que "los IDs de la base
  son únicos"… hasta que la base tiene datos sucios. En Compose esto no
  degrada con elegancia: **crashea**. Es de los crashes más frecuentes al usar
  `items(key = …)` con datos remotos.
- **Solución general:** desduplicar **en la fuente de datos** (el ViewModel:
  `users.distinctBy { it.uid }`), no en la UI. Así ninguna pantalla que consuma
  esa lista puede repetir keys, en lugar de parchear cada `LazyColumn` por
  separado. Regla: la UI no debe defenderse de datos que la capa de datos
  puede garantizar.

### Bug 2 — El cropper de imagen que no recortaba (graphicsLayer ≠ píxeles)

- **Síntoma:** el usuario hacía zoom y encuadraba su foto, confirmaba… y la
  imagen se guardaba **entera, sin recortar**.
- **Causa técnica:** el zoom/pan estaba implementado con
  `Modifier.graphicsLayer(scaleX, scaleY, translationX, translationY)`.
  `graphicsLayer` es una transformación **de render**: cambia cómo se dibuja
  el composable en pantalla pero jamás toca el bitmap. El `onConfirm` devolvía
  el URI original.
- **Por qué es un error común:** confundir la capa visual con los datos. Es el
  equivalente Compose de escalar un ImageView con `scaleX` y creer que el PNG
  cambió. La preview "miente": se ve recortado, pero el archivo no lo está.
- **Solución general:** invertir la cadena de transformaciones para proyectar
  el viewport a coordenadas del bitmap:
  `p_layer = (p_pantalla − offset − centro) / scale + centro`, luego
  `p_bitmap = (p_layer − origenDelContenido) / escalaBaseDeContentScale`.
  Con ese rectángulo se hace `Bitmap.createBitmap(src, x, y, w, h)` y se
  persiste. Detalle fino: decodificar con `ALLOCATOR_SOFTWARE`, porque los
  hardware bitmaps de Android no permiten leer píxeles.

### Bug 3 — Toggles que "funcionaban" pero no hacían nada (remember sin persistencia)

- **Síntoma:** la pantalla AI Features tenía 6 switches que se podían activar,
  pero al salir y volver estaban apagados, y activar cualquiera no cambiaba
  ningún comportamiento de la app.
- **Causa técnica:** cada toggle era `remember { mutableStateOf(false) }` —
  estado **local del Composable**. `remember` sobrevive a recomposiciones,
  pero muere con la pantalla. Nada lo leía fuera de esa pantalla y nada lo
  persistía.
- **Por qué es un error común:** `remember` es tan cómodo que se usa como si
  fuera persistencia. La jerarquía real es: `remember` (vive lo que la
  composición) → `rememberSaveable` (sobrevive rotación/proceso) → ViewModel
  (vive lo que la pantalla) → DataStore/Room (vive para siempre). Un setting
  de usuario pertenece al último nivel.
- **Solución aplicada aquí:** ocultarlos honestamente con un TODO que documenta
  qué falta (DataStore + lógica real + consumo desde ChatViewModel), porque una
  feature falsa es peor que una feature ausente: rompe la confianza del usuario.

### Bug 4 — Trabajo asíncrono sin feedback (grupo creado "en el vacío")

- **Síntoma:** al crear un grupo, el grupo aparecía en Firebase pero el usuario
  quedaba en la misma pantalla sin ninguna señal. Lo mismo al agregar contacto:
  el sheet se cerraba mudo. Y al traducir, los mensajes largos se cortaban a
  500 caracteres sin avisar.
- **Causa técnica:** operaciones async (crear en Firebase, llamar una API) cuyo
  "happy path" terminaba en un comentario `// Navigate to group chat` o
  `// Show success snackbar` nunca implementado. El dato se escribía bien; el
  **cierre del ciclo con el usuario** no existía.
- **Por qué es un error común:** en el flujo async es fácil dejar el éxito sin
  rama visible, porque "funciona" (el dato llega a la base) y nadie lo nota en
  el code review. El resultado es UX de acciones que parecen fallar aunque
  funcionen.
- **Solución general:** toda acción del usuario debe terminar en una de dos
  señales observables: navegación o feedback (snackbar/estado). En este caso:
  el callback del sheet ahora recibe el `groupId` y navega a `chats/{groupId}`;
  el traductor devuelve un resultado enriquecido (`wasTruncated`,
  `remainingWords`) y la UI muestra avisos preventivos con un contador local de
  cuota (~1000 palabras/día, persistido con reset diario).

> **Idea central de los 4 bugs:** dato ≠ render ≠ estado persistido ≠
> feedback. Cada bug fue confundir dos de esas capas: keys que asumían datos
> limpios, render que se hacía pasar por datos, estado efímero que se hacía
> pasar por settings, y escrituras exitosas sin señal para el usuario.

---

## 3. Decisiones de diseño defendibles

### ¿Por qué Jetpack Compose y no XML/Views?

- **UI como función del estado:** con Views hay que sincronizar a mano cada
  cambio (`textView.setText`, `adapter.notifyDataSetChanged`…), que es la
  fuente clásica de bugs de UI desincronizada. En Compose se describe *qué* se
  ve para un estado dado y el framework calcula el *cómo*.
- **Menos ceremonias:** sin `findViewById`, sin RecyclerView.Adapter/ViewHolder
  (una lista de chats es `LazyColumn { items(...) }`), sin XML paralelo al
  código.
- **Es la dirección de la plataforma:** Material 3, animaciones y APIs nuevas
  salen primero (o solo) para Compose.
- *Trade-off honesto para la repregunta:* curva de aprendizaje del modelo de
  recomposición (estabilidad, keys, efectos) y tooling de preview menos maduro
  que el editor XML. Compensado con creces en apps con estado que cambia mucho,
  como un chat.

### ¿Por qué Hilt para inyección de dependencias?

- Un ViewModel de esta app necesita varios repositories, servicios de cifrado,
  preferencias… Construir eso a mano significa fábricas anidadas y singletons
  manuales propensos a fugas.
- Hilt genera el grafo **en compile-time**: si falta una dependencia, falla el
  build, no la app en producción (a diferencia de service locators en runtime).
- Está integrado con el ciclo de vida Android: `@HiltViewModel` +
  `hiltViewModel()` resuelven la creación y el scoping del ViewModel sin
  fábricas manuales; `@Singleton` garantiza una sola instancia de cada
  repository.
- *Alternativas para la repregunta:* Koin (más simple, pero resuelve en
  runtime) y Dagger puro (más control, mucho más boilerplate). Hilt es el
  estándar actual recomendado por Google.

### ¿Por qué StateFlow y no LiveData?

- **Siempre tiene valor:** `StateFlow` exige un estado inicial, así que la UI
  nunca observa "nada" (LiveData puede estar vacío y obliga a null-checks).
- **Es Kotlin puro, no Android:** los ViewModels que exponen StateFlow se
  pueden testear en JVM sin dependencias de framework; LiveData necesita
  utilidades específicas de Android para testearse.
- **Operadores de Flow:** `map`, `combine`, `debounce` (p.ej. para el buscador
  de contactos) vienen gratis; con LiveData eso son Transformations limitadas.
- **Encaja nativo con Compose:** `collectAsState()` consume el flujo respetando
  el ciclo de vida.
- LiveData no está "mal": es legado de la era XML. En un proyecto 100% Compose
  + corrutinas, StateFlow es la pieza que corresponde.

### Bonus: el patrón de la migración de colores (Fase 4)

Había 478 `Color(0xFF…)` hardcodeados en 50 archivos. Se migraron a **design
tokens** centralizados en `ui/theme/Color.kt`, por lotes de pantallas
relacionadas. Por qué esto es arquitectura y no cosmética:

- **Single source of truth:** un color semántico (`DarkSurface`, `ErrorRed`)
  definido una vez. Cambiar la marca o corregir contraste de accesibilidad es
  tocar UNA línea, no 91 archivos con búsqueda y reemplazo arriesgado.
- **Habilita theming real:** con hex crudos incrustados, un light theme es
  imposible; con tokens, es re-mapear la paleta en un solo lugar.
- **Vocabulario compartido:** `DarkBorder` comunica intención; `0xFF3D3D5C` no
  dice nada. El code review pasa de "¿este hex está bien?" a "¿este token es el
  correcto?".
- **Regla del refactor seguro:** cada token conservó el hex exacto que
  reemplaza → cero regresión visual. Consolidar matices casi-iguales
  (`ErrorRed` 0xFFEF4444 vs `Error` 0xFFF44336) es un segundo paso deliberado y
  separado, para que cada commit haga una sola cosa.
- **Criterio de exclusión (importante en entrevista):** NO se tokenizó lo que
  es *dato* y no *estilo*: la paleta de temas seleccionables por el usuario
  (`ThemePreferences`), los swatches del color picker y la paleta hash de
  avatares. Un token de diseño dice "así se ve la app"; esos valores dicen
  "estas son las opciones del usuario". Confundirlos es sobre-abstraer.

---

## 4. Features de la app y su propósito técnico

- **Mensajería en tiempo real (Firebase RTDB):** los chats viven en
  `/chats/{chatId}` con un índice `/userChats/{uid}` para que la home cargue
  solo los chats del usuario. Los listeners de RTDB empujan cambios al
  instante: no hay polling.

- **E2EE (cifrado de extremo a extremo):** los mensajes se cifran en el
  dispositivo del emisor y solo el receptor puede descifrarlos; el servidor
  solo ve un blob (`encryptedPayload` en Base64). Ni Firebase ni un atacante
  con acceso a la base leen el contenido. El descifrado está encapsulado en un
  use case (`DecryptMessageUseCase`), no regado por la UI.

- **Llamadas con WebRTC:** protocolo estándar de audio/video peer-to-peer. La
  app solo usa el servidor para la *señalización* (intercambiar ofertas/
  respuestas SDP e ICE candidates vía Firebase); el stream de voz/video viaja
  directo entre dispositivos cuando la red lo permite, lo que baja latencia y
  costo de servidor. `WebRTCManager` encapsula PeerConnection, cámara y mic.

- **Navegación privada con Tor (Orbot):** la app detecta si Orbot está
  instalado y enruta la navegación integrada a través de la red Tor, que
  rebota el tráfico por varios nodos para anonimizar el origen. La app no
  reimplementa Tor: delega en Orbot como proxy local, que es la práctica
  recomendada.

- **Traducción de mensajes (MyMemory API):** traducción on-demand por mensaje,
  con manejo explícito de errores (sin red, timeout, cuota), contador local de
  cuota diaria (~1000 palabras) y aviso cuando el texto se trunca a 500
  caracteres (límite de la API gratuita).

- **Historias (stories):** contenido efímero de 24 h con reacciones y
  respuestas, sobre Firebase Storage (media) + RTDB (metadatos/visibilidad).

- **Extras técnicos:** bloqueo biométrico, backups cifrados (AES-256),
  detección de root/tampering, terminal y editor de código integrados, y un
  asistente IA (Gemini con API key del usuario, guardada cifrada en
  `AiKeyStore`).

---

## 5. Qué mejoraría con más tiempo (y deuda técnica detectada)

Esta lista sale de una auditoría real del código — sirve tal cual para la
pregunta "¿qué harías distinto?":

### Arquitectura

1. **ViewModels que acceden a Firebase directo.** 10+ ViewModels llaman
   `FirebaseDatabase.getInstance()` / `FirebaseAuth.getInstance()` saltándose
   el Repository (p.ej. `NewConversationViewModel.startConversation` escribe
   `/chats` a mano). Rompe la testeabilidad: no se puede fakear Firebase en un
   test de JVM. Movería todo acceso a datos detrás de interfaces de repository
   inyectadas.

2. **`NavController` dentro de un ViewModel** (`NewConversationViewModel`
   recibe el NavController como parámetro). El ViewModel no debería conocer la
   UI: lo correcto es exponer eventos de navegación (SharedFlow/Channel) y que
   el Composable navegue. Como está, el ViewModel retiene una referencia a un
   objeto de UI (riesgo de leak) y es imposible de testear sin Android.

3. **Clases God:** `ChatScreen.kt` (~1800 líneas), `ChatViewModel` (~1050
   líneas, estado de 20+ campos), `RealtimeDatabaseRepository` (~1100 líneas,
   80 funciones: usuarios, chats, grupos, amigos, notificaciones…). Dividiría
   el repository por agregado (UserRepository, ChatRepository,
   GroupRepository) y extraería sub-composables/archivos de ChatScreen.

4. **Capa de dominio a medio adoptar.** Existen use cases
   (`domain/usecase/*`) para cifrado, backups y stories, pero la lógica de
   chats/contactos vive en ViewModels. Elegiría un criterio único: o use cases
   consistentes para toda operación de negocio, o repositories gordos y
   ViewModels finos — pero no la mezcla.

5. **Dos sistemas de design tokens en paralelo:** `Color.kt` (plano) y
   `NexusTokens` (objeto). Ambos definen "surface/background/acentos" con
   valores distintos. Consolidaría en uno, idealmente mapeado a
   `MaterialTheme.colorScheme` para heredar light/dark gratis.

6. **Duplicación entre paquetes:** había pantallas duplicadas
   (`call/ActiveCallScreen` real vs `calls/ActiveCallScreen` maqueta — la
   maqueta ya fue eliminada) y helpers repetidos (`TypingDots`,
   `formatTimestamp`, `StatCard` definidos 2-3 veces). Extraería a
   `ui/components` compartidos.

### Producto / calidad

7. **Light theme real:** ahora que los colores están tokenizados, mapear los
   tokens a `colorScheme` de Material 3 y soportar tema claro/dinámico
   (Material You).
8. **Tests:** no hay tests unitarios de ViewModels ni de mappers. Con la
   migración a repositories inyectados, agregaría tests de `ChatViewModel`
   (envío, paginación, traducción) y del mapper de usuarios.
9. **i18n:** strings hardcodeados mezclando español e inglés en los
   Composables. Migraría a `strings.xml` con recursos por idioma.
10. **Restos de configuración muerta:** el enum `AIProvider.OLLAMA_LOCAL` y
    `AIPreferences.customOllamaUrl` sobreviven aunque `OllamaApiService` fue
    eliminado. Limpiaría el enum y la preferencia.
11. **Consolidación de paleta:** segundo paso de la migración de colores:
    unificar matices casi idénticos (dos rojos de error, tres verdes de
    "online") y decidir un único verde/rojo semántico.
12. **Logging:** `android.util.Log` directo por todos lados, con emojis en
    producción. Lo encapsularía (Timber o wrapper propio) con niveles por
    build type.

> **Idea central:** la app funciona, pero su punto débil es la disciplina de
> capas (datos colándose en ViewModels y UI). Las mejoras 1-4 son la misma
> frase dicha de cuatro formas: "cada capa debe poder testearse sin la de
> abajo".

---

*Generado durante la sesión de refactor del 2026-07-16 (fases: estabilidad,
limpieza, funcionalidad real, arquitectura). Los commits de esa rama están
separados por fase para poder revisar cada bloque de forma aislada.*
