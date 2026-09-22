import { Ionicons } from "@expo/vector-icons";
import { IconName, iconSize } from "../../lib/icons";
import { colors } from "../../lib/theme";

type IconProps = {
  name: IconName;
  size?: keyof typeof iconSize | number;
  color?: string;
};

export function Icon({ name, size = "md", color = colors.text }: IconProps) {
  const resolved = typeof size === "number" ? size : iconSize[size];
  return <Ionicons name={name} size={resolved} color={color} />;
}
