import { Ionicons } from "@expo/vector-icons";

export type IconName = keyof typeof Ionicons.glyphMap;

export const navIcons: Record<string, { active: IconName; inactive: IconName }> = {
  home: { active: "home", inactive: "home-outline" },
  reports: { active: "document-text", inactive: "document-text-outline" },
  family: { active: "people", inactive: "people-outline" },
  trends: { active: "trending-up", inactive: "trending-up-outline" },
  dashboard: { active: "grid", inactive: "grid-outline" },
  settings: { active: "settings", inactive: "settings-outline" },
  features: { active: "sparkles", inactive: "sparkles-outline" },
  ask: { active: "chatbubbles", inactive: "chatbubbles-outline" },
  ops: { active: "stats-chart", inactive: "stats-chart-outline" },
  feedback: { active: "chatbox-ellipses", inactive: "chatbox-ellipses-outline" },
};

export const iconSize = {
  sm: 18,
  md: 22,
  lg: 28,
  xl: 40,
} as const;
