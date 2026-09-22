import DateTimePicker, { DateTimePickerEvent } from "@react-native-community/datetimepicker";
import { useState } from "react";
import { Platform, Pressable, StyleSheet, Text, View } from "react-native";
import { formatDisplayDate, formatIsoDate, parseIsoDate } from "../../lib/reportDate";
import { colors, radii, spacing, typography } from "../../lib/theme";
import { Icon } from "./Icon";

type ReportDatePickerProps = {
  value: string | null;
  onChange: (isoDate: string) => void;
  label?: string;
  hint?: string;
  required?: boolean;
};

export function ReportDatePicker({
  value,
  onChange,
  label = "Report date",
  hint,
  required = false,
}: ReportDatePickerProps) {
  const [showPicker, setShowPicker] = useState(false);
  const selectedDate = value ? parseIsoDate(value) : new Date();

  function handleChange(event: DateTimePickerEvent, date?: Date) {
    if (Platform.OS === "android") {
      setShowPicker(false);
    }
    if (event.type === "dismissed" || !date) {
      return;
    }
    onChange(formatIsoDate(date));
  }

  return (
    <View style={styles.wrap}>
      <Text style={styles.label}>
        {label}
        {required ? " *" : " (optional)"}
      </Text>
      {hint ? <Text style={styles.hint}>{hint}</Text> : null}

      {Platform.OS === "web" ? (
        <input
          type="date"
          value={value ?? ""}
          max={formatIsoDate(new Date())}
          onChange={(event) => {
            if (event.target.value) {
              onChange(event.target.value);
            }
          }}
          style={{
            width: "100%",
            borderWidth: 1,
            borderColor: colors.border,
            borderRadius: radii.md,
            padding: 12,
            fontSize: 16,
            backgroundColor: colors.surface,
            color: colors.text,
          }}
        />
      ) : (
        <>
          <Pressable style={styles.field} onPress={() => setShowPicker(true)}>
            <Icon name="calendar-outline" size="sm" color={colors.primary} />
            <Text style={value ? styles.valueText : styles.placeholderText}>
              {value ? formatDisplayDate(value) : "Select lab test date"}
            </Text>
            <Icon name="chevron-down" size="sm" color={colors.textSoft} />
          </Pressable>
          {showPicker && (
            <DateTimePicker
              value={selectedDate}
              mode="date"
              display={Platform.OS === "ios" ? "spinner" : "default"}
              maximumDate={new Date()}
              onChange={handleChange}
            />
          )}
          {Platform.OS === "ios" && (
            <Pressable style={styles.doneBtn} onPress={() => setShowPicker(false)}>
              <Text style={styles.doneText}>Done</Text>
            </Pressable>
          )}
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: spacing.xs },
  label: { ...typography.subtitle, fontSize: 15 },
  hint: { ...typography.caption, lineHeight: 20 },
  field: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radii.md,
    paddingHorizontal: spacing.md,
    paddingVertical: 14,
    backgroundColor: colors.surface,
  },
  valueText: { ...typography.body, flex: 1 },
  placeholderText: { ...typography.body, color: colors.textSoft, flex: 1 },
  doneBtn: { alignSelf: "flex-end", paddingVertical: spacing.xs },
  doneText: { color: colors.primary, fontWeight: "700" },
});
