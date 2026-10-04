"use client";
import {useEffect,useRef,useState} from "react";
import {accessShare,type PublicSnapshot} from "@/lib/share-client";
import {SharedSnapshotView,SharedUnavailableView} from "@/lib/share-view";
export function PublicShare(){
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
 return <main id="main" className="min-h-screen bg-muted/30 px-4 py-10"><p className="mx-auto mb-5 max-w-3xl text-xs text-muted-foreground">Private temporary access · Do not forward this link. Previously viewed information cannot be recalled.</p>{loading?<p role="status" className="text-center">Checking temporary access…</p>:snapshot?<SharedSnapshotView snapshot={snapshot}/>:<SharedUnavailableView/>}</main>;
}
