import { StyleSheet, Text, View } from "react-native";
import { useTheme } from "../../lib/ThemeContext";
import { radii } from "../../lib/theme";

type BadgeVariant = "default" | "success" | "warning" | "danger" | "primary";

type BadgeProps = {
  label: string;
  variant?: BadgeVariant;
};

export function Badge({ label, variant = "default" }: BadgeProps) {
  const { colors } = useTheme();

  const variantStyles: Record<BadgeVariant, { bg: string; text: string }> = {
    default: { bg: colors.surfaceMuted, text: colors.textMuted },
    success: { bg: colors.successLight, text: colors.success },
    warning: { bg: colors.warningLight, text: colors.warning },
    danger: { bg: colors.dangerLight, text: colors.danger },
    primary: { bg: colors.primaryLight, text: colors.primaryDark },
  };

  const v = variantStyles[variant];

  return (
    <View style={[styles.badge, { backgroundColor: v.bg }]}>
      <Text style={[styles.text, { color: v.text }]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    borderRadius: radii.sm,
    paddingHorizontal: 8,
    paddingVertical: 4,
    alignSelf: "flex-start",
  },
  text: { fontSize: 11, fontWeight: "700", textTransform: "uppercase" },
});
