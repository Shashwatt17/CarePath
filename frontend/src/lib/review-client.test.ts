import { test } from "node:test";
import assert from "node:assert/strict";
import { authClient } from "./auth-client.ts";
import { reviewClient, permittedFields, type Fields } from "./review-client.ts";
const fields: Fields = { testName: "Hb", value: "104", unit: "g/L", referenceRange: "120 - 160", date: "2026-01-15", conceptId: null };
test("review writes allow only editable fields, version and explicit review acknowledgement", async () => {
  const original = authClient.api; const calls: { path: string; body: unknown }[] = [];
  authClient.api = async (path, init) => { calls.push({ path, body: init?.body ? JSON.parse(String(init.body)) : null }); return new Response("{}"); };
  try {
    await reviewClient.confirm("a/b", 2); await reviewClient.correct("a/b", 2, { ...fields, ownerId: "forged" } as Fields, true, "Checked the original source"); await reviewClient.reject("a/b", 2, null);
    assert.equal(calls[0].path, "/api/v1/review/candidates/a%2Fb/confirm"); assert.deepEqual(calls[0].body, { version: 2 });
    assert.deepEqual(calls[1].body, { version: 2, fields, sourceReviewed: true, reason: "Checked the original source" });
    assert.deepEqual(calls[2].body, { version: 2, reason: null });
  } finally { authClient.api = original; }
});
test("normalization preview preserves decimal strings and does not submit trust or provenance fields", async () => {
  const original = authClient.api;
  authClient.api = async (_path, init) => { assert.deepEqual(JSON.parse(String(init?.body)), fields); return new Response(JSON.stringify({ normalized: { value: "10.400", unit: "g/dL" } })); };
  try { assert.equal((await reviewClient.preview("id", fields)).normalized.value, "10.400"); } finally { authClient.api = original; }
});
test("review queue paginates on the server and observation identifiers are encoded", async () => {
  const original = authClient.api; const paths: string[] = [];
  authClient.api = async (path) => { paths.push(path); return new Response("{}"); };
  try { await reviewClient.list("doc", "PENDING_REVIEW", 3); await reviewClient.observation("a/b"); assert.equal(paths[0], "/api/v1/review/candidates?state=PENDING_REVIEW&page=3&size=10&documentId=doc"); assert.equal(paths[1], "/api/v1/observations/a%2Fb"); } finally { authClient.api = original; }
});
test("correction allowlist preserves missing fields without inferring medical context", () => {
  assert.deepEqual(permittedFields({ ...fields, unit: null, date: null, referenceRange: null, confidence: "HIGH", documentId: "forged" } as Fields), { ...fields, unit: null, date: null, referenceRange: null });
});
