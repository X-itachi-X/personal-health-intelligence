import { BlurView } from "expo-blur";
import { ReactNode } from "react";
import { Platform, StyleSheet, View, ViewStyle } from "react-native";
import { useTheme } from "../../lib/ThemeContext";
import { radii } from "../../lib/theme";

type MatteSurfaceVariant = "default" | "muted" | "elevated";

type MatteSurfaceProps = {
  children: ReactNode;
  style?: ViewStyle;
  variant?: MatteSurfaceVariant;
  borderRadius?: number;
  padded?: boolean;
  elevated?: boolean;
};

const webBlurStyle: ViewStyle =
  Platform.OS === "web"
    ? ({
        backdropFilter: "blur(20px) saturate(140%)",
        WebkitBackdropFilter: "blur(20px) saturate(140%)",
      } as ViewStyle)
    : {};

/** Frosted glass panel — light matte-blue tint with blur-backed translucency. */
export function MatteSurface({
  children,
  style,
  variant = "default",
  borderRadius = radii.lg,
  padded = true,
  elevated = false,
}: MatteSurfaceProps) {
  const { colors, glass, elevation } = useTheme();

  const fill =
    variant === "muted"
      ? colors.glassFillMuted
      : variant === "elevated"
        ? colors.surfaceElevated
        : colors.glassFill;

  const shell = [
    styles.base,
    elevated && elevation.md,
    { borderRadius, borderColor: colors.glassBorder },
    style,
  ];

  const inner = [
    styles.inner,
    {
      backgroundColor: fill,
      borderTopColor: colors.glassHighlight,
    },
    padded && styles.padded,
  ];

  if (Platform.OS === "web") {
    return (
      <View style={[...shell, webBlurStyle]}>
        <View style={inner}>{children}</View>
      </View>
    );
  }

  return (
    <View style={shell}>
      <BlurView
        intensity={glass.blurIntensity}
        tint={glass.tint}
        experimentalBlurMethod={Platform.OS === "android" ? "dimezisBlurView" : undefined}
        style={[styles.blur, { borderRadius }]}
      >
        <View style={inner}>{children}</View>
      </BlurView>
    </View>
  );
}

const styles = StyleSheet.create({
  base: {
    overflow: "hidden",
    borderWidth: 1,
  },
  blur: {
    overflow: "hidden",
  },
  inner: {
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  padded: {
    padding: 16,
  },
});
