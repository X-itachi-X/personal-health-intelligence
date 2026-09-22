import { StyleSheet } from "react-native";
import { useTheme } from "../../lib/ThemeContext";
import { radii, spacing } from "../../lib/theme";

export function useConfirmFormStyles() {
  const { colors, typography } = useTheme();

  return StyleSheet.create({
    hint: { ...typography.caption, lineHeight: 20 },
    fieldLabel: { ...typography.caption, color: colors.textMuted, marginTop: spacing.sm, marginBottom: 4 },
    input: {
      borderWidth: 1,
      borderColor: colors.border,
      borderRadius: radii.sm,
      paddingHorizontal: spacing.md,
      paddingVertical: 10,
      fontSize: 15,
      color: colors.text,
      backgroundColor: colors.surfaceInset,
    },
    multilineInput: {
      minHeight: 88,
      textAlignVertical: "top",
    },
    itemCard: {
      marginTop: spacing.sm,
      padding: spacing.sm,
      borderRadius: radii.sm,
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.surfaceMuted,
      gap: spacing.xs,
    },
    itemHeader: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: spacing.xs,
    },
    itemTitle: { ...typography.subtitle, fontSize: 14 },
    removeBtn: { paddingVertical: 4, paddingHorizontal: spacing.sm },
    removeBtnText: { fontSize: 13, color: colors.danger, fontWeight: "600" },
    chipRow: { flexDirection: "row", flexWrap: "wrap", gap: spacing.xs, marginTop: 4 },
    chip: {
      paddingHorizontal: 10,
      paddingVertical: 6,
      borderRadius: radii.sm,
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.surface,
    },
    chipActive: {
      borderColor: colors.primary,
      backgroundColor: colors.primaryLight,
    },
    chipText: { fontSize: 13, color: colors.textMuted },
    chipTextActive: { color: colors.primaryDark, fontWeight: "600" },
    addBtn: { marginTop: spacing.sm, alignSelf: "flex-start" },
    addBtnText: { fontSize: 14, color: colors.primary, fontWeight: "600" },
    saveBtn: { marginTop: spacing.md },
  });
}
