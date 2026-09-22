import { ReactNode } from "react";
import { RefreshControl, ScrollView, StyleSheet, View, ViewStyle } from "react-native";
import { colors, spacing } from "../../lib/theme";

type ScreenProps = {
  children: ReactNode;
  refreshing?: boolean;
  onRefresh?: () => void;
  contentStyle?: ViewStyle;
  scroll?: boolean;
};

export function Screen({ children, refreshing, onRefresh, contentStyle, scroll = true }: ScreenProps) {
  if (!scroll) {
    return <View style={[styles.container, contentStyle]}>{children}</View>;
  }

  return (
    <ScrollView
      contentContainerStyle={[styles.container, contentStyle]}
      showsVerticalScrollIndicator={false}
      refreshControl={
        onRefresh ? (
          <RefreshControl refreshing={refreshing ?? false} onRefresh={onRefresh} tintColor={colors.primary} />
        ) : undefined
      }
    >
      {children}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: spacing.lg,
    gap: spacing.md,
    paddingBottom: spacing.xl,
  },
});
