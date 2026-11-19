import React from "react";

/**
 * Icono actual de NexusChat (ic_launcher_nexus_*): fondo casi negro con halo jade,
 * un anillo de luz y la N de un solo trazo. `draw` va de 0 a 1 y dibuja el anillo y
 * la N como en la animación de entrada de la app; con 1 se ve completo.
 */
export const Logo: React.FC<{ size: number; draw?: number; radius?: number; glow?: boolean }> = ({
  size,
  draw = 1,
  radius = 0.26,
  glow = true,
}) => {
  const ring = 2 * Math.PI * 28.5;
  const nLength = 24 + Math.hypot(25, 24) + 24;
  const ringP = Math.min(1, draw / 0.7);
  const nP = Math.max(0, Math.min(1, (draw - 0.35) / 0.65));
  const id = React.useId().replace(/:/g, "");
  return (
    <svg width={size} height={size} viewBox="0 0 108 108" style={{ borderRadius: size * radius, display: "block", boxShadow: glow ? "0 0 120px #2FB38E55" : undefined }}>
      <defs>
        <linearGradient id={`bg${id}`} x1="54" y1="0" x2="54" y2="108" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#12211D" />
          <stop offset="1" stopColor="#080D0C" />
        </linearGradient>
        <radialGradient id={`halo${id}`} cx="54" cy="54" r="44" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#2FB38E" stopOpacity="0.35" />
          <stop offset="0.7" stopColor="#2FB38E" stopOpacity="0.08" />
          <stop offset="1" stopColor="#2FB38E" stopOpacity="0" />
        </radialGradient>
        <linearGradient id={`ring${id}`} x1="30" y1="28" x2="78" y2="80" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#FFFFFF" />
          <stop offset="0.4" stopColor="#7FD6BC" />
          <stop offset="1" stopColor="#2FB38E" />
        </linearGradient>
        <linearGradient id={`n${id}`} x1="41" y1="42" x2="67" y2="66" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#FFFFFF" />
          <stop offset="1" stopColor="#7FD6BC" />
        </linearGradient>
      </defs>
      <rect width="108" height="108" fill={`url(#bg${id})`} />
      <rect width="108" height="108" fill={`url(#halo${id})`} />
      <circle
        cx="54" cy="54" r="28.5" fill="none" stroke={`url(#ring${id})`} strokeWidth="3.6" strokeLinecap="round"
        strokeDasharray={ring} strokeDashoffset={ring * (1 - ringP)} transform="rotate(-90 54 54)"
      />
      <path
        d="M41.5,66V42L66.5,66V42" fill="none" stroke={`url(#n${id})`} strokeWidth="6.4" strokeLinecap="round" strokeLinejoin="round"
        strokeDasharray={nLength} strokeDashoffset={nLength * (1 - nP)}
      />
    </svg>
  );
};
