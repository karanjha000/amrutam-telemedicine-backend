import type { DailyConsultations } from '../types';

interface Bar {
  doctorId: string;
  label: string;
  value: number;
}

/**
 * Simple inline-SVG bar chart of consultations-per-doctor for a day.
 * Pure and data-driven (no network): the parent fetches the analytics and
 * passes the payload in, which also keeps the component trivially testable.
 */
export function DailyConsultationsChart({
  data,
  doctorNames
}: {
  data: DailyConsultations;
  doctorNames?: Record<string, string>;
}): JSX.Element {
  const bars: Bar[] = Object.entries(data.perDoctor).map(([doctorId, value]) => ({
    doctorId,
    label: doctorNames?.[doctorId] ?? doctorId.slice(0, 8),
    value
  }));

  const max = Math.max(1, ...bars.map((b) => b.value));
  const width = Math.max(280, bars.length * 90 + 40);
  const height = 180;
  const chartTop = 24;
  const chartBottom = height - 40;
  const plotHeight = chartBottom - chartTop;
  const barGap = 12;
  const barWidth = bars.length > 0 ? (width - 40 - (bars.length - 1) * barGap) / bars.length : 0;

  return (
    <figure className="chart" role="img" aria-label={`Consultations on ${data.date}: total ${data.total}`}>
      <svg
        className="chart__svg"
        width={width}
        height={height}
        viewBox={`0 0 ${width} ${height}`}
        data-testid="daily-consultations-chart"
      >
        {/* Baseline */}
        <line x1={20} x2={width - 20} y1={chartBottom} y2={chartBottom} stroke="var(--chart-axis)" />
        {bars.map((bar, i) => {
          const barHeight = Math.round((bar.value / max) * plotHeight);
          const x = 20 + i * (barWidth + barGap);
          const y = chartBottom - barHeight;
          return (
            <g key={bar.doctorId}>
              <rect
                className="chart__bar"
                x={x}
                y={y}
                width={barWidth}
                height={Math.max(barHeight, 2)}
                data-doctor-id={bar.doctorId}
              >
                <title>{`${bar.label}: ${bar.value}`}</title>
              </rect>
              <text className="chart__bar-value" x={x + barWidth / 2} y={y - 6} textAnchor="middle">
                {bar.value}
              </text>
              <text
                className="chart__bar-label"
                x={x + barWidth / 2}
                y={chartBottom + 16}
                textAnchor="middle"
              >
                {bar.label}
              </text>
            </g>
          );
        })}
      </svg>
      <figcaption>
        {data.date} · {data.total} consultation{data.total === 1 ? '' : 's'} total
      </figcaption>
    </figure>
  );
}
