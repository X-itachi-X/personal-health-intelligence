import { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import * as DocumentPicker from "expo-document-picker";
import {
  API_BASE,
  Biomarker,
  fetchHealth,
  pollReportUntilDone,
  uploadReport,
} from "../lib/api";

export default function HomeScreen() {
  const [uploading, setUploading] = useState(false);
  const [status, setStatus] = useState<string | null>(null);
  const [backendStatus, setBackendStatus] = useState<"loading" | "ok" | "error">("loading");
  const [biomarkers, setBiomarkers] = useState<Biomarker[]>([]);

  useEffect(() => {
    console.log("[phi] app started, API:", API_BASE);
    fetchHealth()
      .then(() => setBackendStatus("ok"))
      .catch(() => setBackendStatus("error"));
  }, []);

  async function pickAndUpload() {
    const result = await DocumentPicker.getDocumentAsync({
      type: ["application/pdf", "image/*"],
      copyToCacheDirectory: true,
    });

    if (result.canceled || !result.assets?.[0]) {
      return;
    }

    const asset = result.assets[0];

    setUploading(true);
    setStatus("Processing your report...");
    setBiomarkers([]);

    try {
      const upload = await uploadReport({
        uri: asset.uri,
        name: asset.name,
        mimeType: asset.mimeType,
        file: asset.file,
      });

      const report = await pollReportUntilDone(upload.reportId, (update) => {
        setStatus(`Extraction: ${update.extractionStatus}...`);
      });

      if (report.extractionStatus === "TEXT_ONLY") {
        setStatus("Text extracted. Set CLAUDE_API_KEY for full biomarker parsing.");
      } else {
        setStatus(`Extracted ${report.biomarkerCount} biomarkers from your report.`);
      }
      setBiomarkers(report.biomarkers.slice(0, 8));
    } catch (error) {
      console.error("[phi] upload flow failed", error);
      setStatus(error instanceof Error ? error.message : "Upload failed");
    } finally {
      setUploading(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>How are you doing?</Text>

      <View style={styles.card}>
        <Text style={styles.section}>What is happening?</Text>
        {backendStatus === "loading" && <Text style={styles.item}>Checking backend...</Text>}
        {backendStatus === "ok" && <Text style={styles.item}>✓ Backend connected</Text>}
        {backendStatus === "error" && (
          <Text style={styles.item}>⚠ Backend offline — start Spring Boot on port 8080</Text>
        )}
        {biomarkers.length > 0 && (
          <>
            <Text style={styles.item}>✓ Latest report parsed</Text>
            {biomarkers.map((b) => (
              <Text key={b.canonical} style={styles.biomarker}>
                {b.canonical}: {b.value ?? b.textValue} {b.unit ?? ""}
              </Text>
            ))}
          </>
        )}
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
  biomarker: { fontSize: 13, color: "#555", fontFamily: "monospace" },
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
