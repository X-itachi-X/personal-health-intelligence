import { useEffect, useRef } from "react";
import { Animated, Modal, StyleSheet, Text, View } from "react-native";
import { colors, radii, spacing, typography } from "../lib/theme";
import { Icon } from "./ui/Icon";

type FamilySwitchOverlayProps = {
  visible: boolean;
  familyName: string;
  onFinished: () => void;
};

export function FamilySwitchOverlay({ visible, familyName, onFinished }: FamilySwitchOverlayProps) {
  const fade = useRef(new Animated.Value(0)).current;
  const scale = useRef(new Animated.Value(0.85)).current;
  const slide = useRef(new Animated.Value(40)).current;
  const dot1 = useRef(new Animated.Value(0.4)).current;
  const dot2 = useRef(new Animated.Value(0.4)).current;
  const dot3 = useRef(new Animated.Value(0.4)).current;

  useEffect(() => {
    if (!visible) return;

    fade.setValue(0);
    scale.setValue(0.85);
    slide.setValue(40);

    const dotPulse = Animated.loop(
      Animated.sequence([
        Animated.stagger(120, [
          Animated.timing(dot1, { toValue: 1, duration: 300, useNativeDriver: true }),
          Animated.timing(dot2, { toValue: 1, duration: 300, useNativeDriver: true }),
          Animated.timing(dot3, { toValue: 1, duration: 300, useNativeDriver: true }),
        ]),
        Animated.stagger(120, [
          Animated.timing(dot1, { toValue: 0.4, duration: 300, useNativeDriver: true }),
          Animated.timing(dot2, { toValue: 0.4, duration: 300, useNativeDriver: true }),
          Animated.timing(dot3, { toValue: 0.4, duration: 300, useNativeDriver: true }),
        ]),
      ])
    );

    dotPulse.start();

    Animated.sequence([
      Animated.parallel([
        Animated.timing(fade, { toValue: 1, duration: 280, useNativeDriver: true }),
        Animated.spring(scale, { toValue: 1, friction: 6, tension: 80, useNativeDriver: true }),
        Animated.spring(slide, { toValue: 0, friction: 7, tension: 70, useNativeDriver: true }),
      ]),
      Animated.delay(600),
      Animated.parallel([
        Animated.timing(fade, { toValue: 0, duration: 350, useNativeDriver: true }),
        Animated.timing(scale, { toValue: 1.08, duration: 350, useNativeDriver: true }),
      ]),
    ]).start(() => {
      dotPulse.stop();
      onFinished();
    });

    return () => dotPulse.stop();
  }, [visible, familyName, fade, scale, slide, dot1, dot2, dot3, onFinished]);

  if (!visible) return null;

  return (
    <Modal transparent visible animationType="none" statusBarTranslucent>
      <Animated.View style={[styles.backdrop, { opacity: fade }]}>
        <Animated.View
          style={[
            styles.card,
            {
              opacity: fade,
              transform: [{ scale }, { translateY: slide }],
            },
          ]}
        >
          <View style={styles.iconWrap}>
            <Icon name="people" size="xl" color={colors.primary} />
          </View>
          <Text style={styles.switching}>Switching family</Text>
          <Text style={styles.familyName}>{familyName}</Text>
          <View style={styles.dots}>
            <Animated.View style={[styles.dot, { opacity: dot1 }]} />
            <Animated.View style={[styles.dot, styles.dotMid, { opacity: dot2 }]} />
            <Animated.View style={[styles.dot, { opacity: dot3 }]} />
          </View>
        </Animated.View>
      </Animated.View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: colors.overlayHeavy,
    alignItems: "center",
    justifyContent: "center",
    padding: spacing.lg,
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: radii.xl,
    padding: spacing.xl,
    alignItems: "center",
    width: "100%",
    maxWidth: 320,
    gap: spacing.sm,
  },
  iconWrap: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: colors.primaryLight,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: spacing.sm,
  },
  switching: {
    ...typography.label,
    letterSpacing: 1.5,
  },
  familyName: {
    ...typography.hero,
    color: colors.primaryDark,
    textAlign: "center",
  },
  dots: { flexDirection: "row", gap: 8, marginTop: spacing.lg },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: colors.primary,
  },
  dotMid: { transform: [{ scale: 1.3 }] },
});
