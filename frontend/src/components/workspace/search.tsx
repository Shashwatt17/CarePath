"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { workspace, resultLink, type SearchQuery, type SearchResult } from "@/lib/workspace-client";
import type { Page } from "@/lib/history-client";
import { label } from "@/lib/vault-client";
import { Button } from "@/components/ui/button";
import { Pages, Loading, ErrorNote, fieldClass } from "@/components/care/shared";
const initial:SearchQuery={q:"",kind:"ALL",from:"",to:"",provider:"",page:0};
export function RecordSearch() {
 const [form,setForm]=useState(initial),[query,setQuery]=useState<SearchQuery|null>(null),[revision,setRevision]=useState(0);
 const [data,setData]=useState<Page<SearchResult>|null>(null),[error,setError]=useState("");
 useEffect(()=>{if(!query)return;const c=new AbortController();workspace.search(query,c.signal).then(r=>{if(!c.signal.aborted){setData(r);setError("");}}).catch(()=>{if(!c.signal.aborted)setError("Search unavailable. Check the filters or retry shortly.");});return()=>c.abort();},[query,revision]);
 return <><p className="text-xs uppercase tracking-widest text-primary">Your records only</p><h1 className="mt-2 text-3xl font-semibold">Search CarePath</h1><p className="my-4 max-w-2xl text-sm text-muted-foreground">Find documents, verified tests, symptoms and appointments. Try CBC, hemoglobin or September prescription. Month names filter recorded dates; use the date range to select a year. Appointment dates use UTC.</p>
 <form className="grid gap-4 border-y border-border py-5 md:grid-cols-3" onSubmit={e=>{e.preventDefault();setQuery({...form,page:0});setData(null);setError("");}}>
 <label className="text-sm md:col-span-2">Search terms<input className={fieldClass} maxLength={80} value={form.q} onChange={e=>setForm({...form,q:e.target.value})} placeholder="Hemoglobin or September prescription"/></label>
 <label className="text-sm">Record type<select className={fieldClass} value={form.kind} onChange={e=>setForm({...form,kind:e.target.value as SearchQuery["kind"]})}>{["ALL","DOCUMENT","OBSERVATION","SYMPTOM","APPOINTMENT"].map(k=><option key={k}>{k}</option>)}</select></label>
 <label className="text-sm">From date<input type="date" className={fieldClass} value={form.from} onChange={e=>setForm({...form,from:e.target.value})}/></label><label className="text-sm">To date<input type="date" className={fieldClass} min={form.from} value={form.to} onChange={e=>setForm({...form,to:e.target.value})}/></label>
 <label className="text-sm">Provider / laboratory<input className={fieldClass} maxLength={80} value={form.provider} onChange={e=>setForm({...form,provider:e.target.value})}/></label><Button type="submit" className="justify-self-start">Search</Button></form>
 <ErrorNote message={error}/>{error&&<Button onClick={()=>{setError("");setData(null);setRevision(v=>v+1);}}>Retry</Button>}
 {query&&!data&&!error?<Loading/>:data?.items.length?<ul className="mt-5 divide-y divide-border">{data.items.map(r=><li key={`${r.kind}-${r.id}`} className="py-5"><p className="text-xs text-muted-foreground">{label(r.kind)}{r.kind==="OBSERVATION"?" · Verified":r.kind==="SYMPTOM"?" · User-reported":""} · {r.date??"Date not supplied"}</p><Link className="mt-1 block break-words text-lg font-medium text-primary underline" href={resultLink(r)}>{r.title}</Link><p className="mt-2 break-words text-sm">{r.summary}</p>{r.provider&&<p className="mt-1 text-sm text-muted-foreground">{r.provider}</p>}</li>)}</ul>:data&&<p role="status" className="my-8 text-muted-foreground">No matching records. Try fewer terms or a wider date range.</p>}
 {data&&query&&<Pages page={query.page} total={data.total} change={page=>{setQuery({...query,page});setData(null);setError("");}}/>}</>;
}
