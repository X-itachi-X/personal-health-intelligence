import { ReactNode, useRef } from "react";
import { Animated, Pressable, PressableProps, StyleProp, ViewStyle } from "react-native";
import { pressScale } from "../../lib/motion";

type AnimatedPressableProps = PressableProps & {
  children: ReactNode;
  style?: StyleProp<ViewStyle>;
};

export function AnimatedPressable({ children, style, onPressIn, onPressOut, ...rest }: AnimatedPressableProps) {
  const scale = useRef(new Animated.Value(1)).current;

  return (
    <Pressable
      onPressIn={(e) => {
        pressScale(scale, true);
        onPressIn?.(e);
      }}
      onPressOut={(e) => {
        pressScale(scale, false);
        onPressOut?.(e);
      }}
      {...rest}
    >
      <Animated.View style={[style, { transform: [{ scale }] }]}>{children}</Animated.View>
    </Pressable>
  );
}
