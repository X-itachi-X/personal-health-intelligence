import { Link, router } from "expo-router";
import { useEffect, useState } from "react";
import { KeyboardAvoidingView, Platform, StyleSheet, Text, View } from "react-native";
import { fetchMe, login, loginWithGoogle } from "../lib/api";
import { saveSession, getSession } from "../lib/auth";
import { authenticateUser, canUseBiometrics } from "../lib/biometrics";
import { useGoogleAuth } from "../lib/googleAuth";
import { useTheme } from "../lib/ThemeContext";
import { useThemedStyles } from "../lib/useThemedStyles";
import { radii, spacing } from "../lib/theme";
import { AnimatedPressable } from "../components/ui/AnimatedPressable";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { FadeInView } from "../components/ui/FadeInView";
import { Icon } from "../components/ui/Icon";
import { MatteBackground } from "../components/ui/MatteBackground";
import { MatteTextField } from "../components/ui/MatteTextField";

export default function LoginScreen() {
  const { colors } = useTheme();
  const styles = useThemedStyles(({ colors, typography }) =>
    StyleSheet.create({
      root: { flex: 1 },
      container: { flex: 1, padding: spacing.lg, justifyContent: "center", gap: spacing.md },
      brandRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginBottom: spacing.sm },
      brand: { fontSize: 14, fontWeight: "800", color: colors.primaryDark, letterSpacing: 2 },
      title: { ...typography.hero, fontSize: 30 },
      subtitle: { ...typography.body, marginBottom: spacing.sm },
      primaryButton: {
        flexDirection: "row",
        alignItems: "center",
        gap: spacing.md,
        backgroundColor: colors.primary,
        borderRadius: radii.lg,
        padding: spacing.md,
      },
      bioIcon: {
        width: 48,
        height: 48,
        borderRadius: 24,
        backgroundColor: colors.surfaceMuted,
        alignItems: "center",
        justifyContent: "center",
      },
      primaryButtonText: { color: colors.white, fontSize: 17, fontWeight: "700" },
      primaryButtonHint: { color: colors.primaryLight, fontSize: 13, marginTop: 2 },
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
      infoRow: { flexDirection: "row", alignItems: "flex-start", gap: spacing.sm },
      infoContent: { flex: 1 },
      infoTitle: { ...typography.subtitle, fontSize: 14 },
      infoText: { ...typography.caption, lineHeight: 20, marginTop: 4 },
      emailForm: { gap: spacing.sm },
      errorRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm, justifyContent: "center" },
      error: { color: colors.danger, fontSize: 14 },
      link: { marginTop: spacing.sm, textAlign: "center", color: colors.primaryDark, fontSize: 16, fontWeight: "500" },
      linkSecondary: { textAlign: "center", color: colors.textMuted, fontSize: 14 },
    })
  );

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [savedSession, setSavedSession] = useState<Awaited<ReturnType<typeof getSession>>>(null);
  const [biometricsAvailable, setBiometricsAvailable] = useState(false);

  const { request, response, promptAsync, isAvailable: googleAvailable, isExpoGo } = useGoogleAuth();

  useEffect(() => {
    getSession().then(setSavedSession);
    canUseBiometrics().then(setBiometricsAvailable);
  }, []);

  useEffect(() => {
    if (response?.type === "success" && response.params.id_token) {
      handleGoogleToken(response.params.id_token);
    }
    if (response?.type === "error") {
      setError(
        response.params?.error_description ??
          response.error?.description ??
          "Google sign-in failed."
      );
    }
  }, [response]);

  async function enterApp(session: Awaited<ReturnType<typeof getSession>>) {
    if (!session) return;
    await saveSession(session);
    router.replace("/(app)");
  }

  async function handleBiometricUnlock() {
    setError(null);
    const ok = await authenticateUser("Unlock your health app");
    if (!ok || !savedSession) return;
    try {
      const me = await fetchMe();
      await enterApp({ ...savedSession, ...me });
    } catch {
      setSavedSession(null);
      setError("Session expired — sign in with email again.");
    }
  }

  async function handleGoogleToken(idToken: string) {
    setLoading(true);
    setError(null);
    try {
      const session = await loginWithGoogle(idToken);
      await enterApp(session);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Google sign-in failed");
    } finally {
      setLoading(false);
    }
  }

  async function handleGooglePress() {
    setError(null);
    await promptAsync();
  }

  async function handleEmailLogin() {
    if (!email.trim() || !password) {
      setError("Enter your email and password");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const session = await login({ email, password });
      await enterApp(session);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Login failed";
      setError(
        message.toLowerCase().includes("invalid credentials")
          ? "Invalid email or password. Local dev and edge use separate databases — register here if you have not created an account on this server."
          : message
      );
    } finally {
      setLoading(false);
    }
  }

  const showFingerprint = savedSession && biometricsAvailable && Platform.OS !== "web";

  return (
    <MatteBackground>
      <KeyboardAvoidingView style={styles.root} behavior={Platform.OS === "ios" ? "padding" : undefined}>
      <View style={styles.container}>
        <FadeInView delay={0}>
          <View style={styles.brandRow}>
            <Icon name="pulse" size="lg" color={colors.primary} />
            <Text style={styles.brand}>PHI</Text>
          </View>
          <Text style={styles.title}>Your health, simplified</Text>
          <Text style={styles.subtitle}>
            {showFingerprint
              ? "Use your fingerprint to get in instantly."
              : "Sign in once — fingerprint unlock works after that."}
          </Text>
        </FadeInView>

        {showFingerprint && (
          <FadeInView delay={80}>
            <AnimatedPressable style={styles.primaryButton} onPress={handleBiometricUnlock}>
              <View style={styles.bioIcon}>
                <Icon name="finger-print" size="lg" color={colors.white} />
              </View>
              <View>
                <Text style={styles.primaryButtonText}>Continue as {savedSession.displayName}</Text>
                <Text style={styles.primaryButtonHint}>Fingerprint or face</Text>
              </View>
            </AnimatedPressable>
          </FadeInView>
        )}

        {!showFingerprint && (
          <FadeInView delay={80}>
            <View style={styles.emailForm}>
              <MatteTextField
                autoCapitalize="none"
                keyboardType="email-address"
                placeholder="Email"
                value={email}
                onChangeText={setEmail}
                leading={<Icon name="mail-outline" size="sm" color={colors.textMuted} />}
              />
              <MatteTextField
                secureTextEntry
                placeholder="Password"
                value={password}
                onChangeText={setPassword}
                leading={<Icon name="lock-closed-outline" size="sm" color={colors.textMuted} />}
              />
              <Button variant="secondary" onPress={handleEmailLogin} loading={loading}>
                Sign in
              </Button>
            </View>
          </FadeInView>
        )}

        {googleAvailable && (
          <FadeInView delay={160}>
            <AnimatedPressable style={styles.googleButton} onPress={handleGooglePress} disabled={loading || !request}>
              <Icon name="logo-google" size="md" color={colors.white} />
              <Text style={styles.googleButtonText}>Continue with Google</Text>
            </AnimatedPressable>
          </FadeInView>
        )}

        {isExpoGo && (
          <FadeInView delay={200}>
            <Card variant="muted">
              <View style={styles.infoRow}>
                <Icon name="information-circle-outline" size="md" color={colors.textMuted} />
                <View style={styles.infoContent}>
                  <Text style={styles.infoTitle}>Google sign-in unavailable in Expo Go</Text>
                  <Text style={styles.infoText}>
                    Use email above, then fingerprint on your next visit. Google works in a full app build.
                  </Text>
                </View>
              </View>
            </Card>
          </FadeInView>
        )}

        {error && (
          <FadeInView delay={0}>
            <View style={styles.errorRow}>
              <Icon name="alert-circle-outline" size="sm" color={colors.danger} />
              <Text style={styles.error}>{error}</Text>
            </View>
          </FadeInView>
        )}

        <FadeInView delay={240}>
          <Link href="/register" style={styles.link}>
            New here? Create an account
          </Link>
          <Link href="/register" style={styles.linkSecondary}>
            Have an invite code? Register here
          </Link>
        </FadeInView>
      </View>
    </KeyboardAvoidingView>
    </MatteBackground>
  );
}
