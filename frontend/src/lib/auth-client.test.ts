import { test } from "node:test";
import assert from "node:assert/strict";
import { createAuthClient, ApiError } from "./auth-client.ts";
const grant = { accessToken: "test-only-access", expiresIn: 300, user: { id: "synthetic", email: "synthetic@example.invalid", displayName: "Synthetic" } };
function reply(body: unknown, status = 200) { return new Response(status === 204 ? null : JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } }); }
test("restoration is single-flight and access is sent only as bearer header", async () => {
  let refreshes = 0;
  const client = createAuthClient(async (url, init) => {
    assert.equal(init?.credentials, "same-origin");
    if (String(url).endsWith("refresh")) { refreshes++; return reply(grant); }
    assert.equal((init?.headers as Record<string,string>).Authorization, "Bearer test-only-access"); return reply(grant.user);
  });
  await Promise.all([client.restore(), client.restore(), client.restore()]); assert.equal(refreshes, 1);
  assert.deepEqual(await client.me(), grant.user);
});
test("401 refresh ends session; outage is surfaced", async () => {
  const denied = createAuthClient(async () => reply({ code: "SESSION_INVALID" }, 401));
  assert.equal(await denied.restore(), null);
  const unavailable = createAuthClient(async () => reply({ code: "AUTH_UNAVAILABLE" }, 503));
  await assert.rejects(unavailable.restore(), (e: unknown) => e instanceof ApiError && e.status === 503);
});
test("logout clears local session only after successful server revocation", async () => {
  let fail = true;
  const client = createAuthClient(async (url) => String(url).endsWith("logout") ? reply({}, fail ? 503 : 204) : reply(grant));
  await client.login("synthetic@example.invalid", "not-a-real-password");
  await assert.rejects(client.logout()); assert.deepEqual(client.currentUser(), grant.user);
  fail = false; await client.logout(); assert.equal(client.currentUser(), null);
});
test("invalid response cannot authenticate and raw errors are not reflected", async () => {
  const client = createAuthClient(async () => reply({ user: grant.user }));
  await assert.rejects(client.login("a@example.invalid", "test")); assert.equal(client.currentUser(), null);
  const bad = createAuthClient(async () => reply({ code: "UNKNOWN", message: "sensitive backend data" }, 500));
  await assert.rejects(bad.login("a@example.invalid", "test"), (e: unknown) => e instanceof ApiError && !e.message.includes("sensitive"));
});

test("vault requests restore memory access and retry one rejected bearer", async () => {
  let refreshes = 0, calls = 0;
  const client = createAuthClient(async (url, init) => {
    if (String(url).endsWith("refresh")) { refreshes++; return reply(grant); }
    assert.equal(init?.cache, "no-store");
    assert.equal((init?.headers as Record<string, string>).Authorization, "Bearer test-only-access");
    return ++calls === 1 ? reply({}, 401) : reply({ items: [] });
  });
  assert.equal((await client.api("/api/v1/documents")).status, 200);
  assert.equal(refreshes, 2); assert.equal(calls, 2);
});
test("vault API never sends access credentials to an arbitrary URL", async () => {
  let called = false;
  const client = createAuthClient(async () => { called = true; return reply(grant); });
  await assert.rejects(client.api("https://attacker.invalid/api/v1/documents"));
  await assert.rejects(client.api("/api/v1/documents/../../auth"));
  assert.equal(called, false);
});
