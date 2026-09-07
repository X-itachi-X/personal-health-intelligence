import { useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import * as DocumentPicker from "expo-document-picker";

const API_BASE = process.env.EXPO_PUBLIC_API_URL ?? "http://192.168.1.71:8080";

export default function HomeScreen() {
  const [uploading, setUploading] = useState(false);
  const [status, setStatus] = useState<string | null>(null);

  async function pickAndUpload() {
    const result = await DocumentPicker.getDocumentAsync({
      type: ["application/pdf", "image/*"],
      copyToCacheDirectory: true,
    });

    if (result.canceled || !result.assets?.[0]) {
      return;
    }

    setUploading(true);
    setStatus("Processing your report...");

    // Phase 3: wire to POST /api/v1/reports
    await new Promise((r) => setTimeout(r, 800));

    setUploading(false);
    setStatus("Your health has been updated (stub).");
  }

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>How are you doing?</Text>

      <View style={styles.card}>
        <Text style={styles.section}>What is happening?</Text>
        <Text style={styles.item}>✓ Connect backend to see live summary</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.section}>What should you do?</Text>
        <Text style={styles.item}>• Upload a lab report to get started</Text>
      </View>

      <Pressable style={styles.button} onPress={pickAndUpload} disabled={uploading}>
        {uploading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>Upload report</Text>
        )}
      </Pressable>

      {status ? <Text style={styles.status}>{status}</Text> : null}
      <Text style={styles.hint}>API: {API_BASE}</Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 24, gap: 16 },
  title: { fontSize: 28, fontWeight: "700" },
  card: {
    backgroundColor: "#f4f4f5",
    borderRadius: 12,
    padding: 16,
    gap: 8,
  },
  section: { fontSize: 16, fontWeight: "600" },
  item: { fontSize: 15, color: "#333" },
  button: {
    backgroundColor: "#111",
    padding: 16,
    borderRadius: 12,
    alignItems: "center",
  },
  buttonText: { color: "#fff", fontSize: 16, fontWeight: "600" },
  status: { textAlign: "center", color: "#444" },
  hint: { fontSize: 12, color: "#888", textAlign: "center" },
});
