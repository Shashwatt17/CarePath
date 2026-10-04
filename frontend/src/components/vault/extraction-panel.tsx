"use client";
import { useEffect, useRef, useState } from "react";
import { FileSearch, LoaderCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ReviewPanel } from "@/components/review/review-panel";
import { label } from "@/lib/vault-client";
import { extractionClient, type Extraction, type ProcessingStatus, type Source } from "@/lib/extraction-client";
export function ExtractionPanel({ id, onSource, onStateChange }: { id: string; onSource: (source: Source) => void; onStateChange: () => void }) {
  const [status, setStatus] = useState<ProcessingStatus | null>(null);
  const [extraction, setExtraction] = useState<Extraction | null>(null);
  const [error, setError] = useState(""); const [busy, setBusy] = useState(false); const [refresh, setRefresh] = useState(0);
  const callbacks = useRef({ onStateChange }); useEffect(() => { callbacks.current = { onStateChange }; }, [onStateChange]);
  const previous = useRef<string | null>(null);
  useEffect(() => {
    const controller = new AbortController(); let timer: ReturnType<typeof setTimeout>;
    async function poll() {
      try {
        const next = await extractionClient.status(id, controller.signal); if (controller.signal.aborted) return;
        setStatus(next); setError("");
        if (previous.current !== null && previous.current !== next.state) callbacks.current.onStateChange();
        previous.current = next.state;
        if (next.state === "COMPLETED" || next.state === "NEEDS_REVIEW") {
          const result = await extractionClient.get(id, controller.signal); if (!controller.signal.aborted) setExtraction(result);
        } else if (next.state === "PROCESSING") timer = setTimeout(poll, 2000);
      } catch (e) { if (!controller.signal.aborted) setError(e instanceof Error ? e.message : "Processing status unavailable."); }
    }
    void poll(); return () => { controller.abort(); clearTimeout(timer); };
  }, [id, refresh]);
  async function start(retry = false) {
    setBusy(true); setError("");
    try { const next = await (retry ? extractionClient.retry(id) : extractionClient.process(id)); setStatus(next); callbacks.current.onStateChange(); setRefresh((v) => v + 1); }
    catch (e) { setError(e instanceof Error ? e.message : "Processing could not be started."); }
    finally { setBusy(false); }
  }
  return <section className="mt-8 rounded-xl border border-border bg-white p-5 sm:p-7" aria-labelledby="extraction-title">
    <div className="flex flex-wrap items-center justify-between gap-3"><div><p className="text-xs font-medium uppercase tracking-widest text-primary">Document intelligence</p><h2 id="extraction-title" className="mt-2 text-xl font-semibold">Extracted results</h2></div>
      {status && <span role="status" className="rounded-full bg-muted px-3 py-1 text-sm">{label(status.state)}</span>}</div>
    {error && <p role="alert" className="mt-4 text-sm text-red-800">{error} <button className="underline" onClick={() => setRefresh((v) => v + 1)}>Reload status</button></p>}
    {!status && !error && <div role="status" className="mt-5 h-24 rounded bg-muted motion-safe:animate-pulse"><span className="sr-only">Loading processing status</span></div>}
    {status?.state === "UPLOADED" && <div className="mt-5 flex flex-wrap items-center justify-between gap-5"><p className="max-w-xl text-sm leading-relaxed text-muted-foreground">Read text and identify possible lab results from this document. The original stays unchanged. Extracted candidates are not verified medical observations.</p><Button disabled={busy} onClick={() => start()}><FileSearch size={16} aria-hidden="true" />{busy ? "Starting…" : "Process document"}</Button></div>}
    {status?.state === "PROCESSING" && <p role="status" className="mt-5 flex items-center gap-3 text-sm text-muted-foreground"><LoaderCircle size={18} className="motion-safe:animate-spin" aria-hidden="true" />Reading your document. You can leave this page; processing continues on the server.{status.attempts > 1 ? ` Attempt ${status.attempts}.` : ""}</p>}
    {status?.state === "FAILED" && <div className="mt-5"><p role="alert">We couldn&apos;t process this document.</p><p className="mt-2 text-sm text-muted-foreground">Your original remains in the vault. {status.retryAllowed ? "You can try again." : "Check the file before uploading a corrected copy."}</p>{status.retryAllowed && <Button className="mt-4" variant="outline" disabled={busy} onClick={() => start(true)}>Retry processing</Button>}</div>}
    {extraction && <div className="mt-5">
      <p className="rounded-lg bg-amber-50 p-4 text-sm leading-relaxed text-amber-950">{extraction.result.needsReview ? "CarePath couldn't confidently read every part of this document. Check the source before relying on these results." : "Extraction completed. These are unverified candidates from your document, not confirmed medical observations."} Review decisions below are stored separately from the original extraction.</p>
      <h3 className="mt-6 font-semibold">Document information</h3>
      <dl className="mt-3 grid gap-4 text-sm sm:grid-cols-3"><div><dt className="text-muted-foreground">Predicted category</dt><dd className="mt-1">{label(extraction.result.classification.category)} · {label(extraction.result.classification.confidence)} confidence</dd></div>
        <div><dt className="text-muted-foreground">Reported date</dt><dd className="mt-1">{extraction.result.information.date?.value ?? "Not found"}</dd></div>
        <div><dt className="text-muted-foreground">Reported provider / lab</dt><dd className="mt-1 break-words">{extraction.result.information.provider?.value ?? "Not found"}</dd></div></dl>
      <div className="mt-3 flex flex-wrap gap-4 text-xs">{extraction.result.information.date && <button className="text-primary underline" onClick={() => onSource(extraction.result.information.date!.source)}>View date source</button>}{extraction.result.information.provider && <button className="text-primary underline" onClick={() => onSource(extraction.result.information.provider!.source)}>View provider source</button>}{extraction.result.classification.evidence.map((source, i) => <button key={i} className="text-primary underline" onClick={() => onSource(source)}>View classification source · page {source.page}</button>)}</div>
      <p className="mt-3 text-xs text-muted-foreground">{extraction.result.ocrUsed ? "OCR was used. Recognition errors are possible." : "Read from embedded PDF text."} Your saved category and metadata were not changed.</p>
      {extraction.result.candidates.length ? <ReviewPanel documentId={id} onSource={onSource} onChanged={() => { setRefresh((v) => v + 1); onStateChange(); }} /> : <p className="mt-6 text-sm text-muted-foreground">No supported lab rows were found. This does not mean the document contains no medical information.</p>}
    </div>}
  </section>;
}
