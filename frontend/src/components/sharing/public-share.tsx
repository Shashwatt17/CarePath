"use client";
import {useEffect,useRef,useState} from "react";
import {accessShare,downloadSharedReport,type PublicSnapshot} from "@/lib/share-client";
import {SharedSnapshotView,SharedUnavailableView} from "@/lib/share-view";
export function PublicShare(){
 const [downloadError,setDownloadError]=useState("");const downloading=useRef(false);
 const token=useRef(""); const [snapshot,setSnapshot]=useState<PublicSnapshot|null>(null);const [loading,setLoading]=useState(true);
 useEffect(()=>{
  if(!token.current){token.current=window.location.hash.slice(1);window.history.replaceState(null,"","/share");}
  let active=true;let pending=false;let generation=0;let controller:AbortController|null=null;let expiry:ReturnType<typeof setTimeout>|undefined;
  async function load(){if(pending||document.hidden)return;pending=true;const request=++generation;controller=new AbortController();
   try{const result=await accessShare(token.current,controller.signal);if(!active||request!==generation)return;
    const remaining=Date.parse(result.expiresAt)-Date.now();if(remaining<=0)throw new Error("Expired");setSnapshot(result);clearTimeout(expiry);expiry=setTimeout(()=>setSnapshot(null),Math.min(remaining,2147483647));
   }catch{if(active&&request===generation)setSnapshot(null);}finally{pending=false;if(active)setLoading(false);}
  }
  function visibility(){if(document.hidden){generation++;controller?.abort();setSnapshot(null);}else void load();}
  void load();const poll=setInterval(()=>void load(),15000);document.addEventListener("visibilitychange",visibility);
  return()=>{active=false;controller?.abort();clearInterval(poll);clearTimeout(expiry);document.removeEventListener("visibilitychange",visibility);};
 },[]);
 async function download(item:number,evidence:number,filename:string){
  if(downloading.current)return;downloading.current=true;setDownloadError("");
  try{const blob=await downloadSharedReport(token.current,item,evidence);const url=URL.createObjectURL(blob);const a=document.createElement("a");a.href=url;a.download=filename;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}catch{setDownloadError("This report is unavailable. It may have been deleted, or access may have expired or been revoked.");}finally{downloading.current=false;}
 }
 return <main id="main" className="min-h-screen bg-muted/30 px-4 py-10"><p className="mx-auto mb-5 max-w-3xl text-xs text-muted-foreground">Private temporary access · Do not forward this link. Previously viewed information cannot be recalled.</p>{downloadError&&<p role="alert" className="mx-auto mb-4 max-w-3xl text-sm">{downloadError}</p>}{loading?<p role="status" className="text-center">Checking temporary access…</p>:snapshot?<SharedSnapshotView snapshot={snapshot} onDownload={download}/>:<SharedUnavailableView/>}</main>;
}
