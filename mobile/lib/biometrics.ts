import * as LocalAuthentication from "expo-local-authentication";
import { Platform } from "react-native";

export async function canUseBiometrics(): Promise<boolean> {
  if (Platform.OS === "web") {
    return false;
  }
  const compatible = await LocalAuthentication.hasHardwareAsync();
  const enrolled = await LocalAuthentication.isEnrolledAsync();
  return compatible && enrolled;
}

export async function authenticateUser(prompt = "Unlock PHI"): Promise<boolean> {
  const result = await LocalAuthentication.authenticateAsync({
    promptMessage: prompt,
    cancelLabel: "Use password",
    disableDeviceFallback: false,
  });
  return result.success;
}
