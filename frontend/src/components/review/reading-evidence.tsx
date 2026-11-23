"use client";
import { useEffect, useRef, useState } from "react";
import { extractionClient } from "@/lib/extraction-client";
import { vault } from "@/lib/vault-client";
import type { ReviewCandidate } from "@/lib/review-client";

export function ReadingEvidence({ candidate }: { candidate: ReviewCandidate }) {
  const canvas = useRef<HTMLCanvasElement>(null);
  const [status,setStatus] = useState("Loading source crop…");
  const [options,setOptions] = useState<{observed:string[];calculated:string[]}|null>(null);
  useEffect(() => {
    const controller = new AbortController();
    let url: string | undefined;
    const source = candidate.extracted.source;
    async function load() {
      const result = await extractionClient.get(candidate.documentId);
      if(controller.signal.aborted)return;
      const sameRevision=result.result.candidates.some(c=>c.id===candidate.id);
      const check=sameRevision ? result.result.pages?.find(p=>p.number===source.page)?.lines
        .find(l=>l.start===source.start && l.end===source.end)?.check : null;
      setOptions(check ?? null);
      const box=check?.box ?? source.box;
      if(!box){setStatus("Open the original page to check this reading.");return;}
      const blob=await vault.preview(candidate.documentId,source.page,controller.signal);
      if(controller.signal.aborted)return;
      url=URL.createObjectURL(blob);
      const img=new Image();img.src=url;await img.decode();
      const el=canvas.current;
      if(!el || controller.signal.aborted)return;
      const x=Math.max(0,Math.floor(img.width*box.x)-4), y=Math.max(0,Math.floor(img.height*box.y)-4);
      const w=Math.min(img.width-x,Math.ceil(img.width*box.width)+8), h=Math.min(img.height-y,Math.ceil(img.height*box.height)+8);
      const scale=Math.min(3,1000/w,400/h);
      el.width=Math.max(1,Math.round(w*scale));el.height=Math.max(1,Math.round(h*scale));
      const ctx=el.getContext("2d");if(!ctx)throw new Error();
      ctx.drawImage(img,x,y,w,h,0,0,el.width,el.height);setStatus("");
    }
    load().catch(()=>{if(!controller.signal.aborted)setStatus("Source crop unavailable. Open the original page to check this reading.");});
    return ()=>{controller.abort();if(url)URL.revokeObjectURL(url);};
  },[candidate]);
  return <section className="my-4 rounded-lg border border-border p-3"><h3 className="text-sm font-semibold">Original page {candidate.extracted.source.page}</h3>
    <canvas ref={canvas} hidden={!!status} aria-label="Crop of the original report" className="my-2 max-w-full"/>
    {status && <p role="status" className="text-sm">{status}</p>}
    {options && <><p className="text-sm">Read from image: {options.observed.join(" / ")}</p>
      {!!options.calculated.length && <p className="text-sm">Calculated suggestions (unverified): {options.calculated.join(" / ")}</p>}</>}
  </section>;
}
