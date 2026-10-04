import { test } from "node:test";
import assert from "node:assert/strict";
import { authClient, ApiError } from "./auth-client.ts";
import { extractionClient } from "./extraction-client.ts";
import { checked } from "./vault-client.ts";
test("processing client uses authenticated owner-scoped routes and encoded identifiers", async () => {
  const original = authClient.api; const calls: Array<{ path: string; method: string }> = [];
  authClient.api = async (path, init) => { calls.push({ path, method: init?.method ?? "GET" }); return new Response(JSON.stringify({ state: "PROCESSING" })); };
  try {
    await extractionClient.process("a/b"); await extractionClient.status("a/b"); await extractionClient.get("a/b");
    await extractionClient.evidence("a/b", "c/d"); await extractionClient.retry("a/b");
    assert.deepEqual(calls, [
      { path: "/api/v1/documents/a%2Fb/process", method: "POST" }, { path: "/api/v1/documents/a%2Fb/processing-status", method: "GET" },
      { path: "/api/v1/documents/a%2Fb/extraction", method: "GET" }, { path: "/api/v1/documents/a%2Fb/extraction/evidence/c%2Fd", method: "GET" },
      { path: "/api/v1/documents/a%2Fb/retry", method: "POST" },
    ]);
  } finally { authClient.api = original; }
});
test("processing errors never reflect raw extracted text or server internals", async () => {
  for (const body of ["<html>private path /srv/medical</html>", JSON.stringify({ code: "UNKNOWN", message: "private medical content" })]) {
    await assert.rejects(checked(new Response(body, { status: 500 })), (e: unknown) => e instanceof ApiError && !/private|srv|html/.test(e.message));
  }
  await assert.rejects(checked(new Response(JSON.stringify({ code: "RETRY_NOT_ALLOWED" }), { status: 409 })), (e: unknown) => e instanceof ApiError && e.message.includes("cannot be retried"));
});
