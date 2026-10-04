"use client";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { reminderOffsets } from "@/lib/care-client";
export const fieldClass = "mt-1 block w-full rounded-md border border-border bg-white px-3 py-2 text-sm";
export function Field({ label, ...props }: React.InputHTMLAttributes<HTMLInputElement> & { label: string }) { return <label className="block text-sm">{label}<input {...props} className={fieldClass} /></label>; }
export function Notes({ value }: { value?: string | null }) { return <label className="block text-sm md:col-span-2">Notes<textarea name="notes" maxLength={2000} defaultValue={value ?? ""} className={fieldClass} rows={3} /></label>; }
export function OffsetFields({ values = [1440] }: { values?: number[] }) { return <fieldset className="md:col-span-2"><legend className="text-sm font-medium">In-app reminders</legend><div className="mt-2 flex flex-wrap gap-4">{reminderOffsets.map((o) => <label key={o.value} className="flex items-center gap-2 text-sm"><input type="checkbox" name="offsets" value={o.value} defaultChecked={values.includes(o.value)} />{o.label}</label>)}</div><p className="mt-2 text-xs text-muted-foreground">Past trigger times are delivered on the next scheduler run. Email is not configured.</p></fieldset>; }
export function Pages({ page, total, change }: { page: number; total: number; change: (page: number) => void }) { return <nav className="mt-6 flex items-center justify-between gap-3" aria-label="Result pages"><Button variant="outline" disabled={page === 0} onClick={() => change(page - 1)}>Previous</Button><span className="text-sm">Page {page + 1} · {total} items</span><Button variant="outline" disabled={(page + 1) * 20 >= total} onClick={() => change(page + 1)}>Next</Button></nav>; }
export function ErrorNote({ message }: { message: string }) { return message ? <p role="alert" className="my-4 rounded-lg bg-red-50 p-4 text-sm text-red-900">{message}</p> : null; }
export function Loading() { return <div role="status" className="my-6 h-40 rounded-lg bg-muted motion-safe:animate-pulse"><span className="sr-only">Loading care records</span></div>; }
export function useCarePage<T>(load: () => Promise<T>, deps: readonly unknown[]) {
 const [data, setData] = useState<T | null>(null); const [error, setError] = useState(""); const [revision, setRevision] = useState(0); const [loading, setLoading] = useState(true);
 useEffect(() => { let active = true; load().then((d) => { if (active) { setData(d); setError(""); setLoading(false); } }).catch(() => { if (active) { setError("Care records are unavailable. Please retry."); setLoading(false); } }); return () => { active = false; }; /* Caller supplies query dependencies. */
 // eslint-disable-next-line react-hooks/exhaustive-deps
 }, [...deps, revision]);
 return { data, error, loading, reload: () => setRevision((n) => n + 1), setError };
}
