import { useEffect, useRef } from "react";
import { Animated, StyleSheet, View, ViewStyle } from "react-native";
import { useTheme } from "../../lib/ThemeContext";
import { radii } from "../../lib/theme";
import { MatteSurface } from "./MatteSurface";

type SkeletonProps = {
  width?: number | `${number}%`;
  height?: number;
  borderRadius?: number;
  style?: ViewStyle;
};

export function Skeleton({ width = "100%", height = 16, borderRadius = radii.sm, style }: SkeletonProps) {
  const { colors } = useTheme();
  const pulse = useRef(new Animated.Value(0.4)).current;

  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, { toValue: 1, duration: 800, useNativeDriver: true }),
        Animated.timing(pulse, { toValue: 0.4, duration: 800, useNativeDriver: true }),
      ])
    );
    loop.start();
    return () => loop.stop();
  }, [pulse]);

  return (
    <Animated.View
      style={[
        { backgroundColor: colors.surfaceMuted },
        { width, height, borderRadius, opacity: pulse },
        style,
      ]}
    />
  );
}

export function SkeletonCard() {
  return (
    <MatteSurface padded={false}>
      <View style={styles.card}>
        <Skeleton height={20} width="60%" />
        <Skeleton height={14} width="40%" style={styles.gap} />
        <Skeleton height={48} style={styles.gap} />
      </View>
    </MatteSurface>
  );
}

const styles = StyleSheet.create({
  card: {
    padding: 16,
    gap: 8,
  },
  gap: { marginTop: 8 },
});
