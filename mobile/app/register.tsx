import { Link, router, useLocalSearchParams } from "expo-router";
import { useEffect, useState } from "react";
import {
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { previewInvite, register, loginWithGoogle } from "../lib/api";
import { saveSession } from "../lib/auth";
import { useGoogleAuth } from "../lib/googleAuth";
import { useThemedStyles } from "../lib/useThemedStyles";
import { useTheme } from "../lib/ThemeContext";
import { radii, spacing } from "../lib/theme";
import { AnimatedPressable } from "../components/ui/AnimatedPressable";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { FadeInView } from "../components/ui/FadeInView";
import { Icon } from "../components/ui/Icon";
import { MatteBackground } from "../components/ui/MatteBackground";

export default function RegisterScreen() {
  const { colors, typography } = useTheme();
  const styles = useThemedStyles(({ colors, typography }) =>
    StyleSheet.create({
      root: { flex: 1 },
      container: { flex: 1, padding: spacing.lg, justifyContent: "center", gap: spacing.md },
      brandRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginBottom: spacing.sm },
      brand: { fontSize: 14, fontWeight: "800", color: colors.primaryDark, letterSpacing: 2 },
      title: { ...typography.hero, fontSize: 30 },
      subtitle: { ...typography.body, marginBottom: spacing.sm },
      form: { gap: spacing.sm },
      inputWrap: { position: "relative" },
      inputIcon: { position: "absolute", left: 14, top: 16, zIndex: 1 },
      input: {
        backgroundColor: colors.surfaceInset,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radii.md,
        padding: 14,
        fontSize: 16,
        color: colors.text,
      },
      inputWithIcon: { paddingLeft: 42 },
      invitePreviewRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
      invitePreview: { ...typography.caption, color: colors.success },
      inviteInvalid: { color: colors.danger },
      googleButton: {
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "center",
        gap: spacing.sm,
        backgroundColor: colors.google,
        borderRadius: radii.lg,
        paddingVertical: 16,
      },
      googleButtonText: { color: colors.white, fontSize: 16, fontWeight: "600" },
      errorRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm, justifyContent: "center" },
      error: { color: colors.danger, fontSize: 14 },
      link: { marginTop: spacing.sm, textAlign: "center", color: colors.primaryDark, fontSize: 16, fontWeight: "500" },
      hint: { ...typography.caption, textAlign: "center", lineHeight: 20 },
    })
  );
  const params = useLocalSearchParams<{ invite?: string }>();
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [inviteCode, setInviteCode] = useState(params.invite ?? "");
  const [invitePreview, setInvitePreview] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const { request, response, promptAsync, isAvailable: googleAvailable, isExpoGo } = useGoogleAuth();

  useEffect(() => {
    if (response?.type === "success" && response.params.id_token) {
      handleGoogleToken(response.params.id_token);
    }
  }, [response]);

  useEffect(() => {
    const code = inviteCode.trim();
    if (code.length < 6) {
      setInvitePreview(null);
      return;
    }
    previewInvite(code)
      .then((p) => {
        if (p.valid && p.familyName) {
          setInvitePreview(`Joining ${p.familyName} as ${p.personName ?? "member"}`);
        } else {
          setInvitePreview("Invalid or expired invite code");
        }
      })
      .catch(() => setInvitePreview(null));
  }, [inviteCode]);

  async function handleGoogleToken(idToken: string) {
    setLoading(true);
    setError(null);
    try {
      const session = await loginWithGoogle(idToken);
      await saveSession(session);
      router.replace("/(app)");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Google sign-up failed");
    } finally {
      setLoading(false);
    }
  }

  async function handleRegister() {
    if (!displayName.trim()) {
      setError("Please enter your name");
      return;
    }
    if (!email.trim() || password.length < 8) {
      setError("Enter email and a password (min 8 characters)");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const session = await register({
        email,
        password,
        displayName,
        dateOfBirth: "1990-01-01",
        sex: "other",
        relationship: "self",
        inviteCode: inviteCode.trim() || undefined,
      });
      await saveSession(session);
      router.replace("/(app)");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Registration failed";
      setError(
        message.includes("Email already registered")
          ? "That email is already registered — try signing in instead."
          : message.includes("password")
            ? "Password must be at least 8 characters."
            : message
      );
    } finally {
      setLoading(false);
    }
  }

  const inviteValid = invitePreview && !invitePreview.startsWith("Invalid");

  return (
    <MatteBackground>
      <KeyboardAvoidingView style={styles.root} behavior={Platform.OS === "ios" ? "padding" : undefined}>
      <View style={styles.container}>
        <FadeInView delay={0}>
          <View style={styles.brandRow}>
            <Icon name="pulse" size="lg" color={colors.primary} />
            <Text style={styles.brand}>PHI</Text>
          </View>
          <Text style={styles.title}>Create your account</Text>
          <Text style={styles.subtitle}>Name, email, password — then use fingerprint next time.</Text>
        </FadeInView>

        <FadeInView delay={80}>
          <View style={styles.form}>
            <View style={styles.inputWrap}>
              <View style={styles.inputIcon}>
                <Icon name="key-outline" size="sm" color={colors.textSoft} />
              </View>
              <TextInput
                style={[styles.input, styles.inputWithIcon]}
                autoCapitalize="characters"
                placeholder="Invite code (optional)"
                placeholderTextColor={colors.textSoft}
                value={inviteCode}
                onChangeText={setInviteCode}
              />
            </View>
            {invitePreview && (
              <View style={styles.invitePreviewRow}>
                <Icon
                  name={inviteValid ? "checkmark-circle" : "close-circle"}
                  size="sm"
                  color={inviteValid ? colors.success : colors.danger}
                />
                <Text style={[styles.invitePreview, !inviteValid && styles.inviteInvalid]}>
                  {invitePreview}
                </Text>
              </View>
            )}
            <TextInput
              style={styles.input}
              placeholder="Your name"
              placeholderTextColor={colors.textSoft}
              value={displayName}
              onChangeText={setDisplayName}
            />
            <TextInput
              style={styles.input}
              autoCapitalize="none"
              keyboardType="email-address"
              placeholder="Email"
              placeholderTextColor={colors.textSoft}
              value={email}
              onChangeText={setEmail}
            />
            <TextInput
              style={styles.input}
              secureTextEntry
              placeholder="Password (min 8 characters)"
              placeholderTextColor={colors.textSoft}
              value={password}
              onChangeText={setPassword}
            />
            <Button variant="secondary" onPress={handleRegister} loading={loading}>
              Create account
            </Button>
          </View>
        </FadeInView>

        {googleAvailable && (
          <FadeInView delay={160}>
            <AnimatedPressable
              style={styles.googleButton}
              onPress={() => promptAsync()}
              disabled={loading || !request}
            >
              <Icon name="logo-google" size="md" color={colors.white} />
              <Text style={styles.googleButtonText}>Sign up with Google</Text>
            </AnimatedPressable>
          </FadeInView>
        )}

        {isExpoGo && (
          <FadeInView delay={200}>
            <Card variant="muted">
              <Text style={styles.hint}>Google sign-up needs a full app build. Email registration works in Expo Go.</Text>
            </Card>
          </FadeInView>
        )}

        {error && (
          <View style={styles.errorRow}>
            <Icon name="alert-circle-outline" size="sm" color={colors.danger} />
            <Text style={styles.error}>{error}</Text>
          </View>
        )}

        <FadeInView delay={240}>
          <Link href="/login" style={styles.link}>
            Already have an account? Sign in
          </Link>
        </FadeInView>
      </View>
    </KeyboardAvoidingView>
    </MatteBackground>
  );
}
