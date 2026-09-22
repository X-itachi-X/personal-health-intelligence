import * as SecureStore from "expo-secure-store";
import { Platform } from "react-native";

const TOKEN_KEY = "phi_auth_token";
const SESSION_KEY = "phi_auth_session";

export type AuthSession = {
  token: string;
  accountId: string;
  personId: number;
  email: string;
  displayName: string;
  uiMode: "basic" | "advanced";
  platformAdmin?: boolean;
};

async function setItem(key: string, value: string): Promise<void> {
  if (Platform.OS === "web") {
    localStorage.setItem(key, value);
    return;
  }
  await SecureStore.setItemAsync(key, value);
}

async function getItem(key: string): Promise<string | null> {
  if (Platform.OS === "web") {
    return localStorage.getItem(key);
  }
  return SecureStore.getItemAsync(key);
}

async function deleteItem(key: string): Promise<void> {
  if (Platform.OS === "web") {
    localStorage.removeItem(key);
    return;
  }
  await SecureStore.deleteItemAsync(key);
}

export async function saveSession(session: AuthSession): Promise<void> {
  await setItem(TOKEN_KEY, session.token);
  await setItem(SESSION_KEY, JSON.stringify(session));
}

export async function getToken(): Promise<string | null> {
  return getItem(TOKEN_KEY);
}

export async function getSession(): Promise<AuthSession | null> {
  const raw = await getItem(SESSION_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as AuthSession;
  } catch {
    return null;
  }
}

export async function clearSession(): Promise<void> {
  await deleteItem(TOKEN_KEY);
  await deleteItem(SESSION_KEY);
}
