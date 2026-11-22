<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=2,22,28,40&height=200&section=header&text=CHANGELOG&fontSize=70&fontColor=fff&animation=twinkling&fontAlignY=35&desc=NexusChat%20—%20Historial%20de%20Cambios&descAlignY=60&descSize=20&descAlign=50" />

<br>

<p align="center">
  <a href="https://github.com/Azelmods677/NexusChat">
    <img src="https://img.shields.io/badge/Version-8.0.0-2FB38E?style=for-the-badge&logo=android&logoColor=white&labelColor=0d1117" alt="Version" />
  </a>
  <a href="https://github.com/Azelmods677/NexusChat/releases">
    <img src="https://img.shields.io/github/v/release/Azelmods677/NexusChat?style=for-the-badge&logo=github&color=00E676&labelColor=0d1117" alt="Latest Release" />
  </a>
  <a href="https://github.com/Azelmods677/NexusChat/commits/main">
    <img src="https://img.shields.io/github/last-commit/Azelmods677/NexusChat?style=for-the-badge&logo=git&color=FFD700&labelColor=0d1117" alt="Last Commit" />
  </a>
</p>

<p align="center">
  <a href="#v800">🎨 v8.0.0</a> ·
  <a href="#v700">🚀 v7.0.0</a> ·
  <a href="#v610">v6.1.0</a> ·
  <a href="#v301">v3.0.1</a> ·
  <a href="#v300">🚀 v3.0.0</a> ·
  <a href="#v220">🔧 v2.2.0</a> ·
  <a href="#v210">⚡ v2.1.0</a> ·
  <a href="#v200">🎉 v2.0.0</a> ·
  <a href="#v100">🌱 v1.0.0</a>
</p>

</div>

---

<a id="v841"></a>
## v8.6.1

**Correcciones**
- Vuelven los contadores de mensajes sin leer: cada chat muestra cuántos mensajes nuevos tienes (también fotos, vídeos, audios, ubicaciones y stickers) y el filtro "No leídos" los encuentra. El contador se pone a cero al abrir el chat.
- Las notificaciones llevan el logo de NexusChat en vez de una campana.

---

## v8.6.0

**Conoce a Nexa**
- Nexa, la anfitriona de NexusChat, es una chibi en pixel art con su diseño oficial: melena negra con puntas violeta, cascos con la N, top con la N, chaqueta negra de mangas blancas y botas con suela violeta.
- Al crear tu cuenta te enseña la app de verdad: camina hasta cada botón, lo ilumina, lo señala con un destello que sale de su mano y te explica para qué sirve. Recorre en 27 paradas toda la app: Inicio, llamadas, historias, cómo funciona un chat, canales, difusiones, mensajes guardados, privacidad, notificaciones, almacenamiento, tu perfil, color y fondo, el Estudio de diseño, los chats secretos, Tor y su navegador privado, el descargador de vídeos, la terminal con Kali Linux, el editor de código y Azel IA.
- Habla con la voz del teléfono moviendo la boca, parpadea, respira y cambia de expresión: feliz, guiño, sorpresa, pícara, pensativa, enamorada y emocionada, con sus "?", "!", corazones y brillos.
- Si la tocas te cuenta más de 150 historias, curiosidades y consejos: el cifrado de Julio César, Enigma y Turing, el primer SMS, la @ del correo, el origen de Tor o los primeros emojis, y frases de Sócrates, Séneca, Marco Aurelio, Nietzsche, Camus o Sun Tzu. También te presenta a su creador, Azelmodsx677.
- Escribe lo que dice mientras habla, reacciona si la tocas y Atrás vuelve al paso anterior. Se calla si sales de la app, se puede saltar o silenciar, y la presentación automática solo sale una vez por cuenta.
- Nexa es también el centro de novedades: tras cada actualización te cuenta, con voz, lo nuevo de esa versión. En Ajustes → Información puedes volver a ver la presentación o las novedades.

**Marcas de enviado y leído**
- 13 estilos nuevos: gruesas, neón, círculos, corazones, estrellas, ojo al leer, escudo, avión de papel y la chibi Kawaii.
- Ocho personajes originales en pixel art: cinco chicas (Akari, Yume, Rin, Sora y Mika) y tres chicos (Kai, Ren y Haru). Duermen al enviar, despiertan y guiñan al entregar, y ponen ojos de corazón al leer.
- "Mis marcas": elige tus propias imágenes o GIF animados para enviado, entregado y leído.
- La vista previa del Estudio de diseño enseña los tres estados de cada estilo.

**Correcciones**
- La lista de chats ya no marca como leído un mensaje que la otra persona no ha abierto: muestra el estado real del último mensaje.
- Nueva marca de entregado: cuando el mensaje llega al teléfono de la otra persona, salen dos marcas grises.
- Borrar un canal ya no deja un canal vacío con el contador de seguidores.
- Azel Assistant marca tus mensajes como entregados y leídos antes de responder: las marcas animadas ya cambian de estado en su chat.

**Actualizaciones**
- Si una actualización está firmada con otra llave, la app lo detecta y ofrece desinstalar en vez de mostrar "conflicto con un paquete".
- Librería de red actualizada a OkHttp 5.5.1.

---

<a id="v851"></a>
## v8.5.1

**Privacidad**
- Tu correo y tu teléfono ya no son visibles para otros usuarios. Quien ya los conoce puede seguir encontrándote por ellos, pero nadie puede ver la lista.
- Las cuentas nuevas ya no usan parte del correo como `@usuario`.

**Chats**
- Al borrar mensajes seleccionados vuelve a preguntar: "Eliminar para todos" o "Eliminar para mí". En la 8.5.0 solo se borraban para ti.
- Borrar para todos también quita del servidor el texto cifrado y el nombre del archivo.
- Tocar una respuesta lleva al mensaje original exacto (antes caía en el de al lado) y, si es antiguo, carga la conversación hasta encontrarlo.
- "Bajar al último mensaje" llega de verdad al final.
- El fondo de cada chat es solo tuyo, como el fondo de la app: la otra persona ya no lo ve cambiar.

**App lista para producción**
- Fuera las ventanas de tutorial que salían en Llamadas, Historias, Ajustes y Seguridad.
- En Nueva conversación ya no apareces tú como contacto, y el "Chat de prueba" pasa a ser Azel Assistant.
- Derechos de autor y año correctos en Acerca de y Ayuda; novedades de la 8.5 actualizadas.

**Canales y GIF**
- El contador de seguidores ya no cuenta doble (la 8.5.0 sumaba desde la app y desde el servidor) y los canales nuevos empiezan bien.
- GIF sin claves: envía los GIF y stickers del buscador de tu teclado (Gboard, Xiaomi…) directamente al chat.
- "Enviar como GIF": un vídeo de hasta 15 segundos se manda en bucle y sin sonido, como en WhatsApp.
- El buscador de GIF de la app ya no pide a cada usuario una clave: pasa por el servidor. Si no está disponible, ofrece la galería y el teclado.

**Premium**
- Premium solo aparece cuando el cobro está activado; ya no se muestra una pantalla de pagos sin activar.

**Servidor**
- Los miembros de un grupo vuelven a poder enviar mensajes.
- Se cierran dos nodos antiguos de la base de datos que ya no se usaban.
---

<a id="v850"></a>
## v8.5.0

**Archivos y fotos**
- Envía varias fotos, vídeos o documentos de una vez (hasta 30), cada uno con su descripción y menciones con @.
- Los documentos se abren dentro de la app con su programa correcto, muestran tipo y tamaño, y se pueden guardar en Descargas o compartir.
- Cambia el nombre de un documento antes de enviarlo.
- La foto de la cámara también lleva descripción antes de salir.

**Chats**
- Los enlaces, correos y teléfonos salen en azul y se pueden tocar.
- Mantén pulsado un mensaje para seleccionarlo; el menú se abre con un toque o con "Más".
- Desliza un mensaje a la derecha para responderlo.

**Grupos**
- Tocar el aviso de un grupo abre el grupo, no el chat privado con quien escribió.
- La foto de un grupo en la lista abre la información del grupo.
- Botón claro para añadir o editar la descripción.
- Nuevo: solo los administradores envían mensajes, y quién puede editar la info del grupo.
- Eliminar un grupo siendo miembro ahora es salir de él; antes lo borraba para todos.

**Canales y difusiones**
- Canales y difusiones a la vista en la pestaña Historias.
- Explora los canales más seguidos sin saber su `@nombre`.
- El contador de seguidores ya cuenta de verdad.
- Nuevo: difusiones, el mismo mensaje a varios contactos, cada uno en su privado.

**Descargar vídeos**
- Arranque más fiable: si la instalación quedó a medias, se repara sola.
- Tres intentos con ajustes distintos antes de rendirse, y botón para cancelar.
- Con Tor activo, la descarga también sale por Tor.
- Si falla, puedes ver y copiar el error exacto.

**Seguridad**
- En los grupos, nadie que no sea administrador puede nombrarse administrador, echar a otros, cambiar el nombre o borrar el grupo.
- Nadie puede escribir en un grupo en nombre de otro.
- Nadie puede crear el chat privado de otras dos personas.

---

<a id="v841"></a>
## v8.4.1

- Azel Assistant explica qué es NexusChat de verdad: un mensajero hecho para el anonimato, con cifrado en tu teléfono, Tor integrado y modo anónimo.
- Los chats que perdieron su lista de miembros vuelven a funcionar.

---

<a id="v840"></a>
## v8.4.0

- Seguridad: los archivos de un chat de dos solo los leen, suben y borran sus dos participantes.
- Los enlaces internos del servidor ya no aparecen en mensajes, stickers ni vistas previas.
- Los grupos de los que te expulsaron dejan de dar "Permission denied" y salen de tu lista.
- La lista de chats ya no se queda esperando si un chat desaparece mientras carga.
- El aviso de cifrado explica que el mensaje se cifró con una clave anterior, en vez de parecer un fallo.
- El texto del campo de escribir se puede cambiar por tu nombre o cualquier frase, con un ejemplo en el Estudio.
- Toca una reacción para ver quién reaccionó y con qué; tocando tu fila quitas la tuya.
- Texto oculto: escribe ||así|| y queda tapado hasta que lo toquen.
- Tres índices nuevos en la base de datos para el editor de código y la IA.
- El asistente cuenta las novedades de la versión instalada, leídas del historial de cambios: se actualiza solo con cada versión.

---

<a id="v831"></a>
## v8.3.1

- Búsqueda más rápida: los perfiles se descargan una vez y no con cada letra.
- Los errores del servidor se explican en español y dicen qué hacer, en lugar de mostrar "Permission denied".
- La búsqueda avisa cuando falla en vez de quedarse en blanco.

---

<a id="v830"></a>
## v8.3.0

- El descargador de vídeos vuelve a funcionar: se actualiza solo y reintenta si falla el primer intento.
- Reportar usuarios desde el menú del chat.
- Política de privacidad dentro de la app.
- Campo "Social" con las medidas exactas: iconos de 30 dp y botón de voz de 48 dp.

---

<a id="v820"></a>
## v8.2.0

- Campo "Foto con aro" con el aro animado: gira, crece y se recoge dejando una estela, y responde al toque.
- Exportar un chat como archivo de texto desde su menú.
- La versión publicada ya no escribe registros de depuración en el teléfono.

---

<a id="v810"></a>
## v8.1.0

### Chats
- Las respuestas muestran quién escribió el mensaje y nunca un enlace interno; los stickers aparecen como "Sticker".
- Tocar una respuesta lleva al mensaje original, y tocar una mención con @ abre el perfil.
- Elegir otra vez la misma reacción la quita.
- Los mensajes reenviados llevan la etiqueta "Reenviado".
- Fotos y vídeos con texto antes de enviarlos.
- Notas de voz y archivos de audio se distinguen a simple vista.
- Entrar a un chat es inmediato, también en los grupos.

### Stickers
- Ahora viven en el panel de emojis, con favoritos, quitar de recientes y borrar.

### Grupos e historias
- Foto y descripción de los grupos.
- Las historias se responden con emojis y stickers, con una barra y una respuesta rediseñadas, y la descripción ya no queda tapada.

### Diseño
- Cuatro campos de escribir nuevos: foto con aro, foto cuadrada, cápsula central y marco con línea.
- El estilo de fila se aplica también a Archivados y a la lista de reenviar.
- El estudio muestra una miniatura de cada opción.

---

<a id="v801"></a>
## v8.0.1

- Corregido el cierre inesperado al abrir la app en la versión 8.0.0.

---

<a id="v800"></a>
## v8.0.0: tu app, a tu manera

### Estudio de diseño
- Filas de chat: tarjeta, barra lateral, línea, relieve 3D con la foto hundida, borde, tarjeta 3D, neón, cristal y foto de fondo. Se aplican también a llamadas, historias, contactos y búsqueda.
- Efecto en las fotos de perfil: hundida o en esfera. Color de las filas a elegir.
- Campo de escribir: cápsula, con tu foto, social, plano, 3D, contorno, línea y bloque, con texto propio.
- Burbujas, marcas de lectura, colores, tamaño de letra y foto dentro de la burbuja.
- Siete diseños de inicio y siete de ajustes, con búsqueda en ajustes.
- 8 barras inferiores: clásica, flotante, solo iconos, aro central animado, con tu foto, accesos, línea y panel.
- 7 juegos de iconos para la barra inferior.
- Iconos de llamada y videollamada animados, que se pueden apagar.
- Filtros como pestañas de texto y un engranaje animado en Ajustes.
- 21 tipos de letra para toda la app y 36 colores de acento, o el tuyo en hexadecimal.
- Fotos y portadas de perfil en vídeo de hasta 1 minuto y 30 segundos.
- Panel de funciones rápidas dentro del chat.

### Mensajería
- Grupos cifrados de extremo a extremo, con administradores y enlace de invitación.
- Chats secretos con una clave nueva en cada mensaje.
- Canales, encuestas, mensajes fijados, guardados y programados.
- Reenviar, seleccionar varios mensajes, compartir contactos y vista previa de enlaces.
- Carpetas de chats, chats ocultos con tu huella y marcar como no leído.
- Stickers propios y animados, con paquetes para crear y compartir.

### Herramientas
- Kali Linux de verdad dentro de la terminal.
- Descarga de vídeos de YouTube, Facebook, X e Instagram.
- Los agentes guardan sus archivos y PDF en la carpeta que elijas, con tu permiso.

### Todo lo demás
- Nueva animación de entrada e iconos con movimiento.
- Aviso de actualización al momento y recuperación de contraseña desde la app.
- Las notificaciones muestran el mensaje real y la lista de chats, el último mensaje.
- Preparada para Android 17.

---

<a id="v700"></a>
## v7.0.0: la actualización más grande de NexusChat

### Agentes que trabajan de verdad
- Ahora programan como un desarrollador: escriben el código, lo ejecutan, leen el error, lo corrigen y guardan los archivos en tu editor.
- 17 herramientas reales: búsqueda web, noticias, lectura de páginas, búsqueda de perfiles, GitHub, Stack Overflow, paquetes, clima, divisas, ejecución de JavaScript en el teléfono y de 15 lenguajes en un servidor, y archivos.
- Búsqueda en varios buscadores a la vez, con Google a través de Gemini. Marcan qué resultados contienen de verdad lo buscado y dicen dónde buscaron, en vez de afirmar que algo "no existe".
- Equipo de desarrollo nuevo: Arquitecto, Programador, Probador y Revisor. Se ve en directo lo que escribe cada uno.
- Corregido: el código llegaba cortado porque la respuesta se forzaba dentro de un JSON y se recortaba a 3.000 caracteres.

### Perfiles con fotos y vídeos
- La portada y la foto de perfil ahora pueden ser una foto o un vídeo en bucle.
- Fotos y vídeos a pantalla completa para deslizar, como en Telegram, en tu perfil y en el de los demás.
- Colores y portada en su propia pantalla, "Personalizar perfil", con vista previa en directo.
- 40 colores de perfil: 24 para todos y 16 exclusivos de Premium. Quien ya tenía uno elegido lo conserva.

### Llamadas
- Corregido: la videollamada se veía en negro. La cámara, el codificador y la pantalla usaban contextos gráficos distintos.
- Tu cámara a pantalla completa mientras suena, y el recuadro propio ya no queda tapado por la imagen grande.
- Tono de llamada para quien llama: antes solo había silencio.
- Fondo con la foto desenfocada y el color de la persona.

### Notificaciones
- Corregido: los avisos de mensajes nuevos no llegaban. El servidor enviaba el aviso a un destinatario equivocado.
- Aviso cuando un contacto publica una historia.

### Premium con pago real
- Pago con Mercado Pago, confirmado por el servidor: la app nunca decide por sí sola que alguien ha pagado.
- Insignia, colores exclusivos y galería de perfil sin límite.

### Entrada e interfaz
- Animación de entrada nueva, con opción para apagarla en Apariencia.
- Acerca de renovado, con las novedades de la versión.
- La app completa en español e inglés.

### Estabilidad y seguridad
- Corregido el cierre inesperado "Key was already used" al subir por el historial de un chat.
- El historial cargado ya no desaparece cuando llega un mensaje nuevo.
- Un error al responder a una historia ya no borra la historia de la pantalla.
- Reglas de Storage: borrar archivos dejó de fallar, y las copias de seguridad y los vídeos de perfil ya tienen permiso.
- El bloqueo de contactos ahora se aplica de verdad en el servidor.

### Retirado
- Escudo "anti-traba": no tiene sentido en una app independiente.


---

<a id="v610"></a>

## 🆕 v6.1.0 — Llamadas fiables, agentes de IA y Tor verificado

### 📞 Llamadas
- **Conexión**: el receptor creaba la respuesta antes de aplicar la oferta y la llamada podía quedarse en «Conectando…» para siempre. Candidatos ICE aplicados una sola vez. STUN/TURN configurables en `config/webrtc/iceServers` sin publicar otra versión.
- **Flujo**: fases visibles (llamando, conectando, conectado, reconectando), la pantalla se cierra cuando el otro cuelga o rechaza, cronómetro desde que conecta, duración real en el historial y filtro de **perdidas** que por fin funciona.
- **Notificaciones**: «Aceptar» funciona en Android 12+, estilo de llamada del sistema con tono repetido que se retira al contestar/rechazar/colgar, pantalla completa con el móvil bloqueado (`USE_FULL_SCREEN_INTENT`). «Colgar» y «Silenciar» de la llamada en curso actúan de verdad. La llamada perdida llega a quien no la cogió.
- **Audio**: auricular, altavoz y Bluetooth correctos; sensor de proximidad en llamadas de voz.
- **Privacidad**: con el modo anónimo, el medio va por TURN y el otro no ve tu IP.

### 🤖 Azel IA y agentes
- **Agentes de IA** (pestaña nueva): seis agentes y cuatro equipos que buscan en la web, leen páginas, calculan y se reparten el trabajo. Se ve cada paso en vivo: quién piensa, qué herramienta usa, qué encontró y el resultado final.
- Streaming real con todos los proveedores, botón **Detener**, límites por inactividad (sin cortes a los 60 s), Gemini 2.5 sin respuestas vacías, cabecera con el proveedor y modelo reales y selector de modelo.

### 🧅 Tor / Orbot
- Orbot se enciende desde NexusChat (API de intents) y se usan sus puertos reales.
- Verificación real con `check.torproject.org`: IP de salida, modo proxy o VPN.
- Cierre seguro: si Orbot cae, la app no sale a internet sin Tor; sin fugas de DNS.
- Modo anónimo restaurado al abrir la app (antes solo al entrar en Seguridad).
- Navegador: WebRTC bloqueado (fuga de IP), primera página por Tor (buscador .onion), sin cookies de terceros, datos borrados al salir y botón «Nueva identidad».

### 🏭 Producción
- CI en todas las ramas y en verde (fallaba por la ruta del APK con splits por ABI).
- Retiradas dependencias que no usaba ningún código (zip4j, smali, BouncyCastle, NetCipher).
- Reglas: solo puedes crear llamadas en las que eres el emisor.

<br>

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
