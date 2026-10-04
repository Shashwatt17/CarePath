import Link from "next/link";
import type { Point } from "@/lib/history-client";
import { evidenceUrl } from "@/lib/history-client";
import { label } from "@/lib/vault-client";
export function EvidenceReading({ point, heading }: { point: Point; heading?: string }) {
  return <div className="min-w-0 text-sm">{heading && <p className="mb-2 text-xs font-medium uppercase tracking-widest text-muted-foreground">{heading}</p>}<p className="font-medium tabular-nums">{point.effective.text ?? "Not available"} {point.effective.unit ?? "(unit unknown)"}</p><p className="mt-1 text-muted-foreground">{point.date ?? "Date not supplied"} · {point.provider ?? "Lab not supplied"}</p>
    <details className="mt-2"><summary className="cursor-pointer text-primary">Reading and verification details</summary><dl className="mt-3 space-y-2 rounded-lg bg-muted/50 p-3"><div><dt className="font-medium">Original machine reading</dt><dd>{point.original.text ?? "Not read"} {point.original.unit ?? ""}</dd></div><div><dt className="font-medium">Normalized</dt><dd>{point.normalizedValue === null ? "Unavailable" : `${point.normalizedValue} ${point.normalizedUnit ?? ""}`}</dd></div><div><dt className="font-medium">Supplied reference interval</dt><dd>{point.effective.reference ?? "Not supplied"}</dd></div><div><dt className="font-medium">Source abnormal flag</dt><dd>{point.original.sourceFlag ?? "Not supplied"} — separate from calculated comparisons</dd></div><div><dt className="font-medium">Verification</dt><dd>{label(point.verificationStatus)} · {new Date(point.verifiedAt).toLocaleString()}</dd></div></dl><blockquote className="my-3 whitespace-pre-wrap break-words border-l-2 border-primary pl-3 text-muted-foreground">{point.evidence.text}</blockquote></details>
    <Link className="mt-3 inline-block break-words font-medium text-primary underline underline-offset-4" href={evidenceUrl(point.evidence)}>View evidence · {point.evidence.filename} · page {point.evidence.page}</Link>
  </div>;
}
