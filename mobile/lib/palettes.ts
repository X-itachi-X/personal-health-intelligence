/** Light matte-blue palette (Pantone-inspired) + glass tokens. Blue family only. */
export type ThemeColors = {
  bg: string;
  bgSecondary: string;
  bgTertiary?: string;
  surface: string;
  surfaceMuted: string;
  surfaceElevated: string;
  surfaceInset: string;
  glassFill: string;
  glassFillMuted: string;
  glassBorder: string;
  glassHighlight: string;
  ambientA: string;
  ambientB: string;
  ambientC: string;
  primary: string;
  primaryDark: string;
  primaryLight: string;
  text: string;
  textMuted: string;
  textSoft: string;
  border: string;
  borderLight: string;
  danger: string;
  dangerLight: string;
  dangerBorder: string;
  success: string;
  successLight: string;
  successBorder: string;
  warning: string;
  warningLight: string;
  google: string;
  overlay: string;
  overlayHeavy: string;
  shadow: string;
  white: string;
  adminBadge: string;
};

export type GlassTokens = {
  surfaceAlpha: number;
  chromeAlpha: number;
  nestedAlpha: number;
  blurIntensity: number;
  tint: "light" | "dark";
};

/** Reference swatches — dusty periwinkle, icy cyan, sage-blue, soft steel. */
export const matteBlue = {
  periwinkle: "#C4CEDF",
  icy: "#E3EFF8",
  sage: "#C8D6DF",
  steel: "#8BA8C4",
  sky: "#A8BFD4",
  mist: "#D8E6F2",
  fog: "#EBF3F8",
  dusk: "#7A97B5",
};

export const lightGlass: GlassTokens = {
  surfaceAlpha: 0.42,
  chromeAlpha: 0.55,
  nestedAlpha: 0.34,
  blurIntensity: 36,
  tint: "light",
};

export const darkGlass: GlassTokens = {
  surfaceAlpha: 0.42,
  chromeAlpha: 0.55,
  nestedAlpha: 0.32,
  blurIntensity: 24,
  tint: "dark",
};

export const lightMatte: ThemeColors = {
  bg: "#FCFEFF",
  bgSecondary: "#F7FAFD",
  bgTertiary: "#F2F7FB",
  surface: "rgba(248, 252, 255, 0.55)",
  surfaceMuted: "rgba(227, 239, 248, 0.45)",
  surfaceElevated: "rgba(252, 254, 255, 0.62)",
  surfaceInset: "rgba(216, 230, 242, 0.4)",
  glassFill: "rgba(248, 252, 255, 0.46)",
  glassFillMuted: "rgba(227, 239, 248, 0.38)",
  glassBorder: "rgba(168, 191, 212, 0.38)",
  glassHighlight: "rgba(255, 255, 255, 0.72)",
  ambientA: matteBlue.periwinkle,
  ambientB: matteBlue.icy,
  ambientC: matteBlue.sage,
  primary: matteBlue.steel,
  primaryDark: matteBlue.dusk,
  primaryLight: matteBlue.mist,
  text: "#4A5D73",
  textMuted: "#6E8198",
  textSoft: "#95A5BA",
  border: "rgba(168, 191, 212, 0.45)",
  borderLight: "rgba(216, 230, 242, 0.65)",
  danger: matteBlue.dusk,
  dangerLight: matteBlue.icy,
  dangerBorder: matteBlue.periwinkle,
  success: matteBlue.sky,
  successLight: matteBlue.icy,
  successBorder: matteBlue.mist,
  warning: matteBlue.steel,
  warningLight: "#EEF4FA",
  google: matteBlue.steel,
  overlay: "rgba(74, 93, 115, 0.28)",
  overlayHeavy: "rgba(74, 93, 115, 0.48)",
  shadow: "rgba(122, 151, 181, 0.14)",
  white: "#FCFEFF",
  adminBadge: matteBlue.mist,
};

export const darkMatte: ThemeColors = {
  bg: "#1C2430",
  bgSecondary: "#243040",
  surface: "rgba(196, 206, 223, 0.12)",
  surfaceMuted: "rgba(168, 191, 212, 0.1)",
  surfaceElevated: "rgba(216, 230, 242, 0.16)",
  surfaceInset: "rgba(140, 168, 196, 0.12)",
  glassFill: "rgba(196, 206, 223, 0.14)",
  glassFillMuted: "rgba(168, 191, 212, 0.1)",
  glassBorder: "rgba(216, 230, 242, 0.22)",
  glassHighlight: "rgba(227, 239, 248, 0.28)",
  ambientA: "#3D4F66",
  ambientB: "#2E3D52",
  ambientC: "#354858",
  primary: matteBlue.sky,
  primaryDark: matteBlue.steel,
  primaryLight: "rgba(168, 191, 212, 0.2)",
  text: "#E8EFF8",
  textMuted: "#A8BFD4",
  textSoft: "#7A97B5",
  border: "rgba(168, 191, 212, 0.28)",
  borderLight: "rgba(122, 151, 181, 0.18)",
  danger: "#A8BFD4",
  dangerLight: "rgba(122, 151, 181, 0.18)",
  dangerBorder: "rgba(168, 191, 212, 0.35)",
  success: "#C4CEDF",
  successLight: "rgba(168, 191, 212, 0.16)",
  successBorder: "rgba(196, 206, 223, 0.3)",
  warning: matteBlue.periwinkle,
  warningLight: "rgba(168, 191, 212, 0.14)",
  google: matteBlue.sky,
  overlay: "rgba(0, 0, 0, 0.45)",
  overlayHeavy: "rgba(0, 0, 0, 0.68)",
  shadow: "rgba(0, 0, 0, 0.32)",
  white: "#F0F6FB",
  adminBadge: "rgba(168, 191, 212, 0.22)",
};
