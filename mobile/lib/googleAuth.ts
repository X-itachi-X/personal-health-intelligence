import { makeRedirectUri } from "expo-auth-session";
import * as Google from "expo-auth-session/providers/google";
import * as WebBrowser from "expo-web-browser";
import Constants from "expo-constants";
import { Platform } from "react-native";

WebBrowser.maybeCompleteAuthSession();

const WEB_CLIENT_ID = process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID;
const ANDROID_CLIENT_ID = process.env.EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID;

/** True when running inside the Expo Go app (QR scan). Google OAuth does not work here. */
export const isExpoGo = Constants.appOwnership === "expo";

export function isGoogleConfigured(): boolean {
  return Boolean(
    WEB_CLIENT_ID &&
    ANDROID_CLIENT_ID &&
    WEB_CLIENT_ID !== ANDROID_CLIENT_ID
  );
}

/** Google Sign-In only works in a development/production build, not Expo Go. */
export function isGoogleAvailable(): boolean {
  return isGoogleConfigured() && !isExpoGo;
}

export function useGoogleAuth() {
  const redirectUri =
    Platform.OS === "android"
      ? undefined
      : makeRedirectUri({ preferLocalhost: true });

  const [request, response, promptAsync] = Google.useIdTokenAuthRequest({
    webClientId: WEB_CLIENT_ID,
    androidClientId: ANDROID_CLIENT_ID,
    redirectUri,
  });

  return {
    request,
    response,
    promptAsync,
    isConfigured: isGoogleConfigured(),
    isAvailable: isGoogleAvailable(),
    isExpoGo,
  };
}
