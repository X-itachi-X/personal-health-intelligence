import { useMemo } from "react";
import { StyleSheet, Text, View } from "react-native";
import Svg, { Circle, Line, Polyline, Rect, Text as SvgText } from "react-native-svg";
import { colors, radii, spacing, typography } from "../../lib/theme";

export type ChartPoint = {
  date: string;
  value: number;
};

type TrendLineChartProps = {
  points: ChartPoint[];
  unit?: string | null;
  height?: number;
  compact?: boolean;
};

const CHART_PADDING = { top: 12, right: 8, bottom: 28, left: 40 };

export function TrendLineChart({ points, unit, height = 200, compact = false }: TrendLineChartProps) {
  const width = compact ? 280 : 340;

  const layout = useMemo(() => {
    if (points.length === 0) {
      return null;
    }

    const values = points.map((p) => p.value);
    const rawMin = Math.min(...values);
    const rawMax = Math.max(...values);
    const span = rawMax - rawMin;
    const padding = span === 0 ? Math.max(rawMax * 0.1, 1) : span * 0.12;
    const minY = rawMin - padding;
    const maxY = rawMax + padding;
    const plotW = width - CHART_PADDING.left - CHART_PADDING.right;
    const plotH = height - CHART_PADDING.top - CHART_PADDING.bottom;

    const coords = points.map((point, index) => {
      const x = CHART_PADDING.left + (points.length === 1 ? plotW / 2 : (index / (points.length - 1)) * plotW);
      const y = CHART_PADDING.top + plotH - ((point.value - minY) / (maxY - minY)) * plotH;
      return { x, y, point };
    });

    const polyline = coords.map((c) => `${c.x},${c.y}`).join(" ");

    const yTicks = [minY, minY + (maxY - minY) / 2, maxY].map((value) => {
      const y = CHART_PADDING.top + plotH - ((value - minY) / (maxY - minY)) * plotH;
      return { value, y };
    });

    const xLabels = compact
      ? [coords[0], coords[coords.length - 1]].filter(Boolean)
      : coords.filter((_, i) => i === 0 || i === coords.length - 1 || i === Math.floor(coords.length / 2));

    return { coords, polyline, yTicks, xLabels, plotW, plotH, minY, maxY };
  }, [compact, height, points, width]);

  if (!layout || points.length === 0) {
    return <Text style={styles.empty}>No chart data</Text>;
  }

  const formatValue = (value: number) => {
    if (Math.abs(value) >= 100) return value.toFixed(0);
    if (Math.abs(value) >= 10) return value.toFixed(1);
    return value.toFixed(2);
  };

  const formatDate = (iso: string) => {
    const parts = iso.split("-");
    if (parts.length === 3) return `${parts[1]}/${parts[2]}`;
    return iso;
  };

  return (
    <View style={styles.wrap}>
      <Svg width={width} height={height} viewBox={`0 0 ${width} ${height}`}>
        <Rect
          x={CHART_PADDING.left}
          y={CHART_PADDING.top}
          width={layout.plotW}
          height={layout.plotH}
          fill={colors.surfaceMuted}
          rx={radii.sm}
        />

        {layout.yTicks.map((tick) => (
          <Line
            key={tick.y}
            x1={CHART_PADDING.left}
            y1={tick.y}
            x2={CHART_PADDING.left + layout.plotW}
            y2={tick.y}
            stroke={colors.border}
            strokeWidth={1}
            strokeDasharray="4 4"
          />
        ))}

        <Polyline
          points={layout.polyline}
          fill="none"
          stroke={colors.primary}
          strokeWidth={2.5}
          strokeLinejoin="round"
          strokeLinecap="round"
        />

        {layout.coords.map(({ x, y, point }) => (
          <Circle key={`${point.date}-${point.value}`} cx={x} cy={y} r={compact ? 3 : 4} fill={colors.primaryDark} />
        ))}

        {layout.yTicks.map((tick) => (
          <SvgText
            key={`y-${tick.y}`}
            x={CHART_PADDING.left - 6}
            y={tick.y + 4}
            fontSize={10}
            fill={colors.textMuted}
            textAnchor="end"
          >
            {formatValue(tick.value)}
          </SvgText>
        ))}

        {layout.xLabels.map(({ x, point }) => (
          <SvgText
            key={`x-${point.date}`}
            x={x}
            y={height - 6}
            fontSize={10}
            fill={colors.textMuted}
            textAnchor="middle"
          >
            {formatDate(point.date)}
          </SvgText>
        ))}
      </Svg>

      {!compact && unit ? <Text style={styles.unitLabel}>Unit: {unit}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { alignItems: "center", width: "100%" },
  empty: { ...typography.caption, color: colors.textMuted, paddingVertical: spacing.md },
  unitLabel: { ...typography.caption, color: colors.textMuted, marginTop: spacing.xs },
});
