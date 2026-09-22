import { Pressable, Text, TextInput, View } from "react-native";
import { Button } from "../ui/Button";
import { Card } from "../ui/Card";
import { Icon } from "../ui/Icon";
import { colors, spacing } from "../../lib/theme";
import { useConfirmFormStyles } from "./confirmFormStyles";

export type ImagingFindingDraft = {
  id: string;
  findingText: string;
  severity: string;
  measurementValue: string;
  measurementUnit: string;
};

export type ImagingDraft = {
  modality: string;
  bodyRegion: string;
  studyDate: string;
  facility: string;
  impression: string;
  findings: ImagingFindingDraft[];
};

const MODALITY_OPTIONS = [
  { value: "XRAY", label: "X-ray" },
  { value: "ULTRASOUND", label: "Ultrasound" },
  { value: "CT", label: "CT" },
  { value: "MRI", label: "MRI" },
  { value: "OTHER", label: "Other" },
] as const;

type Props = {
  draft: ImagingDraft;
  onChange: (draft: ImagingDraft) => void;
  onConfirm: () => void;
  loading?: boolean;
};

function updateFinding(
  findings: ImagingFindingDraft[],
  id: string,
  patch: Partial<ImagingFindingDraft>
): ImagingFindingDraft[] {
  return findings.map((finding) => (finding.id === id ? { ...finding, ...patch } : finding));
}

export function ImagingConfirmCard({ draft, onChange, onConfirm, loading }: Props) {
  const styles = useConfirmFormStyles();
  function addFinding() {
    onChange({
      ...draft,
      findings: [
        ...draft.findings,
        {
          id: `finding-new-${Date.now()}`,
          findingText: "",
          severity: "",
          measurementValue: "",
          measurementUnit: "",
        },
      ],
    });
  }

  function removeFinding(id: string) {
    onChange({
      ...draft,
      findings: draft.findings.filter((finding) => finding.id !== id),
    });
  }

  return (
    <Card variant="outlined" style={{ borderColor: colors.warning, gap: spacing.sm }}>
      <View style={{ flexDirection: "row", alignItems: "center", gap: spacing.sm }}>
        <Icon name="scan-outline" size="md" color={colors.primary} />
        <Text style={{ fontSize: 15, fontWeight: "700", flex: 1 }}>Confirm imaging study</Text>
      </View>
      <Text style={styles.hint}>
        Correct the study details and impression before saving to your imaging timeline.
      </Text>

      <Text style={styles.fieldLabel}>Modality</Text>
      <View style={styles.chipRow}>
        {MODALITY_OPTIONS.map((option) => (
          <Pressable
            key={option.value}
            style={[styles.chip, draft.modality === option.value && styles.chipActive]}
            onPress={() => onChange({ ...draft, modality: option.value })}
          >
            <Text style={[styles.chipText, draft.modality === option.value && styles.chipTextActive]}>
              {option.label}
            </Text>
          </Pressable>
        ))}
      </View>

      <Text style={styles.fieldLabel}>Body region</Text>
      <TextInput
        style={styles.input}
        value={draft.bodyRegion}
        onChangeText={(bodyRegion) => onChange({ ...draft, bodyRegion })}
        placeholder="e.g. Abdomen, Chest"
        placeholderTextColor={colors.textMuted}
      />

      <Text style={styles.fieldLabel}>Study date</Text>
      <TextInput
        style={styles.input}
        value={draft.studyDate}
        onChangeText={(studyDate) => onChange({ ...draft, studyDate })}
        placeholder="YYYY-MM-DD"
        placeholderTextColor={colors.textMuted}
        autoCapitalize="none"
      />

      <Text style={styles.fieldLabel}>Facility</Text>
      <TextInput
        style={styles.input}
        value={draft.facility}
        onChangeText={(facility) => onChange({ ...draft, facility })}
        placeholder="Hospital or lab name"
        placeholderTextColor={colors.textMuted}
      />

      <Text style={styles.fieldLabel}>Impression</Text>
      <TextInput
        style={[styles.input, styles.multilineInput]}
        value={draft.impression}
        onChangeText={(impression) => onChange({ ...draft, impression })}
        placeholder="Radiologist summary"
        placeholderTextColor={colors.textMuted}
        multiline
      />

      {draft.findings.map((finding, index) => (
        <View key={finding.id} style={styles.itemCard}>
          <View style={styles.itemHeader}>
            <Text style={styles.itemTitle}>Finding {index + 1}</Text>
            <Pressable style={styles.removeBtn} onPress={() => removeFinding(finding.id)}>
              <Text style={styles.removeBtnText}>Remove</Text>
            </Pressable>
          </View>
          <TextInput
            style={styles.input}
            value={finding.findingText}
            onChangeText={(findingText) =>
              onChange({
                ...draft,
                findings: updateFinding(draft.findings, finding.id, { findingText }),
              })
            }
            placeholder="Finding description"
            placeholderTextColor={colors.textMuted}
            multiline
          />
        </View>
      ))}

      <Pressable style={styles.addBtn} onPress={addFinding}>
        <Text style={styles.addBtnText}>+ Add finding</Text>
      </Pressable>

      <Button variant="primary" onPress={onConfirm} loading={loading} style={styles.saveBtn}>
        Save to imaging timeline
      </Button>
    </Card>
  );
}
