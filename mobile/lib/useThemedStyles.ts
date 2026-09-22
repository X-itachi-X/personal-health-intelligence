import { useMemo } from "react";
import { useTheme } from "./ThemeContext";

export function useThemedStyles<T>(factory: (theme: ReturnType<typeof useTheme>) => T): T {
  const theme = useTheme();
  return useMemo(() => factory(theme), [theme]);
}
