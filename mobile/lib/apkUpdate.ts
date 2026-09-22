import * as Application from "expo-application";
import * as FileSystem from "expo-file-system/legacy";
import * as IntentLauncher from "expo-intent-launcher";
import { Alert, Linking, Platform } from "react-native";

const API_BASE = process.env.EXPO_PUBLIC_API_URL ?? "http://localhost:8080";

export type AndroidReleaseManifest = {
  version: string;
  versionCode: number;
  apkUrl: string;
  sha256?: string;
  publishedAt: string;
  releaseNotes?: string;
};

function manifestUrl(): string {
  return `${API_BASE.replace(/\/$/, "")}/downloads/android.json`;
}

function currentVersionCode(): number {
  const raw = Application.nativeBuildVersion;
  const parsed = Number.parseInt(raw ?? "0", 10);
  return Number.isFinite(parsed) ? parsed : 0;
}

async function fetchManifest(): Promise<AndroidReleaseManifest | null> {
  try {
    const response = await fetch(manifestUrl(), { headers: { Accept: "application/json" } });
    if (!response.ok) {
      return null;
    }
    return (await response.json()) as AndroidReleaseManifest;
  } catch {
    return null;
  }
}

async function installApk(apkUrl: string): Promise<void> {
  const dest = `${FileSystem.cacheDirectory ?? ""}phi-update.apk`;
  const download = await FileSystem.downloadAsync(apkUrl, dest);
  const contentUri = await FileSystem.getContentUriAsync(download.uri);
  await IntentLauncher.startActivityAsync("android.intent.action.INSTALL_PACKAGE", {
    data: contentUri,
    flags: 1,
  });
}

function promptUpdate(manifest: AndroidReleaseManifest, onDone?: () => void): void {
  const notes = manifest.releaseNotes?.trim();
  const message = [
    `A new test build is available (v${manifest.version}).`,
    notes ? `\n${notes}` : "",
    "\n\nAndroid will ask you to confirm the install.",
  ].join("");

  Alert.alert("Update available", message, [
    { text: "Later", style: "cancel", onPress: onDone },
    {
      text: "Download & install",
      onPress: () => {
        installApk(manifest.apkUrl).catch((err) => {
          Alert.alert(
            "Update failed",
            err instanceof Error ? err.message : "Could not download the APK. Try the downloads page in your browser.",
            [
              { text: "OK", onPress: onDone },
              {
                text: "Open downloads page",
                onPress: () => {
                  Linking.openURL(`${API_BASE.replace(/\/$/, "")}/downloads/`).catch(() => undefined);
                  onDone?.();
                },
              },
            ]
          );
        });
      },
    },
  ]);
}

/** Check edge-hosted android.json and prompt to install when versionCode increases. */
export async function checkForApkUpdate(interactive = false): Promise<boolean> {
  if (Platform.OS !== "android" || __DEV__) {
    return false;
  }

  const manifest = await fetchManifest();
  if (!manifest?.versionCode || !manifest.apkUrl) {
    if (interactive) {
      Alert.alert("No update info", "Could not reach the update server. Try again when you are online.");
    }
    return false;
  }

  if (manifest.versionCode <= currentVersionCode()) {
    if (interactive) {
      Alert.alert("Up to date", `You have the latest test build (v${Application.nativeApplicationVersion ?? "?"}).`);
    }
    return false;
  }

  promptUpdate(manifest);
  return true;
}
