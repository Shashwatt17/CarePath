"use client";
import { useState, useEffect, useRef } from "react";
import { UploadCloud, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { vault, validateFile, fileSize, type Metadata } from "@/lib/vault-client";
import { MetadataFields, emptyMetadata, cleanMetadata } from "./metadata-fields";
export function UploadPanel({ onUploaded }: { onUploaded: () => void }) {
  const [maximum, setMaximum] = useState<number | null>(null); const [file, setFile] = useState<File | null>(null);
  const [metadata, setMetadata] = useState<Metadata>(emptyMetadata); const [busy, setBusy] = useState(false); const [progress, setProgress] = useState(0);
  const [error, setError] = useState(""); const [success, setSuccess] = useState(""); const input = useRef<HTMLInputElement>(null);
  useEffect(() => { let active = true; vault.config().then((c) => { if (active) setMaximum(c.maxBytes); }).catch(() => { if (active) setError("Upload configuration is unavailable. Reload to retry."); }); return () => { active = false; }; }, []);
  function choose(next: File | undefined) {
    if (busy || !next || !maximum) return; setSuccess(""); const problem = validateFile(next, maximum); setError(problem ?? ""); setFile(problem ? null : next);
  }
  async function upload(event: React.FormEvent) {
    event.preventDefault(); if (!file) return; setBusy(true); setError(""); setSuccess(""); setProgress(0);
    try { await vault.upload(file, cleanMetadata(metadata), setProgress); setSuccess("Uploaded. Your original is private to your account."); setFile(null); setMetadata(emptyMetadata); if (input.current) input.current.value = ""; onUploaded(); }
    catch (e) { setError(e instanceof Error ? e.message : "Upload could not be completed."); }
    finally { setBusy(false); }
  }
  return <details className="my-7 rounded-xl border border-border bg-white p-5" open><summary className="cursor-pointer font-semibold">Upload a medical document</summary><form className="mt-5 space-y-5" onSubmit={upload}>
    <div className="rounded-lg border-2 border-dashed border-border bg-background p-6 text-center" onDragOver={(e) => e.preventDefault()} onDrop={(e) => { e.preventDefault(); choose(e.dataTransfer.files[0]); }}>
      <UploadCloud className="mx-auto mb-3 text-primary" aria-hidden="true" /><label className="cursor-pointer font-medium">Choose a file or drop it here<input ref={input} className="mt-3 block w-full text-sm file:mr-3 file:rounded-md file:border-0 file:bg-muted file:px-4 file:py-2" aria-label="Medical document file" type="file" accept=".pdf,.jpg,.jpeg,.png" disabled={busy || !maximum} onChange={(e) => choose(e.target.files?.[0])} /></label>
      <p className="mt-3 text-sm text-muted-foreground">PDF, JPG, JPEG or PNG · {maximum ? `Maximum ${fileSize(maximum)}` : "Loading upload limit…"}</p>{file && <p className="mt-3 break-all text-sm">Selected: {file.name} · {fileSize(file.size)}</p>}
    </div>
    {file && <MetadataFields value={metadata} onChange={setMetadata} disabled={busy} />}
    {busy && <div role="status"><label htmlFor="upload-progress" className="text-sm">{progress === 100 ? "Upload received. Validating and saving…" : `Uploading ${progress}%`}</label><progress id="upload-progress" className="mt-2 block w-full accent-primary" value={progress} max={100} /></div>}
    {error && <p role="alert" className="text-sm text-red-800">{error}</p>}{success && <p role="status" className="text-sm text-primary">{success}</p>}
    <div className="flex flex-wrap items-center justify-between gap-4"><p className="flex items-center gap-2 text-xs text-muted-foreground"><ShieldCheck size={16} aria-hidden="true" />Private to your account. Never automatically shared.</p><Button type="submit" disabled={!file || busy}>{busy ? "Uploading…" : "Upload document"}</Button></div>
  </form></details>;
}
