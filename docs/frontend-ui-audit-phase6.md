# NexusChat — Auditoría Global del Frontend (Fase 6.1)

**Rol:** Senior Product Designer, primera revisión completa del producto.
**Alcance:** solo UI/UX. Cero cambios de lógica, arquitectura o datos.
**Método:** métricas estáticas sobre los 72 archivos de `ui/screens` (cobertura total,
datos duros reproducibles con grep) + inspección profunda de las superficies clave
(theme layer completo, MainScreen, Login/Register, HomeScreenRedesigned, ChatScreen parcial,
UnifiedTopBar, NexusComponents, MediaGallery, AboutScreen). Donde no hubo inspección visual
en dispositivo se indica: este equipo no puede compilar (Gradle bloqueado por firewall), así
que las notas de render quedan como hipótesis a validar en Android Studio.

---

## 1. Resumen ejecutivo

NexusChat está en un punto **mucho mejor del que su dueño cree en lo macro, y peor en lo
micro**. Lo macro (sistema): la paleta está unificada con test de contraste, la tipografía
M3 completa, la iconografía es UNA familia bien usada, el fondo por defecto es la identidad,
y existen componentes insignia (`NexusButton`, `NexusGlassCard`, `UnifiedTopBar`) con
microinteracciones spring propias. Eso es nivel de equipo senior y **no debe tocarse**.

Lo micro (aplicación): las pantallas todavía no consumen el sistema de forma disciplinada.
Hay **305 `fontSize` literales**, **131 tamaños de icono a mano**, **73 radios literales**,
**55 gradientes locales en 21 archivos** y **60 `tween()` ad-hoc** conviviendo con los
patrones de motion del sistema. Ninguno de estos es un bug visible por sí solo; en conjunto
son la diferencia entre "app que usa un design system" y "app diseñada por un equipo".

**UI Score global: 6.5/10** — identidad fuerte y coherencia de color excelente; ejecución
tipográfica y de espaciado aún artesanal por pantalla.
**UX Score global: 7/10** — flujos completos y estados de carga presentes; estados vacíos
inconsistentes y monolitos de 1.000+ líneas que frenan la iteración visual.

---

## 2. Auditoría de componentes reutilizables

| Componente | Nota | Veredicto |
|---|---|---|
| `NexusButton` | 9 | Recién creado: token-driven, 5 variantes, spring, loading. **Intocable.** Falta adopción (solo Login/Register). |
| `NexusGlassCard` | 8.5 | Consume `nexusGlass()` y `springBouncy()`. Correcto. Subutilizado (AboutScreen es casi su único cliente). |
| `Modifier.nexusGlass()` | 9 | El glass canónico. **Intocable.** Los 21 archivos con gradientes/glass locales deben migrar aquí. |
| `UnifiedTopBar` | 7.5 | API de slots correcta, identidad fuerte (anillo animado, glass). Debt: 18/13/22sp hardcodeados; `isPressed` manual con `delay(100)` en vez de `collectIsPressedAsState`. |
| `ModernIconButton` | 6 | Funciona, pero duplica el patrón de press de NexusButton con otra curva (`Spring.DampingRatioMediumBouncy` vs `springBouncy`) y el hack de `delay`. Unificar curva. |
| `NexusStatusBadge` | 8 | Correcto y token-based. |
| Bottom nav (MainScreen) | 6 | Funcional y con el par filled/outlined correcto, pero es Material default transparente: la única superficie de navegación sin identidad Nexus (ni glass ni indicador de marca). |
| `SettingsItem` (SettingsComponents) | 7 | Slot-based, reutilizado por 7 pantallas de settings. 2 fontSize literales. |
| `UserAvatar` | 8 | Color determinista por hash + `AvatarPalette` del sistema. Correcto. |
| Burbujas de chat (en ChatScreen) | — | No auditables como componente: viven DENTRO del monolito de 1.820 líneas. Eso ya es el hallazgo. |

**Duplicación detectada (documentar, no arreglar aún):**
- Patrón "press scale + spring" implementado 3 veces con 3 curvas distintas
  (NexusButton ✔ canónico, NexusGlassCard ✔ canónico, ModernIconButton ✖ propio,
  UnifiedTopBar ✖ propio con `delay(100)`).
- Glass/gradiente translúcido re-implementado localmente en ~21 archivos de pantallas
  en vez de `nexusGlass()`/`NexusTokens.Gradient`.
- Spinner de carga en botones: patrón repetido a mano en pantallas que aún no usan
  `NexusButton` (p.ej. EditProfile, diálogos de confirmación).

---

## 3. Auditoría de pantallas (nota 1–10)

Criterio: consumo del sistema (tokens/M3), literales tipográficos, motion, tamaño del
archivo (mantenibilidad UI), estados. Métricas por archivo disponibles en el historial
de la sesión; aquí el veredicto.

### Núcleo diario (lo que el usuario ve el 90% del tiempo)
| Pantalla | Nota | Diagnóstico |
|---|---|---|
| `MainScreen` (shell + bottom nav) | 7 | Limpia tras quitar la sealed muerta. Falta identidad en la nav (HIGH-1). Fondo por capas correcto. |
| `HomeScreenRedesigned` (lista chats) | 6.5 | Fix de avatar/nombre/online aplicado. 18 fontSize literales, 11 animaciones ad-hoc, 891 líneas. Buen bottom sheet de acciones. Estado vacío: presente pero básico. |
| `ChatScreen` | 5.5 | **El monolito: 1.820 líneas** con burbujas, input, attachment, reply, reacciones dentro. 13 fontSize literales, 39 puntos de animación ad-hoc. Funciona, pero cada mejora visual aquí es arqueología. Mayor deuda de mantenibilidad UI del proyecto. |
| `StoriesScreen` | 7.5 | Buen consumo de M3 (16 refs), motion moderado, estados de carga/vacío presentes. |
| `StoryViewerScreen` | 6.5 | 1.016 líneas, 29 refs M3 (bien), pero motion escaso para ser un viewer inmersivo (2 puntos): oportunidad de transiciones de historia. |
| `CallsScreen` | 7 | Correcta, badge de perdidas bien resuelto en nav. |
| `ProfileScreen` | 6.5 | 5 gradientes/animaciones locales; jerarquía visual correcta. |

### Autenticación y entrada
| Pantalla | Nota | Diagnóstico |
|---|---|---|
| `LoginScreen` / `RegisterScreen` | 8 | Ya usan `NexusButton` (CTA con identidad). Quedan 3 fontSize literales c/u y el botón de Google con estilo propio (blanco sobre blanco — revisar contraste del borde en dispositivo). |
| `SplashScreen` | 7 | 8 puntos de animación, coherente con identidad. |
| `TutorialScreen` | 7 | Consumo M3 decente (10). |

### Llamadas
| Pantalla | Nota | Diagnóstico |
|---|---|---|
| `ActiveCallScreen` | 6.5 | 11 fontSize literales; controles circulares consistentes con el sistema de botones-icono. |
| `IncomingCallScreen` | 7.5 | 14 puntos de motion — la más animada del bloque, correcto para su función de alerta. |

### Creación y media
| Pantalla | Nota | Diagnóstico |
|---|---|---|
| `CreateStoryScreen` | 6 | 1.095 líneas, 16 fontSize literales; editor complejo que merece extracción de sub-componentes (solo UI, sin tocar lógica). |
| `MediaGalleryScreen` | 7.5 | Reproductor de video real con zoom añadido en la sesión de limpieza. Correcta. |
| `PhotoViewerScreen` | 7.5 | 8 animaciones, zoom, limpia. |
| `BackgroundPickerScreen` | 6 | 877 líneas, 18 fontSize literales, 15 usos de tokens (mixto). |
| `CodeEditorScreen` / `TerminalScreen` | 7 | Dominio propio (paleta Terminal*) — su divergencia visual es DELIBERADA y correcta. No "arreglar". |

### Seguridad
| Pantalla | Nota | Diagnóstico |
|---|---|---|
| `SecurityScreen` | 7.5 | 20 refs M3, bien estructurada. |
| `AppLockScreen` | 7.5 | 9 animaciones, PIN pad correcto. Touch targets a validar en dispositivo. |
| `OrbotWelcomeScreen` / `TorBrowserScreenNew` / `TorControlScreen` | 6.5 | Funcionales; TorBrowser 1.028 líneas con 13 fontSize literales. |
| `AnonymousModeToggle` | 7.5 | 17 refs M3, motion presente. |

### Settings (bloque de 12 pantallas)
| Pantalla | Nota | Diagnóstico |
|---|---|---|
| `AboutScreen` | 8.5 | **La vitrina del sistema:** 58 usos de NexusTokens, 18 animaciones, NexusGlassCard. Es la prueba de cómo debe verse todo lo demás. |
| `SettingsScreen` | 7 | 11 fontSize literales; estructura de secciones correcta. |
| `AiFeaturesScreen` | 7.5 | Honesta tras retirar los toggles decorativos; 19 refs M3. |
| Resto (Account, Appearance, Notifications, Privacy, Storage, Help, DeviceInfo, FontSize, TranslationLanguage) | 6.5–7.5 | Homogéneas gracias a `SettingsItem`. Literales dispersos (4–11 por archivo). |
| `PremiumScreen` | 6 | 10 fontSize literales, 0 refs M3 directas + 6 animaciones propias: la más "por libre" del bloque. |
| `AzelAIScreen` | 6.5 | 671 líneas, 17 fontSize literales, iconos 16/18/32 a mano; los 3 `Outlined.AutoAwesome` son deliberados (marca IA). |

---

## 4. Auditoría de accesibilidad

**Bien:** contraste de paleta garantizado por test (`NexusPaletteContrastTest`); iconos de
navegación con `contentDescription`; `NexusButton` con rol semántico y alturas 48–56dp;
tipografía escalable por preferencia de usuario (`getTypographyForSize`).

**Riesgos (documentados, no corregidos):**
1. ~305 `fontSize` literales **ignoran la preferencia de tamaño de fuente del usuario** —
   la feature de accesibilidad ya construida (FontSizeScreen) solo afecta a los textos que
   usan la escala M3. Este es el hallazgo de accesibilidad más importante de la auditoría.
2. Iconos con `contentDescription = null` en botones puros-icono fuera de nav (muestreo en
   AzelAIScreen) — auditar TalkBack pantalla por pantalla en Fase 6.2.
3. Touch targets: `ModernIconButton` = 48dp ✔; iconos clicables sueltos en pantallas densas
   (ChatScreen input, story editor) a validar en dispositivo.
4. Botón Google (Login): blanco sobre fondo oscuro ✔, pero el foco/borde en modo claro debe
   validarse visualmente.

## 5. Auditoría de motion

- Sistema: `springDefault()`/`springBouncy()` + duraciones nombradas. ✔
- Realidad: **60 `tween()` y 9 `spring()` locales** en pantallas + 2 curvas propias en
  componentes → 4 "personalidades" de movimiento conviviendo.
- ChatScreen concentra 39 puntos de animación ad-hoc (el timing de burbujas/reacciones no
  responde al sistema).
- Oportunidades de alto valor: transición de historia en StoryViewer, transición
  Home→Chat (shared-element-like con Compose 1.7), aparición escalonada de listas.

## 6. Auditoría tipográfica

- Escala M3 15/15 completa con criterio de pesos. ✔
- **305 literales** esquivan la escala → doble sistema tipográfico de facto.
- 13sp (UnifiedTopBar), 10sp (badges) fuera de escala; regla "10–12sp = solo metadatos"
  documentada pero no ejecutada en pantallas.

## 7. Auditoría de iconografía

**Veredicto ya cerrado (no reabrir):** una sola familia (Material Symbols), 322 Filled +
67 AutoMirrored + 7 Outlined correctos. **Activo del producto.**
Deuda real: **131 tamaños literales** (16/18/20/22/24/28/32/40/48) sin token. Falta
`NexusTokens.IconSize` (sm 18 / md 24 / lg 32) y migración mecánica.

## 8. Auditoría de glass

- Canon: `nexusGlass()` ✔ (definición única, borde degradado, tokens).
- Deuda: ~21 archivos con gradientes/translúcidos locales (55 usos de `Brush.*Gradient`)
  que reconstruyen glass o gradientes de marca a mano. Ninguno es incorrecto visualmente;
  todos son divergencia futura.

---

## 9. DESIGN DEBT (inventario completo — debe desaparecer con el tiempo)

| # | Deuda | Magnitud | Riesgo si persiste |
|---|---|---|---|
| D1 | `fontSize` literales | ~305 en 55 archivos | Rompe accesibilidad (preferencia de fuente) y consistencia |
| D2 | Tamaños de icono literales | 131 | Ritmo visual irregular entre pantallas |
| D3 | `RoundedCornerShape(n.dp)` literales | 73 | Radios divergentes del sistema (4 pasos oficiales) |
| D4 | `tween/spring` locales | 69 | 4 personalidades de motion |
| D5 | Gradientes locales | 55 en 21 archivos | Deriva del gradiente de marca |
| D6 | Monolitos de pantalla | ChatScreen 1820, CreateStory 1095, TorBrowser 1028, StoryViewer 1016 | Cada mejora visual cuesta 5× |
| D7 | Patrón press duplicado | 2 componentes con curva propia + `delay(100)` | Feedback táctil inconsistente |
| D8 | Estados vacíos ad-hoc | Sin componente `NexusEmptyState` | Percepción de app incompleta |
| D9 | Tamaños de texto de UnifiedTopBar | 3 valores fijos | Fuera de la escala y de la preferencia de usuario |
| D10 | Botón Google con estilo propio | Login/Register | Único CTA fuera del sistema |

**Nota:** ya NO hay colores mágicos en pantallas (0 `Color(0x…)` — deuda saldada en Fase 4/5).

---

## 10. Oportunidades priorizadas

### HIGH IMPACT
| Op | Impacto visual | Complejidad | Descripción |
|---|---|---|---|
| H1. Bottom nav premium | Alto (se ve SIEMPRE) | Media | Glass + indicador de marca animado con `springDefault` sobre la NavigationBar existente |
| H2. `NexusTokens.IconSize` + migración | Alto (ritmo global) | Baja (mecánica) | Mata D2 |
| H3. `NexusEmptyState` + adopción en Chats/Search/Calls/Stories | Alto (percepción de acabado) | Baja | Mata D8 |
| H4. Tipografía: literales → escala (por lotes, empezando por Home/Chat/Settings) | Alto + accesibilidad | Media (validar layouts) | Mata D1 gradualmente |

### MEDIUM IMPACT
| Op | Impacto | Complejidad | Descripción |
|---|---|---|---|
| M1. Unificar curva de press (ModernIconButton, UnifiedTopBar → `springBouncy` + `collectIsPressedAsState`) | Medio (coherencia táctil) | Baja | Mata D7 |
| M2. Adopción `NexusButton` en EditProfile, diálogos, Premium, Google button variante Ghost | Medio | Baja | Mata D10 |
| M3. Gradientes locales → `NexusTokens.Gradient`/`nexusGlass()` | Medio | Media | Mata D5 |
| M4. Transiciones StoryViewer + Home→Chat | Medio-alto | Alta | Motion insignia |

### LOW IMPACT (pero necesario)
| Op | Impacto | Complejidad | Descripción |
|---|---|---|---|
| L1. Radios literales → `NexusTokens.Radius` | Bajo | Baja | Mata D3 |
| L2. Extracción UI de monolitos (burbuja/input de ChatScreen como archivos propios, sin tocar lógica) | Bajo visual, alto futuro | Alta | Mata D6, habilita todo lo demás en Chat |
| L3. Pase de `contentDescription` TalkBack | Bajo visual, alto a11y | Baja | Screen readers |

---

## 11. Matriz de prioridad y orden de ejecución recomendado

**Regla:** cada lote se compila en Android Studio antes del siguiente (Gradle CLI bloqueado
en la máquina de desarrollo).

1. **Lote 1 — H2 + L1** (mecánico, bajo riesgo): IconSize + Radius tokens y migración.
2. **Lote 2 — H3**: `NexusEmptyState` (glass + icono + texto + acción opcional) y adopción
   en las 4 listas principales.
3. **Lote 3 — H1**: bottom nav premium (la mejora más visible del proyecto).
4. **Lote 4 — M1 + M2**: coherencia táctil + NexusButton en el resto de CTAs.
5. **Lote 5 — H4**: tipografía por lotes (Home → Settings → Chat), validando en dispositivo.
6. **Lote 6 — M3**: gradientes → tokens.
7. **Lote 7 — M4**: motion insignia (transiciones).
8. **Lote 8 — L2 + L3**: extracción de monolitos y pase de accesibilidad.

## 12. Qué es excelente y NO debe tocarse
- El theme layer completo (paleta unificada + test de contraste + tipografía 15/15).
- `NexusButton`, `nexusGlass()`, patrones de motion del sistema.
- La familia de iconos y su convención filled/outlined.
- `AboutScreen` como vitrina de referencia.
- La divergencia deliberada de Terminal/CodeEditor (paleta propia de dominio).
- Los 25 acentos de usuario (`AppTheme`) y su integración con M3.
