import { useMemo } from "react";
import { StyleSheet, Text, View } from "react-native";
import Svg, { Rect, Text as SvgText } from "react-native-svg";
import { colors, radii, spacing, typography } from "../../lib/theme";

export type BarDatum = {
  label: string;
  value: number;
};

type OpsBarChartProps = {
  data: BarDatum[];
  title?: string;
  color?: string;
  height?: number;
};

const PADDING = { top: 8, right: 8, bottom: 36, left: 8 };

export function OpsBarChart({
  data,
  title,
  color = colors.primary,
  height = 160,
}: OpsBarChartProps) {
  const width = 340;

  const layout = useMemo(() => {
    if (data.length === 0) return null;
    const max = Math.max(...data.map((d) => d.value), 1);
    const plotW = width - PADDING.left - PADDING.right;
    const plotH = height - PADDING.top - PADDING.bottom;
    const gap = 6;
    const barW = Math.max(8, (plotW - gap * (data.length - 1)) / data.length);

    const bars = data.map((datum, index) => {
      const barH = (datum.value / max) * plotH;
      const x = PADDING.left + index * (barW + gap);
      const y = PADDING.top + plotH - barH;
      return { ...datum, x, y, barW, barH };
    });

    return { bars, plotH, max };
  }, [data, height, width]);

  if (!layout) {
    return <Text style={styles.empty}>No data</Text>;
  }

  const shortLabel = (label: string) => {
    const parts = label.split("-");
    return parts.length === 3 ? `${parts[1]}/${parts[2]}` : label;
  };

  return (
    <View style={styles.wrap}>
      {title ? <Text style={styles.title}>{title}</Text> : null}
      <Svg width={width} height={height} viewBox={`0 0 ${width} ${height}`}>
        {layout.bars.map((bar) => (
          <Rect
            key={bar.label}
            x={bar.x}
            y={bar.y}
            width={bar.barW}
            height={bar.barH}
            rx={radii.sm}
            fill={color}
            opacity={0.9}
          />
        ))}
        {layout.bars.map((bar) => (
          <SvgText
            key={`${bar.label}-label`}
            x={bar.x + bar.barW / 2}
            y={height - 8}
            fontSize={9}
            fill={colors.textMuted}
            textAnchor="middle"
          >
            {shortLabel(bar.label)}
          </SvgText>
        ))}
      </Svg>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { alignItems: "center", width: "100%" },
  title: { ...typography.subtitle, fontSize: 14, alignSelf: "flex-start", marginBottom: spacing.xs },
  empty: { ...typography.caption, color: colors.textMuted },
});
