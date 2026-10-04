"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { care, sourceLink } from "@/lib/care-client";
import { Button } from "@/components/ui/button";
import { Pages, ErrorNote, Loading, useCarePage } from "./shared";
export function Notifications() {
 const [page, setPage] = useState(0); const state = useCarePage(() => care.notices(page), [page]); const [busy, setBusy] = useState(false);
 async function read(id?: string) { setBusy(true); try { if (id) await care.read(id); else await care.readAll(); state.reload(); window.dispatchEvent(new Event("carepath-notifications")); } catch (e) { state.setError(e instanceof Error ? e.message : "Could not mark read."); } finally { setBusy(false); } }
 return <><div className="flex flex-wrap justify-between gap-3"><h1 className="text-3xl font-semibold">Notifications</h1><Button variant="outline" disabled={busy} onClick={() => read()}>Mark all read</Button></div><p className="mt-3 text-sm text-muted-foreground">Persistent in-app reminders. Email delivery is not configured.</p><ErrorNote message={state.error} />{state.error && <Button onClick={state.reload}>Retry</Button>}{state.loading ? <Loading /> : !state.data?.items.length ? <p className="mt-6 rounded-lg border border-dashed p-6">You have no notifications yet.</p> : <ul className="mt-6 divide-y divide-border">{state.data.items.map((n) => <li key={n.id} className="py-5"><h2 className={n.readAt ? "font-medium" : "font-semibold"}>{n.title} {!n.readAt && <span className="ml-2 text-xs text-primary">Unread</span>}</h2><p className="mt-2 text-sm">{n.message}</p><p className="mt-1 text-xs text-muted-foreground">{new Date(n.createdAt).toLocaleString()}</p><div className="mt-3 flex items-center gap-4"><Link className="text-sm text-primary underline" href={sourceLink(n.sourceType, n.sourceId)}>Open scheduled item</Link>{!n.readAt && <Button variant="outline" size="sm" disabled={busy} onClick={() => read(n.id)}>Mark read</Button>}</div></li>)}</ul>}<Pages page={page} total={state.data?.total ?? 0} change={setPage} /></>;
}
export function NotificationIndicator() {
 const state = useCarePage(care.unread, []);
 useEffect(() => { const reload = state.reload; window.addEventListener("focus", reload); window.addEventListener("carepath-notifications", reload); return () => { window.removeEventListener("focus", reload); window.removeEventListener("carepath-notifications", reload); }; }, [state.reload]);
 // The indicator reflects server state; it does not deliver reminders.
 return <Link className="text-sm font-medium text-primary" href="/notifications">Notifications{state.data ? ` (${state.data.count})` : ""}</Link>;
}
