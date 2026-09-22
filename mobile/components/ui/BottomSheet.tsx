import { ReactNode } from "react";
import { Modal, Pressable, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../../lib/ThemeContext";
import { radii, spacing } from "../../lib/theme";
import { MatteSurface } from "./MatteSurface";

type BottomSheetProps = {
  visible: boolean;
  onClose: () => void;
  title?: string;
  children: ReactNode;
};

export function BottomSheet({ visible, onClose, title, children }: BottomSheetProps) {
  const insets = useSafeAreaInsets();
  const { colors, typography } = useTheme();

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <View style={[styles.backdrop, { backgroundColor: colors.overlay }]}>
        <Pressable style={StyleSheet.absoluteFill} onPress={onClose} />
        <MatteSurface
          variant="elevated"
          borderRadius={radii.xl}
          padded={false}
          style={{ ...styles.sheetWrap, paddingBottom: insets.bottom + spacing.lg }}
        >
          <View style={styles.sheet}>
            {title && <Text style={[typography.title, styles.title]}>{title}</Text>}
            {children}
          </View>
        </MatteSurface>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    justifyContent: "flex-end",
  },
  sheetWrap: {
    borderBottomLeftRadius: 0,
    borderBottomRightRadius: 0,
  },
  sheet: {
    padding: spacing.lg,
    gap: spacing.sm,
  },
  title: { marginBottom: spacing.sm },
});
