"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useSyncExternalStore } from "react";
import { Button } from "@/components/ui/button";
import { onboardingSteps } from "@/lib/workspace-client";

const memory = new Map<string, string>();
function key(id: string) { return "carepath-walkthrough-v2:" + id; }
function subscribe(callback: () => void) {
  window.addEventListener("carepath-tour", callback);
  window.addEventListener("storage", callback);
  return () => {
    window.removeEventListener("carepath-tour", callback);
    window.removeEventListener("storage", callback);
  };
}
function progress(id: string) {
  try { return localStorage.getItem(key(id)) ?? memory.get(id) ?? "new"; }
  catch { return memory.get(id) ?? "new"; }
}
function save(id: string, value: string) {
  memory.set(id, value);
  try { localStorage.setItem(key(id), value); } catch { /* Keep progress during this visit. */ }
  window.dispatchEvent(new Event("carepath-tour"));
}
export function restartTour(id: string) { save(id, "0"); }

export function WorkspaceTour({ userId }: { userId: string }) {
  const path = usePathname();
  const router = useRouter();
  const stored = useSyncExternalStore(subscribe, () => progress(userId), () => "loading");
  const index = Math.max(0, Math.min(onboardingSteps.length - 1, Number(stored) || 0));
  const step = onboardingSteps[index];
  const next = onboardingSteps[index + 1];
  const onPage = path === step.href || path.startsWith(step.href + "/");

  useEffect(() => {
    if (stored === "new" && path === "/app") {
      router.replace(onboardingSteps[0].href);
    } else if (stored === "new" && onPage) {
      save(userId, "0");
    } else if (stored !== "new" && stored !== "done" && stored !== "loading" && path === next?.href) {
      save(userId, String(index + 1));
    }
  }, [stored, path, onPage, next, index, userId, router]);

  if (stored === "loading" || stored === "done") return null;
  return <aside aria-label="Workspace tour" className="mb-6 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 sm:p-5 print:hidden">
    <div className="flex items-center justify-between gap-4">
      <span className="text-xs font-semibold text-emerald-800">Workspace tour · {index + 1} of {onboardingSteps.length}</span>
      <button type="button" className="rounded px-2 py-1 text-sm underline" onClick={() => save(userId, "done")}>Skip tour</button>
    </div>
    <div aria-live="polite"><h2 className="mt-1 text-lg font-semibold">{step.title}</h2>
      <p className="mt-1 max-w-2xl text-sm">{onPage ? step.text : "Open this screen to continue your walkthrough."}</p></div>
    <div className="mt-3 flex flex-wrap items-center gap-4">
      {!onPage ? <Button size="sm" asChild><Link href={step.href}>{step.action}</Link></Button>
        : next ? <Button size="sm" asChild><Link href={next.href}>{next.action} →</Link></Button>
        : <Button size="sm" onClick={() => save(userId, "done")}>Finish tour</Button>}
      {onPage && next?.href === "/review" && <Link className="text-sm underline" href={onboardingSteps[index + 2].href}
        onClick={() => save(userId, String(index + 2))}>Skip review step</Link>}
    </div>
  </aside>;
}
