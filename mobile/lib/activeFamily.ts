import * as SecureStore from "expo-secure-store";
import { Platform } from "react-native";

const ACTIVE_FAMILY_KEY = "phi_active_family_id";

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

export async function getActiveFamilyId(): Promise<string | null> {
  return getItem(ACTIVE_FAMILY_KEY);
}

export async function setActiveFamilyId(familyId: string): Promise<void> {
  await setItem(ACTIVE_FAMILY_KEY, familyId);
}

export async function clearActiveFamilyId(): Promise<void> {
  await deleteItem(ACTIVE_FAMILY_KEY);
}
