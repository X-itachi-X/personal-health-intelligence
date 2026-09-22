import * as SecureStore from "expo-secure-store";
import { Platform } from "react-native";

const ACTIVE_PERSON_KEY = "phi_active_view_person_id";
const ACTIVE_PERSON_NAME_KEY = "phi_active_view_person_name";

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

export type ViewedPerson = {
  personId: number;
  displayName: string;
};

export async function getViewedPerson(): Promise<ViewedPerson | null> {
  const id = await getItem(ACTIVE_PERSON_KEY);
  const name = await getItem(ACTIVE_PERSON_NAME_KEY);
  if (!id || !name) {
    return null;
  }
  const personId = Number(id);
  if (!personId) {
    return null;
  }
  return { personId, displayName: name };
}

export async function setViewedPerson(personId: number, displayName: string): Promise<void> {
  await setItem(ACTIVE_PERSON_KEY, String(personId));
  await setItem(ACTIVE_PERSON_NAME_KEY, displayName);
}

export async function clearViewedPerson(): Promise<void> {
  await deleteItem(ACTIVE_PERSON_KEY);
  await deleteItem(ACTIVE_PERSON_NAME_KEY);
}
