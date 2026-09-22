import { StyleSheet, Text, View } from "react-native";
import { IconName } from "../../lib/icons";
import { useTheme } from "../../lib/ThemeContext";
import { useThemedStyles } from "../../lib/useThemedStyles";
import { spacing } from "../../lib/theme";
import { Button } from "./Button";
import { Icon } from "./Icon";

type EmptyStateProps = {
  icon: IconName;
  title: string;
  message: string;
  actionLabel?: string;
  onAction?: () => void;
};

export function EmptyState({ icon, title, message, actionLabel, onAction }: EmptyStateProps) {
  const { colors } = useTheme();
  const styles = useThemedStyles(({ colors, typography }) =>
    StyleSheet.create({
      container: {
        alignItems: "center",
        paddingVertical: spacing.xl,
        paddingHorizontal: spacing.md,
        gap: spacing.sm,
      },
      iconWrap: {
        width: 72,
        height: 72,
        borderRadius: 36,
        backgroundColor: colors.primaryLight,
        alignItems: "center",
        justifyContent: "center",
        marginBottom: spacing.sm,
      },
      title: { ...typography.subtitle, textAlign: "center" },
      message: { ...typography.body, textAlign: "center" },
      button: { marginTop: spacing.md, alignSelf: "stretch" },
    })
  );

  return (
    <View style={styles.container}>
      <View style={styles.iconWrap}>
        <Icon name={icon} size="xl" color={colors.primary} />
      </View>
      <Text style={styles.title}>{title}</Text>
      <Text style={styles.message}>{message}</Text>
      {actionLabel && onAction && (
        <Button variant="primary" onPress={onAction} style={styles.button}>
          {actionLabel}
        </Button>
      )}
    </View>
  );
}
