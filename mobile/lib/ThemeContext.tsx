import { ReactNode, createContext, useContext, useEffect, useMemo, useState } from "react";
import { ActivityIndicator, useColorScheme, View } from "react-native";
import { AppTheme, createTheme } from "./theme";
import { loadThemePreference, saveThemePreference, ThemePreference } from "./themePreference";

type ThemeContextValue = AppTheme & {
  preference: ThemePreference;
  setPreference: (preference: ThemePreference) => Promise<void>;
};

const ThemeContext = createContext<ThemeContextValue | null>(null);

export function ThemeProvider({ children }: { children: ReactNode }) {
  const systemScheme = useColorScheme();
  const [preference, setPreferenceState] = useState<ThemePreference>("system");
  const [ready, setReady] = useState(false);

  useEffect(() => {
    loadThemePreference()
      .then(setPreferenceState)
      .finally(() => setReady(true));
  }, []);

  const scheme = preference === "system" ? (systemScheme === "dark" ? "dark" : "light") : preference;
  const theme = useMemo(() => createTheme(scheme), [scheme]);

  const setPreference = async (next: ThemePreference) => {
    await saveThemePreference(next);
    setPreferenceState(next);
  };

  const value = useMemo(
    () => ({
      ...theme,
      preference,
      setPreference,
    }),
    [theme, preference]
  );

  if (!ready) {
    return (
      <View style={{ flex: 1, justifyContent: "center", alignItems: "center", backgroundColor: theme.colors.bg }}>
        <ActivityIndicator size="large" color={theme.colors.primary} />
      </View>
    );
  }

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error("useTheme must be used within ThemeProvider");
  }
  return ctx;
}
