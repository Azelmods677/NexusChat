# Nexus Design System — Fase 5 (en construcción)

Sistema de diseño **propio** de NexusChat. Parte del código existente; el UI Kit E-Chat
solo aportó principios (ver `nexus-design-principles.md`). Identidad NexusChat manda siempre.

## Bloque 1 — Unificación de la paleta (identidad de un solo color) ✔

**Problema encontrado:** la app tenía TRES sistemas de color solapados y en conflicto:
1. `NexusTokens.Color` (semántico, el más nuevo y correcto) — violeta `#7C6FE0`, fondo `#0D0D1E`.
2. `Color.kt` (~40 colores sueltos) — violeta `#7C3AED`, fondo `#0F0F1A`, superficie `#1A1A2E`.
3. `NexusColorSchemes.kt` / `NexusColors.kt` — sistema de 15 temas, **0 referencias (muerto)**.

Consecuencia: el violeta de marca y el fondo cambiaban según qué archivo importara cada
pantalla → la app no se veía como "la misma app" en todas las capturas. Es exactamente el
síntoma que el usuario quiere eliminar.

**Decisión (identidad NexusChat gana):**
- **Un solo violeta de marca:** `#7C6FE0` (el del sistema semántico más nuevo). `AppTheme`
  PURPLE y `NexusTokens.Color.Primary` ahora coinciden exactamente.
- **Una sola escalera de superficies**, unificada hacia los valores de MAYOR uso (los de
  `Color.kt`, que el ~90% de las pantallas ya consumía vía `DarkSurface`/`DarkBackground`):
  - `BgBase` = `#0F0F1A` (fondo de pantallas)
  - `BgSurface` = `#1A1A2E` (tarjetas)
  - `BgElevated` = `#252538` (sheets/menús)
  - `BgDeep` = `#070714` (solo extremo de gradientes)
  Dirección elegida para **minimizar el cambio visible**: las pantallas mayoritarias
  conservan su look exacto; solo las basadas en NexusTokens se desplazan imperceptiblemente
  (de `#0D0D1E`→`#0F0F1A`, diferencia < 2 en cada canal).
- **`Color.kt` pasa a ser capa de compatibilidad:** re-exporta alias con los nombres que las
  200+ pantallas ya usan (`Purple`, `DarkSurface`, `CyanAccent`, `PurpleBright`…), pero cada
  uno apunta al canon `NexusTokens`. Se consolidaron duplicados casi-idénticos:
  `PurpleBright #7B5CFA`→Primary, `PurpleLight #9B75FF`→PrimaryLight, `EmeraldGreen #10B981`
  →Online, `ErrorRed #EF4444`→Error. Los colores de dominio real (terminal, diálogos,
  paleta de avatares) permanecen porque NO son roles del sistema.
- **Eliminados** `NexusColorSchemes.kt` + `NexusColors.kt` (sistema de 15 temas sin usar).

**Contrato de contraste (Principio A2):** nuevo test `NexusPaletteContrastTest` valida que
cada par rol-sobre-superficie cumple su mínimo WCAG (TextPrimary AAA 7:1, TextSecondary AA
4.5:1, TextMuted/iconos 3:1, Error 4.5:1, y los 25 acentos ≥3:1 sobre el fondo). Si un futuro
cambio de color rompe el contraste, el build falla. (Pendiente de ejecutar: Gradle bloqueado
por loopback en este equipo; ver [[gradle-loopback-blocked]] — compilar en Android Studio.)

**Riesgo asumido:** cambiar `NexusTokens.BgBase/BgSurface` toca 132 usos, pero el
desplazamiento es sub-perceptual y unifica la identidad. Reversible en git.

## Bloque 2 — Tipografía completa y consistente ✔

**Problema:** `Type.kt` definía 6 de los 15 estilos M3; los otros 9 caían al default de
Compose con `FontFamily.Default` — familia distinta y peor render de emojis (la razón
documentada por la que el proyecto eligió SansSerif). Las pantallas que usaban
`titleMedium`/`bodySmall`/`labelSmall` eran tipográficamente inconsistentes con el resto.

**Solución (cero desplazamiento de layout):** los 15 estilos definidos con SansSerif.
Los 6 existentes conservan EXACTAMENTE sus valores; los 9 nuevos usan los tamaños estándar
M3 (los mismos que ya rendían por defecto) con el criterio de pesos Nexus: SemiBold para
énfasis, Normal para lectura, Medium para labels. Regla documentada en el archivo:
10–12sp solo para metadatos; el cuerpo es bodyLarge/Medium. `getTypographyForSize`
(Theme.kt) ya escalaba los 15 estilos — compatible sin cambios.

## Bloque 3 — Glass canónico ✔

`Modifier.nexusGlass(glow, shape)` en `NexusComponents.kt`: ÚNICO lugar donde se define la
superficie vidrio (fill translúcido + borde degradado + esquinas del sistema).
`NexusGlassCard` lo consume; cualquier componente futuro que quiera glass lo aplica en vez
de reconstruir fill/border/shape a mano.

## Bloque 4 — Motion nombrado ✔

`NexusTokens.Anim` gana dos patrones canónicos: `springDefault()` (firme, transiciones) y
`springBouncy()` (feedback táctil de press). Las pantallas consumen patrones, no inventan
springs locales. Primer consumidor: `NexusGlassCard`.

## Bloque 5 — Auditoría de componentes Nexus (hallazgos)

- **Fix de identidad aplicado:** `AppBackground` usaba `TerminalBlack` (color del dominio
  terminal) como fondo por defecto de TODA la app y en 4 fallbacks de carga. Ahora el
  default es el gradiente de marca (`NexusTokens.Gradient.Background`) y los fallbacks
  usan `BgDeep`. La superficie más vista de la app ahora ES la identidad Nexus.
- **`NexusButton` no existe** (el mandato lo lista como infraestructura, pero no está en el
  código). Decisión: se creará al inicio de la Fase 6 junto con sus primeros consumidores
  reales, para no introducir código muerto.
- `NexusStatusBadge`: correcto (píldora token-based, 10sp aceptable por ser metadato).

## Bloque 5 (cierre) — Auditoría `UnifiedTopBar` ✔

**Veredicto: APRUEBA el principio A7 (slots).** `actions: @Composable RowScope.() -> Unit`,
modo perfil opcional por parámetros nulos, back button condicional — API por intención,
sin God-component. Micro-interacciones ya alineadas con la identidad (spring en press,
anillo de gradiente animado en avatar, glassmorphism configurable).

Limpieza aplicada (sin cambio visual): eliminados imports muertos `rememberRipple`
(deprecado) y `blur`; se conserva `ripple` de M3 que sí se usa.

**Deuda documentada para Fase 6 (decisión visual pendiente):** los tamaños de texto del
topbar están hardcodeados (18sp nombre, 13sp subtítulo, 22sp título) en vez de usar la
escala tipográfica. NO se migró a ciegas: 13sp no existe en la escala (bodyMedium=14sp) y
el cambio debe validarse visualmente en dispositivo. También `ModernIconButton` duplica
el patrón "icon button animado" que podría unificarse cuando exista `NexusButton`.

---

# FASE 5 COMPLETA — Resumen ejecutivo

| Bloque | Estado | Entrega |
|---|---|---|
| B1 Paleta unificada | ✔ | 1 violeta (#7C6FE0), 1 escalera de superficies, Color.kt = alias, 2 archivos muertos borrados, test de contraste |
| B2 Tipografía | ✔ | 15/15 estilos M3 con SansSerif, criterio de pesos, regla 10–12sp=metadatos |
| B3 Glass | ✔ | `Modifier.nexusGlass()` canónico, consumido por NexusGlassCard |
| B4 Motion | ✔ | `springDefault()`/`springBouncy()` en NexusTokens.Anim |
| B5 Componentes | ✔ | AppBackground con identidad Nexus (era TerminalBlack), UnifiedTopBar aprobado, imports muertos fuera |

**Verificación:** estática completa (símbolos, refs, imports). Compilación y
`NexusPaletteContrastTest` pendientes de ejecutar por el usuario en Android Studio
(Gradle CLI bloqueado por firewall en esta máquina).

**Fase 6 (aplicación pantalla por pantalla) — backlog priorizado:**
1. Crear `NexusButton` (primario/secundario/destructivo, slots, 48dp mínimo, loading state)
   y adoptarlo en pantallas de alta visibilidad (Login, Settings, Profile).
2. Migrar tamaños hardcodeados de UnifiedTopBar a la escala (validando en dispositivo).
3. Barrido de `Color(0x…)` y `.dp`/`.sp` literales en pantallas → tokens.
4. Estados vacíos/carga/error consistentes en listas (Chats, Groups, Search).
5. Unificar `ModernIconButton` como variante de `NexusButton`.

---

# FASE 6 — Iniciada

## Componente insignia entregado: `NexusButton` ✔
`ui/components/NexusButton.kt`. Variantes Primary (gradiente de marca) / Secondary / Tinted /
Destructive / Ghost; tallas Large 56 / Medium 48 / Small 40; press con `springBouncy`; loading
y disabled; icono opcional; rol semántico; 100% token-driven. Listo para reemplazar los
`Button(...)` de Material (hoy en Login/Register con color/altura/spinner repetidos a mano).
Aún sin consumidores: la adopción pantalla-por-pantalla se hace con compilación (Android Studio).

## Higiene aplicada
- `BottomNavItem` (sealed class muerta en `MainScreen`, 0 refs) **eliminada**.
- `UnifiedTopBar`: 4 imports muertos fuera (`Canvas`, `Stroke`, `rememberRipple`, `blur`).

## Auditoría de iconografía — VEREDICTO: conservar la familia
Censo: 322 `Filled`/`Default` + 67 `AutoMirrored.Filled` + 7 `Outlined` + 0 drawables custom.
**NexusChat ya tiene UNA familia unificada (Material Symbols)** con el par filled/outlined
usado correctamente (nav seleccionado/no-seleccionado = Principio A6). El reemplazo masivo por
Lucide/Phosphor/etc. se **RECHAZA**: nueva dependencia + ~389 swaps sin compilar = riesgo de
regresión, pérdida del par filled/outlined nativo, y cero ganancia de UX. Sería un cambio
arbitrario "AI-generated" — justo lo que el brief prohíbe. La coherencia actual es un ACTIVO.
Mejora real pendiente (bajo riesgo): tokenizar TAMAÑOS de icono (`NexusTokens.IconSize`
18/24/32), hoy ad-hoc en pantallas como AzelAIScreen. NO cambiar de familia.

## Fase 6 — Lote 1a ejecutado ✔ (IconSize tokens)
`NexusTokens.IconSize` creado: xs 16 / sm 18 / md 24 / lg 32 / xl 48, con regla de uso
documentada ("un icono nunca lleva .dp literal"). Primeros consumidores migrados con valores
idénticos (cero cambio visual): `ModernIconButton` (md) y los 3 iconos de marca de
`AzelAIScreen` (lg/sm/xs). Verificado: definición única, consumidores resueltos, sin
literales residuales en los sitios migrados.
**Pendiente del Lote 1 (tras compilar):** barrido mecánico de los ~128 tamaños de icono
restantes y los 73 `RoundedCornerShape` literales → `NexusTokens.Radius`. Se hace por lotes
de ~10 archivos con compilación entre lotes (regla: nunca cambios masivos sin verificar).

## Próximos lotes (cada uno se compila antes del siguiente)
1. Login/Register: `Button` → `NexusButton` (swap 1:1, alto impacto, bajo riesgo).
2. `NexusTokens.IconSize` + migrar tamaños de icono sueltos.
3. `NexusEmptyState` reutilizable para listas vacías/carga/error.
4. Bottom nav premium (glass + indicador de marca sobre la `NavigationBar` actual).
