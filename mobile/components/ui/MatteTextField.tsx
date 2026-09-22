import { ReactNode } from "react";
import { StyleSheet, TextInput, TextInputProps, View } from "react-native";
import { useTheme } from "../../lib/ThemeContext";
import { radii } from "../../lib/theme";
import { MatteSurface } from "./MatteSurface";

type MatteTextFieldProps = TextInputProps & {
  leading?: ReactNode;
};

export function MatteTextField({ leading, style, ...props }: MatteTextFieldProps) {
  const { colors } = useTheme();

  return (
    <MatteSurface variant="muted" borderRadius={radii.md} padded={false} elevated={false}>
      <View style={styles.row}>
        {leading ? <View style={styles.leading}>{leading}</View> : null}
        <TextInput
          {...props}
          style={[styles.input, { color: colors.text }, style]}
          placeholderTextColor={colors.textSoft}
        />
      </View>
    </MatteSurface>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: "row",
    alignItems: "center",
    minHeight: 52,
    paddingHorizontal: 14,
  },
  leading: {
    marginRight: 10,
  },
  input: {
    flex: 1,
    fontSize: 16,
    paddingVertical: 14,
  },
});
