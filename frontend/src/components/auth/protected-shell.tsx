"use client";
import Link from "next/link";
import { NotificationIndicator } from "@/components/care/notifications";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { authClient, ApiError } from "@/lib/auth-client";
import { useAuth } from "./auth-provider";

export function ProtectedShell({ children }: { children: React.ReactNode }) {
  const { user, state, error, retry, notify } = useAuth(); const router = useRouter();
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
  return <><header className="border-b border-border bg-white"><div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-6 py-5"><Link className="text-xl font-semibold" href="/app">CarePath<span className="text-primary"> +</span></Link><nav aria-label="Workspace navigation" className="flex flex-wrap items-center gap-4"><Link className="text-sm font-medium text-primary" href="/records">Records</Link><Link className="text-sm font-medium text-primary" href="/review">Review</Link><Link className="text-sm font-medium text-primary" href="/timeline">Timeline</Link><Link className="text-sm font-medium text-primary" href="/changes">Changes</Link><Link className="text-sm font-medium text-primary" href="/assistant">Ask records</Link><Link className="text-sm text-primary" href="/symptoms">Symptoms</Link><Link className="text-sm text-primary" href="/appointments">Appointments</Link><Link className="text-sm text-primary" href="/follow-ups">Follow-ups</Link><Link className="text-sm text-primary" href="/visit-packs">Visit Packs</Link><Link className="text-sm text-primary" href="/shares">Shares</Link><Link className="text-sm text-primary" href="/nearby-care">Nearby Care</Link><Link className="text-sm text-primary" href="/search">Search</Link><Link className="text-sm text-primary" href="/activity">Activity</Link><Link className="text-sm text-primary" href="/settings">Settings</Link><NotificationIndicator /><span className="max-w-40 truncate text-sm text-muted-foreground">{user.displayName}</span><Button variant="outline" size="sm" onClick={logout} disabled={busy}>{busy ? "Signing out…" : "Sign out"}</Button></nav></div></header>{logoutError && <p role="alert" className="mx-auto max-w-6xl px-6 py-3 text-red-900">{logoutError}</p>}<main id="main" className="mx-auto max-w-6xl px-6 py-12">{children}</main></>;
}
