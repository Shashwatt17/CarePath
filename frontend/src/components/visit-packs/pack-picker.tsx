"use client";
import { useEffect, useState } from "react";
import { care } from "@/lib/care-client";
import { vault } from "@/lib/vault-client";
import { assistantClient } from "@/lib/assistant-client";
import { historyClient, type Concept } from "@/lib/history-client";
import { type PackType, type Selection } from "@/lib/pack-client";
import { Button } from "@/components/ui/button";
import { fieldClass, ErrorNote, Loading } from "@/components/care/shared";
type Choice = { id: string; title: string; context: string };
export function PackPicker({ kind, add, target = false }: { kind: Exclude<PackType, "MANUAL_QUESTION">; add: (selection: Selection, label: string) => void; target?: boolean }) {
 const [items, setItems] = useState<Choice[]>([]); const [page, setPage] = useState(0); const [total, setTotal] = useState(0); const [concepts, setConcepts] = useState<Concept[]>([]); const [concept, setConcept] = useState(""); const [error, setError] = useState(""); const [loading, setLoading] = useState(false); const [previous, setPrevious] = useState<Choice | null>(null);
 const numeric = kind === "OBSERVATION" || kind === "CHANGE";
 useEffect(() => { if (!numeric) return; let active = true; historyClient.concepts().then((p) => { if (active) setConcepts(p.items); }).catch(() => { if (active) setError("Verified concepts unavailable."); }); return () => { active = false; }; }, [numeric]);
 useEffect(() => {
  let active = true;
  async function load() {
   if (numeric && !concept) { setItems([]); setTotal(0); return; } setLoading(true); setError("");
   try {
    let rows: Choice[], count: number;
    if (numeric) { const p = await historyClient.history(concept, page); rows = p.history.items.map((v) => ({ id: v.id, title: `${v.concept}: ${v.effective.text ?? ""} ${v.effective.unit ?? ""}`, context: `${v.date ?? "Date unavailable"} · Verified · ${v.evidence.filename} · Page ${v.evidence.page}` })); count = p.history.total; }
    else if (kind === "SYMPTOM") { const p = await care.symptoms(page); rows = p.items.map((s) => ({ id: s.id, title: s.name, context: `${s.status} · severity ${s.severity}/10 · ${new Date(s.startedAt).toLocaleDateString()}` })); count = p.total; }
    else if (kind === "DOCUMENT") { const p = await vault.list(new URLSearchParams({ page: String(page), size: "20" })); rows = p.items.map((d) => ({ id: d.id, title: d.originalFilename, context: `${d.documentDate ?? "Date unavailable"} · ${d.documentType} · ${d.status}` })); count = p.total; }
    else if (kind === "APPOINTMENT") { const p = await care.appointments(page, target ? "UPCOMING" : "ALL"); rows = p.items.map((a) => ({ id: a.id, title: a.providerName, context: `${new Date(a.startsAt).toLocaleString()} · ${a.status}` })); count = p.total; }
    else if (kind === "FOLLOW_UP") { const p = await care.followUps(page); rows = p.items.filter((f) => ["CONFIRMED", "EDITED"].includes(f.status)).map((f) => ({ id: f.id, title: f.instruction, context: `${f.confirmedDate} · ${f.filename}` })); count = p.total; }
    else { const p = await assistantClient.list(page); rows = p.items.map((q) => ({ id: q.id, title: q.text, context: "Saved user draft · editable in this pack" })); count = p.total; }
    if (active) { setItems(rows); setTotal(count); }
   } catch { if (active) setError("Choices unavailable. Switch section or retry."); } finally { if (active) setLoading(false); }
  }
  void load(); return () => { active = false; };
 }, [kind, concept, page, numeric, target]);
 function choose(c: Choice) { if (kind === "CHANGE" && !previous) { setPrevious(c); return; } add({ type: kind, sourceId: previous?.id ?? c.id, ...(previous ? { otherId: c.id } : {}) }, previous ? `${previous.title} → ${c.title}` : c.title); setPrevious(null); }
 return <div className="mt-4"><ErrorNote message={error} />{numeric && <label className="block text-sm">Verified concept<select className={fieldClass} value={concept} onChange={(e) => { setConcept(e.target.value); setPage(0); setPrevious(null); }}><option value="">Choose a test</option>{concepts.map((c) => <option key={c.id} value={c.id}>{c.name} ({c.count} verified)</option>)}</select></label>}{kind === "CHANGE" && <p className="my-3 text-sm text-muted-foreground">Select an earlier reading, then a later reading of the same concept. CarePath will determine whether they can be compared.{previous && <span className="block mt-2">Earlier: {previous.title} · {previous.context} <button type="button" className="underline" onClick={() => setPrevious(null)}>Reset</button></span>}</p>}{loading ? <Loading /> : <ul className="mt-3 max-h-80 divide-y divide-border overflow-y-auto">{items.map((c) => <li className="flex items-start justify-between gap-3 py-3" key={c.id}><div className="min-w-0"><p className="break-words text-sm font-medium">{c.title}</p><p className="mt-1 break-words text-xs text-muted-foreground">{c.context}</p></div><Button type="button" variant="outline" size="sm" disabled={previous?.id === c.id} onClick={() => choose(c)}>{target ? "Use" : kind === "CHANGE" ? previous ? "Compare" : "Earlier" : "Add"}</Button></li>)}</ul>}{!loading && !items.length && <p className="my-4 text-sm text-muted-foreground">No eligible items on this page. Nothing is selected automatically.</p>}<div className="mt-3 flex gap-3"><Button type="button" variant="outline" size="sm" disabled={!page} onClick={() => setPage(page - 1)}>Previous</Button><Button type="button" variant="outline" size="sm" disabled={(page + 1) * 20 >= total} onClick={() => setPage(page + 1)}>More</Button></div></div>;
}
