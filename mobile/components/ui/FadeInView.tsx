import { ReactNode, useEffect, useRef } from "react";
import { Animated, ViewStyle } from "react-native";
import { fadeIn, slideUp } from "../../lib/motion";
import { animation } from "../../lib/theme";

type FadeInViewProps = {
  children: ReactNode;
  delay?: number;
  style?: ViewStyle;
};

export function FadeInView({ children, delay = 0, style }: FadeInViewProps) {
  const opacity = useRef(new Animated.Value(0)).current;
  const translateY = useRef(new Animated.Value(16)).current;

  useEffect(() => {
    Animated.parallel([
      fadeIn(opacity, animation.normal, delay),
      slideUp(translateY, 16, animation.normal, delay),
    ]).start();
  }, [delay, opacity, translateY]);

  return (
    <Animated.View style={[style, { opacity, transform: [{ translateY }] }]}>
      {children}
    </Animated.View>
  );
}
