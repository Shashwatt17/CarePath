import { test } from "node:test";import assert from "node:assert/strict";import { createElement } from "react";import { renderToStaticMarkup } from "react-dom/server";
import { requestLocation, safeWeb, categories, type Facility } from "./nearby-client.ts";import { NearbyResults } from "./nearby-view.ts";
const base: Facility = { providerId: "synthetic", name: "Synthetic test facility", category: "HOSPITAL", address: null, distanceMeters: null, phone: null, openNow: null, hours: [], website: null, directions: null, bookingUrl: null, attributions: [] };
const render = (f: Facility[]) => renderToStaticMarkup(createElement(NearbyResults, { results: f }));
test("location is requested only by explicit function invocation", async () => {let calls=0;const geo={getCurrentPosition(success: PositionCallback){calls++;success({coords:{latitude:1,longitude:2}} as GeolocationPosition);}};assert.equal(calls,0);assert.deepEqual(await requestLocation(geo),{latitude:1,longitude:2});assert.equal(calls,1);});
test("permission denial and unavailable location allow manual fallback",async()=>{await assert.rejects(requestLocation(undefined),/manually/);await assert.rejects(requestLocation({getCurrentPosition(_s,e){e?.({code:1} as GeolocationPositionError);}}),/permission/);});
test("all requested categories exist",()=>assert.deepEqual(Object.keys(categories),["HOSPITAL","CLINIC","PHARMACY","DIAGNOSTIC_CENTER"]));
test("missing fields never invent contacts hours or availability",()=>{const x=render([base]);assert.match(x,/Hours unavailable/);assert.doesNotMatch(x,/href=|Book externally|Open now/);});
test("zero results has useful retry guidance",()=>assert.match(render([]),/larger radius/));
test("safe supplied actions and booking only if supplied",()=>{const x=render([{...base,phone:"+1234567890",website:"https://example.org",directions:"https://www.google.com/maps/dir/?api=1",bookingUrl:"https://example.org/book"}]);assert.match(x,/tel:\+1234567890/);assert.match(x,/Book externally/);assert.match(x,/noopener noreferrer/);assert.doesNotMatch(render([base]),/Book externally/);});
test("provider text is escaped and unsafe links excluded",()=>{const x=render([{...base,name:"<script>alert(1)</script>",website:"javascript:alert(1)",phone:"123;bad"}]);assert.doesNotMatch(x,/<script|href=/);assert.match(x,/&lt;script/);assert.equal(safeWeb("javascript:alert(1)"),undefined);});

test("authenticated client permits intended modules but rejects external paths", async () => {
 const { createAuthClient } = await import("./auth-client.ts");
 const c = createAuthClient(async (path, init) => { if(String(path).endsWith("refresh")) return Response.json({accessToken:"synthetic",expiresIn:300,user:{id:"synthetic",email:"synthetic@example.invalid",displayName:"Synthetic"}}); assert.equal((init?.headers as Record<string,string>).Authorization,"Bearer synthetic"); return Response.json({}); });
 for(const path of ["nearby-care/config","care/symptoms","visit-packs","shares"]) assert.equal((await c.api(`/api/v1/${path}`)).status,200);
 await assert.rejects(c.api("https://example.org")); await assert.rejects(c.api("/api/v1/nearby-care/../auth"));
});
test("provider failure is safe and search sends only consented coordinates/category", async () => {
 const { authClient } = await import("./auth-client.ts"); const { nearby } = await import("./nearby-client.ts"); const old = authClient.api;
 try { authClient.api = async (path, init) => {assert.equal(path,"/api/v1/nearby-care/search");assert.deepEqual(JSON.parse(String(init?.body)),{latitude:1,longitude:2,radiusMeters:1000,category:"PHARMACY",locationConsent:true});return Response.json({message:"secret"},{status:503});}; await assert.rejects(nearby.search(1,2,1000,"PHARMACY"), /Provider configuration/); }
 finally {authClient.api=old;}
});
