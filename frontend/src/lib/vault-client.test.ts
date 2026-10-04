import { test } from "node:test";
import assert from "node:assert/strict";
import { validateFile } from "./vault-client.ts";
test("upload feedback rejects empty, oversized and unsupported files", () => {
  assert.ok(validateFile({ name: "x.pdf", type: "application/pdf", size: 0 }, 100));
  assert.ok(validateFile({ name: "x.pdf", type: "application/pdf", size: 101 }, 100));
  assert.ok(validateFile({ name: "x.html", type: "text/html", size: 10 }, 100));
});
test("supported upload feedback permits all original formats", () => {
  for (const [name, type] of [["x.pdf", "application/pdf"], ["x.jpg", "image/jpeg"], ["x.jpeg", "image/jpeg"], ["x.png", "image/png"]]) assert.equal(validateFile({ name, type, size: 100 }, 100), null);
});
