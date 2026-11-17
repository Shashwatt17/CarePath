import { PGlite } from "@electric-sql/pglite";
import { readFileSync, readdirSync } from "node:fs";
const dir = new URL("../../backend/src/main/resources/db/migration/", import.meta.url);
const db = new PGlite();
try {
  const files = readdirSync(dir).filter(f => /^V\d+__.*\.sql$/.test(f)).sort((a,b) => Number(a.match(/^V(\d+)/)[1])-Number(b.match(/^V(\d+)/)[1]));
  for (const file of files) await db.exec(readFileSync(new URL(file, dir), "utf8"));
  const constraints = await db.query("SELECT pg_get_constraintdef(oid) AS definition FROM pg_constraint WHERE conrelid='vision_page_proposal'::regclass");
  for (const required of ["FOREIGN KEY (extraction_id, document_id, owner_id)", "FOREIGN KEY (document_id, owner_id)", "UNIQUE (extraction_id, page_number)"]) {
    if (!constraints.rows.some(r => r.definition.includes(required))) throw new Error("Missing constraint: " + required);
  }
  console.log(`PASS: ${files.length} migrations applied; vision ownership, extraction identity and duplicate-page constraints present. PGlite only, not hosted Flyway verification.`);
} finally { await db.close(); }
