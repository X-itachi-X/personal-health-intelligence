import { Pressable, StyleSheet, Text, View } from "react-native";
import { useTheme } from "../lib/ThemeContext";
import { useThemedStyles } from "../lib/useThemedStyles";
import { radii, spacing } from "../lib/theme";
import { Icon } from "./ui/Icon";

type Props = {
  name: string;
  profileOnly?: boolean;
  onClear: () => void;
};

export function ViewingPersonBanner({ name, profileOnly, onClear }: Props) {
  const { colors } = useTheme();
  const styles = useThemedStyles(({ colors, typography }) =>
    StyleSheet.create({
      banner: {
        backgroundColor: colors.primaryLight,
        borderRadius: radii.md,
        borderWidth: 1,
        borderColor: colors.primary,
        padding: spacing.md,
        gap: spacing.sm,
      },
      content: { flexDirection: "row", alignItems: "flex-start", gap: spacing.sm },
      textWrap: { flex: 1 },
      title: { ...typography.subtitle, fontSize: 15, color: colors.primaryDark },
      subtitle: { ...typography.caption, marginTop: 4, lineHeight: 18 },
      clearBtn: { alignSelf: "flex-start" },
      clearText: { fontSize: 14, fontWeight: "600", color: colors.primaryDark },
    })
  );

  return (
    <View style={styles.banner}>
      <View style={styles.content}>
        <Icon name="eye-outline" size="sm" color={colors.primaryDark} />
        <View style={styles.textWrap}>
          <Text style={styles.title}>Viewing {name}'s health</Text>
          <Text style={styles.subtitle}>
            {profileOnly
              ? "Profile only — no phone or account needed. You can upload and manage their data."
              : "Reports, trends, and medications below are for this family member."}
          </Text>
        </View>
      </View>
      <Pressable onPress={onClear} style={styles.clearBtn}>
        <Text style={styles.clearText}>Back to mine</Text>
      </Pressable>
    </View>
  );
}
