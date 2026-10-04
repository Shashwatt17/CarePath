import "server-only";
export type SystemState = { available: boolean; stage?: string };
export async function readSystemState(): Promise<SystemState> {
  try {
    const base = process.env.BACKEND_URL ?? "http://localhost:8080";
    const response = await fetch(new URL("/api/v1/system/info", base), { cache: "no-store", signal: AbortSignal.timeout(2500) });
    if (!response.ok) return { available: false };
    const value: unknown = await response.json();
    if (typeof value !== "object" || value === null || !("stage" in value) || typeof value.stage !== "string") return { available: false };
    return { available: true, stage: value.stage };
  } catch {
    return { available: false };
  }
}
