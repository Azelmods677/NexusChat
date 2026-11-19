import React from "react";
import {
  AbsoluteFill,
  Easing,
  interpolate,
  OffthreadVideo,
  Sequence,
  spring,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { loadFont } from "@remotion/google-fonts/SpaceGrotesk";
import { Logo } from "./Logo";
import { loadFont as loadBody } from "@remotion/google-fonts/Inter";
import { loadFont as loadMono } from "@remotion/google-fonts/JetBrainsMono";

const { fontFamily: display } = loadFont();
const { fontFamily: body } = loadBody();
const { fontFamily: mono } = loadMono();

const C = {
  bg: "#07080A",
  surface: "#171A1D",
  brand: "#2FB38E",
  brandSoft: "#7FE0C2",
  text: "#ECEEF0",
  muted: "#A6ACB3",
  coral: "#E07A5F",
  line: "#24282C",
};

const FPS = 30;
const sec = (s: number) => Math.round(s * FPS);
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const ease = Easing.bezier(0.16, 1, 0.3, 1);

/** Zona de la grabación que se difumina (en píxeles del móvil, 1080×2340). */
type Blur = { x: number; y: number; w: number; h: number };

type Scene = {
  clip: string;
  kicker: string;
  title: string;
  lines: string[];
  /** Etiquetas que flotan alrededor del móvil, en profundidad. */
  chips: string[];
  seconds: number;
  from?: number;
  blur?: Blur[];
};

/**
 * Escenas en orden. Cada clip es una grabación real del móvil en public/clips.
 * Lo que enseñe nombres o fotos de otras personas se tapa con `blur`.
 */
export const SCENES: Scene[] = [
  {
    clip: "d.mp4", from: 53, kicker: "Privacidad primero", title: "Modo anónimo con Tor integrado",
    lines: ["Tú eliges qué pasa por Tor", "Verificación en dos pasos", "Bloqueo de la app con PIN"],
    chips: ["🧅 Tor", "🕶️ Modo anónimo", "🔐 2FA"], seconds: 14,
  },
  {
    clip: "f.mp4", from: 3, kicker: "Chats vivos", title: "Enlaces que se tocan",
    lines: ["Enlaces, correos y teléfonos en azul", "Azel Assistant te enseña la app", "Todo cifrado en tu teléfono"],
    chips: ["🔗 Enlaces", "✨ Azel", "🔒 Cifrado"], seconds: 13,
  },
  {
    clip: "a.mp4", from: 36, kicker: "Llega a más gente", title: "Canales y difusiones",
    lines: ["Crea tu canal con @nombre", "Síguelos y explora los populares", "Un mensaje a muchos, cada uno en privado"],
    chips: ["📣 Canales", "📨 Difusión", "🧭 Explorar"], seconds: 11,
  },
  {
    clip: "a.mp4", from: 6, kicker: "Hazla tuya", title: "Fondos animados",
    lines: ["Vídeo en bucle de fondo", "Para toda la app o para cada chat", "Con la opacidad que quieras"],
    chips: ["🎬 Vídeo", "🖼️ Imagen", "🌈 Degradado"], seconds: 16,
  },
  {
    clip: "f.mp4", from: 30, kicker: "Estudio de diseño", title: "Diseña cada detalle",
    lines: ["Burbujas, campo y barra inferior", "Vista previa en directo", "Decenas de estilos"],
    chips: ["🫧 Burbujas", "⌨️ Campo", "🧭 Barra"], seconds: 16,
  },
  {
    clip: "d.mp4", from: 0, kicker: "A tu estilo", title: "Tu color, tu letra",
    lines: ["Color de acento a elegir", "Aros de color en los avatares", "Tamaño de letra ajustable"],
    chips: ["🎨 Colores", "⭕ Aros", "🔠 Letra"], seconds: 12,
  },
  {
    clip: "b.mp4", from: 34, kicker: "Comparte el momento", title: "Historias, canales y más",
    lines: ["Historias de 24 horas", "Canales y difusiones a un toque", "Con tu fondo animado detrás"],
    chips: ["⏳ 24 h", "📣 Canales", "🎶 Música"], seconds: 7,
  },
  {
    clip: "d.mp4", from: 27, kicker: "Inteligencia artificial", title: "Azel IA y sus agentes",
    lines: ["Mejora textos y explica conceptos", "Agentes que investigan por ti", "Tú pones tu propia clave"],
    chips: ["✨ IA", "🤖 Agentes", "🌍 Traducir"], seconds: 12,
  },
  {
    clip: "d.mp4", from: 15, kicker: "Para los curiosos", title: "Terminal Kali y editor de código",
    lines: ["Kali Linux real, sin root", "Editor de código en el móvil", "Ayuda integrada"],
    chips: ["🐧 Kali", "$ terminal", "</> Código"], seconds: 12,
  },
  {
    clip: "d.mp4", from: 39, kicker: "Todo bajo control", title: "Ajustes completos",
    lines: ["Almacenamiento y datos", "Idioma de traducción", "Notificaciones a tu medida"],
    chips: ["💾 Datos", "🌍 Idiomas", "🔔 Avisos"], seconds: 11,
  },
  {
    clip: "a.mp4", from: 72, kicker: "Ya disponible", title: "NexusChat 8.5",
    lines: ["Gratis y de código abierto", "Hecha con Jetpack Compose", "Para Android 12 o superior"],
    chips: ["🆕 8.5.0", "🤖 Android", "💚 Gratis"], seconds: 7,
  },
];

const COLD = sec(7);
const LOGO = sec(5);
const WALL = sec(9);
const OUTRO = sec(9);
const sceneFrames = (s: Scene) => sec(s.seconds);
const SCENES_TOTAL = SCENES.reduce((n, s) => n + sceneFrames(s), 0);
export const TRAILER_FRAMES = COLD + LOGO + SCENES_TOTAL + WALL + OUTRO;

// ── Fondo ────────────────────────────────────────────────────────────────────

/** Fondo vivo: dos luces que se mueven despacio, rejilla en perspectiva y grano. */
const Backdrop: React.FC = () => {
  const frame = useCurrentFrame();
  const a = Math.sin(frame / 90) * 140;
  const b = Math.cos(frame / 120) * 160;
  return (
    <AbsoluteFill style={{ backgroundColor: C.bg, overflow: "hidden" }}>
      <div style={{ position: "absolute", width: 1200, height: 1200, borderRadius: "50%", left: 900 + a, top: -380 + b, filter: "blur(170px)", background: `radial-gradient(circle, ${C.brand}50, transparent 65%)` }} />
      <div style={{ position: "absolute", width: 1000, height: 1000, borderRadius: "50%", left: -320 - b, top: 520 + a / 2, filter: "blur(170px)", background: `radial-gradient(circle, ${C.coral}26, transparent 65%)` }} />
      {/* Suelo en perspectiva que avanza: da sensación de escenario. */}
      <div style={{ position: "absolute", left: -600, right: -600, bottom: -200, height: 700, transform: "perspective(900px) rotateX(70deg)", transformOrigin: "bottom",
        backgroundImage: `linear-gradient(${C.brand}22 1px, transparent 1px), linear-gradient(90deg, ${C.brand}22 1px, transparent 1px)`,
        backgroundSize: "80px 80px", backgroundPosition: `0 ${(frame * 2) % 80}px`,
        maskImage: "linear-gradient(to top, black, transparent 85%)" }} />
      <AbsoluteFill style={{ opacity: 0.06, backgroundImage: "url(\"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='160' height='160'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2' seed='" + (frame % 6) + "'/></filter><rect width='100%' height='100%' filter='url(%23n)'/></svg>\")" }} />
      <AbsoluteFill style={{ background: "radial-gradient(ellipse at center, transparent 55%, rgba(0,0,0,0.65))" }} />
    </AbsoluteFill>
  );
};

// ── Apertura ─────────────────────────────────────────────────────────────────

const ColdOpen: React.FC = () => {
  const frame = useCurrentFrame();
  const beats = ["Tus mensajes.", "Tus reglas.", "Nadie más."];
  const per = sec(2);
  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
      {beats.map((t, i) => {
        const local = frame - i * per;
        if (local < 0 || local > per + 6) return null;
        const inP = interpolate(local, [0, 12], [0, 1], { ...clamp, easing: ease });
        const outP = interpolate(local, [per - 8, per + 6], [0, 1], clamp);
        const last = i === beats.length - 1;
        return (
          <div key={t} style={{ position: "absolute", fontFamily: display, fontWeight: 700, fontSize: last ? 180 : 150, letterSpacing: -4,
            color: last ? C.brand : C.text, opacity: inP * (1 - outP),
            transform: `translateY(${(1 - inP) * 50}px) scale(${1 + outP * 0.15})`, filter: `blur(${outP * 14}px)`,
            textShadow: last ? `0 0 60px ${C.brand}88` : "none" }}>
            {t}
          </div>
        );
      })}
      {/* Línea de escaneo que cruza la pantalla. */}
      <div style={{ position: "absolute", left: 0, right: 0, height: 2, top: interpolate(frame, [0, COLD], [0, 1080]), background: `linear-gradient(90deg, transparent, ${C.brand}, transparent)`, opacity: 0.5 }} />
    </AbsoluteFill>
  );
};

/** El logo aparece con un barrido de luz y el nombre se ensambla letra a letra. */
const LogoReveal: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const p = spring({ frame, fps, config: { damping: 14 } });
  const sweep = interpolate(frame, [10, 40], [-120, 220], clamp);
  const out = interpolate(frame, [LOGO - 12, LOGO], [1, 0], clamp);
  const name = "NexusChat".split("");
  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", opacity: out }}>
      <div style={{ position: "relative", width: 220, height: 220, borderRadius: 56, overflow: "hidden", transform: `scale(${p}) rotateY(${(1 - p) * 90}deg)`, boxShadow: `0 0 140px ${C.brand}66` }}>
        <Logo size={220} draw={interpolate(frame, [4, 34], [0, 1], clamp)} glow={false} />
        <div style={{ position: "absolute", top: 0, bottom: 0, width: 70, left: `${sweep}%`, transform: "skewX(-20deg)", background: "linear-gradient(90deg, transparent, rgba(255,255,255,0.55), transparent)" }} />
      </div>
      <div style={{ display: "flex", marginTop: 44 }}>
        {name.map((ch, i) => {
          const q = spring({ frame: frame - 8 - i * 2, fps, config: { damping: 15 } });
          return (
            <span key={i} style={{ fontFamily: display, fontSize: 140, fontWeight: 700, letterSpacing: -3, color: C.text, display: "inline-block", opacity: q, transform: `translateY(${(1 - q) * 60}px) rotateX(${(1 - q) * 80}deg)` }}>
              {ch}
            </span>
          );
        })}
      </div>
      <div style={{ fontFamily: mono, fontSize: 28, color: C.brandSoft, marginTop: 8, letterSpacing: 6, opacity: interpolate(frame, [30, 45], [0, 1], clamp) }}>
        VERSIÓN 8.5
      </div>
    </AbsoluteFill>
  );
};

// ── Escenas con el móvil ─────────────────────────────────────────────────────

/** Móvil 3D: entra con giro de cámara, flota, y un reflejo recorre el cristal. */
const Phone: React.FC<{ scene: Scene; side: 1 | -1 }> = ({ scene, side }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const total = sceneFrames(scene);
  const enter = spring({ frame, fps, config: { damping: 20, mass: 1.1 } });
  const exit = interpolate(frame, [total - 14, total], [0, 1], { ...clamp, easing: Easing.in(Easing.cubic) });
  const drift = interpolate(frame, [0, total], [0, 1]);
  const screenW = 380;
  const screenH = (screenW * 2340) / 1080;
  const scale = screenW / 1080;
  const rotY = interpolate(enter, [0, 1], [side * 55, side * 14]) - side * drift * 10 + exit * side * -40;
  const rotX = interpolate(enter, [0, 1], [18, 4]) + Math.sin(frame / 50) * 2;
  const glare = interpolate(frame % sec(6), [0, sec(2)], [-60, 160], clamp);
  return (
    <div style={{ position: "absolute", top: 540 - screenH / 2 - 16 + Math.sin(frame / 40) * 10,
      [side === 1 ? "right" : "left"]: 230,
      transform: `perspective(2000px) translateZ(${interpolate(enter, [0, 1], [-600, 0]) - exit * 400}px) rotateY(${rotY}deg) rotateX(${rotX}deg)`,
      opacity: enter * (1 - exit), transformStyle: "preserve-3d" } as React.CSSProperties}>
      <div style={{ padding: 15, borderRadius: 66, background: "linear-gradient(145deg,#3a4046,#0c0e10 60%)",
        boxShadow: `0 80px 140px rgba(0,0,0,0.7), 0 0 0 2px #3a4046, inset 0 0 0 2px #15181b, 0 0 120px ${C.brand}25` }}>
        <div style={{ width: screenW, height: screenH, borderRadius: 52, overflow: "hidden", position: "relative", background: "#000" }}>
          <OffthreadVideo src={staticFile(`clips/${scene.clip}`)} startFrom={sec(scene.from ?? 0)} muted style={{ width: "100%", height: "100%", objectFit: "cover" }} />
          {(scene.blur ?? []).map((r, i) => (
            <div key={i} style={{ position: "absolute", left: r.x * scale, top: r.y * scale, width: r.w * scale, height: r.h * scale, backdropFilter: "blur(20px)", WebkitBackdropFilter: "blur(20px)", background: "rgba(16,18,20,0.3)" }} />
          ))}
          <div style={{ position: "absolute", inset: 0, background: `linear-gradient(115deg, transparent ${glare - 20}%, rgba(255,255,255,0.12) ${glare}%, transparent ${glare + 20}%)` }} />
          {/* Isla de la cámara. */}
          <div style={{ position: "absolute", top: 16, left: "50%", width: 110, height: 30, marginLeft: -55, borderRadius: 20, background: "#000" }} />
        </div>
      </div>
      {/* Reflejo en el suelo. */}
      <div style={{ position: "absolute", left: 30, right: 30, bottom: -70, height: 60, borderRadius: "50%", background: `radial-gradient(ellipse, ${C.brand}40, transparent 70%)`, filter: "blur(20px)" }} />
    </div>
  );
};

/** Etiquetas de cristal que flotan delante y detrás del móvil. */
const Chips: React.FC<{ scene: Scene; side: 1 | -1 }> = ({ scene, side }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const total = sceneFrames(scene);
  const out = interpolate(frame, [total - 12, total], [1, 0], clamp);
  const spots = [
    { x: 0.62, y: 0.2, z: 120 },
    { x: 0.86, y: 0.52, z: 220 },
    { x: 0.6, y: 0.78, z: 60 },
  ];
  return (
    <>
      {scene.chips.map((label, i) => {
        const s = spots[i % spots.length];
        const p = spring({ frame: frame - 30 - i * 12, fps, config: { damping: 14 } });
        const x = side === 1 ? s.x : 1 - s.x;
        return (
          <div key={label} style={{ position: "absolute", left: x * 1920 - 150, top: s.y * 1080 + Math.sin((frame + i * 30) / 35) * 12,
            transform: `perspective(1400px) translateZ(${s.z * p}px) scale(${0.6 + p * 0.4})`, opacity: p * out,
            padding: "16px 26px", borderRadius: 18, fontFamily: body, fontSize: 28, fontWeight: 600, color: C.text,
            background: "rgba(23,26,29,0.62)", border: `1px solid ${C.brand}55`, backdropFilter: "blur(14px)",
            boxShadow: `0 20px 50px rgba(0,0,0,0.45), 0 0 30px ${C.brand}22`, whiteSpace: "nowrap" }}>
            {label}
          </div>
        );
      })}
    </>
  );
};

/** Rótulo: número, etiqueta, título palabra a palabra y viñetas. */
const Caption: React.FC<{ scene: Scene; index: number; side: 1 | -1 }> = ({ scene, index, side }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const total = sceneFrames(scene);
  const out = interpolate(frame, [total - 14, total], [1, 0], clamp);
  const words = scene.title.split(" ");
  const kick = spring({ frame, fps, config: { damping: 20 } });
  const bar = interpolate(frame, [0, 20], [0, 1], { ...clamp, easing: ease });
  return (
    <div style={{ position: "absolute", [side === 1 ? "left" : "right"]: 150, top: 230, width: 800, opacity: out, textAlign: side === 1 ? "left" : "right" } as React.CSSProperties}>
      <div style={{ fontFamily: display, fontSize: 200, fontWeight: 700, lineHeight: 0.8, color: "transparent", WebkitTextStroke: `2px ${C.brand}2a`, position: "absolute", top: -175, [side === 1 ? "left" : "right"]: -20, opacity: kick } as React.CSSProperties}>
        {String(index + 1).padStart(2, "0")}
      </div>
      <div style={{ display: "inline-flex", alignItems: "center", gap: 14, fontFamily: mono, fontSize: 24, letterSpacing: 4, textTransform: "uppercase", color: C.brand, opacity: kick, position: "relative" }}>
        <span style={{ width: 60 * bar, height: 2, background: C.brand }} />
        {scene.kicker}
      </div>
      <div style={{ fontFamily: display, fontSize: 86, lineHeight: 1.02, fontWeight: 700, color: C.text, marginTop: 26, letterSpacing: -2, position: "relative" }}>
        {words.map((w, i) => {
          const p = spring({ frame: frame - 6 - i * 3, fps, config: { damping: 16 } });
          return (
            <span key={i} style={{ display: "inline-block", marginRight: 22, opacity: p, transform: `translateY(${(1 - p) * 50}px)`, filter: `blur(${(1 - p) * 8}px)` }}>
              {w}
            </span>
          );
        })}
      </div>
      <div style={{ marginTop: 44 }}>
        {scene.lines.map((line, i) => {
          const p = spring({ frame: frame - 28 - i * 10, fps, config: { damping: 18 } });
          return (
            <div key={i} style={{ display: "flex", flexDirection: side === 1 ? "row" : "row-reverse", alignItems: "center", gap: 18, fontFamily: body, fontSize: 36, color: C.muted, marginBottom: 20, opacity: p, transform: `translateX(${(1 - p) * -30 * side}px)` }}>
              <span style={{ width: 12, height: 12, borderRadius: 6, background: C.brand, boxShadow: `0 0 18px ${C.brand}` }} />
              {line}
            </div>
          );
        })}
      </div>
    </div>
  );
};

/** Cortina de marca entre escenas: una franja verde cruza la pantalla. */
const Wipe: React.FC = () => {
  const frame = useCurrentFrame();
  const x = interpolate(frame, [0, 14], [-30, 130], { ...clamp, easing: Easing.inOut(Easing.cubic) });
  return (
    <AbsoluteFill style={{ pointerEvents: "none" }}>
      <div style={{ position: "absolute", top: -200, bottom: -200, width: 260, left: `${x}%`, transform: "skewX(-18deg)", background: `linear-gradient(90deg, transparent, ${C.brand}cc, ${C.brandSoft}, ${C.brand}cc, transparent)`, filter: "blur(6px)", opacity: 0.9 }} />
    </AbsoluteFill>
  );
};

const Hud: React.FC<{ index: number }> = ({ index }) => (
  <>
    <div style={{ position: "absolute", left: 150, top: 80, display: "flex", alignItems: "center", gap: 16 }}>
      <Logo size={48} glow={false} />
      <span style={{ fontFamily: display, fontSize: 28, fontWeight: 700, color: C.text }}>NexusChat</span>
      <span style={{ fontFamily: mono, fontSize: 20, color: C.muted }}>8.5</span>
    </div>
    <div style={{ position: "absolute", right: 150, top: 92, fontFamily: mono, fontSize: 20, color: C.muted, letterSpacing: 2 }}>
      {String(index + 1).padStart(2, "0")} / {String(SCENES.length).padStart(2, "0")}
    </div>
    <div style={{ position: "absolute", left: 150, right: 150, bottom: 70, height: 3, background: C.line, borderRadius: 2 }}>
      <div style={{ width: `${((index + 1) / SCENES.length) * 100}%`, height: "100%", background: C.brand, borderRadius: 2, boxShadow: `0 0 12px ${C.brand}` }} />
    </div>
  </>
);

// ── Mosaico final y cierre ───────────────────────────────────────────────────

const WALL_ITEMS = [
  "Cifrado E2EE", "Tor integrado", "Chats secretos", "Modo anónimo", "Mensajes temporales", "Ver una vez",
  "Grupos", "Canales", "Difusiones", "Historias", "Llamadas", "Encuestas",
  "Stickers y GIF", "Traductor", "Azel IA", "Agentes", "Descargador", "Editor de código",
  "Terminal Linux", "Temas", "Copias cifradas", "Bloqueo con huella", "Mensajes programados", "Guardados",
];

const FeatureWall: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const cols = 6;
  const tilt = interpolate(frame, [0, WALL], [28, 14]);
  const pan = interpolate(frame, [0, WALL], [60, -60]);
  const out = interpolate(frame, [WALL - 12, WALL], [1, 0], clamp);
  const head = spring({ frame, fps, config: { damping: 18 } });
  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", opacity: out }}>
      <div style={{ position: "absolute", top: 90, fontFamily: display, fontSize: 72, fontWeight: 700, color: C.text, opacity: head, letterSpacing: -2 }}>
        Todo en <span style={{ color: C.brand }}>una sola app</span>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: `repeat(${cols}, 260px)`, gap: 22, marginTop: 120,
        transform: `perspective(1600px) rotateX(${tilt}deg) rotateZ(-4deg) translateX(${pan}px)`, transformStyle: "preserve-3d" }}>
        {WALL_ITEMS.map((item, i) => {
          const row = Math.floor(i / cols);
          const col = i % cols;
          const p = spring({ frame: frame - 6 - (row + col) * 3, fps, config: { damping: 14 } });
          const lit = Math.floor(frame / 6) % WALL_ITEMS.length === (i * 7) % WALL_ITEMS.length;
          return (
            <div key={item} style={{ height: 110, borderRadius: 20, display: "flex", alignItems: "center", justifyContent: "center",
              fontFamily: body, fontWeight: 600, fontSize: 28, color: lit ? "#06110d" : C.text, textAlign: "center", padding: "0 14px",
              background: lit ? C.brand : "rgba(23,26,29,0.75)", border: `1px solid ${lit ? C.brand : C.line}`,
              transform: `translateZ(${(1 - p) * -400}px)`, opacity: p, boxShadow: lit ? `0 0 50px ${C.brand}88` : "0 20px 40px rgba(0,0,0,0.4)" }}>
              {item}
            </div>
          );
        })}
      </div>
    </AbsoluteFill>
  );
};

const Outro: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const p = spring({ frame, fps, config: { damping: 16 } });
  const cta = spring({ frame: frame - 24, fps, config: { damping: 12 } });
  const pulse = 1 + Math.sin(frame / 8) * 0.015;
  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center" }}>
      <div style={{ transform: `scale(${p})` }}>
        <Logo size={160} draw={interpolate(frame, [0, 30], [0.2, 1], clamp)} />
      </div>
      <div style={{ fontFamily: display, fontSize: 110, fontWeight: 700, color: C.text, marginTop: 34, opacity: p, letterSpacing: -3 }}>
        Habla libre.
      </div>
      <div style={{ fontFamily: body, fontSize: 40, color: C.muted, marginTop: 6, opacity: p }}>NexusChat 8.5 · gratis y de código abierto</div>
      <div style={{ marginTop: 56, padding: "24px 50px", borderRadius: 999, background: C.brand, color: "#06110d", fontFamily: display, fontSize: 40, fontWeight: 700,
        transform: `scale(${interpolate(cta, [0, 1], [0.7, 1]) * pulse})`, opacity: cta, boxShadow: `0 0 80px ${C.brand}88` }}>
        Descárgala en github.com/Azelmods677/NexusChat
      </div>
    </AbsoluteFill>
  );
};

export const Trailer: React.FC = () => {
  let start = COLD + LOGO;
  return (
    <AbsoluteFill>
      <Backdrop />
      <Sequence durationInFrames={COLD}><ColdOpen /></Sequence>
      <Sequence from={COLD} durationInFrames={LOGO}><LogoReveal /></Sequence>
      {SCENES.map((scene, i) => {
        const from = start;
        start += sceneFrames(scene);
        const side: 1 | -1 = i % 2 === 0 ? 1 : -1;
        return (
          <React.Fragment key={scene.clip}>
            <Sequence from={from} durationInFrames={sceneFrames(scene)}>
              <Hud index={i} />
              <Caption scene={scene} index={i} side={side} />
              <Phone scene={scene} side={side} />
              <Chips scene={scene} side={side} />
            </Sequence>
            <Sequence from={from - 7} durationInFrames={15}><Wipe /></Sequence>
          </React.Fragment>
        );
      })}
      <Sequence from={start} durationInFrames={WALL}><FeatureWall /></Sequence>
      <Sequence from={start - 7} durationInFrames={15}><Wipe /></Sequence>
      <Sequence from={start + WALL} durationInFrames={OUTRO}><Outro /></Sequence>
    </AbsoluteFill>
  );
};
