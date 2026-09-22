import { LinearGradient } from "expo-linear-gradient";
import { ReactNode } from "react";
import { StyleSheet, View, ViewStyle } from "react-native";
import { useTheme } from "../../lib/ThemeContext";
import { AmbientBackdrop } from "./AmbientBackdrop";
import { AnimatedGradientWash } from "./AnimatedGradientWash";

type MatteBackgroundProps = {
  children: ReactNode;
  style?: ViewStyle;
};

/** Soft light-blue backdrop with slowly drifting ambient washes behind glass panels. */
export function MatteBackground({ children, style }: MatteBackgroundProps) {
  const { colors, scheme } = useTheme();
  const isDark = scheme === "dark";

  return (
    <View style={[styles.root, { backgroundColor: colors.bg }, style]}>
      {isDark ? (
        <LinearGradient
          colors={[colors.bg, colors.bgSecondary, colors.bg]}
          locations={[0, 0.5, 1]}
          style={StyleSheet.absoluteFill}
        />
      ) : (
        <LinearGradient
          colors={[colors.bg, colors.bgSecondary, colors.bgTertiary ?? colors.bgSecondary, colors.bg]}
          locations={[0, 0.32, 0.68, 1]}
          style={StyleSheet.absoluteFill}
        />
      )}
      <AnimatedGradientWash colors={colors} isDark={isDark} />
      <AmbientBackdrop colors={colors} isDark={isDark} />
      <View style={styles.content}>{children}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    overflow: "hidden",
  },
  content: {
    flex: 1,
  },
});
