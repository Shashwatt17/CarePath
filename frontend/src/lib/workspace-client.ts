import { authClient } from "./auth-client.ts";
import { checked } from "./vault-client.ts";
import type { Page, TimelineEvent } from "./history-client.ts";
export type Activity = { action: string; outcome: string; actor: string; occurredAt: string };
export const activityCategories = ["ALL", "SECURITY", "DOCUMENT", "CARE", "VISIT_PACK", "SHARE"] as const;
export type SearchKind = "ALL" | "DOCUMENT" | "OBSERVATION" | "SYMPTOM" | "APPOINTMENT";
export type SearchQuery = { q: string; kind: SearchKind; provider: string; from: string; to: string; page: number };
export type SearchResult = { kind: Exclude<SearchKind,"ALL">; id: string; title: string; date: string | null; provider: string | null; summary: string; documentId: string | null; candidateId: string | null };
export const workspace = {
 activity: async (category = "ALL", page = 0, signal?: AbortSignal): Promise<Page<Activity>> => (await checked(await authClient.api(`/api/v1/activity?${new URLSearchParams({category,page:String(page)})}`, {signal}))).json(),
 search: async (q: SearchQuery, signal?: AbortSignal): Promise<Page<SearchResult>> => (await checked(await authClient.api("/api/v1/search", {method:"POST", signal, headers:{"Content-Type":"application/json"}, body:JSON.stringify({q:q.q,kind:q.kind,provider:q.provider || null,from:q.from || null,to:q.to || null,page:q.page})}))).json(),
};
export function resultLink(r: SearchResult): string {
 if(r.kind === "DOCUMENT") return `/records/${encodeURIComponent(r.id)}`;
 if(r.kind === "OBSERVATION" && r.documentId && r.candidateId) return `/records/${encodeURIComponent(r.documentId)}?candidate=${encodeURIComponent(r.candidateId)}`;
 if(r.kind === "APPOINTMENT") return `/appointments?id=${encodeURIComponent(r.id)}`;
 if(r.kind === "SYMPTOM") return `/symptoms?id=${encodeURIComponent(r.id)}`;
 return "/timeline";
}
export function timelineLink(e: TimelineEvent): string {
 if(e.type === "APPOINTMENT") return `/appointments?id=${encodeURIComponent(e.id)}`;
 if(e.type === "FOLLOW_UP") return `/follow-ups?id=${encodeURIComponent(e.id)}`;
 if(e.type === "SYMPTOM") return `/symptoms?id=${encodeURIComponent(e.id)}`;
 return e.documentId ? `/records/${encodeURIComponent(e.documentId)}` : "/timeline";
}
export const onboardingSteps = [
 {title:"Keep originals private",text:"Upload PDF, JPG or PNG records in your private vault. Files are not automatically shared.",href:"/records",action:"Open Records"},
 {title:"Review before trust",text:"Processing produces candidates. Compare each result with its source before you confirm or correct it. Rejected and pending results do not drive trends.",href:"/review",action:"Open Review"},
 {title:"Follow the evidence",text:"The timeline and What Changed? use verified readings and deterministic comparisons. Every reading links back to its uploaded source.",href:"/timeline",action:"Explore Timeline"},
 {title:"Prepare for your clinician",text:"Select relevant records and questions for a Visit Pack. Temporary sharing exposes only your selected snapshot and can be revoked.",href:"/visit-packs",action:"Prepare a Visit Pack"},
] as const;
