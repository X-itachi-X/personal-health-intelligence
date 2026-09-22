import { StyleSheet, Text, View } from "react-native";
import { ThemePreference } from "../../lib/themePreference";
import { useTheme } from "../../lib/ThemeContext";
import { radii, spacing } from "../../lib/theme";
import { AnimatedPressable } from "./AnimatedPressable";
import { Icon } from "./Icon";

const OPTIONS: { value: ThemePreference; label: string; icon: "sunny-outline" | "moon-outline" | "phone-portrait-outline" }[] = [
  { value: "light", label: "Light", icon: "sunny-outline" },
  { value: "dark", label: "Dark", icon: "moon-outline" },
  { value: "system", label: "System", icon: "phone-portrait-outline" },
];

export function ThemeModePicker() {
  const { colors, preference, setPreference, typography } = useTheme();

  return (
    <View style={styles.row}>
      {OPTIONS.map((option) => {
        const active = preference === option.value;
        return (
          <AnimatedPressable
            key={option.value}
            onPress={() => setPreference(option.value)}
            style={[
              styles.option,
              {
                backgroundColor: active ? colors.primaryLight : colors.surfaceMuted,
                borderColor: active ? colors.primary : colors.border,
              },
            ]}
          >
            <Icon name={option.icon} size="sm" color={active ? colors.primaryDark : colors.textMuted} />
            <Text
              style={[
                typography.caption,
                styles.label,
                { color: active ? colors.primaryDark : colors.textMuted, fontWeight: active ? "700" : "500" },
              ]}
            >
              {option.label}
            </Text>
          </AnimatedPressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: "row",
    gap: spacing.sm,
    marginTop: spacing.sm,
  },
  option: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
    paddingVertical: spacing.md,
    borderRadius: radii.md,
    borderWidth: 1,
  },
  label: {
    textTransform: "none",
    fontSize: 13,
  },
});
