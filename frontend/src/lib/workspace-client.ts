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
 {title:"My Reports",text:"Upload a PDF or photo here. Open a report to see its readings beside the original page. You can explore the tour with an empty account.",href:"/records",action:"Open My Reports"},
 {title:"Review readings",text:"Compare an extracted value with the printed result before confirming or correcting it. You can leave a reading pending and return later.",href:"/review",action:"Open Review"},
 {title:"Patient Summary",text:"Add your introduction, medical history, medications and allergies here so your clinician can get to know you before a visit.",href:"/patient-summary",action:"Open Patient Summary"},
 {title:"Health History",text:"Follow your readings over time. Open a reading to trace it back to its report.",href:"/timeline",action:"Open Health History"},
 {title:"Find Care",text:"Search the directory for nearby providers. Check the listing before arranging a visit.",href:"/nearby-care",action:"Open Find Care"},
 {title:"Visit Packs",text:"Choose which records and questions to include in a summary for your clinician. Preview the pack before sharing.",href:"/visit-packs",action:"Open Visit Packs"},
 {title:"Manage sharing",text:"Check your temporary links here and revoke access when you no longer want to share a pack.",href:"/shares",action:"Open sharing"},
 {title:"Overview",text:"Return here to find recent reports and pick up where you left off. Replay this walkthrough from My account.",href:"/app",action:"Open Overview"},
] as const;
