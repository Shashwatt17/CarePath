import test from "node:test";
import assert from "node:assert/strict";
import { authClient } from "./auth-client.ts";
import { care, offsetDate, localInput, sourceLink } from "./care-client.ts";
test("care mutations allow only editable fields and encode identifiers", async () => {
 const old = authClient.api; const calls: { path: string; init?: RequestInit }[] = []; authClient.api = async (path, init) => { calls.push({ path, init }); return new Response("{}", { status: 200 }); };
 try { await care.saveSymptom("a/b", { name: "Synthetic", startedAt: "2026-01-01T00:00:00Z", resolvedAt: null, severity: 2, frequency: "ONCE", notes: null, version: 0, ...{ ownerId: "forged", status: "VERIFIED" } }); assert.equal(calls[0].path, "/api/v1/care/symptoms/a%2Fb"); const body = JSON.parse(String(calls[0].init?.body)); assert.equal(body.ownerId, undefined); assert.equal(body.status, undefined); await care.appointmentStatus("a/b", "CANCELLED", 2); assert.deepEqual(JSON.parse(String(calls[1].init?.body)), { status: "CANCELLED", version: 2 }); } finally { authClient.api = old; }
});
test("follow-up confirmation sends explicit time and separate version", async () => {
 const old = authClient.api; let body: unknown; authClient.api = async (_p, init) => { body = JSON.parse(String(init?.body)); return new Response("{}"); };
 try { await care.decide("f", "CONFIRM", 3, "2026-10-13T09:00:00+05:30", "Asia/Kolkata", [60]); assert.deepEqual(body, { action: "CONFIRM", version: 3, confirmedAt: "2026-10-13T09:00:00+05:30", timeZone: "Asia/Kolkata", offsets: [60] }); } finally { authClient.api = old; }
});
test("care errors do not reflect sensitive provider or database messages", async () => { const old = authClient.api; authClient.api = async () => new Response("secret medical SQL", { status: 500 }); try { await assert.rejects(care.symptoms(), (e: unknown) => e instanceof Error && !e.message.includes("SQL")); } finally { authClient.api = old; } });
test("date conversion preserves local time and internal source links are encoded", () => { const value = "2026-10-13T09:30"; assert.equal(localInput(offsetDate(value)), value); assert.equal(sourceLink("APPOINTMENT", "a/b"), "/appointments?id=a%2Fb"); assert.throws(() => offsetDate("invalid")); });
