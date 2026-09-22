import { useEffect, useRef, useState } from "react";
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { getSession, AuthSession } from "../../lib/auth";
import { useFamily } from "../../lib/FamilyContext";
import { runAgentQuery } from "../../lib/agentChat/execute";
import { SUGGESTED_QUESTIONS } from "../../lib/agentChat/intents";
import { colors, radii, spacing, typography } from "../../lib/theme";
import { Card } from "../../components/ui/Card";
import { FadeInView } from "../../components/ui/FadeInView";
import { Icon } from "../../components/ui/Icon";
import { Screen } from "../../components/ui/Screen";
import { ViewingPersonBanner } from "../../components/ViewingPersonBanner";

type ChatMessage = {
  id: string;
  role: "user" | "assistant";
  text: string;
};

export default function AskScreen() {
  const {
    effectivePersonId,
    isViewingOther,
    viewedPersonName,
    viewedMember,
    clearViewedPerson,
    viewedPersonId,
  } = useFamily();
  const [session, setSession] = useState<AuthSession | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: "welcome",
      role: "assistant",
      text:
        "Ask about your labs, medications, or imaging. I query your structured health database — I don't re-read PDFs on every question.",
    },
  ]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const scrollRef = useRef<ScrollView>(null);

  useEffect(() => {
    getSession().then(setSession);
  }, []);

  useEffect(() => {
    const viewing = session?.personId ? isViewingOther(session.personId) : false;
    const firstName = viewedPersonName?.split(" ")[0];
    setMessages([
      {
        id: "welcome",
        role: "assistant",
        text:
          viewing && firstName
            ? `Ask about ${firstName}'s labs, medications, or imaging. Answers come from their structured health data — not PDF re-reads.`
            : "Ask about your labs, medications, or imaging. I query your structured health database — I don't re-read PDFs on every question.",
      },
    ]);
  }, [session?.personId, viewedPersonId, viewedPersonName, isViewingOther]);

  async function submitQuery(query: string) {
    const trimmed = query.trim();
    if (!trimmed || !session?.personId || loading) return;
    const personId = effectivePersonId(session.personId);

    const userMessage: ChatMessage = {
      id: `user-${Date.now()}`,
      role: "user",
      text: trimmed,
    };

    setMessages((prev) => [...prev, userMessage]);
    setInput("");
    setLoading(true);

    try {
      const result = await runAgentQuery(personId, trimmed);
      setMessages((prev) => [
        ...prev,
        {
          id: `assistant-${Date.now()}`,
          role: "assistant",
          text: result.answer,
        },
      ]);
    } catch (error) {
      setMessages((prev) => [
        ...prev,
        {
          id: `assistant-error-${Date.now()}`,
          role: "assistant",
          text: error instanceof Error ? error.message : "Something went wrong. Try again.",
        },
      ]);
    } finally {
      setLoading(false);
      setTimeout(() => scrollRef.current?.scrollToEnd({ animated: true }), 100);
    }
  }

  const viewingOther = session?.personId ? isViewingOther(session.personId) : false;

  return (
    <Screen scroll={false}>
      {viewingOther && viewedPersonName && (
        <ViewingPersonBanner
          name={viewedPersonName}
          profileOnly={viewedMember != null && !viewedMember.hasAccount}
          onClear={() => clearViewedPerson()}
        />
      )}

      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === "ios" ? "padding" : undefined}
        keyboardVerticalOffset={80}
      >
        <ScrollView
          ref={scrollRef}
          style={styles.flex}
          contentContainerStyle={styles.scrollContent}
          onContentSizeChange={() => scrollRef.current?.scrollToEnd({ animated: true })}
        >
          <FadeInView delay={0}>
            <Card variant="muted" style={styles.disclaimer}>
              <View style={styles.disclaimerRow}>
                <Icon name="server-outline" size="sm" color={colors.primary} />
                <Text style={styles.disclaimerText}>
                  SQL-grounded answers from biomarkers, meds, and imaging — not a generic chat wrapper.
                </Text>
              </View>
            </Card>
          </FadeInView>

          {messages.map((message, index) => (
            <FadeInView key={message.id} delay={index * 20}>
              <View
                style={[
                  styles.bubble,
                  message.role === "user" ? styles.userBubble : styles.assistantBubble,
                ]}
              >
                <Text
                  style={[
                    styles.bubbleText,
                    message.role === "user" ? styles.userBubbleText : styles.assistantBubbleText,
                  ]}
                >
                  {message.text}
                </Text>
              </View>
            </FadeInView>
          ))}

          {loading && (
            <View style={[styles.bubble, styles.assistantBubble]}>
              <Text style={styles.assistantBubbleText}>
                {viewingOther && viewedPersonName
                  ? `Looking up ${viewedPersonName.split(" ")[0]}'s data…`
                  : "Looking up your data…"}
              </Text>
            </View>
          )}
        </ScrollView>

        <View style={styles.composer}>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipScroll}>
            {SUGGESTED_QUESTIONS.map((question) => (
              <Pressable
                key={question}
                style={styles.chip}
                onPress={() => submitQuery(question)}
                disabled={loading}
              >
                <Text style={styles.chipText}>{question}</Text>
              </Pressable>
            ))}
          </ScrollView>

          <View style={styles.inputRow}>
            <TextInput
              style={styles.input}
              placeholder="Ask about your health data…"
              placeholderTextColor={colors.textMuted}
              value={input}
              onChangeText={setInput}
              onSubmitEditing={() => submitQuery(input)}
              editable={!loading}
              returnKeyType="send"
            />
            <Pressable
              style={[styles.sendBtn, (!input.trim() || loading) && styles.sendBtnDisabled]}
              onPress={() => submitQuery(input)}
              disabled={!input.trim() || loading}
            >
              <Icon name="send" size="sm" color={colors.white} />
            </Pressable>
          </View>
        </View>
      </KeyboardAvoidingView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  scrollContent: {
    padding: spacing.md,
    gap: spacing.sm,
    paddingBottom: spacing.lg,
  },
  disclaimer: { marginBottom: spacing.sm },
  disclaimerRow: { flexDirection: "row", alignItems: "flex-start", gap: spacing.sm },
  disclaimerText: { ...typography.caption, flex: 1, lineHeight: 18 },
  bubble: {
    maxWidth: "92%",
    padding: spacing.md,
    borderRadius: radii.md,
  },
  userBubble: {
    alignSelf: "flex-end",
    backgroundColor: colors.primary,
  },
  assistantBubble: {
    alignSelf: "flex-start",
    backgroundColor: colors.surfaceMuted,
    borderWidth: 1,
    borderColor: colors.border,
  },
  bubbleText: { ...typography.body, lineHeight: 22 },
  userBubbleText: { color: colors.white },
  assistantBubbleText: { color: colors.text },
  composer: {
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.surface,
    paddingHorizontal: spacing.md,
    paddingTop: spacing.sm,
    paddingBottom: spacing.md,
  },
  chipScroll: { marginBottom: spacing.sm, maxHeight: 40 },
  chip: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: radii.sm,
    backgroundColor: colors.primaryLight,
    marginRight: spacing.sm,
  },
  chipText: { fontSize: 13, color: colors.primaryDark },
  inputRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  input: {
    flex: 1,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radii.md,
    paddingHorizontal: spacing.md,
    paddingVertical: 10,
    fontSize: 15,
    color: colors.text,
    backgroundColor: colors.surface,
  },
  sendBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.primary,
    alignItems: "center",
    justifyContent: "center",
  },
  sendBtnDisabled: { opacity: 0.5 },
});
