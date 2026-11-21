import Link from "next/link";
import { Plus, ArrowUpRight } from "lucide-react";
import { ReportPreview } from "@/components/workspace/report-preview";
import { Button } from "@/components/ui/button";
import { readSystemState } from "@/lib/system";
export const dynamic = "force-dynamic";

export default async function Home() {
  const backend = await readSystemState();
  return <div className="care-landing"><div className="mx-auto max-w-7xl px-5 sm:px-10">
    <header className="flex items-center justify-between border-b border-emerald-950/15 py-5">
      <Link href="/" className="flex items-center gap-2 text-xl font-bold"><Plus className="rounded-lg bg-primary p-1 text-white" size={30}/>CarePath</Link>
      <Link href="/login" className="flex items-center gap-1 text-sm font-semibold">Sign in <ArrowUpRight size={16}/></Link>
    </header>
    <main id="main" className="grid items-center gap-8 py-10 sm:py-16 lg:grid-cols-[0.85fr_1.15fr] lg:gap-14 lg:py-24">
      <div><h1 className="text-5xl font-semibold leading-[1.02] tracking-[-.055em] sm:text-7xl">Your records.<br/><span className="text-primary">One clear history.</span></h1>
        <p className="my-6 max-w-md text-lg leading-relaxed text-muted-foreground">Bring your reports, readings and medical history together for your next visit. Explore a sample to see how a reading gets from the page to your profile.</p>
        <Button asChild><Link href="/register">Create your workspace <ArrowUpRight size={16}/></Link></Button>
      </div><ReportPreview/>
    </main>
    <footer className="flex flex-wrap justify-between gap-4 border-t border-emerald-950/15 py-6 text-sm text-muted-foreground"><Link className="underline" href="/privacy">Privacy &amp; security</Link><span>{backend.available ? "Backend connected" : "Connection could not be confirmed"}</span></footer>
  </div></div>;
}
