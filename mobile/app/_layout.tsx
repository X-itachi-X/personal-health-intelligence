import { Stack, router, useSegments } from "expo-router";
import { useEffect, useState } from "react";
import { ActivityIndicator, View } from "react-native";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { ApiAuthError, fetchMe, setUnauthorizedListener } from "../lib/api";
import { clearSession, getSession, saveSession } from "../lib/auth";
import { ThemeProvider, useTheme } from "../lib/ThemeContext";
import { checkForAppUpdate } from "../lib/updates";

function RootNavigator() {
  const [checkingAuth, setCheckingAuth] = useState(true);
  const segments = useSegments();
  const { colors } = useTheme();

  useEffect(() => {
    checkForAppUpdate();
  }, []);

  useEffect(() => {
    setUnauthorizedListener(() => router.replace("/login"));
    return () => setUnauthorizedListener(null);
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      const session = await getSession();
      if (!session?.token) {
        setCheckingAuth(false);
        return;
      }

      try {
        const me = await fetchMe();
        if (!cancelled) {
          await saveSession(me);
        }
      } catch (err) {
        if (!cancelled && err instanceof ApiAuthError) {
          await clearSession();
        }
      } finally {
        if (!cancelled) {
          setCheckingAuth(false);
        }
      }
    }

    bootstrap();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (checkingAuth) {
      return;
    }

    const onAuthScreen = segments[0] === "login" || segments[0] === "register";
    getSession().then((session) => {
      if (!session?.token && !onAuthScreen) {
        router.replace("/login");
      } else if (session?.token && onAuthScreen) {
        router.replace("/(app)");
      }
    });
  }, [checkingAuth, segments]);

  if (checkingAuth) {
    return (
      <View style={{ flex: 1, justifyContent: "center", alignItems: "center", backgroundColor: colors.bg }}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="(app)" />
      <Stack.Screen name="login" />
      <Stack.Screen name="register" />
    </Stack>
  );
}

export default function RootLayout() {
  return (
    <ThemeProvider>
      <SafeAreaProvider>
        <RootNavigator />
      </SafeAreaProvider>
    </ThemeProvider>
  );
}
