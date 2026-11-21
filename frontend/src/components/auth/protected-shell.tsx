"use client";
import Link from "next/link";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { authClient, ApiError } from "@/lib/auth-client";
import { useAuth } from "./auth-provider";

import { WorkspaceTour, restartTour } from "./workspace-tour";

export function ProtectedShell({ children }: { children: React.ReactNode }) {
  const { user, state, error, retry, notify } = useAuth(); const router = useRouter();
  const pathname = usePathname();
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
  return <><header className="sticky top-0 z-30 border-b border-border bg-white/95 backdrop-blur"><div className="mx-auto max-w-6xl px-4 sm:px-6"><div className="flex items-center justify-between gap-3 py-4"><Link className="text-xl font-bold tracking-tight" href="/app">CarePath<span className="text-primary"> +</span></Link><div className="flex items-center gap-3"><Button asChild size="sm"><Link href="/records">Upload report</Link></Button><details className="relative"><summary className="cursor-pointer rounded-full border border-border px-3 py-2 text-sm">My account</summary><div className="absolute right-0 mt-2 w-56 rounded-xl border border-border bg-white p-4 shadow-lg"><p className="mb-3 truncate font-medium">{user.displayName}</p><Link className="block py-2 text-sm" href="/settings">Settings</Link><Link className="block py-2 text-sm" href="/records" onClick={() => restartTour(user.id)}>Replay workspace tour</Link><Link className="block py-2 text-sm" href="/shares">Manage sharing</Link><Button className="mt-3" variant="outline" size="sm" onClick={logout} disabled={busy}>{busy ? "Signing out…" : "Sign out"}</Button></div></details></div></div><nav aria-label="Workspace navigation" className="mb-3 flex gap-1 overflow-x-auto rounded-2xl border border-emerald-900/10 bg-gradient-to-r from-emerald-50 to-stone-50 p-1">{[["/app","Overview"],["/records","My Reports"],["/patient-summary","Patient Summary"],["/timeline","Health History"],["/nearby-care","Find Care"]].map(([href,label]) => { const active=pathname===href || pathname.startsWith(href+"/") || (href==="/records" && pathname==="/review"); return <Link key={href} href={href} aria-current={active ? "page" : undefined} className={`shrink-0 rounded-lg px-4 py-2.5 text-sm font-medium transition-colors ${active ? "bg-primary text-white" : "text-muted-foreground hover:bg-muted hover:text-foreground"}`}>{label}</Link>; })}</nav></div></header>{logoutError && <p role="alert" className="mx-auto max-w-6xl px-6 py-3 text-red-900">{logoutError}</p>}<main id="main" className="mx-auto max-w-6xl px-6 py-12"><WorkspaceTour userId={user.id} />{children}</main></>;
}
