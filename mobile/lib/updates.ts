import * as Updates from "expo-updates";
import { Platform } from "react-native";
import { checkForApkUpdate } from "./apkUpdate";

/** Check for updates on app start — edge APK manifest first, then EAS JS bundle if configured. */
export async function checkForAppUpdate(): Promise<void> {
  if (__DEV__ || Platform.OS === "web") {
    return;
  }

  if (Platform.OS === "android") {
    try {
      const prompted = await checkForApkUpdate(false);
      if (prompted) {
        return;
      }
    } catch {
      // fall through to EAS check
    }
  }

  if (!Updates.isEnabled) {
    return;
  }
  try {
    const result = await Updates.checkForUpdateAsync();
    if (!result.isAvailable) {
      return;
    }
    await Updates.fetchUpdateAsync();
    await Updates.reloadAsync();
  } catch {
    // Expected before EAS project is linked
  }
}
