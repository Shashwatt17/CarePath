"use client";
import Image from "next/image";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { visionClient, type VisionStatus, type VisionView } from "@/lib/vision-client";
import { vault, label } from "@/lib/vault-client";
export function VisionPanel({ id, extractionId }: { id: string; extractionId: string }) {
  const [status, setStatus] = useState<VisionStatus | null>(null);
  const [page, setPage] = useState(0); const [consent, setConsent] = useState(false);
  const [view, setView] = useState<VisionView | null>(null); const [busy, setBusy] = useState(false);
  const [error, setError] = useState(""); const [preview, setPreview] = useState("");
  useEffect(() => { const c = new AbortController(); visionClient.status(id, c.signal).then(s => { setStatus(s); setPage(s.pages[0]?.page ?? 0); }).catch(() => { /* Optional service does not block original results. */ }); return () => c.abort(); }, [id, extractionId]);
  useEffect(() => {
    if (!page) return;
    const c = new AbortController();
    visionClient.get(id, page, c.signal).then(v => { if (!c.signal.aborted) setView(v); }).catch(() => { if (!c.signal.aborted) setError("Second-reading status is unavailable. Original readings remain available."); });
    return () => c.abort();
  }, [id, page, extractionId]);
  useEffect(() => {
    if (!view?.result) return; const c = new AbortController(); let url = "";
    vault.preview(id, page, c.signal).then(blob => { if (!c.signal.aborted) { url = URL.createObjectURL(blob); setPreview(url); } }).catch(() => { if (!c.signal.aborted) setError("Source preview unavailable. Check the original before reviewing."); });
    return () => { c.abort(); if (url) URL.revokeObjectURL(url); };
  }, [id, page, view]);
  async function read() {
    if (!status || !consent || busy) return; setBusy(true); setError("");
    try { setView(await visionClient.read(id, extractionId, page, status.consentVersion)); }
    catch (e) { setError(e instanceof Error ? e.message : "Second reading failed."); const saved = await visionClient.get(id, page).catch(() => null); if (saved) setView(saved); }
    finally { setBusy(false); }
  }
  if (!status || !page) return null;
  return <section className="mt-5 rounded-xl border border-teal-200 bg-teal-50/60 p-4" aria-labelledby="vision-title">
    <h3 id="vision-title" className="font-semibold text-emerald-950">Second reading for difficult pages</h3>
    <p className="mt-2 text-sm">Compare an optional AI reading with the original. It may still misread numbers and will not update your medical history automatically.</p>
    {!status.available && <p className="mt-2 text-sm text-muted-foreground">External reading is not enabled. You can still review and correct the original extraction below.</p>}
    <label className="mt-3 block text-sm">Page <select className="ml-2 rounded border bg-white p-2" value={page} disabled={busy} onChange={e => { setConsent(false); setView(null); setError(""); setPreview(""); setPage(Number(e.target.value)); }}>{status.pages.map(p => <option key={p.page} value={p.page}>{p.page}</option>)}</select></label>
    {status.available && view?.state === "NOT_REQUESTED" && <div className="mt-3 space-y-3">
      <p className="text-sm">This sends the selected page, including any personal and medical information visible on it, to OpenAI. Provider retention depends on the configured account; this is not a promise of zero retention. The second reading is stored with this document.</p>
      <label className="flex items-start gap-2 text-sm"><input className="mt-1" type="checkbox" checked={consent} disabled={busy} onChange={e => setConsent(e.target.checked)} />I consent to sending this page to OpenAI for a second reading.</label>
      <Button disabled={!consent || busy} onClick={read}>{busy ? "Reading page…" : "Read this page with AI"}</Button>
    </div>}
    {error && <p role="alert" className="mt-3 text-sm text-red-800">{error}</p>}
    {view?.state === "REQUESTED" && <p role="status" className="mt-3 text-sm">A reading was requested. Reload this report shortly to check its status. No duplicate request will be sent.</p>}
    {view?.state === "FAILED" && <p className="mt-3 text-sm">The second reading did not complete. Check the original or upload a clearer copy. Automatic paid retries are disabled.</p>}
    {view?.result && <div className="mt-4">
      <p className="rounded bg-amber-100 p-3 text-sm text-amber-950">Unverified AI transcription. Check every proposed value against the original before using Correct / verify below. New readings absent from the original extraction are not added to your profile by this feature.</p>
      {view.result.unreadable && <p className="mt-2 text-sm">The model reported unclear content. A clearer original is recommended.</p>}
      <div className="mt-3 grid gap-4 lg:grid-cols-2">
        {preview && <div><a href={preview} target="_blank" rel="noreferrer" className="text-sm underline">Open original page at full size</a>{/* Blob URL is owner-scoped and revoked on page change. */}<Image src={preview} alt={`Original report page ${page}`} width={1000} height={1400} unoptimized className="mt-2 h-auto w-full rounded border bg-white" /></div>}
        <div className="overflow-x-auto"><table className="w-full text-left text-sm"><caption className="mb-2 text-left">AI proposals · all require source review</caption><thead className="bg-emerald-900 text-white"><tr><th className="p-2">Test</th><th className="p-2">Reading</th><th className="p-2">Date / column</th></tr></thead><tbody>{view.result.readings.map(({ reading: r, warnings }, i) => <tr key={i} className="border-b bg-white/80 align-top"><td className="p-2 font-medium">{r.testName}<div className="mt-2 text-xs font-normal text-amber-900">{warnings.filter(w => !["MODEL_OUTPUT_UNVERIFIED", "SOURCE_REVIEW_REQUIRED"].includes(w)).map(label).join(" · ")}</div></td><td className="p-2">{r.value ?? "Unreadable"} {r.unit}<div className="mt-1 whitespace-pre-wrap text-xs">Printed range: {r.referenceRange ?? "Not found"}</div></td><td className="p-2">{r.date ?? "Unknown date"}<div className="mt-1 text-xs">{r.column ?? "Unknown column"}</div></td></tr>)}</tbody></table>{!view.result.readings.length && <p className="mt-3 text-sm">No readings were returned. This does not mean the page contains no results.</p>}</div>
      </div>
    </div>}
  </section>;
}
