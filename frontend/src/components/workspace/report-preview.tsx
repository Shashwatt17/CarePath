"use client";

import { useState } from "react";
import { ArrowRight, FileText, FolderOpen, History } from "lucide-react";

const stops = [
  { title: "Report library", icon: FolderOpen, heading: "Your original report", label: "Lab report · 12 March", detail: "Lipid profile", note: "Open the document to see its extracted readings.", action: "Inspect the reading" },
  { title: "Source review", icon: FileText, heading: "Compare with the page", label: "Original · page 1", detail: "HDL cholesterol · 52.8 mg/dL", note: "Check the decimal and unit against the original before confirming.", action: "See patient summary" },
  { title: "Patient summary", icon: History, heading: "Ready for the visit", label: "Reviewed reading · 12 March", detail: "HDL cholesterol · 52.8 mg/dL", note: "Your clinician can follow a reading back to the report it came from.", action: "Back to the report" },
] as const;

export function ReportPreview() {
  const [index, setIndex] = useState(0);
  const stop = stops[index];
  return <section aria-label="Interactive sample workspace" className="sample-stage">
    <div className="sample-frame">
      <div className="flex items-center justify-between gap-3 border-b border-emerald-950/10 pb-4 text-xs font-semibold"><span className="flex items-center gap-2"><span className="size-2 rounded-full bg-primary" />Sample workspace</span><span>Fictional record</span></div>
      <nav aria-label="Explore the sample" className="my-5 flex flex-wrap gap-2">{stops.map(({title,icon:Icon},i)=><button key={title} aria-pressed={i===index} onClick={()=>setIndex(i)} className="sample-tab"><Icon size={15} aria-hidden="true"/>{title}</button>)}</nav>
      <article key={index} aria-live="polite" className="sample-paper">
        <div className="flex justify-between gap-3 text-sm text-primary"><span>{stop.label}</span><stop.icon size={22} aria-hidden="true"/></div>
        <h2 className="my-5 text-3xl font-semibold tracking-tight">{stop.heading}</h2>
        <p className="border-y border-dashed border-emerald-900/25 py-4 font-medium">{stop.detail}</p>
        <p className="mb-5 mt-4 text-sm text-muted-foreground">{stop.note}</p>
        <button className="inline-flex items-center gap-2 font-semibold text-primary underline underline-offset-4" onClick={()=>setIndex((index+1)%stops.length)}>{stop.action}<ArrowRight size={16}/></button>
      </article>
    </div>
  </section>;
}
