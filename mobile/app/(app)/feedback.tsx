import Constants from "expo-constants";
import { useLocalSearchParams } from "expo-router";
import { useState } from "react";
import { Platform, Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { FeedbackCategory, submitFeedback } from "../../lib/api";
import { useConfirmFormStyles } from "../../components/report/confirmFormStyles";
import { useTheme } from "../../lib/ThemeContext";
import { useThemedStyles } from "../../lib/useThemedStyles";
import { radii, spacing } from "../../lib/theme";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { FadeInView } from "../../components/ui/FadeInView";
import { Icon } from "../../components/ui/Icon";
import { Screen } from "../../components/ui/Screen";

const CATEGORIES: { value: FeedbackCategory; label: string; hint: string }[] = [
  { value: "praise", label: "Love it", hint: "What's working well?" },
  { value: "improvement", label: "Improve", hint: "What would make this better?" },
  { value: "bug", label: "Bug", hint: "Something broken or confusing?" },
  { value: "other", label: "Other", hint: "Anything else on your mind" },
];

function appVersion(): string {
  return Constants.expoConfig?.version ?? Constants.nativeAppVersion ?? "dev";
}

export default function FeedbackScreen() {
  const { colors, typography } = useTheme();
  const confirmFormStyles = useConfirmFormStyles();
  const styles = useThemedStyles(({ colors, typography }) =>
    StyleSheet.create({
      headerRow: { flexDirection: "row", alignItems: "flex-start", gap: spacing.sm },
      headerText: { flex: 1 },
      title: { ...typography.subtitle, fontSize: 18, marginBottom: 4 },
      lead: { ...typography.body, lineHeight: 22 },
      fieldLabel: { ...typography.label, marginTop: spacing.md, marginBottom: spacing.sm },
      hint: { ...typography.caption, marginTop: spacing.sm },
      stars: { flexDirection: "row", gap: spacing.xs },
      starButton: { padding: 4 },
      input: {
        minHeight: 140,
        backgroundColor: colors.surfaceInset,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radii.md,
        padding: spacing.md,
        fontSize: 16,
        color: colors.text,
      },
      error: { color: colors.danger, marginTop: spacing.sm },
      submit: { marginTop: spacing.md },
      successRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
      successText: { ...typography.subtitle, flex: 1 },
    })
  );
  const params = useLocalSearchParams<{ from?: string }>();
  const [category, setCategory] = useState<FeedbackCategory>("improvement");
  const [message, setMessage] = useState("");
  const [rating, setRating] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  async function handleSubmit() {
    if (message.trim().length < 3) {
      setError("Please write at least a few words so we can understand your feedback.");
      return;
    }

    setLoading(true);
    setError(null);
    try {
      await submitFeedback({
        category,
        message: message.trim(),
        rating: rating ?? undefined,
        screenContext: params.from?.trim() || undefined,
        appPlatform: Platform.OS,
        appVersion: appVersion(),
      });
      setSent(true);
      setMessage("");
      setRating(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not send feedback");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Screen>
      <FadeInView delay={0}>
        <Card>
          <View style={styles.headerRow}>
            <Icon name="chatbox-ellipses-outline" size="md" color={colors.primary} />
            <View style={styles.headerText}>
              <Text style={styles.title}>Help us improve PHI</Text>
              <Text style={styles.lead}>
                You're an early tester — tell us what's working, what's confusing, and what you'd love to see next.
                Your message goes directly to the product team on our server.
              </Text>
            </View>
          </View>
        </Card>
      </FadeInView>

      {sent && (
        <FadeInView delay={0}>
          <Card variant="muted">
            <View style={styles.successRow}>
              <Icon name="checkmark-circle" size="md" color={colors.primary} />
              <Text style={styles.successText}>Thanks — we received your feedback.</Text>
            </View>
          </Card>
        </FadeInView>
      )}

      <FadeInView delay={80}>
        <Card>
          <Text style={styles.fieldLabel}>What kind of feedback?</Text>
          <View style={confirmFormStyles.chipRow}>
            {CATEGORIES.map((option) => (
              <Pressable
                key={option.value}
                style={[confirmFormStyles.chip, category === option.value && confirmFormStyles.chipActive]}
                onPress={() => setCategory(option.value)}
              >
                <Text
                  style={[
                    confirmFormStyles.chipText,
                    category === option.value && confirmFormStyles.chipTextActive,
                  ]}
                >
                  {option.label}
                </Text>
              </Pressable>
            ))}
          </View>
          <Text style={styles.hint}>{CATEGORIES.find((c) => c.value === category)?.hint}</Text>

          <Text style={styles.fieldLabel}>Optional rating</Text>
          <View style={styles.stars}>
            {[1, 2, 3, 4, 5].map((value) => (
              <Pressable key={value} onPress={() => setRating(value)} style={styles.starButton}>
                <Icon
                  name={rating != null && value <= rating ? "star" : "star-outline"}
                  size="lg"
                  color={rating != null && value <= rating ? colors.warning : colors.textSoft}
                />
              </Pressable>
            ))}
          </View>

          <Text style={styles.fieldLabel}>Your message</Text>
          <TextInput
            style={styles.input}
            multiline
            numberOfLines={6}
            textAlignVertical="top"
            placeholder="Be specific — which screen, what you expected, what happened instead…"
            placeholderTextColor={colors.textMuted}
            value={message}
            onChangeText={setMessage}
          />

          {error ? <Text style={styles.error}>{error}</Text> : null}

          <Button variant="secondary" onPress={handleSubmit} loading={loading} style={styles.submit}>
            Send feedback
          </Button>
        </Card>
      </FadeInView>
    </Screen>
  );
}
