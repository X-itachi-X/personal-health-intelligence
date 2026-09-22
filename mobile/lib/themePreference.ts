import * as SecureStore from "expo-secure-store";
import { Platform } from "react-native";

export type ThemePreference = "light" | "dark" | "system";

const KEY = "phi_theme_preference";

async function setItem(value: string): Promise<void> {
  if (Platform.OS === "web") {
    localStorage.setItem(KEY, value);
    return;
  }
  await SecureStore.setItemAsync(KEY, value);
}

async function getItem(): Promise<string | null> {
  if (Platform.OS === "web") {
    return localStorage.getItem(KEY);
  }
  return SecureStore.getItemAsync(KEY);
}

export async function loadThemePreference(): Promise<ThemePreference> {
  const raw = await getItem();
  if (raw === "light" || raw === "dark" || raw === "system") {
    return raw;
  }
  return "system";
}

export async function saveThemePreference(preference: ThemePreference): Promise<void> {
  await setItem(preference);
}
