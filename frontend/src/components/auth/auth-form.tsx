"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { authClient, ApiError } from "@/lib/auth-client";
import { useAuth } from "./auth-provider";

export function AuthForm({ mode }: { mode: "login" | "register" }) {
  const register = mode === "register";
  const router = useRouter();
  const { notify } = useAuth();
  const [busy, setBusy] = useState(false), [error, setError] = useState(""), [created, setCreated] = useState(false);
  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = new FormData(event.currentTarget);
    const email = String(form.get("email") ?? "").trim(); const password = String(form.get("password") ?? "");
    if (new TextEncoder().encode(password).length > 72) { setError("Password must be no more than 72 UTF-8 bytes. Some characters use multiple bytes."); return; }
    setBusy(true); setError("");
    try {
      if (register) { await authClient.register(email, password, String(form.get("displayName") ?? "").trim()); setCreated(true); }
      else { await authClient.login(email, password); notify("signed-in"); router.replace("/app"); }
    } catch (e) { setError(e instanceof ApiError ? e.message : "The request could not be completed. Please try again."); }
    finally { setBusy(false); }
  }
  if (created) return <div role="status"><h1 className="text-3xl font-semibold">Your account is ready.</h1><p className="my-5 text-muted-foreground">Take a short introduction, then sign in to your workspace.</p><Button asChild><Link href="/onboarding">Get started</Link></Button></div>;
  return <>
    <p className="mb-3 text-xs font-semibold uppercase tracking-widest text-primary">Your CarePath account</p>
    <h1 className="text-3xl font-semibold tracking-tight">{register ? "Start with a secure account." : "Welcome back."}</h1>
    <p className="mb-8 mt-3 leading-relaxed text-muted-foreground">{register ? "Create your private workspace for records, evidence and visit preparation." : "Sign in to your personal CarePath workspace."}</p>
    <form onSubmit={submit} className="space-y-5" aria-busy={busy}>
      {register && <div><label htmlFor="displayName" className="field-label">Name</label><input id="displayName" name="displayName" autoComplete="name" required maxLength={120} className="field-input" disabled={busy} /></div>}
      <div><label htmlFor="email" className="field-label">Email</label><input id="email" name="email" type="email" autoComplete="email" required maxLength={254} className="field-input" disabled={busy} /></div>
      <div><label htmlFor="password" className="field-label">Password</label><input id="password" name="password" type="password" autoComplete={register ? "new-password" : "current-password"} required minLength={register ? 12 : 1} maxLength={72} className="field-input" aria-describedby={register ? "password-help" : undefined} disabled={busy} />
        {register && <p id="password-help" className="mt-2 text-xs leading-5 text-muted-foreground">Use a unique passphrase of 12–72 characters, within 72 UTF-8 bytes. Password managers and paste are supported.</p>}</div>
      {error && <p role="alert" className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-900">{error}</p>}
      <Button type="submit" className="w-full" disabled={busy}>{busy ? "Please wait…" : register ? "Create account" : "Sign in"}</Button>
    </form>
    <p className="mt-6 text-sm text-muted-foreground">{register ? "Already have an account? " : "New to CarePath? "}<Link className="font-medium text-primary underline underline-offset-4" href={register ? "/login" : "/register"}>{register ? "Sign in" : "Create an account"}</Link></p>
  </>;
}
