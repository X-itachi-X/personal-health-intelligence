import { LinearGradient } from "expo-linear-gradient";
import { useEffect, useRef } from "react";
import { Animated, Easing, StyleSheet, View } from "react-native";
import { ThemeColors } from "../../lib/palettes";
import { useReduceMotionPreference, useSlowSpin } from "../../lib/soothingMotion";

type AnimatedGradientWashProps = {
  colors: ThemeColors;
  isDark: boolean;
};

/** Very slow shifting light wash — adds depth behind glass without distraction. */
export function AnimatedGradientWash({ colors, isDark }: AnimatedGradientWashProps) {
  const motionEnabled = !useReduceMotionPreference();
  const spin = useSlowSpin(isDark ? 90000 : 72000, motionEnabled, 0);
  const drift = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (!motionEnabled) {
      drift.setValue(0);
      return;
    }

    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(drift, {
          toValue: 1,
          duration: 45000,
          easing: Easing.inOut(Easing.sin),
          useNativeDriver: true,
        }),
        Animated.timing(drift, {
          toValue: 0,
          duration: 45000,
          easing: Easing.inOut(Easing.sin),
          useNativeDriver: true,
        }),
      ])
    );

    loop.start();
    return () => loop.stop();
  }, [drift, motionEnabled]);

  const translateY = drift.interpolate({
    inputRange: [0, 1],
    outputRange: [-28, 28],
  });

  const washA = isDark ? "rgba(168, 191, 212, 0.08)" : "rgba(196, 206, 223, 0.14)";
  const washB = isDark ? "rgba(122, 151, 181, 0.06)" : "rgba(227, 239, 248, 0.22)";
  const washC = isDark ? "rgba(74, 93, 115, 0.04)" : "rgba(248, 252, 255, 0.12)";

  return (
    <View pointerEvents="none" style={StyleSheet.absoluteFill}>
      <Animated.View
        style={[
          styles.washLayer,
          {
            transform: [{ rotate: spin }, { translateY }],
          },
        ]}
      >
        <LinearGradient
          colors={[washC, washA, washB, washC]}
          locations={[0, 0.35, 0.65, 1]}
          start={{ x: 0.1, y: 0 }}
          end={{ x: 0.9, y: 1 }}
          style={styles.gradient}
        />
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  washLayer: {
    position: "absolute",
    top: "-35%",
    left: "-35%",
    width: "170%",
    height: "170%",
  },
  gradient: {
    flex: 1,
  },
});
