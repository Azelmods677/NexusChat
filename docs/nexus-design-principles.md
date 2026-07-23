# Nexus Design Principles — Fase 4

**Qué es esto:** los principios extraídos del análisis del UI Kit E-Chat (Fases 1–3, ver
`design-audit-echat-uikit.md`), filtrados con criterio propio. No se copia nada del kit:
ni layouts, ni colores, ni branding, ni componentes. Solo reglas de calidad aplicables
al frontend existente de NexusChat sin tocar una línea de lógica.

Cada principio indica: de dónde sale, por qué importa, y cómo se traduce a Compose
en NUESTRA app (no en la del kit).

---

## A. Principios adoptados (lo que el kit hace bien y nosotros haremos mejor)

### A1. Los roles semánticos van antes que los colores
El kit documenta una tabla de roles (fondo, texto primario, texto secundario, borde,
tarjeta, input…) con su valor por tema — pero nunca la implementó, y por eso duplicó
130 pantallas. Nosotros tenemos la ventaja de estar en código:

**Regla Nexus:** ningún composable de pantalla referencia un color crudo. Solo roles
(`NexusTheme.colors.textPrimary`, `surface`, `bubbleOwn`, `bubbleOther`…). Un solo árbol
de UI, temas derivados. Si una pantalla necesita un color que no existe como rol,
el rol se añade al sistema primero.

### A2. El contraste es una propiedad del token, no una auditoría posterior
Lo mejor del kit: cada swatch lleva su ratio AA/AAA impreso. Lo peor: luego usa pares
que fallan (texto secundario a 2.7:1, CTAs a <2:1).

**Regla Nexus:** cada PAR rol-sobre-rol del sistema (texto/fondo, badge/fondo, icono/superficie)
se define con su ratio calculado y mínimo exigido: 4.5:1 texto normal, 3:1 texto grande
e iconos funcionales. Los pares que fallen no entran al sistema. Esto es verificable
en un test unitario de la paleta — gratis en Kotlin.

### A3. Estados exhaustivos, no happy path
El kit diseña 10 pantallas solo para OTP (vacío/escribiendo/lleno/reenviado/error).
Esa disciplina es la diferencia entre demo y producto.

**Regla Nexus:** todo componente interactivo define visualmente: default, pressed,
disabled, loading, error, y —en listas— empty. Toda pantalla con datos remotos define:
cargando, contenido, vacío, error de red. Si un estado no está diseñado, no está terminado.
(Nuestra app ya tiene los UiState en los ViewModels; esto es solo darles cara.)

### A4. Escalas cortas y publicadas
Kit: 3 radios, espaciado base-4 de 9 pasos, una familia tipográfica. La restricción
produce consistencia; la escala infinita produce caos.

**Regla Nexus:** NexusTokens es la ÚNICA fuente de valores. Radios: máximo 4 pasos.
Espaciado: escala base-4 cerrada. Tipos: pares Regular/SemiBold por tamaño. Cualquier
`16.dp` literal en una pantalla es un bug de diseño. (Hoy NexusTokens existe pero
convive con valores sueltos — eso se corrige en Fase 6.)

### A5. Jerarquía por peso, no por explosión de tamaños
El kit resuelve casi todo con pares Regular/SemiBold del mismo tamaño.

**Regla Nexus:** el énfasis dentro de un nivel se hace con peso y color de rol,
no inventando un tamaño nuevo. Corregimos además el error del kit: incluimos el
escalón de 14sp (ellos saltan de 12 a 16 y castigan la legibilidad) y 12sp queda
reservado para metadatos reales (timestamps, captions), nunca para contenido.

### A6. El estilo del icono codifica el estado
Filled = activo, outline = inactivo. Sistemático, barato, sin necesidad de color extra.

**Regla Nexus:** adoptamos la convención con NUESTRA iconografía Material Extended ya
presente en el proyecto (`Icons.Filled` / `Icons.Outlined`), aplicada consistentemente
en navegación y toggles de estado.

### A7. API de componentes por rol e intención
El kit expone Button Lg/Md/Sm y familias de Card — el consumidor elige por intención.
Pero degeneró en God-components de 49 variantes.

**Regla Nexus:** componentes por rol (`NexusButton`, `NexusListRow`, `NexusCard`) con
**slots** (`@Composable` lambdas) para el contenido variable — nunca un enum gigante.
Una sola fila de lista con slot trailing reemplaza a las 4 familias de cards del kit.

### A8. Elevación con intención y tinte de marca
Las sombras del kit son suaves, de gran radio y tintadas con su color de marca — por eso
se ven premium y no "sucias".

**Regla Nexus:** definimos una escala ordinal de elevación (0–3 niveles, no 6 sombras
sin orden como el kit) con tinte derivado de NUESTRA paleta. En dark mode la elevación
se expresa por tono de superficie (nuestro fondo es oscuro-primero), no por sombra.

### A9. El motion pertenece al sistema, no a cada pantalla
El kit *sugiere* motion (splash secuencial, scanning, píldora de navbar) pero no lo
especifica, y su construcción (píldora en posición fija) lo hace imposible.

**Regla Nexus:** el sistema define specs de motion reutilizables: UNA curva spring
estándar (ya existe en NexusTokens.Anim — se consolida), duraciones nombradas
(fast/normal), y tres patrones: transición de contenido, indicador deslizante,
aparición de listas. Las pantallas consumen los patrones; no inventan tweens locales.

### A10. El sistema se documenta a sí mismo
Los frames de fundamentos del kit (paleta anotada, escalas, tabla de mapeo) son su
mayor virtud.

**Regla Nexus:** cada token y componente del Nexus DS lleva KDoc con su rol y regla de
uso, y una pantalla interna de galería (debug-only) muestra los componentes en todos
sus estados — nuestro equivalente al "sticker sheet".

---

## B. Anti-principios (errores del kit que prohibimos)

1. **Nunca duplicar estructura por tema.** Un árbol de UI; los temas son datos.
   (El kit duplicó ~130 pantallas y ya tiene deriva entre modos.)
2. **Nunca posicionar en absoluto lo que depende de sus hermanos.** Hora y badge del
   kit flotan en absoluto y colisionan con nombres largos. En Compose: `Row` + `weight`,
   jamás offsets mágicos para contenido variable.
3. **Nunca God-components.** 49 variantes de una fila de settings = deuda. Slots.
4. **Nunca ejes de variante sin semántica.** Un parámetro llamado `type: Int` o un
   enum `Type4` está prohibido; los nombres declaran intención.
5. **El tamaño mínimo de texto no es el caballo de carga.** 12sp solo para metadatos.
6. **La documentación no puede contradecir la aplicación.** Si el sistema declara un
   mínimo de contraste, ningún componente lo viola "porque se ve bonito".
7. **Higiene de fuente de verdad.** Nada de `Draft`, duplicados ni "(Ver 2)" en el
   sistema; las exploraciones viven fuera del código de producción.
8. **Chrome de plataforma nativo.** Status bar, teclado, gestos y back son Android.
   Nada de metáforas iOS heredadas de kits.
9. **Touch targets mínimos de 48dp SIEMPRE**, aunque el glifo mida 24dp. El kit no
   los especifica; nosotros los imponemos con `minimumInteractiveComponentSize`.

---

## C. Lo que explícitamente NO tomamos del kit

- Su paleta (azul claro/blanco aireado). NexusChat ya tiene identidad oscura con
  gradientes y glassmorphism — se refina, no se reemplaza.
- Su tipografía como identidad (Roboto genérica). Evaluaremos en Fase 5 si nuestra
  identidad tipográfica se diferencia.
- Sus layouts de pantalla, su navbar de píldora, sus burbujas, su onboarding.
- Su iconografía (Solar) y sus ilustraciones.
- Su ausencia de Stories/estados vacíos: nuestra app YA tiene Stories, búsqueda,
  terminal, editor, AzelAI — el sistema debe cubrir NUESTRO alcance real, mayor que el del kit.

---

## D. Entrada a la Fase 5 (Nexus Design System propio)

La Fase 5 parte de un inventario del theme layer real del proyecto (ya identificado):
`ui/theme/Color.kt` (~50 colores), `NexusDesignTokens.kt` (`NexusTokens`: Anim, spacing),
`Theme.kt`, `Shape.kt`, `DynamicTheme.kt`, `ColorUtils.kt`, y los componentes `Nexus*`
existentes (`NexusGlassCard`, `NexusStatusBadge`, `UnifiedTopBar`, `AppBackground`…).
Sobre ese inventario se definirá: paleta semántica Nexus (A1+A2), escala tipográfica
con 14sp (A5), elevación/glass (A8), specs de motion (A9) y el catálogo de componentes
por rol con slots (A7). Después, la Fase 6 los aplica pantalla por pantalla sin tocar
ViewModels ni navegación.
