import Link from "next/link";
import { Fingerprint, Layers3, MoveUpRight, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { readSystemState } from "@/lib/system";
export const dynamic = "force-dynamic";

export default async function Home() {
  const backend = await readSystemState();
  return <div className="min-h-screen">
    <header className="mx-auto flex max-w-6xl items-center justify-between border-b border-border px-6 py-6">
      <Link href="/" aria-label="CarePath home" className="flex items-center gap-2 text-xl font-semibold tracking-tight"><span className="grid size-8 place-items-center rounded-lg bg-primary text-white"><Plus size={22} aria-hidden="true" /></span>CarePath</Link>
      <span className="rounded-full border border-border px-3 py-1 text-xs font-medium text-muted-foreground">Evidence-grounded records</span>
    </header>
    <main id="main" className="mx-auto max-w-6xl px-6">
      <section className="grid gap-12 py-16 md:grid-cols-[1.3fr_1fr] md:py-24">
        <div><p className="mb-5 text-xs font-semibold uppercase tracking-[0.18em] text-primary">Records with context. Care with clarity.</p>
          <h1 className="max-w-2xl text-4xl font-semibold leading-[1.08] tracking-tight sm:text-6xl">Your health story,<br /><span className="text-primary">connected by evidence.</span></h1>
          <p className="mt-6 max-w-lg text-lg leading-relaxed text-muted-foreground">Connect verified medical records over time, inspect numerical changes and prepare for your next clinical visit.</p>
          <div className="mt-8 flex gap-3"><Button asChild><Link href="/register">Create account</Link></Button><Button asChild variant="outline"><Link href="/login">Sign in</Link></Button></div>
        </div>
        <aside aria-label="CarePath overview" className="self-center rounded-2xl border border-border bg-white p-8 shadow-sm">
          <div className="mb-8 flex items-center justify-between"><span className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">Your private workspace</span><Layers3 size={20} className="text-primary" aria-hidden="true" /></div>
          <h2 className="text-2xl font-semibold tracking-tight">Your records, in one private place.</h2>
          <p className="mt-3 leading-relaxed text-muted-foreground">Upload and review medical records, explore verified history, prepare a Visit Pack and share it temporarily. CarePath organizes your records; it does not diagnose or prescribe.</p>
          <dl className="mt-7 space-y-4 border-t border-border pt-6 text-sm">
            <div className="flex justify-between gap-3"><dt className="text-muted-foreground">Backend connection</dt><dd className="font-medium">{backend.available ? "Connected" : "Unavailable"}</dd></div>
            <div className="flex justify-between gap-3"><dt className="text-muted-foreground">Product features</dt><dd>Records, history & visit preparation</dd></div>
          </dl>
          {!backend.available && <p className="mt-4 text-xs leading-relaxed text-muted-foreground">Start the backend using the repository README, then refresh this page.</p>}
        </aside>
      </section>
      <section id="foundation" className="border-t border-border py-10">
        <h2 className="text-sm font-semibold uppercase tracking-widest text-muted-foreground">Design commitments</h2>
        <div className="mt-8 grid gap-9 pb-12 md:grid-cols-3">{[
          ["01", "Evidence first", "Verified observations and record-based explanations link back to their original sources."],
          ["02", "Uncertainty made visible", "Extracted readings require review before they enter the trusted health timeline."],
          ["03", "Preparation, not diagnosis", "CarePath helps organize records and clinical questions. It does not prescribe treatment."],
        ].map(([n, title, text]) => <article key={n}><span className="text-xs text-primary">{n}</span><h3 className="mb-3 mt-3 text-xl font-medium">{title}</h3><p className="text-sm leading-7 text-muted-foreground">{text}</p></article>)}</div>
      </section>
    </main>
    <footer className="border-t border-border"><div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-6 py-6 text-xs text-muted-foreground"><span className="flex items-center gap-2"><Fingerprint size={16} aria-hidden="true" /> Private documents. No automatic sharing.</span><Link href="/privacy" className="underline">Privacy & security</Link><span className="flex items-center gap-2">Built for a more informed visit <MoveUpRight size={14} aria-hidden="true" /></span></div></footer>
  </div>;
}
