import { Platform, TextStyle, ViewStyle } from "react-native";
import { darkGlass, darkMatte, lightGlass, lightMatte, ThemeColors } from "./palettes";

export type ColorScheme = "light" | "dark";

export type AppTheme = {
  scheme: ColorScheme;
  colors: ThemeColors;
  glass: typeof lightGlass;
  typography: ReturnType<typeof createTypography>;
  elevation: ReturnType<typeof createElevation>;
};

export function createTheme(scheme: ColorScheme): AppTheme {
  const colors = scheme === "dark" ? darkMatte : lightMatte;
  const glass = scheme === "dark" ? darkGlass : lightGlass;
  return {
    scheme,
    colors,
    glass,
    typography: createTypography(colors),
    elevation: createElevation(colors),
  };
}

export const lightTheme = createTheme("light");
export const darkTheme = createTheme("dark");

/** @deprecated Use useTheme() — kept for gradual migration */
export const colors = lightMatte;

export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
};

export const radii = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  full: 9999,
};

function createTypography(colors: ThemeColors) {
  return {
    hero: { fontSize: 28, fontWeight: "800" as TextStyle["fontWeight"], color: colors.text, lineHeight: 34 },
    title: { fontSize: 20, fontWeight: "700" as TextStyle["fontWeight"], color: colors.text, lineHeight: 26 },
    subtitle: { fontSize: 16, fontWeight: "600" as TextStyle["fontWeight"], color: colors.text, lineHeight: 22 },
    body: { fontSize: 15, fontWeight: "400" as TextStyle["fontWeight"], color: colors.textMuted, lineHeight: 22 },
    caption: { fontSize: 13, fontWeight: "500" as TextStyle["fontWeight"], color: colors.textMuted, lineHeight: 18 },
    label: {
      fontSize: 12,
      fontWeight: "600" as TextStyle["fontWeight"],
      color: colors.textSoft,
      textTransform: "uppercase" as TextStyle["textTransform"],
      letterSpacing: 0.5,
    },
    data: {
      fontSize: 15,
      fontWeight: "700" as TextStyle["fontWeight"],
      fontVariant: ["tabular-nums"] as TextStyle["fontVariant"],
    },
    mono: {
      fontSize: 32,
      fontWeight: "800" as TextStyle["fontWeight"],
      fontFamily: Platform.select({ ios: "Menlo", android: "monospace", default: "monospace" }),
      letterSpacing: 4,
    },
  };
}

function createElevation(colors: ThemeColors) {
  return {
    sm: Platform.select<ViewStyle>({
      ios: {
        shadowColor: colors.shadow,
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 1,
        shadowRadius: 3,
      },
      android: { elevation: 1 },
      default: {},
    }),
    md: Platform.select<ViewStyle>({
      ios: {
        shadowColor: colors.shadow,
        shadowOffset: { width: 0, height: 3 },
        shadowOpacity: 1,
        shadowRadius: 8,
      },
      android: { elevation: 2 },
      default: {},
    }),
    lg: Platform.select<ViewStyle>({
      ios: {
        shadowColor: colors.shadow,
        shadowOffset: { width: 0, height: 6 },
        shadowOpacity: 1,
        shadowRadius: 14,
      },
      android: { elevation: 3 },
      default: {},
    }),
  };
}

/** @deprecated Use useTheme().typography */
export const typography = createTypography(lightMatte);

/** @deprecated Use useTheme().elevation */
export const elevation = createElevation(lightMatte);

export const animation = {
  fast: 150,
  normal: 200,
  slow: 280,
  pressScale: 0.97,
};
