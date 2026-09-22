import { StyleSheet, Text, View } from "react-native";
import {
  IMPLEMENTED_FEATURES,
  PLANNED_FEATURES,
  statusLabel,
} from "../../lib/features";
import { useTheme } from "../../lib/ThemeContext";
import { useThemedStyles } from "../../lib/useThemedStyles";
import { spacing } from "../../lib/theme";
import { Badge } from "../../components/ui/Badge";
import { Card } from "../../components/ui/Card";
import { FadeInView } from "../../components/ui/FadeInView";
import { Icon } from "../../components/ui/Icon";
import { Screen } from "../../components/ui/Screen";

function statusToBadge(status: string): "success" | "warning" | "default" {
  if (status === "done") return "success";
  if (status === "partial") return "warning";
  return "default";
}

export default function FeaturesScreen() {
  const { colors } = useTheme();
  const styles = useThemedStyles(({ colors, typography }) =>
    StyleSheet.create({
      introRow: { flexDirection: "row", alignItems: "flex-start", gap: spacing.sm },
      intro: { ...typography.body, flex: 1 },
      section: { gap: spacing.sm },
      sectionHeader: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
      sectionTitle: { ...typography.subtitle, fontSize: 18 },
      featureCard: { gap: 6 },
      featureHeader: { flexDirection: "row", justifyContent: "space-between", alignItems: "flex-start", gap: spacing.sm },
      featureName: { ...typography.subtitle, fontSize: 15, flex: 1 },
      featureNote: { ...typography.caption, lineHeight: 19 },
    })
  );

  function FeatureList({
    title,
    items,
    icon,
  }: {
    title: string;
    items: typeof IMPLEMENTED_FEATURES;
    icon: "checkmark-circle" | "time-outline";
  }) {
    return (
      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Icon name={icon} size="md" color={colors.primary} />
          <Text style={styles.sectionTitle}>{title}</Text>
        </View>
        {items.map((item, index) => (
          <FadeInView key={item.name} delay={index * 40}>
            <Card style={styles.featureCard}>
              <View style={styles.featureHeader}>
                <Text style={styles.featureName}>{item.name}</Text>
                <Badge label={statusLabel(item.status)} variant={statusToBadge(item.status)} />
              </View>
              <Text style={styles.featureNote}>{item.note}</Text>
            </Card>
          </FadeInView>
        ))}
      </View>
    );
  }

  return (
    <Screen>
      <FadeInView delay={0}>
        <Card variant="muted">
          <View style={styles.introRow}>
            <Icon name="sparkles" size="md" color={colors.primary} />
            <Text style={styles.intro}>
              What works today vs what is still being built. Check back as we ship new phases.
            </Text>
          </View>
        </Card>
      </FadeInView>
      <FeatureList title="Implemented" items={IMPLEMENTED_FEATURES} icon="checkmark-circle" />
      <FeatureList title="Not yet / coming soon" items={PLANNED_FEATURES} icon="time-outline" />
    </Screen>
  );
}
