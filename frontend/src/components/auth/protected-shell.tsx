"use client";
import Link from "next/link";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, useSyncExternalStore } from "react";
import { Button } from "@/components/ui/button";
import { authClient, ApiError } from "@/lib/auth-client";
import { useAuth } from "./auth-provider";

function subscribeTour(callback:()=>void){window.addEventListener("storage",callback);window.addEventListener("carepath-tour",callback);return()=>{window.removeEventListener("storage",callback);window.removeEventListener("carepath-tour",callback);};}

export function ProtectedShell({ children }: { children: React.ReactNode }) {
  const { user, state, error, retry, notify } = useAuth(); const router = useRouter();
  const pathname = usePathname();
  const [dismissedUser,setDismissedUser]=useState<string|null>(null);
  const tourStored=useSyncExternalStore(subscribeTour,()=>{try{return user?localStorage.getItem(`carepath-tour-v1:${user.id}`)==="done":true;}catch{return false;}},()=>true);
  const tour=Boolean(user&&!tourStored&&dismissedUser!==user.id);
  function dismissTour(){if(user){try{localStorage.setItem(`carepath-tour-v1:${user.id}`,"done");window.dispatchEvent(new Event("carepath-tour"));}catch{ /* Dismiss for this session when storage is unavailable. */ }setDismissedUser(user.id);}}
  const [logoutError, setLogoutError] = useState(""); const [busy, setBusy] = useState(false);
  useEffect(() => { if (state === "ready" && !user) router.replace("/login"); }, [state, user, router]);
  async function logout() {
    setBusy(true); setLogoutError("");
    try { await authClient.logout(); notify("signed-out"); router.replace("/login"); }
    catch (e) { setLogoutError(e instanceof ApiError ? e.message : "Sign out could not be confirmed. Please retry."); }
    finally { setBusy(false); }
  }
  if (state === "error") return <main id="main" className="mx-auto max-w-lg px-6 py-24"><h1 className="text-2xl font-semibold">Session check unavailable</h1><p role="alert" className="my-5">{error}</p><Button onClick={retry}>Retry</Button></main>;
  if (state === "loading" || !user) return <main id="main" className="mx-auto max-w-6xl px-6 py-20" aria-busy="true"><p role="status">Checking your session…</p><div className="mt-6 h-32 rounded-lg bg-muted motion-safe:animate-pulse" aria-hidden="true" /></main>;
  return <><header className="sticky top-0 z-30 border-b border-border bg-white/95 backdrop-blur"><div className="mx-auto max-w-6xl px-4 sm:px-6"><div className="flex items-center justify-between gap-3 py-4"><Link className="text-xl font-bold tracking-tight" href="/app">CarePath<span className="text-primary"> +</span></Link><div className="flex items-center gap-3"><Button asChild size="sm"><Link href="/records">Upload report</Link></Button><details className="relative"><summary className="cursor-pointer rounded-full border border-border px-3 py-2 text-sm">My account</summary><div className="absolute right-0 mt-2 w-56 rounded-xl border border-border bg-white p-4 shadow-lg"><p className="mb-3 truncate font-medium">{user.displayName}</p><Link className="block py-2 text-sm" href="/settings">Settings</Link><Link className="block py-2 text-sm" href="/onboarding">Help · Take the tour</Link><Link className="block py-2 text-sm" href="/shares">Manage sharing</Link><Button className="mt-3" variant="outline" size="sm" onClick={logout} disabled={busy}>{busy ? "Signing out…" : "Sign out"}</Button></div></details></div></div><nav aria-label="Workspace navigation" className="flex gap-1 overflow-x-auto pb-3">{[["/app","Overview"],["/records","My Reports"],["/patient-summary","Patient Summary"],["/timeline","Health History"],["/nearby-care","Find Care"]].map(([href,label]) => { const active=pathname===href || pathname.startsWith(href+"/") || (href==="/records" && pathname==="/review"); return <Link key={href} href={href} aria-current={active ? "page" : undefined} className={`shrink-0 rounded-lg px-4 py-2.5 text-sm font-medium transition-colors ${active ? "bg-primary text-white" : "text-muted-foreground hover:bg-muted hover:text-foreground"}`}>{label}</Link>; })}</nav></div></header>{logoutError && <p role="alert" className="mx-auto max-w-6xl px-6 py-3 text-red-900">{logoutError}</p>}<main id="main" className="mx-auto max-w-6xl px-6 py-12">{tour && <section aria-label="Welcome to CarePath" className="mb-8 rounded-2xl border border-primary/20 bg-white p-6 print:hidden"><p className="text-xs font-semibold uppercase tracking-wide text-primary">Start here · a quick guide</p><h2 className="mt-2 text-xl font-semibold">From scattered reports to a clear health history.</h2><ol className="my-4 grid gap-4 text-sm sm:grid-cols-3"><li><strong>1. Upload</strong><p className="mt-1 text-muted-foreground">Add your original PDFs in My Reports.</p></li><li><strong>2. Review</strong><p className="mt-1 text-muted-foreground">Check extracted readings before trusting them.</p></li><li><strong>3. Prepare</strong><p className="mt-1 text-muted-foreground">Add your background in Patient Summary, explore Health History, and use Find Care for nearby providers.</p></li></ol><div className="flex flex-wrap gap-3"><Button asChild onClick={dismissTour}><Link href="/patient-summary">Set up my summary</Link></Button><Button variant="outline" onClick={dismissTour}>Skip guide</Button><Link href="/onboarding" className="self-center text-sm text-primary underline">Read the full guide</Link></div></section>}{children}</main></>;
}
