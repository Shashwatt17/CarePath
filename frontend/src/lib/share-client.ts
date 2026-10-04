import QRCode from "qrcode";
import { authClient, ApiError } from "./auth-client.ts";
import type { Page } from "./history-client.ts";
import type { PackType } from "./pack-client.ts";
export const expiryOptions = [15, 30, 60, 1440] as const;
export type Share = { id: string; packId: string; revision: number; createdAt: string; expiresAt: string; revokedAt: string | null; status: "ACTIVE" | "EXPIRED" | "REVOKED"; version: number };
export type PublicSnapshot = { title: string; reasonForVisit: string; packDate: string | null; revision: number; generatedAt: string; expiresAt: string; items: { type: PackType; heading: string; fields: {label: string; value: string}[]; evidence: {filename: string; page: number; snippet: string}[] }[] };
const unavailable = () => new Error("This shared Visit Pack is unavailable. It may have expired or been revoked.");
async function owner<T>(path: string, method = "GET", body?: unknown): Promise<T> {
 const r = await authClient.api(`/api/v1/shares${path}`, {method, ...(body === undefined ? {} : {headers:{"Content-Type":"application/json"},body:JSON.stringify(body)})});
 if(!r.ok) throw new ApiError(r.status,"SHARE_FAILED",r.status===409?"Generate the Visit Pack before sharing.":"Sharing is unavailable. Please retry.");
 return r.json() as Promise<T>;
}
export const shares = {
 create: (packId: string, expiryMinutes: number) => owner<{share: Share; token: string}>("", "POST", {packId,expiryMinutes}),
 list: (page=0) => owner<Page<Share>>(`?page=${page}`),
 revoke: (id:string) => owner<Share>(`/${encodeURIComponent(id)}/revoke`,"POST"),
};
export function shareLink(origin: string, token: string) {
 const url = new URL(origin);
 if(!/^[A-Za-z0-9_-]{43}$/.test(token) || (url.protocol!=="https:" && !(url.protocol==="http:" && ["localhost","127.0.0.1","[::1]"].includes(url.hostname)))) throw unavailable();
 return `${url.origin}/share#${token}`;
}
export async function shareQr(link: string) { return QRCode.toDataURL(link,{errorCorrectionLevel:"M",margin:4,width:300}); }
export async function accessShare(token: string, signal?: AbortSignal): Promise<PublicSnapshot> {
 if(!/^[A-Za-z0-9_-]{43}$/.test(token)) throw unavailable();
 const r=await fetch("/api/v1/public/share/access",{method:"POST",credentials:"omit",cache:"no-store",referrerPolicy:"no-referrer",headers:{"Content-Type":"application/json"},body:JSON.stringify({token}),signal:signal?AbortSignal.any([signal,AbortSignal.timeout(10000)]):AbortSignal.timeout(10000)});
 if(!r.ok)throw unavailable();
 return r.json() as Promise<PublicSnapshot>;
}
