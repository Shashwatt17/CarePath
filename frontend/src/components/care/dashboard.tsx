"use client";
import Link from "next/link";

import { vault } from "@/lib/vault-client";

import { useCarePage, ErrorNote, Loading } from "./shared";
import { Button } from "@/components/ui/button";
export function CareDashboard() {
 const docs = useCarePage(() => vault.list(new URLSearchParams({ page: "0", size: "5" })), []);
 return <div className="mt-8 space-y-6"><section className="rounded-2xl border border-border bg-white p-6"><div className="flex items-center justify-between"><h2 className="text-lg font-semibold">Recent reports</h2><Link href="/records" className="text-sm font-medium text-primary">View all →</Link></div><ErrorNote message={docs.error}/>{docs.error && <Button onClick={docs.reload}>Retry</Button>}{docs.loading ? <Loading/> : <ul className="mt-4 divide-y divide-border">{docs.data?.items.map(d=><li key={d.id} className="flex flex-wrap justify-between gap-2 py-4"><Link href={`/records/${encodeURIComponent(d.id)}`} className="text-sm font-medium text-primary">{d.originalFilename}</Link><span className="rounded-full bg-muted px-3 py-1 text-xs">{d.status}</span></li>)}</ul>}{!docs.loading && !docs.error && !docs.data?.items.length && <p className="py-8 text-sm text-muted-foreground">Your library is empty. Upload your first report to get started.</p>}</section><section className="flex flex-wrap items-center justify-between gap-4 rounded-2xl bg-muted p-6"><div><h2 className="font-semibold">Looking for care nearby?</h2><p className="mt-2 text-sm text-muted-foreground">Explore providers and check their details before visiting.</p></div><Button asChild variant="outline"><Link href="/nearby-care">Find Care</Link></Button></section><p className="text-xs text-muted-foreground">Coming soon: appointment planning, reminders and advanced AI assistance.</p></div>;
}
