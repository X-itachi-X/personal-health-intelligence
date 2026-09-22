import { ReactNode } from "react";
import { StyleSheet, View, ViewStyle } from "react-native";
import { useTheme } from "../../lib/ThemeContext";
import { radii, spacing } from "../../lib/theme";
import { MatteSurface } from "./MatteSurface";

type CardVariant = "default" | "muted" | "outlined" | "accent";

type CardProps = {
  children: ReactNode;
  variant?: CardVariant;
  style?: ViewStyle;
  accentColor?: string;
};

export function Card({ children, variant = "default", style, accentColor }: CardProps) {
  const { colors } = useTheme();
  const surfaceVariant = variant === "muted" ? "muted" : "default";

  return (
    <MatteSurface variant={surfaceVariant} style={style} padded={false} elevated>
      <View
        style={[
          styles.inner,
          variant === "outlined" && styles.outlined,
          variant === "accent" && { borderLeftWidth: 4, borderLeftColor: accentColor ?? colors.primary },
        ]}
      >
        {children}
      </View>
    </MatteSurface>
  );
}

const styles = StyleSheet.create({
  inner: {
    padding: spacing.md,
  },
  outlined: {
    borderRadius: radii.lg,
  },
});
