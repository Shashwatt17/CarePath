"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { AuthProvider, useAuth } from "@/components/auth/auth-provider";
import { restartTour } from "@/components/auth/workspace-tour";

export default function Onboarding() { return <AuthProvider><TourEntry /></AuthProvider>; }

function TourEntry() {
  const { user, state, retry } = useAuth();
  const router = useRouter();
  useEffect(() => {
    if (state !== "ready") return;
    if (user) restartTour(user.id);
    router.replace(user ? "/records" : "/login");
  }, [user, state, router]);
  return <main id="main" className="p-8">{state === "error"
    ? <button onClick={retry}>Retry session check</button>
    : <p role="status">Opening your workspace…</p>}</main>;
}
