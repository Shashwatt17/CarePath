"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { FileText, ArrowUpRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { vault, categories, label, fileSize, type DocumentPage } from "@/lib/vault-client";
import { UploadPanel } from "./upload-panel";
export function RecordsVault() {
  const [data, setData] = useState<DocumentPage | null>(null); const [loading, setLoading] = useState(true); const [error, setError] = useState("");
  const [query, setQuery] = useState(""); const [page, setPage] = useState(0); const [revision, setRevision] = useState(0);
  useEffect(() => { let active = true; const params = new URLSearchParams(query); params.set("page", String(page)); params.set("size", "10");
    vault.list(params).then((result) => { if (active) { setData(result); setError(""); } }).catch((e: unknown) => { if (active) setError(e instanceof Error ? e.message : "Records could not be loaded."); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [query, page, revision]);
  function reload() { setLoading(true); setRevision((v) => v + 1); }
  function filter(event: React.FormEvent<HTMLFormElement>) { event.preventDefault(); const values = new FormData(event.currentTarget); const params = new URLSearchParams(); for (const [key, value] of values) if (String(value)) params.set(key, String(value)); setLoading(true); setPage(0); setQuery(params.toString()); setRevision((v) => v + 1); }
  return <><p className="text-xs font-semibold uppercase tracking-widest text-primary">Your private health library</p><h1 className="mt-3 text-3xl font-semibold tracking-tight">Medical records</h1><p className="mt-3 max-w-2xl text-muted-foreground">Keep your reports and medical documents together. Originals stay private. Open a document to extract candidate results and inspect their source.</p>
    <UploadPanel onUploaded={() => { setPage(0); reload(); }} />
    <section aria-labelledby="records-heading"><div className="mb-5 flex items-center justify-between"><h2 id="records-heading" className="text-xl font-semibold">Your documents {data && <span className="ml-2 text-sm font-normal text-muted-foreground">{data.total}</span>}</h2><Button variant="outline" size="sm" onClick={reload}>Refresh</Button></div>
    <form onSubmit={filter} className="mb-5 grid gap-3 rounded-lg border border-border bg-white p-4 sm:grid-cols-2 lg:grid-cols-4">
      <label><span className="field-label">Filename</span><input className="field-input" name="filename" placeholder="Search documents" maxLength={255} /></label>
      <label><span className="field-label">Category</span><select className="field-input" name="type"><option value="">All categories</option>{categories.map((c) => <option key={c} value={c}>{label(c)}</option>)}</select></label>
      <label><span className="field-label">Provider / lab</span><input className="field-input" name="provider" maxLength={255} /></label>
      <label><span className="field-label">Processing status</span><select className="field-input" name="status"><option value="">All statuses</option>{["UPLOADED", "PROCESSING", "NEEDS_REVIEW", "COMPLETED", "FAILED"].map((s) => <option key={s} value={s}>{label(s)}</option>)}</select></label>
      <label><span className="field-label">Document date from</span><input className="field-input" name="from" type="date" /></label><label><span className="field-label">Document date to</span><input className="field-input" name="to" type="date" /></label>
      <label><span className="field-label">Uploaded from</span><input className="field-input" name="uploadedFrom" type="date" /></label><label><span className="field-label">Uploaded to</span><input className="field-input" name="uploadedTo" type="date" /></label>
      <Button type="submit" variant="outline" className="sm:col-span-2 lg:col-span-4">Apply filters</Button>
    </form>
    {error && <p role="alert" className="mb-5 rounded-lg bg-red-50 p-4 text-red-900">{error} <button className="underline" onClick={reload}>Retry</button></p>}
    {loading ? <div aria-busy="true" aria-label="Loading records" className="space-y-3">{[1, 2, 3].map((n) => <div key={n} className="h-20 rounded-lg bg-muted motion-safe:animate-pulse" />)}</div> : !error && (!data?.items.length ? <div className="rounded-xl border border-dashed border-border p-12 text-center"><FileText className="mx-auto mb-4 text-muted-foreground" aria-hidden="true" /><h3 className="font-semibold">No documents to show</h3><p className="mt-2 text-sm text-muted-foreground">Upload your first document above, or adjust your filters.</p></div> : <ul className="divide-y divide-border rounded-xl border border-border bg-white">{data.items.map((d) => <li key={d.id}><Link className="flex items-center gap-4 p-5 transition-colors hover:bg-muted focus-visible:outline-primary" href={`/records/${d.id}`}><FileText className="shrink-0 text-primary" aria-hidden="true" /><div className="min-w-0 flex-1"><h3 className="truncate font-medium">{d.originalFilename}</h3><p className="mt-1 text-sm text-muted-foreground">{label(d.documentType)} · {d.documentDate ?? "Date not supplied"} · {fileSize(d.byteSize)}</p>{d.providerName && <p className="mt-1 truncate text-xs text-muted-foreground">{d.providerName}</p>}</div><span className="rounded-full bg-muted px-3 py-1 text-xs">{label(d.status)}</span><ArrowUpRight size={18} className="hidden sm:block" aria-hidden="true" /></Link></li>)}</ul>)}
    <nav aria-label="Document pages" className="mt-5 flex items-center justify-between"><Button variant="outline" disabled={loading || page === 0} onClick={() => { setLoading(true); setPage((v) => v - 1); }}>Previous</Button><span className="text-sm">Page {page + 1}</span><Button variant="outline" disabled={loading || !data || (page + 1) * data.size >= data.total} onClick={() => { setLoading(true); setPage((v) => v + 1); }}>Next</Button></nav>
    </section></>;
}
