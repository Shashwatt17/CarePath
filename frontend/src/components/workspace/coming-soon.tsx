import Link from "next/link";
import { Button } from "@/components/ui/button";
export function ComingSoon({title,description}:{title:string;description:string}) {
 return <section className="mx-auto max-w-2xl rounded-2xl border border-border bg-white p-8 sm:p-12"><p className="text-xs font-semibold uppercase tracking-widest text-primary">Coming soon</p><h1 className="mt-3 text-3xl font-semibold">{title}</h1><p className="my-5 leading-relaxed text-muted-foreground">{description} Existing saved data is retained.</p><p className="mb-6 text-sm">You can still organize reports, review your health history, prepare a patient summary and find nearby care.</p><Button asChild><Link href="/app">Back to overview</Link></Button></section>;
}
