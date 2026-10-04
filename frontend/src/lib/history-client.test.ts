import { test } from "node:test";
import assert from "node:assert/strict";
import { authClient } from "./auth-client.ts";
import { historyClient, evidenceUrl, type Evidence } from "./history-client.ts";
test("history client uses owner-authenticated paginated query contracts", async () => {
  const original = authClient.api; const calls: string[] = [];
  authClient.api = async (path) => { calls.push(path); return new Response("{}"); };
  try { await historyClient.events({ conceptId: "concept", documentId: "doc", q: "Hb & HGB", page: 2 }); await historyClient.history("a/b", 3); await historyClient.concepts(); assert.equal(calls[0], "/api/v1/history/events?page=2&size=20&conceptId=concept&documentId=doc&q=Hb+%26+HGB"); assert.equal(calls[1], "/api/v1/history/concepts/a%2Fb?page=3&size=20"); assert.equal(calls[2], "/api/v1/history/concepts?page=0&size=100"); } finally { authClient.api = original; }
});
test("report selection and source navigation encode identifiers and never build external URLs", async () => {
  const original = authClient.api; const calls: string[] = [];
  authClient.api = async (path) => { calls.push(path); return new Response("{}"); };
  try { await historyClient.changes("earlier&owner=forged", "later"); await historyClient.evidence("a/b"); assert.equal(calls[0], "/api/v1/history/changes?previousReport=earlier%26owner%3Dforged&currentReport=later"); assert.equal(calls[1], "/api/v1/history/observations/a%2Fb/evidence"); assert.equal(evidenceUrl({ documentId: "https://evil.invalid", candidateId: "x&y" } as Evidence), "/records/https%3A%2F%2Fevil.invalid?candidate=x%26y"); } finally { authClient.api = original; }
});
test("history retains exact decimal strings and surfaces safe errors", async () => {
  const original = authClient.api;
  authClient.api = async () => new Response(JSON.stringify({ changes: [{ absoluteDelta: "-0.9000", percentageDelta: null }] }));
  try { const result = await historyClient.changes("a", "b"); assert.equal(result.changes[0].absoluteDelta, "-0.9000"); assert.equal(result.changes[0].percentageDelta, null); authClient.api = async () => new Response("<script>sensitive content</script>", { status: 500 }); await assert.rejects(historyClient.events({}), (e: unknown) => e instanceof Error && !/script|sensitive/.test(e.message)); } finally { authClient.api = original; }
});
