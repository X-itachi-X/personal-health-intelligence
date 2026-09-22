import { ActivityIndicator, StyleSheet, Text, View } from "react-native";
import { colors, radii, spacing, typography } from "../../lib/theme";
import { Icon } from "./Icon";

export type ProgressStepState = "pending" | "active" | "done";

export type ProgressStep = {
  id: string;
  label: string;
  state: ProgressStepState;
};

type ProgressStepsProps = {
  steps: ProgressStep[];
  caption?: string | null;
};

export function ProgressSteps({ steps, caption }: ProgressStepsProps) {
  return (
    <View style={styles.container}>
      {steps.map((step, index) => (
        <View key={step.id} style={styles.row}>
          <View style={styles.rail}>
            {step.state === "done" ? (
              <Icon name="checkmark-circle" size="sm" color={colors.success} />
            ) : step.state === "active" ? (
              <ActivityIndicator size="small" color={colors.primary} />
            ) : (
              <View style={styles.dot} />
            )}
            {index < steps.length - 1 && <View style={styles.line} />}
          </View>
          <Text
            style={[
              styles.label,
              step.state === "active" && styles.labelActive,
              step.state === "done" && styles.labelDone,
            ]}
          >
            {step.label}
          </Text>
        </View>
      ))}
      {caption ? <Text style={styles.caption}>{caption}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: spacing.xs,
    width: "100%",
  },
  row: {
    flexDirection: "row",
    gap: spacing.sm,
    minHeight: 28,
  },
  rail: {
    width: 22,
    alignItems: "center",
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: colors.border,
    marginTop: 6,
  },
  line: {
    position: "absolute",
    top: 22,
    bottom: -6,
    width: 2,
    backgroundColor: colors.borderLight,
  },
  label: {
    ...typography.caption,
    flex: 1,
    paddingTop: 2,
    color: colors.textMuted,
  },
  labelActive: {
    color: colors.primaryDark,
    fontWeight: "600",
  },
  labelDone: {
    color: colors.text,
  },
  caption: {
    ...typography.caption,
    color: colors.textSoft,
    marginTop: spacing.xs,
    fontStyle: "italic",
  },
});
