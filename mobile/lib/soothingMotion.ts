import { useEffect, useRef, useState } from "react";
import { AccessibilityInfo, Animated, Easing } from "react-native";

/** Sine keyframes for smooth orbital drift (maps linear 0→1 to sin(2πt)). */
const SINE_IN = [0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1] as const;
const SINE_OUT = [0, 0.707, 1, 0.707, 0, -0.707, -1, -0.707, 0] as const;

export type SoothingMotionConfig = {
  amplitudeX: number;
  amplitudeY: number;
  periodX: number;
  periodY: number;
  rotate?: number;
  scalePulse?: number;
  opacityPulse?: number;
  phaseOffset?: number;
  delay?: number;
};

export function useReduceMotionPreference(): boolean {
  const [reduced, setReduced] = useState(false);

  useEffect(() => {
    AccessibilityInfo.isReduceMotionEnabled().then(setReduced);
    const sub = AccessibilityInfo.addEventListener("reduceMotionChanged", setReduced);
    return () => sub.remove();
  }, []);

  return reduced;
}

function startLoop(value: Animated.Value, duration: number, delay = 0): Animated.CompositeAnimation {
  return Animated.loop(
    Animated.timing(value, {
      toValue: 1,
      duration,
      delay,
      easing: Easing.linear,
      useNativeDriver: true,
    })
  );
}

function startBreath(value: Animated.Value, duration: number, delay = 0): Animated.CompositeAnimation {
  return Animated.loop(
    Animated.sequence([
      Animated.timing(value, {
        toValue: 1,
        duration,
        delay,
        easing: Easing.inOut(Easing.sin),
        useNativeDriver: true,
      }),
      Animated.timing(value, {
        toValue: 0,
        duration,
        easing: Easing.inOut(Easing.sin),
        useNativeDriver: true,
      }),
    ])
  );
}

export function useSoothingMotion(config: SoothingMotionConfig, enabled: boolean) {
  const {
    amplitudeX,
    amplitudeY,
    periodX,
    periodY,
    rotate = 3,
    scalePulse = 0.03,
    opacityPulse = 0.12,
    phaseOffset = 0,
    delay = 0,
  } = config;

  const phaseX = useRef(new Animated.Value(0)).current;
  const phaseY = useRef(new Animated.Value(0)).current;
  const breath = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (!enabled) {
      phaseX.setValue(0);
      phaseY.setValue(0);
      breath.setValue(0);
      return;
    }

    const loops = [
      startLoop(phaseX, periodX, delay),
      startLoop(phaseY, periodY, delay + periodY * phaseOffset),
      startBreath(breath, Math.max(periodX, periodY) * 0.55, delay + 400),
    ];

    loops.forEach((loop) => loop.start());
    return () => loops.forEach((loop) => loop.stop());
  }, [breath, delay, enabled, periodX, periodY, phaseOffset, phaseX, phaseY]);

  const translateX = phaseX.interpolate({
    inputRange: [...SINE_IN],
    outputRange: SINE_OUT.map((v) => v * amplitudeX),
  });

  const translateY = phaseY.interpolate({
    inputRange: [...SINE_IN],
    outputRange: SINE_OUT.map((v) => v * amplitudeY),
  });

  const spin = phaseX.interpolate({
    inputRange: [0, 1],
    outputRange: [`-${rotate}deg`, `${rotate}deg`],
  });

  const scale = breath.interpolate({
    inputRange: [0, 1],
    outputRange: [1 - scalePulse, 1 + scalePulse],
  });

  const opacity = breath.interpolate({
    inputRange: [0, 1],
    outputRange: [1 - opacityPulse, 1],
  });

  return { translateX, translateY, spin, scale, opacity };
}

export function useSlowSpin(duration: number, enabled: boolean, delay = 0) {
  const spin = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (!enabled) {
      spin.setValue(0);
      return;
    }

    const loop = startLoop(spin, duration, delay);
    loop.start();
    return () => loop.stop();
  }, [delay, duration, enabled, spin]);

  const rotate = spin.interpolate({
    inputRange: [0, 1],
    outputRange: ["0deg", "360deg"],
  });

  return rotate;
}
