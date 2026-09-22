import { ReactNode } from "react";
import { Animated, StyleProp, StyleSheet, View, ViewStyle } from "react-native";
import Svg, { Path } from "react-native-svg";
import { ThemeColors } from "../../lib/palettes";
import { SoothingMotionConfig, useReduceMotionPreference, useSoothingMotion } from "../../lib/soothingMotion";

type AmbientBackdropProps = {
  colors: ThemeColors;
  isDark: boolean;
};

function FloatingShape({
  style,
  motion,
  enabled,
  children,
}: {
  style: StyleProp<ViewStyle>;
  motion: SoothingMotionConfig;
  enabled: boolean;
  children: ReactNode;
}) {
  const { translateX, translateY, spin, scale, opacity } = useSoothingMotion(motion, enabled);

  return (
    <Animated.View
      style={[
        style,
        {
          opacity,
          transform: [
            { translateX },
            { translateY },
            { rotate: spin },
            { scale },
          ],
        },
      ]}
    >
      {children}
    </Animated.View>
  );
}

export function AmbientBackdrop({ colors, isDark }: AmbientBackdropProps) {
  const motionEnabled = !useReduceMotionPreference();
  const washOpacity = isDark ? 0.55 : 0.58;

  return (
    <View pointerEvents="none" style={StyleSheet.absoluteFill}>
      <FloatingShape
        enabled={motionEnabled}
        motion={{
          amplitudeX: 22,
          amplitudeY: 28,
          periodX: 28000,
          periodY: 36000,
          rotate: 4,
          scalePulse: 0.035,
          opacityPulse: 0.1,
          delay: 0,
        }}
        style={[styles.shape, styles.blobA]}
      >
        <View style={[styles.circle, styles.sizeXL, { backgroundColor: colors.ambientA, opacity: washOpacity }]} />
      </FloatingShape>

      <FloatingShape
        enabled={motionEnabled}
        motion={{
          amplitudeX: 30,
          amplitudeY: 18,
          periodX: 42000,
          periodY: 31000,
          rotate: 5,
          scalePulse: 0.04,
          opacityPulse: 0.14,
          phaseOffset: 0.25,
          delay: 900,
        }}
        style={[styles.shape, styles.blobB]}
      >
        <View style={[styles.ellipse, { backgroundColor: colors.ambientB, opacity: washOpacity * 0.92 }]} />
      </FloatingShape>

      <FloatingShape
        enabled={motionEnabled}
        motion={{
          amplitudeX: 16,
          amplitudeY: 24,
          periodX: 34000,
          periodY: 26000,
          rotate: 7,
          scalePulse: 0.03,
          opacityPulse: 0.12,
          phaseOffset: 0.5,
          delay: 1400,
        }}
        style={[styles.shape, styles.blobC]}
      >
        <View style={[styles.organic, { backgroundColor: colors.ambientC, opacity: washOpacity * 0.78 }]} />
      </FloatingShape>

      <FloatingShape
        enabled={motionEnabled}
        motion={{
          amplitudeX: 14,
          amplitudeY: 20,
          periodX: 48000,
          periodY: 39000,
          rotate: 3,
          scalePulse: 0.05,
          opacityPulse: 0.18,
          phaseOffset: 0.35,
          delay: 2200,
        }}
        style={[styles.shape, styles.blobD]}
      >
        <View
          style={[
            styles.ring,
            { borderColor: colors.ambientA, opacity: isDark ? 0.22 : 0.28 },
          ]}
        />
      </FloatingShape>

      <FloatingShape
        enabled={motionEnabled}
        motion={{
          amplitudeX: 26,
          amplitudeY: 14,
          periodX: 30000,
          periodY: 44000,
          rotate: 6,
          scalePulse: 0.025,
          opacityPulse: 0.1,
          phaseOffset: 0.15,
          delay: 500,
        }}
        style={[styles.shape, styles.blobE]}
      >
        <View style={[styles.capsule, { backgroundColor: colors.ambientB, opacity: washOpacity * 0.55 }]} />
      </FloatingShape>

      <FloatingShape
        enabled={motionEnabled}
        motion={{
          amplitudeX: 12,
          amplitudeY: 22,
          periodX: 25000,
          periodY: 33000,
          rotate: 9,
          scalePulse: 0.045,
          opacityPulse: 0.15,
          phaseOffset: 0.65,
          delay: 1800,
        }}
        style={[styles.shape, styles.blobF]}
      >
        <View style={[styles.pebble, { backgroundColor: colors.ambientC, opacity: washOpacity * 0.62 }]} />
      </FloatingShape>

      <FloatingShape
        enabled={motionEnabled}
        motion={{
          amplitudeX: 18,
          amplitudeY: 10,
          periodX: 52000,
          periodY: 46000,
          rotate: 2,
          scalePulse: 0.02,
          opacityPulse: 0.08,
          phaseOffset: 0.4,
          delay: 700,
        }}
        style={[styles.shape, styles.blobG]}
      >
        <View style={[styles.disc, { backgroundColor: colors.ambientA, opacity: washOpacity * 0.35 }]} />
      </FloatingShape>

      <FloatingShape
        enabled={motionEnabled}
        motion={{
          amplitudeX: 20,
          amplitudeY: 8,
          periodX: 38000,
          periodY: 50000,
          rotate: 1,
          scalePulse: 0.015,
          opacityPulse: 0.1,
          phaseOffset: 0.55,
          delay: 1100,
        }}
        style={[styles.shape, styles.arcWrap]}
      >
        <Svg width={340} height={140} viewBox="0 0 340 140" style={styles.arc}>
          <Path
            d="M 0 98 Q 90 22 170 58 T 340 44"
            stroke={colors.ambientA}
            strokeWidth={1.5}
            fill="none"
            opacity={isDark ? 0.16 : 0.22}
          />
          <Path
            d="M 24 108 Q 120 42 205 72 T 320 58"
            stroke={colors.ambientB}
            strokeWidth={1}
            fill="none"
            opacity={isDark ? 0.12 : 0.18}
          />
          <Path
            d="M 48 118 Q 150 68 230 88 T 300 78"
            stroke={colors.ambientC}
            strokeWidth={0.75}
            fill="none"
            opacity={isDark ? 0.1 : 0.14}
          />
        </Svg>
      </FloatingShape>
    </View>
  );
}

const styles = StyleSheet.create({
  shape: {
    position: "absolute",
  },
  circle: {
    borderRadius: 9999,
  },
  sizeXL: {
    width: 340,
    height: 340,
  },
  ellipse: {
    width: 380,
    height: 240,
    borderRadius: 9999,
  },
  organic: {
    width: 260,
    height: 300,
    borderTopLeftRadius: 140,
    borderTopRightRadius: 88,
    borderBottomLeftRadius: 108,
    borderBottomRightRadius: 156,
  },
  ring: {
    width: 210,
    height: 210,
    borderRadius: 105,
    borderWidth: 1.5,
    backgroundColor: "transparent",
  },
  capsule: {
    width: 220,
    height: 72,
    borderRadius: 9999,
  },
  pebble: {
    width: 120,
    height: 150,
    borderTopLeftRadius: 58,
    borderTopRightRadius: 42,
    borderBottomLeftRadius: 48,
    borderBottomRightRadius: 64,
  },
  disc: {
    width: 160,
    height: 160,
    borderRadius: 80,
  },
  blobA: {
    top: -96,
    right: -108,
  },
  blobB: {
    bottom: 72,
    left: -120,
  },
  blobC: {
    top: "32%",
    right: -58,
  },
  blobD: {
    bottom: "24%",
    right: "14%",
  },
  blobE: {
    top: "18%",
    left: -40,
  },
  blobF: {
    bottom: "12%",
    left: "22%",
  },
  blobG: {
    top: "58%",
    left: "8%",
  },
  arcWrap: {
    top: "44%",
    left: -28,
  },
  arc: {
    opacity: 0.9,
  },
});
