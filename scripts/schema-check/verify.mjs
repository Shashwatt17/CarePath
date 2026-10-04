import { verifyClosureQueries } from "./closure-queries.mjs";
import { PGlite } from "@electric-sql/pglite";
import { readFileSync } from "node:fs";
const root = new URL("../", import.meta.url);
const db = new PGlite();
try {
  await db.exec(readFileSync(new URL("../backend/src/main/resources/db/migration/V1__foundation.sql", root), "utf8"));
  await db.exec(readFileSync(new URL("../backend/src/main/resources/db/migration/V2__authentication_security.sql", root), "utf8"));
  await db.exec(readFileSync(new URL("../backend/src/main/resources/db/migration/V3__secure_document_vault.sql", root), "utf8"));
  await db.exec(readFileSync(new URL("../backend/src/main/resources/db/migration/V4__extraction_candidates.sql", root), "utf8"));
  await db.exec(readFileSync(new URL("../backend/src/main/resources/db/migration/V5__normalization_and_verification.sql", root), "utf8"));
  await db.exec(readFileSync(new URL("../backend/src/main/resources/db/migration/V6__longitudinal_read_indexes.sql", root), "utf8"));
  await db.exec(readFileSync(new URL("../backend/src/main/resources/db/migration/V7__saved_question_evidence.sql", root), "utf8"));
  await db.exec(readFileSync(new URL("../backend/src/main/resources/db/migration/V8__care_organization.sql", root), "utf8"));
  await db.exec(readFileSync(new URL("../backend/src/main/resources/db/migration/V9__visit_pack_snapshots.sql", root), "utf8"));
  await db.exec(readFileSync(new URL("../backend/src/main/resources/db/migration/V10__scoped_pack_sharing.sql", root), "utf8"));

  await db.exec(readFileSync(new URL("review-schema-invariants.sql", root), "utf8"));
  await db.exec(readFileSync(new URL("extraction-schema-invariants.sql", root), "utf8"));
  await db.exec(readFileSync(new URL("vault-schema-invariants.sql", root), "utf8"));
  await db.exec(readFileSync(new URL("schema-invariants.sql", root), "utf8"));
  await db.exec(readFileSync(new URL("auth-schema-invariants.sql", root), "utf8"));
  await db.exec(readFileSync(new URL("assistant-schema-invariants.sql", root), "utf8"));
  await verifyClosureQueries(db);
  const result = await db.query("select count(*)::integer as remaining_users from app_user");
  if (result.rows[0].remaining_users !== 0) throw new Error("Test rows were not rolled back");
  console.log("PASS: V1 + V2 + V3 + V4 + V5 + V6 + V7 + V8 + V9 + V10 applied; 40 schema invariants; 0 persisted test users");
  console.log("PGlite check only: not Flyway/JDBC/Docker runtime verification.");
} finally { await db.close(); }
