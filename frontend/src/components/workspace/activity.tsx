"use client";
import { useEffect, useState } from "react";
import { workspace, activityCategories, type Activity } from "@/lib/workspace-client";
import type { Page } from "@/lib/history-client";
import { label } from "@/lib/vault-client";
import { Button } from "@/components/ui/button";
import { Pages, Loading, ErrorNote, fieldClass } from "@/components/care/shared";
export function ActivityHistory() {
 const [category,setCategory]=useState("ALL"), [page,setPage]=useState(0), [revision,setRevision]=useState(0);
 const [data,setData]=useState<Page<Activity>|null>(null), [error,setError]=useState("");
 useEffect(()=>{const c=new AbortController();workspace.activity(category,page,c.signal).then(r=>{if(!c.signal.aborted){setData(r);setError("");}}).catch(()=>{if(!c.signal.aborted)setError("Activity is unavailable. Please retry.");});return()=>c.abort();},[category,page,revision]);
 return <><p className="text-xs uppercase tracking-widest text-primary">Account history</p><h1 className="mt-2 text-3xl font-semibold">Activity & security</h1><p className="my-4 max-w-2xl text-sm text-muted-foreground">Actions recorded for your account, newest first. This history excludes record contents, filenames, tokens and location. Unknown-account sign-in failures are not attributed to you.</p>
 <label className="block max-w-xs text-sm">Event category<select className={fieldClass} value={category} onChange={e=>{setCategory(e.target.value);setPage(0);setData(null);setError("");}}>{activityCategories.map(c=><option key={c} value={c}>{label(c)}</option>)}</select></label>
 <ErrorNote message={error}/>{error && <Button onClick={()=>{setError("");setData(null);setRevision(v=>v+1);}}>Retry</Button>}
 {!data&&!error?<Loading/>:data?.items.length?<ol className="mt-6 divide-y divide-border">{data.items.map((e,i)=><li key={`${e.occurredAt}-${i}`} className="flex flex-wrap items-center justify-between gap-3 py-4"><div><p className="font-medium">{label(e.action)}</p><p className="mt-1 text-xs text-muted-foreground">{label(e.outcome)} · {e.actor === "SHARE" ? "Shared-link recipient" : e.actor === "SYSTEM" ? "CarePath service" : "Account action"}</p></div><time className="text-sm text-muted-foreground" dateTime={e.occurredAt}>{new Date(e.occurredAt).toLocaleString()}</time></li>)}</ol>:!error&&<p role="status" className="my-8 text-muted-foreground">No activity in this category.</p>}
 {data&&<Pages page={page} total={data.total} change={p=>{setPage(p);setData(null);}}/>}</>;
}
