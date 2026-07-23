# Auditoría de Diseño — UI Kit "E-Chat" (Figma Community)

**Documento:** Design Audit / Reverse Engineering
**Archivo auditado:** `Chatting App UI Kit Design | E-Chat` (fileKey `hATkZZ820yALkAHNndb5eD`)
**Alcance:** Solo comprensión y evaluación del kit. Sin rediseño. Sin comparación con otros productos.
**Fecha:** 2026-07-21
**Método:** Inspección directa vía Figma MCP — metadata estructural completa de ambas páginas (890K de XML), definiciones de variables, contexto de diseño (auto layout real) de componentes clave, y verificación visual por capturas.

---

## 0. Resumen ejecutivo

E-Chat es un UI kit de mensajería móvil de gama media-alta con una superficie enorme: **268 frames de nivel superior** en la página Design (≈130 pantallas en modo claro duplicadas íntegramente para modo oscuro) y una librería de **35 component sets con 308 variantes**. Sus fundamentos están inusualmente bien documentados para un kit de Community (rampas de color con ratios de contraste AA/AAA por swatch, tabla explícita de mapeo Light↔Dark, escalas de radio/espaciado/sombra publicadas), pero la **implementación técnica no está a la altura de su documentación**: los tokens existen solo como primitivos (no hay capa semántica en variables ni modos de Figma), el modo oscuro se resolvió por duplicación manual de pantallas, y varios componentes clave mezclan Auto Layout con posicionamiento absoluto, lo que los hace frágiles ante contenido real.

**Veredicto global:** excelente como *referencia visual y de flujo* (IA de navegación completa, estados exhaustivos de OTP/registro/llamadas); débil como *fuente de verdad para producción* sin un trabajo previo de tokenización semántica y saneamiento de componentes. Calificación por dimensión al final del documento.

---

## 1. Organización del archivo

### Qué es
Dos páginas: `🌷 Design` (pantallas) y `🌸 Component` (fundamentos + librería). En Design, las pantallas se agrupan por flujo con frames-cabecera de sección (393×189 o más anchos, actúan como títulos de sección sobre el canvas) y dos tiras de preview (`Light Mode` y `Dark Mode`, 7771×1000) que muestran el kit completo en miniatura. En Component, 19 frames temáticos: Color System, Typography, Border Radius & Spacing, Box Shadow, System, Brand, Images, Flag, Icons, Button, Tab Bars, Components, `Light Mode <=> Dark Mode` (tabla de mapeo), un frame `Draft`, y la portada de Behance.

### Por qué funciona
La separación pantallas/librería es el patrón estándar de la industria y permite a un consumidor del kit navegar sin conocimiento previo. Las tiras de preview funcionan como "mapa del sitio" visual. Los frames-cabecera dan ritmo de lectura en el canvas.

### Ventajas
- Onboarding visual inmediato para stakeholders no-diseñadores.
- La tabla `Light Mode <=> Dark Mode` es documentación semántica explícita — rara vez presente en kits de Community.
- Convención de nombres de pantalla consistente en su patrón base: `Flujo _ Estado` (`Sign Up _ OTP Error`, `Setting _ Face ID _ Scanning`).

### Desventajas
- **Duplicación estructural masiva:** todo el set claro está replicado a mano para oscuro (ids `125:*`). Cualquier cambio de flujo debe hacerse dos veces; la deriva ya es visible (el set claro tiene `Loading _ Middle 2`, el oscuro `Loading _ Middle 3`; el claro tiene `Chats _ Conversation 2`, el oscuro `Chats _ Conversation 3`).
- Errores tipográficos propagados por duplicación: `Sign Up _ User Informarion Empty/Typing/Filled` (sic, ×2 modos).
- Nombres duplicados exactos en la misma página (`Chats _ User Information _ Protected Chat` aparece dos veces; dos frames `Security`; múltiples `Wrapper` sin nombre significativo — 24 frames llamados literalmente "Wrapper").
- Un frame `Draft` (4379×1115) permanece publicado en la librería — contenido de trabajo sin limpiar.
- El sufijo `(Ver 2)` para la segunda dirección artística de conversación contamina el namespace en lugar de resolverse con variantes o una página de exploración.

### Escalabilidad / Mantenibilidad
Baja-media. El modelo "duplicar página entera por tema" escala O(n·temas). Con un tercer tema (p. ej. AMOLED true-black) el archivo sería inmanejable. La deriva claro/oscuro ya demostrada es el síntoma clásico.

### Complejidad de implementación Android
Neutral en sí misma, pero **la duplicación claro/oscuro no debe replicarse en código**: en Android esto es un solo árbol de UI + `Theme` con paletas por modo (`isSystemInDarkTheme()` en Compose). La tabla de mapeo del kit es, de hecho, la especificación exacta para escribir el `darkColorScheme()`.

---

## 2. Arquitectura de información y navegación

### Qué es
- **Estructura raíz:** bottom navigation de 4 tabs — `Chats`, `Groups`, `Profile`, `More`.
- **Flujo de entrada:** Loading (4 estados de splash animado) → Introduce (4 pasos de onboarding con ilustraciones) → Login / Sign Up (teléfono + OTP de 4 dígitos con estados Empty/Typing/Filled/Resent/Error) → configuración biométrica opcional (Face ID / Touch ID / PIN, cada una con Scanning/Done) → Home.
- **Profundidad de chat:** Chats → Conversation (dos direcciones de arte: v1 con burbujas sobre fondo gris claro, "(Ver 2)" con otra densidad) → User/Group Information → sub-vistas Media / Links / Documents / Protected Chat → Custom (color/fondo de chat por conversación).
- **Llamadas:** audio y video, 1:1 y grupo, con estados Call/Calling/Video Calling.
- **Social:** Add Friend (con búsqueda), Create Group, Add Members (selección múltiple), popup de acción flotante con estado Hover.
- **More:** Other Apps, Invite Friends, Security (×2), Help Center (+Detail), Term of Service, Privacy Policy, About App.
- **Conceptos futuros** en frames "Wrapper": Smart Replies, Chat Bots, Game Onlines, Live Translation, Scheduled Messages, Anonymous Chat Rooms, Community, Marketplace, Interactive Whiteboard, Collaboration Tools, Dating, AR Chat.

### Por qué funciona
Es la IA canónica de mensajería (WhatsApp/Telegram-like): el usuario nunca está a más de 2 niveles del mensaje. El flujo de auth phone-first con OTP y biometría opcional refleja el estándar actual del sector. Los estados de error/reenvío de OTP están diseñados — no solo el happy path, que es donde la mayoría de kits fallan.

### Ventajas
- Cobertura de estados excepcional: 10 pantallas solo para OTP (login+signup × empty/resent/filled/error), 9 para biometría.
- Las pantallas "Information" separan contenido multimedia por tipo (Media/Links/Documents) con tabs — patrón probado.
- Los conceptos futuros están segregados en Wrappers, no mezclados con el flujo core.

### Desventajas
- Los 12 conceptos futuros son solo portadas (1–2 frames cada uno), no flujos: valor de marketing, no de producto.
- No hay pantallas de estados vacíos del core (lista de chats vacía, sin resultados de búsqueda) ni de errores de red — los edge states diseñados son solo de formularios.
- No hay sistema de Stories, a pesar de ser estándar del género.
- La duplicación `Security`/`Security 2` y `User Information`/`User Information 2` sugiere iteraciones sin consolidar.

### Escalabilidad / Mantenibilidad
La IA escala bien (el tab More absorbe crecimiento). La deuda está en la consolidación de iteraciones.

### Complejidad Android
Directa: 4 destinos raíz = `NavigationBar` + un `NavHost`; la profundidad de chat mapea 1:1 a rutas anidadas. Los estados OTP mapean a un solo composable con `UiState`. Nada exige arquitectura exótica.

---

## 3. Sistema de color

### Qué es
Seis rampas de 10 pasos (50–900): **Blue** (marca, blue-500 `#1565C0`, blue-600 `#135CAF`), **Light Blue** (acento interactivo, light-blue-500 `#40C4FF`), **Red**, **Yellow**, **Green** (semánticos), **Neutral** (con tinte violeta: neutral-900 `#2C2D3A`, neutral-500 `#686A8A`, neutral-300 `#9A9BB1`). Más "Other": White `#FFFFFF`, Black `#292929` (negro de superficie, no #000), y dos gradientes de marca (`gradient-blue`, `gradient-light-blue` — este último `#40C4FF → #03A9F4` a ~128.6°).

**Cada swatch está anotado con su ratio de contraste y badge AAA/AA** sobre el fondo de referencia.

Como variables de Figma solo existen los **primitivos** (`Blue/blue-600`, `Neutral/neutral-900`, `White`…). La capa semántica existe únicamente como *documentación* en la tabla `Light Mode <=> Dark Mode`:

| Rol | Claro | Oscuro |
|---|---|---|
| Background | White | Black `#292929` |
| Background alt | Primary-50 | Primary-900 |
| Texto primario | Neutral-900 | Neutral-50 |
| Texto secundario | Neutral-500 | Neutral-100 |
| Texto acento | Light-Blue-500 | White |
| Borde | Neutral-100 | Neutral-400 |
| Bottom Sheet / Popup | White | Neutral-700 |
| Input | Neutral-900 @5% | White @20% |
| Card | White | Primary-800 |

### Por qué funciona
Rampas de 10 pasos dan espacio para estados hover/pressed/disabled sin inventar colores. El tinte violeta de los neutrales armoniza con el azul de marca (neutrales puros se verían "sucios" junto a este azul). Anotar contraste por swatch convierte la paleta en herramienta de decisión, no solo de inspiración.

### Ventajas
- La tabla Light↔Dark es, literalmente, la especificación de un `ColorScheme` semántico: alguien pensó en roles, no solo en hex.
- Los gradientes están tokenizados como estilos, no pegados ad-hoc.
- Negro de superficie `#292929` en vez de #000: correcta elección para profundidad con sombras en oscuro.

### Desventajas
- **La capa semántica no está implementada como variables:** los componentes referencian primitivos directamente (`Card Message` usa `Neutral/neutral-900` para el nombre, no `text/primary`). Consecuencia: el modo oscuro no puede derivarse por modos de variable, de ahí la duplicación de pantallas y los props `Dark Mode=True/False` en 6 componentes.
- Inconsistencia de alias: existe `White` y `White/White` como variables separadas con el mismo valor — síntoma de librería crecida sin gobernanza.
- Los ratios anotados usan como referencia el blanco; los pasos claros de rampas (50–200) muestran ratios <2:1 con badge igualmente impreso, lo que puede inducir a error de lectura rápida.
- Rojo/Amarillo/Verde no tienen roles documentados (¿error/aviso/éxito?) — se infiere por uso, no por especificación.

### Escalabilidad / Mantenibilidad
Los primitivos escalan bien; la ausencia de capa semántica es el único —pero crítico— cuello de botella. Añadir un tema nuevo hoy = re-duplicar todo.

### Complejidad Android
Baja y agradecida: la tabla de mapeo se transcribe directamente a `lightColorScheme()`/`darkColorScheme()` de Material 3 o a un objeto de tokens propio. Los gradientes son `Brush.linearGradient` con ángulo fijo. El trabajo duro (decidir roles) ya está hecho en la tabla; el kit simplemente no lo ejecutó en Figma.

---

## 4. Tipografía

### Qué es
Una sola familia: **Roboto** (el specimen declara "12 font weights available"). Escala real usada: 12 estilos —

| Tamaño | Peso | Letter spacing |
|---|---|---|
| 40 | SemiBold | — |
| 30 | Bold | +5% |
| 22 | SemiBold | +5% |
| 22 | SemiBold | — |
| 22 | Regular | — |
| 18 | SemiBold / Regular | — |
| 16 | SemiBold / Regular / Light | — |
| 12 | Medium / Regular | — |

Uso observado: nombre de contacto 16 SemiBold; preview de mensaje y timestamps 12 Regular; títulos de pantalla 22; display de onboarding 30/40.

### Por qué funciona
Roboto es neutra, gratuita, variable y omnipresente; SemiBold como peso de énfasis (en lugar de Bold) da jerarquía sin estridencia en tamaños de lista.

### Ventajas
- Escala corta y aprendible; pares Regular/SemiBold por tamaño cubren el 90 % de casos.
- Line-height implícito "normal" evita sorpresas de recorte.

### Desventajas
- **Falta el escalón 14px.** El salto 12→16 obliga a usar 12px para densidades intermedias (previews, timestamps, labels de navbar), y 12px Regular es el mínimo absoluto legible — el kit lo usa masivamente como texto funcional, no solo como caption.
- 16 Light sobre blanco es decorativo; en producción es un riesgo de contraste.
- El specimen promete 12 pesos pero la escala usa 5; documentación aspiracional.
- Letter-spacing +5% solo en dos estilos, sin regla explicable (¿por qué 22 SemiBold lo lleva en una variante y en otra no?).

### Escalabilidad / Mantenibilidad
Media. La ausencia de nombres de rol (Display/Title/Body/Label) en los estilos dificulta el mapeo mental; se referencian por "22px/Semi Bold", acoplando el nombre al valor — renombrar un tamaño rompe la semántica.

### Complejidad Android
Trivial: Roboto es la fuente del sistema. La escala mapea limpiamente a `Typography` de Material 3 (40→displaySmall, 30→headlineSmall, 22→titleLarge, 16→bodyLarge, 12→labelSmall/bodySmall). Único punto de decisión: unidades — el kit está en px; en Android deben ser `sp` con respeto a la escala de fuente del usuario, y el uso extensivo de 12px se convierte en un riesgo de accesibilidad amplificado cuando el usuario sube el tamaño de fuente.

---

## 5. Radio, espaciado, retícula y espacio en blanco

### Qué es
- **Radius:** 3 pasos — 6 / 12 / 18 px (más 100px implícito para píldoras/avatares).
- **Spacing:** 9 pasos — 4 / 8 / 12 / 16 / 24 / 32 / 40 / 48 / 56 px.
- **Retícula:** pantallas de 393×852 (viewport lógico iPhone 15) con margen lateral de 24px (contenido a 345px); listas a sangre completa con padding interno.
- **Espacio en blanco:** listas con gap ~16–24 entre filas (fila de chat ≈66px de alto efectivo); onboarding con grandes vacíos y olas decorativas de fondo.

### Por qué funciona
Escala de espaciado base-4 estricta y corta; tres radios cubren chip/tarjeta/contenedor sin decisiones ad-hoc. El margen 24 es generoso y da un aspecto "aireado" premium coherente con el vacío del onboarding.

### Ventajas
- Todos los valores son múltiplos de 2 y dp-safe.
- La escala está *publicada* en la librería, no solo aplicada — un implementador puede citarla.

### Desventajas
- No hay retícula de columnas documentada (solo margen); los anchos internos se resuelven por Auto Layout, lo cual es aceptable en móvil pero deja sin guía tablet/landscape.
- No se documenta ningún valor de *touch target* mínimo; hay iconos funcionales de 24px sin zona de toque especificada.
- 56 como techo de la escala se queda corto para secciones de onboarding, que usan vacíos ad-hoc mayores.

### Escalabilidad / Mantenibilidad
Buena en móvil vertical; nula previsión responsive (una sola clase de tamaño).

### Complejidad Android
Baja: `4.dp`-based spacing tokens y `RoundedCornerShape(6/12/18)`. La ausencia de specs de touch target obliga al implementador a imponer los 48dp mínimos de Material por su cuenta (`minimumInteractiveComponentSize`).

---

## 6. Elevación, sombras, blur y jerarquía de superficie

### Qué es
Seis sombras nombradas: `Shadow`, `Shadow-Soft`, `Shadow-Dark`, `Shadow 1`, `Shadow 2`, `Shadow 3`. Valores capturados: Shadow-1 `0 4 12 rgba(0,0,0,.06)`, Shadow-3 `0 0 20 rgba(0,0,0,.15)`, Shadow-Dark `0 6 20 rgba(13,10,44,.10)`; el navbar usa una sombra invertida `0 -4 20 rgba(13,10,44,.06)`. Jerarquía de superficie en claro: fondo blanco → tarjetas blancas con sombra suave → header/bloques de marca con gradiente azul → overlays con scrim. En oscuro: fondo `#292929` → tarjetas Primary-800/Neutral-700 → mismas sombras (menos visibles).

**Blur/glassmorphism: prácticamente ausente.** No hay background-blur en componentes core; la estética es de superficies opacas + sombras suaves de gran radio y baja opacidad. El único "vidrio" es sugerido en scrims de overlay.

### Por qué funciona
Sombras de baja opacidad con tinte azulado (`13,10,44`) en vez de negro puro: se integran con la paleta fría y evitan el look "sucio". Renunciar al glassmorphism mantiene el coste de render bajo y el contraste predecible.

### Desventajas
- **Nomenclatura incoherente** (descriptiva y numerada conviviendo): `Shadow-Soft` vs `Shadow 2` no comunica orden de elevación. No hay escala de elevación documentada (z-levels), solo estilos sueltos.
- En modo oscuro las sombras pierden eficacia y el kit no define alternativa (borde sutil o tonal elevation) — las tarjetas oscuras dependen solo de diferencia de tono.

### Escalabilidad / Mantenibilidad
Media-baja; sin una escala ordinal de elevación, cada diseñador nuevo elegirá sombra por estética, no por nivel.

### Complejidad Android
Media: las sombras difusas tipo CSS (radio 20, offset negativo) **no** se reproducen con `elevation` nativa de Compose; requieren `Modifier.shadow` con `spotColor/ambientColor` ajustados o drawBehind custom. La sombra superior del navbar (offset -4) exige dibujo custom. Decisión recomendada al implementar: aproximar con elevation tonal M3 en oscuro.

---

## 7. Librería de componentes

### Inventario (35 sets, 308 variantes)
Los pesos pesados: `Card Setting` (49), `Button Md` (27), `Button` (19), `Card Friend`/`Card Message`/`Card Group Chat`/`Card Group Chat Add Member` (16 c/u), `Card Input` (14), `Input Phone Number` (10), `Navbar` (10), `Avatar` (9), `Socials`/`Input Message`/`Code Verify`/`Button Sm` (8), `Checkbox`/`Icon File Special` (7), `Tab`/`PIN Security` (6), `Image Introduce` (5), `Card User Information`/`Toggle`/`PopUp Add`/`Input Your Name`/`Button Lg`/`Logo E-Chat` (4), `Body`/`Icon Login` (3), `Radio`/`Drop Button`/`Overlay`/`Home Indicator`/`Status Bar`/`Fingerprint` (2), `Record` (1).

### Construcción interna verificada (hallazgos de ingeniería inversa)

**Card Message** (fila de lista de chats): Auto Layout fila, gap 16; avatar 42px círculo; columna de contenido (gap 8) con nombre 16 SemiBold `neutral-900` y preview 12 Regular `neutral-300`. **Pero** la hora (12 Regular `neutral-500`) y el badge de no-leídos (fondo `light-blue-500`, radio 4, min-width 16, padding 2, texto 12 Medium blanco) están **posicionados en absoluto** sobre la fila, fuera del flujo de Auto Layout. Con un nombre largo o una fuente mayor, nombre y hora colisionan; el kit no lo resolvió con `space-between`.

**Navbar**: contenedor 393×100 (16 inferiores reservados al home indicator), blanco, radio 12, sombra invertida. Cuatro items `flex-1` con icono 24 y label 12 Medium (inactivo `neutral-300`). El indicador activo es una **píldora de gradiente 76×70 posicionada en absoluto a x=11 fijo** — no está anclada al item activo; las variantes `Active=Chats/Groups/Profile/More` mueven la píldora a mano. Además, iconos y labels usan offsets porcentuales y translates dentro de cajas de 24px (el icono se dibuja 10px por encima de su caja). Funciona visualmente; estructuralmente es frágil.

**Variantes y props**: la librería usa propiedades de variante reales (`Type=`, `Active=`, `Status=`, `Number=`, `Name=`, `Dark Mode=`), lo que es correcto. Pero el naming de valores degenera en los sets grandes: `Type=1…8` (sin semántica) en `Card Input`; `Icon File Special=Icon File Special7` (valor basura autogenerado); mezcla de ejes (`Type=Default Normal`). `Card Setting` con 49 variantes es un "God component": una fila de ajustes que intenta cubrir toggle/chevron/contador/idioma/logout/report con un solo eje `Type`.

**Dark mode en componentes**: 6 sets llevan `Dark Mode=True/False` como prop booleana — duplicando cada estado × 2. Es la consecuencia directa de no tener variables con modos.

### Por qué funciona
A pesar de la fragilidad interna, la *superficie de API* de la librería es buena: un consumidor encuentra un componente por rol (Card X, Input Y, Button Z por talla Lg/Md/Sm), con estados hover/filled/disabled/error diseñados. La cobertura de estados de `Code Verify` (8) y `Input Phone Number` (10, con banderas y validación) es de nivel producto real.

### Desventajas (síntesis)
1. Auto Layout inconsistente: lo estructural convive con posicionamiento absoluto en los componentes más usados.
2. Ejes de variante sin gobernanza (`Type=4`), valores autogenerados sin renombrar.
3. God-components (Card Setting 49) donde tocaba slot/instance-swap.
4. `Dark Mode` como prop en vez de modo de tema.
5. Sin componentes para estados vacíos/errores de red/toasts/snackbars.

### Escalabilidad / Mantenibilidad
Media-baja. La librería aguanta consumo pasivo (copiar pantallas) pero castigará la extensión: añadir un item al navbar o un tipo a Card Setting exige tocar decenas de variantes a mano.

### Complejidad Android
Media, con trampa: **el código no debe imitar la estructura Figma.** La fila de chat es un `Row` con `weight(1f)` y columna de trailing (hora+badge) — no absolutos. El navbar activo es un `NavigationBar` custom con la píldora animada por `animateDpAsState`/`Modifier.offset` anclada al índice — el diseño lo sugiere pero su construcción no lo implementa. Quien traduzca literalmente el kit heredará sus bugs.

---

## 8. Patrones de UI específicos

### Burbujas de chat
Entrantes: tarjeta blanca, radio ~12–18 con esquina de "cola" reducida, sombra suave, texto neutral-900, timestamp 10–12px gris dentro de la burbuja (abajo-derecha). Salientes: azul (sólido `blue-600`/gradiente según versión), texto blanco, timestamp + doble check de lectura dentro. Dos direcciones de arte ("v1" fondo gris claro con burbujas flotantes; "(Ver 2)" más densa). Media bubbles y attachment sheet diseñados. *Android:* burbujas = `Surface` con `RoundedCornerShape` asimétrico; el doble-check requiere iconografía de estado de entrega en el modelo de mensaje.

### Input de mensaje
Píldora "Type a message…" con iconos de cámara y clip integrados, más FAB circular de gradiente para mic/send (conmutación por estado de texto). Estado Record dedicado (componente `Record`, pantalla "(Ver 2) … _ Record"). *Android:* `TextField` custom + `AnimatedContent` para mic↔send.

### Sistema de historias
**No existe** en el kit. El género lo esperaría; su ausencia es una laguna de alcance, no un error.

### Avatares
`Avatar` 9 variantes; tamaños observados 42 (listas), ~56–80 (headers/perfil), 170 (logo intro); siempre círculo perfecto (radio 100), foto a sangre, sin borde; grupos con stack de avatares y contador `+15`. Estado online no aparece como sistema (no hay dot verde tokenizado). *Android:* `AsyncImage` + `clip(CircleShape)`; el stack de grupo con `offset` negativos.

### Botones
Tres tallas componentizadas (Lg píldora full-width, Md, Sm chip) × estilos: gradiente primario, tinte claro (light-blue-50 con texto light-blue-500), filled oscuro, destructivo (red-50 + texto rojo, p. ej. Logout), circular-icono (send/next; call verde/rojo), estados disabled al ~40 %. Píldora completa (radio 100) en CTAs; radio 12 en botones-tarjeta. *Android:* un solo `EChatButton` con enum de talla/estilo; los gradientes obligan a `Button` con `background(brush)` custom porque `ButtonDefaults` no acepta brush.

### Search bars / Inputs
Auth con underline-style; búsqueda y formularios con filled rounded (input bg = neutral-900 @5 %); teléfono con selector de país (bandera + prefijo); OTP con 4 cajas y estados; formulario de perfil con selects (género, fecha). Labels flotantes no; placeholder gris. *Android:* `OutlinedTextField`/`TextField` custom; el input teléfono necesita picker de país (lib o custom con la hoja `Flag` de 620×1191 del kit).

### Cards de listas
Cuatro familias paralelas (Message/Friend/Group Chat/Group Add Member) que comparten anatomía (avatar+título+subtítulo+trailing) pero son sets independientes sin componente base común — cuadruplican el mantenimiento de un cambio anatómico. *Android:* un solo `EChatListRow` con slots resuelve las cuatro.

### Settings
`Card Setting` (49): icono + label + trailing (chevron/toggle/valor/contador "153"/idioma "English"); grupos con filas destructivas en rojo (Report/Block/Logout). Pantallas de Settings completas para Notification, Security, Face/Touch/PIN. *Android:* una fila `SettingsRow(icon, title, trailing: @Composable)` — el eje `Type` de Figma se disuelve en un slot.

### Perfil
Profile, Profile _ Edit (formulario completo), ProfileViewer implícito en User Information. Header con avatar grande y acciones. *Android:* directo.

### Llamadas
Audio/video, 1:1/grupo, incoming/active, con controles circulares (mute/cámara/flip/colgar) consistentes con el sistema de botones-icono. *Android:* las pantallas mapean a estados del call-engine; el kit no especifica PiP.

---

## 9. Iconografía e imágenes

### Qué es
Iconos del set **Solar** (delatado por los nombres de capa: `Bold / Messages, Conversation / Chat Round Dots`, `Outline / Users / Users Group Two Rounded`, `Outline / Essentional, UI / Hamburger Menu`) en dos estilos: Bold (relleno, estado activo) y Outline (línea, inactivo), caja 24px. Hoja `Icons` (1282×1173) + `Icon File Special` para tipos de archivo + `Socials` (8 marcas: Facebook, Google, Apple, Twitter, WhatsApp, Telegram, Skype, Messenger, Discord, Weechat) + hoja `Flag` de banderas. Fotografía: avatares de stock consistentes (retratos frontales, fondos neutros); ilustraciones de onboarding estilo unDraw con paleta adaptada al azul de marca.

### Ventajas
- Bold/Outline como codificación de estado activo/inactivo es sistemática y barata cognitivamente.
- Solar existe como fuente libre → viabilidad de implementación real.

### Desventajas
- Los iconos están incrustados como vectores sueltos con insets porcentuales raros (herencia del pipeline de exportación), no como componentes con color-token únicos; recolorearlos en Figma es manual.
- Marcas sociales sin guías de uso (tamaños mínimos, safe area).

### Complejidad Android
Baja-media: importar Solar como `ImageVector` (o Iconify); los insets porcentuales del kit se ignoran — se usa el glifo 24dp nativo con `tint`.

---

## 10. Accesibilidad

### Lo que el kit hace bien
- Ratios de contraste + badges AA/AAA **impresos en la propia paleta** — infra de decisión visible.
- Texto principal neutral-900 sobre blanco ≈ 13.5:1; jerarquía de énfasis por peso, no solo por color.
- Estados de error de formularios con mensaje textual, no solo color.

### Riesgos encontrados
1. **12px como texto funcional masivo** (previews, horas, labels de navbar, badges). En Android con escala de fuente de usuario será aún más crítico.
2. Texto secundario `neutral-300 (#9A9BB1)` sobre blanco ≈ 2.7:1 — **falla AA** incluso para texto grande; se usa en previews de mensaje, el contenido más leído de la app.
3. Botones de tinte claro (texto light-blue-500 `#40C4FF` sobre light-blue-50) ≈ <2:1 — CTA ilegibles en luz solar.
4. Badge no-leídos: blanco 12px sobre `#40C4FF` ≈ 1.9:1 — falla.
5. Touch targets: iconos 24px sin zona de toque especificada; el kit no documenta mínimo 44/48.
6. Sin estados de foco/teclado (aceptable en kit móvil, pero nada para accesibilidad por switch/teclado).
7. Chrome iOS-only (status bar, teclados, home indicator) — sin equivalentes Android diseñados.
8. Sin consideración RTL (layouts direccionales con flechas hardcodeadas).

### Veredicto
La *conciencia* de accesibilidad existe (paleta anotada); la *aplicación* es selectiva: los colores de texto secundario y los CTA de tinte violan lo que la propia paleta documenta.

---

## 11. Animaciones (inferidas)

No hay prototipado conectado detectable en la estructura, pero las secuencias de frames declaran intención de movimiento: Loading Start→Middle→Middle 2→Done (logo desplazándose y slogan apareciendo — splash animado); Face/Touch/PIN Scanning→Done (progreso biométrico); OTP Resent (countdown 00:45→00:00 visible); estados Hover del popup Add (escala/elevación); Record (onda de audio). La píldora del navbar pide explícitamente una transición deslizante entre tabs.

*Android:* todo lo inferido es territorio estándar de Compose (`AnimatedVisibility`, `animateDpAsState`, `InfiniteTransition` para el scanning). Nada exige Lottie salvo, quizá, el scanning biométrico.

---

## 12. Calificaciones finales

| Dimensión | Nota | Justificación breve |
|---|---|---|
| Cobertura de flujos | **9/10** | Auth/OTP/biometría/llamadas/ajustes con estados reales; falta empty/error de red y Stories |
| Fundamentos documentados | **8/10** | Paleta anotada con contraste, escalas publicadas, tabla Light↔Dark |
| Implementación de tokens | **3/10** | Solo primitivos; sin semánticos, sin modos; `White` vs `White/White` |
| Construcción de componentes | **4/10** | Auto Layout contaminado con absolutos en los componentes más usados; God-components; `Type=4` |
| Higiene de archivo | **4/10** | Draft publicado, duplicados, typos propagados, 24 "Wrapper", deriva claro/oscuro |
| Accesibilidad aplicada | **5/10** | Conciencia alta, aplicación contradictoria (neutral-300, CTAs de tinte, 12px funcional) |
| Preparación para Android | **6/10** | Valores dp/sp-safe, Roboto, Solar; pero chrome iOS, sombras CSS, estructura no traducible literalmente |
| **Global como fuente de producción** | **5.5/10** | Excelente referencia; requiere tokenización semántica y saneamiento estructural antes de ser fuente de verdad |

---

## 13. Anexo — Hechos verificados (trazabilidad)

- Páginas: `21:122` (Design), `0:1` (Component). 268 frames nivel-1 en Design; 19 en Component.
- Component sets: 35; symbols totales: 308. Recuento por set en §7.
- Variables capturadas: `Blue/blue-500 #1565C0`, `Blue/blue-600 #135CAF`, `Light Blue/light blue-500 #40C4FF`, `Neutral/neutral-900 #2C2D3A`, `Neutral/neutral-500 #686A8A`, `Neutral/neutral-300 #9A9BB1`, `White #FFFFFF`, `Black #292929`, `Shadow-1 (0,4,12,#0000000F)`, `Shadow-3 (0,0,20,#00000026)`, `Shadow-Dark (0,6,20,#0D0A2C1A)`.
- Construcción interna citada de `Card Message` (`71:1091`) y `Navbar` (`71:1009`/`58:2828`) extraída del contexto de diseño real (Auto Layout + absolutos documentados en §7).
- Tabla Light↔Dark transcrita del frame `125:12081`.
- Tipografía del frame `19:3172`; radios/espaciado de `19:2854`; sombras de `19:3895`; sistema iOS de `19:3114`; botones de `58:2166`.
