import { useEffect, useRef } from "react";
import { AccessibilityInfo, Animated, Easing } from "react-native";
import { animation } from "./theme";

export function useReduceMotion(): boolean {
  const reduced = useRef(false);
  useEffect(() => {
    AccessibilityInfo.isReduceMotionEnabled().then((v) => {
      reduced.current = v;
    });
    const sub = AccessibilityInfo.addEventListener("reduceMotionChanged", (v) => {
      reduced.current = v;
    });
    return () => sub.remove();
  }, []);
  return reduced.current;
}

export function fadeIn(
  value: Animated.Value,
  duration = animation.normal,
  delay = 0
): Animated.CompositeAnimation {
  return Animated.timing(value, {
    toValue: 1,
    duration,
    delay,
    easing: Easing.out(Easing.cubic),
    useNativeDriver: true,
  });
}

export function slideUp(
  value: Animated.Value,
  from = 16,
  duration = animation.normal,
  delay = 0
): Animated.CompositeAnimation {
  value.setValue(from);
  return Animated.timing(value, {
    toValue: 0,
    duration,
    delay,
    easing: Easing.out(Easing.cubic),
    useNativeDriver: true,
  });
}

export function pressScale(anim: Animated.Value, pressed: boolean) {
  Animated.spring(anim, {
    toValue: pressed ? animation.pressScale : 1,
    friction: 5,
    tension: 300,
    useNativeDriver: true,
  }).start();
}
