import { test } from "node:test";
import assert from "node:assert/strict";
import { authClient } from "./auth-client.ts";
import { assistantClient } from "./assistant-client.ts";
test("assistant sends explicit AI consent and only allowed scope fields", async () => {
 const old = authClient.api; let sent: RequestInit | undefined;
 authClient.api = async (path, init) => { assert.equal(path, "/api/v1/assistant/ask"); sent = init; return new Response("{}"); };
 try { await assistantClient.ask({ question: "Explain Hemoglobin", useAi: false, conceptId: "c", ...{ ownerId: "forged" } }); assert.deepEqual(JSON.parse(String(sent?.body)), { question: "Explain Hemoglobin", useAi: false, conceptId: "c" }); } finally { authClient.api = old; }
});
test("saved questions use controlled CRUD bodies and encoded identifiers", async () => {
 const old = authClient.api; const calls: {path:string; init?:RequestInit}[] = [];
 authClient.api = async (path, init) => { calls.push({path,init}); return new Response(init?.method === "DELETE" ? null : "{}", {status:init?.method === "DELETE"?204:200}); };
 try { await assistantClient.save({text:"Discuss result",observationIds:["o"], ...{origin:"AI",ownerId:"forged"}}); await assistantClient.edit("a/b","Edited",2); await assistantClient.delete("a/b"); await assistantClient.list(2); assert.deepEqual(JSON.parse(String(calls[0].init?.body)),{text:"Discuss result",observationIds:["o"]}); assert.equal(calls[1].path,"/api/v1/assistant/questions/a%2Fb"); assert.deepEqual(JSON.parse(String(calls[1].init?.body)),{text:"Edited",version:2}); assert.equal(calls[3].path,"/api/v1/assistant/questions?page=2"); } finally {authClient.api=old;}
});
test("assistant errors never reflect provider payloads", async () => {
 const old=authClient.api;authClient.api=async()=>new Response("<script>private medical data</script>",{status:500});
 try {await assert.rejects(assistantClient.ask({question:"Explain",useAi:true}),(e:unknown)=>e instanceof Error && !/script|private/.test(e.message));} finally {authClient.api=old;}
});
