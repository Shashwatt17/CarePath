"use client";
import { useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { reviewClient, originalFields, type Concept, type Fields, type Preview, type ReviewCandidate } from "@/lib/review-client";
import { NormalizationPreview } from "./normalization-preview";
export function CorrectionDialog({ candidate, onClose, onSaved }: { candidate: ReviewCandidate; onClose: () => void; onSaved: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null); const [fields, setFields] = useState<Fields>(() => originalFields(candidate.extracted));
  const [concepts, setConcepts] = useState<Concept[]>([]); const [preview, setPreview] = useState<Preview | null>(null); const [reason, setReason] = useState(""); const [reviewed, setReviewed] = useState(false); const [busy, setBusy] = useState(false); const [error, setError] = useState("");
  useEffect(() => { dialog.current?.showModal(); let active = true; reviewClient.concepts().then((c) => { if (active) setConcepts(c); }).catch(() => { if (active) setError("Concept choices could not be loaded. Close and retry."); }); return () => { active = false; }; }, []);
  function change(key: keyof Fields, value: string) { setFields((f) => ({ ...f, [key]: value || null })); setPreview(null); }
  async function validate(event: React.FormEvent) { event.preventDefault(); setBusy(true); setError(""); try { setPreview(await reviewClient.preview(candidate.id, fields)); } catch (e) { setError(e instanceof Error ? e.message : "Validation failed."); } finally { setBusy(false); } }
  async function save() { setBusy(true); setError(""); try { await reviewClient.correct(candidate.id, candidate.version, fields, reviewed, reason); onSaved(); } catch (e) { setError(e instanceof Error ? e.message : "Verification failed."); } finally { setBusy(false); } }
  const input = "mt-1 w-full rounded-md border border-border bg-white px-3 py-2 text-sm";
  return <dialog ref={dialog} onCancel={(e) => { if (busy) e.preventDefault(); }} onClose={onClose} aria-labelledby="correction-title" className="m-auto max-h-[90vh] w-[min(94vw,640px)] overflow-y-auto rounded-xl border border-border bg-white p-6 text-foreground backdrop:bg-black/40">
    <h2 id="correction-title" className="text-xl font-semibold">Review against the source</h2><p className="mt-2 text-sm text-muted-foreground">Check the original before confirming. Your changes are recorded separately; the machine extraction stays unchanged.</p>
    <blockquote className="my-4 whitespace-pre-wrap break-words rounded-lg bg-muted p-3 text-sm">{candidate.filename} · page {candidate.extracted.source.page}<br />{candidate.extracted.source.text}</blockquote>
    <form onSubmit={validate} className="space-y-4"><fieldset disabled={busy} className="space-y-4"><div className="grid gap-4 sm:grid-cols-2">
      <label className="text-sm">Test name<input className={input} required maxLength={160} value={fields.testName ?? ""} onChange={(e) => change("testName", e.target.value)} /></label>
      <label className="text-sm">Value as written<input className={input} required maxLength={80} value={fields.value ?? ""} onChange={(e) => change("value", e.target.value)} /></label>
      <label className="text-sm">Unit (if supplied)<input className={input} maxLength={50} value={fields.unit ?? ""} onChange={(e) => change("unit", e.target.value)} /></label>
      <label className="text-sm">Supplied reference range<input className={input} maxLength={255} value={fields.referenceRange ?? ""} onChange={(e) => change("referenceRange", e.target.value)} /></label>
      <label className="text-sm">Report date (if supported)<input className={input} type="date" value={fields.date ?? ""} onChange={(e) => change("date", e.target.value)} /></label>
      <label className="text-sm">Concept selection<select className={input} value={fields.conceptId ?? ""} onChange={(e) => change("conceptId", e.target.value)}><option value="">Use deterministic name mapping</option>{concepts.map((c) => <option key={c.id} value={c.id}>{c.name} ({c.unit ?? "unit unknown"})</option>)}</select></label>
    </div><Button type="submit" variant="outline">{busy ? "Checking…" : "Preview normalization"}</Button></fieldset></form>
    {preview && <div className="mt-4"><NormalizationPreview preview={preview} />{!preview.canVerify && <p role="alert" className="mt-2 text-sm text-red-800">These fields cannot be verified. Correct them using the source, or reject the candidate.</p>}</div>}
    <label className="mt-4 block text-sm">Reason for correction or explicit source verification<textarea className={input} disabled={busy} minLength={10} maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} /></label>
    <label className="my-4 flex items-start gap-2 text-sm"><input type="checkbox" disabled={busy} className="mt-1" checked={reviewed} onChange={(e) => setReviewed(e.target.checked)} />I checked the source and confirm these fields reflect it. Verification does not establish a diagnosis.</label>
    {error && <p role="alert" className="my-3 text-sm text-red-800">{error}</p>}<div className="flex flex-wrap justify-end gap-3"><Button variant="outline" disabled={busy} onClick={() => dialog.current?.close()}>Cancel</Button><Button disabled={busy || !preview?.canVerify || !reviewed || reason.trim().length < 10} onClick={save}>Correct and verify</Button></div>
  </dialog>;
}
