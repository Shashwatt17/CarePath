"use client";
import { CareDashboard } from "@/components/care/dashboard";
import { useEffect, useState } from "react";
import { authClient } from "@/lib/auth-client";
import { useAuth } from "@/components/auth/auth-provider";
import { Button } from "@/components/ui/button";
export default function Workspace() {
  const { user } = useAuth(); const [verified, setVerified] = useState(false); const [error, setError] = useState("");
  async function verify() {
    setError(""); setVerified(false);
    try { await authClient.me(); setVerified(true); } catch { setError("The protected API could not confirm your session. Please retry or sign in again."); }
  }
  useEffect(() => { let active = true; authClient.me().then(() => { if (active) setVerified(true); }).catch(() => { if (active) setError("The protected API could not confirm your session."); }); return () => { active = false; }; }, []);
  return <><p className="text-xs font-semibold uppercase tracking-widest text-primary">Personal workspace</p><h1 className="mt-4 text-3xl font-semibold tracking-tight">Welcome, {user?.displayName}.</h1><p className="mt-4 max-w-xl leading-relaxed text-muted-foreground">Your private document vault is ready. Open Records to upload and manage your medical documents. Verify readings in Review, then explore Timeline and What Changed? for evidence-linked numerical history.</p><CareDashboard /><section className="mt-10 max-w-xl rounded-xl border border-border bg-white p-6"><h2 className="font-semibold">Account connection</h2><p className="mt-3 text-sm" role="status">{verified ? "Your authenticated session was verified by the backend." : error || "Verifying your protected API connection…"}</p>{error && <Button className="mt-4" variant="outline" onClick={verify}>Retry verification</Button>}</section></>;
}
