"use client";
import { useRouter } from "next/navigation";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { evidenceUrl, type Series, type Point } from "@/lib/history-client";
import Link from "next/link";
type ChartPoint = Point & { timestamp: number; plot: number };
export function TrendChart({ series }: { series: Series }) {
  const router = useRouter();
  const data: ChartPoint[] = series.points.map((p) => ({ ...p, timestamp: Date.parse(`${p.date}T00:00:00Z`), plot: Number(p.normalizedValue) }));
  if (data.some((p) => !Number.isFinite(p.plot) || !Number.isFinite(p.timestamp))) return <p>Chart unavailable: a value or date cannot be plotted safely. See the readings below.</p>;
  function dot({ cx, cy, payload }: { cx?: number; cy?: number; payload?: ChartPoint }) {
    if (cx === undefined || cy === undefined || !payload) return <circle r={0} />;
    const open = () => router.push(evidenceUrl(payload.evidence));
    return <circle key={payload.id} cx={cx} cy={cy} r={5} fill="#126b61" stroke="white" strokeWidth={2} tabIndex={0} role="link" aria-label={`View evidence for ${payload.concept}, ${payload.normalizedValue} ${series.unit}, ${payload.date}`} className="cursor-pointer focus:outline-2 focus:outline-offset-2 focus:outline-primary" onClick={open} onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); open(); } }} />;
  }
  return <figure className="my-6 rounded-xl border border-border bg-white p-4 sm:p-6"><figcaption className="font-medium">{series.explanation}</figcaption><p className="mt-2 text-xs text-muted-foreground">Unit: {series.unit}. Observed points only; straight connections are visual guides, not estimated measurements. Select a point for evidence.</p>
    <div className="mt-4 h-64 w-full" role="img" aria-label={`${series.points[0]?.concept} history in ${series.unit}. Exact readings and evidence links follow the chart.`}><ResponsiveContainer width="100%" height="100%"><LineChart data={data} accessibilityLayer margin={{ top: 15, right: 25, bottom: 10, left: 5 }}><CartesianGrid vertical={false} stroke="#e2e8e5" /><XAxis dataKey="timestamp" type="number" domain={["dataMin", "dataMax"]} tickFormatter={(value: number) => new Date(value).toLocaleDateString(undefined, { month: "short", year: "2-digit", timeZone: "UTC" })} minTickGap={35} /><YAxis domain={["auto", "auto"]} width={55} /><Tooltip content={({ active, payload }) => { const p = payload?.[0]?.payload as ChartPoint | undefined; return active && p ? <div className="rounded border border-border bg-white p-3 text-sm shadow-sm"><p>{p.date}</p><p className="font-semibold">{p.normalizedValue} {p.normalizedUnit}</p><p>Page {p.evidence.page} · {p.evidence.filename}</p></div> : null; }} /><Line type="linear" dataKey="plot" stroke="#126b61" strokeWidth={2} dot={dot} activeDot={dot} connectNulls={false} isAnimationActive={false} /></LineChart></ResponsiveContainer></div>
    <details className="mt-3 text-sm"><summary className="cursor-pointer text-primary">Exact readings and accessible evidence links</summary><ul className="mt-3 space-y-2">{series.points.map((p) => <li key={p.id}><Link className="underline" href={evidenceUrl(p.evidence)}>{p.date} — {p.normalizedValue} {p.normalizedUnit} · {p.evidence.filename}, page {p.evidence.page}</Link></li>)}</ul></details>
  </figure>;
}
