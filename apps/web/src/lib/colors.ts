export const GROUP_COLORS = [
  { name: "Azul", value: "#2553E6" },
  { name: "Âmbar", value: "#FBAF24" },
  { name: "Verde", value: "#167A3C" },
  { name: "Navy", value: "#111B35" },
  { name: "Violeta", value: "#6D3CE0" },
] as const;

export const DEFAULT_GROUP_COLOR = GROUP_COLORS[0].value;

export function textOn(hex: string | null | undefined): string {
  const h = (hex ?? DEFAULT_GROUP_COLOR).replace("#", "");
  if (h.length !== 6) return "#ffffff";
  const r = parseInt(h.slice(0, 2), 16);
  const g = parseInt(h.slice(2, 4), 16);
  const b = parseInt(h.slice(4, 6), 16);
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return luminance > 0.6 ? "#111b35" : "#ffffff";
}

export function groupColor(hex: string | null | undefined): string {
  return hex ?? DEFAULT_GROUP_COLOR;
}
