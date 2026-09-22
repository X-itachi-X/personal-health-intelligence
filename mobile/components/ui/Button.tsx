import { Children, isValidElement, ReactNode } from "react";
import { ActivityIndicator, StyleSheet, Text, TextStyle, View, ViewStyle } from "react-native";
import { useTheme } from "../../lib/ThemeContext";
import { radii, spacing } from "../../lib/theme";
import { AnimatedPressable } from "./AnimatedPressable";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";

type ButtonProps = {
  children: ReactNode;
  onPress?: () => void;
  variant?: ButtonVariant;
  disabled?: boolean;
  loading?: boolean;
  style?: ViewStyle;
  fullWidth?: boolean;
};

function isTextOnly(children: ReactNode): boolean {
  if (children == null || typeof children === "boolean") return false;
  if (typeof children === "string" || typeof children === "number") return true;
  const parts = Children.toArray(children);
  return parts.length > 0 && parts.every((part) => typeof part === "string" || typeof part === "number");
}

export function Button({
  children,
  onPress,
  variant = "primary",
  disabled = false,
  loading = false,
  style,
  fullWidth = true,
}: ButtonProps) {
  const { colors } = useTheme();
  const isDisabled = disabled || loading;
  const parts = Children.toArray(children).filter(Boolean);
  const loadingColor = variant === "ghost" ? colors.primary : colors.white;

  const textStyles: TextStyle[] = [
    styles.text,
    variant === "primary" && { color: colors.white },
    variant === "secondary" && { color: colors.white },
    variant === "ghost" && { color: colors.primaryDark },
    variant === "danger" && { color: colors.danger },
  ].filter(Boolean) as TextStyle[];

  let content: ReactNode;
  if (loading) {
    const loadingText =
      isTextOnly(children) || parts.every((part) => typeof part === "string" || typeof part === "number")
        ? parts.join("")
        : "Working…";
    content = (
      <View style={styles.loadingRow}>
        <ActivityIndicator color={loadingColor} size="small" />
        <Text style={[...textStyles, styles.loadingText]} numberOfLines={2}>
          {loadingText}
        </Text>
      </View>
    );
  } else if (parts.length === 1 && isValidElement(parts[0])) {
    content = parts[0];
  } else if (isTextOnly(children) || parts.every((part) => typeof part === "string" || typeof part === "number")) {
    content = <Text style={textStyles}>{parts}</Text>;
  } else {
    content = <Text style={textStyles}>{children}</Text>;
  }

  return (
    <AnimatedPressable
      onPress={onPress}
      disabled={isDisabled}
      style={[
        styles.base,
        fullWidth && styles.fullWidth,
        variant === "primary" && { backgroundColor: colors.primary },
        variant === "secondary" && { backgroundColor: colors.primaryDark },
        variant === "ghost" && { backgroundColor: colors.primaryLight },
        variant === "danger" && { backgroundColor: colors.dangerLight, borderWidth: 1, borderColor: colors.dangerBorder },
        isDisabled && styles.disabled,
        style,
      ]}
    >
      {content}
    </AnimatedPressable>
  );
}

const styles = StyleSheet.create({
  base: {
    borderRadius: radii.md,
    paddingVertical: 14,
    paddingHorizontal: spacing.md,
    alignItems: "center",
    justifyContent: "center",
    minHeight: 48,
  },
  fullWidth: { alignSelf: "stretch" },
  disabled: { opacity: 0.6 },
  loadingRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: spacing.sm,
  },
  loadingText: { flexShrink: 1, textAlign: "center" },
  text: { fontSize: 16, fontWeight: "700" },
});
