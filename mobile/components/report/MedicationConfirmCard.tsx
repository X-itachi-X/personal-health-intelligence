import { Pressable, Text, TextInput, View } from "react-native";
import { MedicationCourseType } from "../../lib/api";
import { Button } from "../ui/Button";
import { Card } from "../ui/Card";
import { Icon } from "../ui/Icon";
import { colors, spacing } from "../../lib/theme";
import { useConfirmFormStyles } from "./confirmFormStyles";

export type MedicationDraftItem = {
  id: string;
  medicationName: string;
  dosage: string;
  scheduleText: string;
  startedOn: string;
  durationDays: string;
  courseType: MedicationCourseType;
};

const COURSE_OPTIONS: { value: MedicationCourseType; label: string }[] = [
  { value: "CHRONIC", label: "Ongoing" },
  { value: "ACUTE", label: "Short course" },
  { value: "UNKNOWN", label: "Unsure" },
];

type Props = {
  items: MedicationDraftItem[];
  onChange: (items: MedicationDraftItem[]) => void;
  onConfirm: () => void;
  loading?: boolean;
};

function updateItem(
  items: MedicationDraftItem[],
  id: string,
  patch: Partial<MedicationDraftItem>
): MedicationDraftItem[] {
  return items.map((item) => (item.id === id ? { ...item, ...patch } : item));
}

export function MedicationConfirmCard({ items, onChange, onConfirm, loading }: Props) {
  const styles = useConfirmFormStyles();
  function addItem() {
    onChange([
      ...items,
      {
        id: `med-new-${Date.now()}`,
        medicationName: "",
        dosage: "",
        scheduleText: "",
        startedOn: items[0]?.startedOn ?? new Date().toISOString().slice(0, 10),
        durationDays: "",
        courseType: "UNKNOWN",
      },
    ]);
  }

  function removeItem(id: string) {
    onChange(items.filter((item) => item.id !== id));
  }

  return (
    <Card variant="outlined" style={{ borderColor: colors.warning, gap: spacing.sm }}>
      <View style={{ flexDirection: "row", alignItems: "center", gap: spacing.sm }}>
        <Icon name="medkit-outline" size="md" color={colors.primary} />
        <Text style={{ fontSize: 15, fontWeight: "700", flex: 1 }}>Confirm medications</Text>
      </View>
      <Text style={styles.hint}>
        Review and correct what we extracted. Remove wrong rows or add any we missed before saving.
      </Text>

      {items.map((item, index) => (
        <View key={item.id} style={styles.itemCard}>
          <View style={styles.itemHeader}>
            <Text style={styles.itemTitle}>Medication {index + 1}</Text>
            {items.length > 1 && (
              <Pressable style={styles.removeBtn} onPress={() => removeItem(item.id)}>
                <Text style={styles.removeBtnText}>Remove</Text>
              </Pressable>
            )}
          </View>

          <Text style={styles.fieldLabel}>Name</Text>
          <TextInput
            style={styles.input}
            value={item.medicationName}
            onChangeText={(text) => onChange(updateItem(items, item.id, { medicationName: text }))}
            placeholder="e.g. Metformin 500 mg"
            placeholderTextColor={colors.textMuted}
          />

          <Text style={styles.fieldLabel}>Dosage</Text>
          <TextInput
            style={styles.input}
            value={item.dosage}
            onChangeText={(text) => onChange(updateItem(items, item.id, { dosage: text }))}
            placeholder="Optional"
            placeholderTextColor={colors.textMuted}
          />

          <Text style={styles.fieldLabel}>Schedule</Text>
          <TextInput
            style={styles.input}
            value={item.scheduleText}
            onChangeText={(text) => onChange(updateItem(items, item.id, { scheduleText: text }))}
            placeholder="e.g. 1 tablet after breakfast"
            placeholderTextColor={colors.textMuted}
          />

          <Text style={styles.fieldLabel}>Started on</Text>
          <TextInput
            style={styles.input}
            value={item.startedOn}
            onChangeText={(text) => onChange(updateItem(items, item.id, { startedOn: text }))}
            placeholder="YYYY-MM-DD"
            placeholderTextColor={colors.textMuted}
            autoCapitalize="none"
          />

          <Text style={styles.fieldLabel}>Duration (days)</Text>
          <TextInput
            style={styles.input}
            value={item.durationDays}
            onChangeText={(text) => onChange(updateItem(items, item.id, { durationDays: text }))}
            placeholder="Leave blank for ongoing"
            placeholderTextColor={colors.textMuted}
            keyboardType="number-pad"
          />

          <Text style={styles.fieldLabel}>Course type</Text>
          <View style={styles.chipRow}>
            {COURSE_OPTIONS.map((option) => (
              <Pressable
                key={option.value}
                style={[styles.chip, item.courseType === option.value && styles.chipActive]}
                onPress={() => onChange(updateItem(items, item.id, { courseType: option.value }))}
              >
                <Text style={[styles.chipText, item.courseType === option.value && styles.chipTextActive]}>
                  {option.label}
                </Text>
              </Pressable>
            ))}
          </View>
        </View>
      ))}

      <Pressable style={styles.addBtn} onPress={addItem}>
        <Text style={styles.addBtnText}>+ Add medication</Text>
      </Pressable>

      <Button variant="primary" onPress={onConfirm} loading={loading} style={styles.saveBtn}>
        Save to medication log
      </Button>
    </Card>
  );
}
