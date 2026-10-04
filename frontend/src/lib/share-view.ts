import { createElement as h } from "react";
import type { PublicSnapshot } from "./share-client.ts";
// Deliberately separate from owner PackView: no IDs, hrefs, auth client or edit controls.
export function SharedSnapshotView({snapshot:s}:{snapshot:PublicSnapshot}) {
 return h("article",{className:"mx-auto max-w-3xl rounded-lg border border-border bg-white p-6 sm:p-10","aria-label":"Shared read-only Visit Pack"},
  h("header",{className:"border-b border-border pb-5"},h("p",{className:"text-xs uppercase tracking-widest text-primary"},"CarePath · Shared Visit Pack"),h("h1",{className:"mt-3 break-words text-2xl font-semibold"},s.title),h("p",{className:"mt-2 text-sm"},`Revision ${s.revision} · Prepared ${new Date(s.generatedAt).toLocaleString()}`),h("p",{className:"mt-3 text-sm text-muted-foreground"},"Read-only historical snapshot. Sources may have changed or been deleted. Source citations below are retained snapshot text; original records are not accessible.")),
  h("section",{className:"py-5"},h("h2",{className:"font-semibold"},"Reason for visit"),h("p",{className:"mt-2 whitespace-pre-wrap break-words text-sm"},s.reasonForVisit)),
  ...s.items.map((item,i)=>h("section",{key:i,className:"border-t border-border py-5"},h("h2",{className:"break-words font-semibold"},item.heading),
   ...item.fields.map((f,j)=>h("p",{key:`f${j}`,className:"mt-2 whitespace-pre-wrap break-words text-sm"},h("strong",null,`${f.label}: `),f.value)),
   ...item.evidence.map((e,j)=>h("div",{key:`e${j}`,className:"mt-4 border-l-2 border-primary/30 pl-3 text-sm"},h("p",{className:"break-words font-medium"},`${e.filename} · Page ${e.page}`),h("p",{className:"mt-1 whitespace-pre-wrap break-words text-muted-foreground"},e.snippet))))),
  h("footer",{className:"border-t border-border pt-5 text-xs text-muted-foreground"},"CarePath organizes user-provided records and does not provide a medical diagnosis. Symptoms and questions are user-reported. Numerical changes do not establish clinical significance."));
}
export function SharedUnavailableView(){return h("section",{role:"alert",className:"mx-auto max-w-xl rounded-lg border border-border bg-white p-8"},h("h1",{className:"text-xl font-semibold"},"Shared Visit Pack unavailable"),h("p",{className:"mt-3 text-sm text-muted-foreground"},"The link may be invalid, expired or revoked, or the service temporarily unavailable. Ask the sender for a new link."));}
