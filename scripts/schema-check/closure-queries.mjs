import { readFileSync } from 'node:fs';
// Exercise the production SQL fragments, not a hand-maintained alternate schema/query.
export async function verifyClosureQueries(db) {
  const base = new URL('../../backend/src/main/java/com/carepath/', import.meta.url);
  const search = readFileSync(new URL('search/SearchService.java', base), 'utf8');
  const sources = search.match(/SOURCES="""([\s\S]*?)"""/)[1];
  const history = readFileSync(new URL('longitudinal/HistoryRepository.java', base), 'utf8');
  const eventCode = history.slice(history.indexOf('public Page<Event> events'));
  const first = eventCode.match(/String sql="([^"]+)"\+where/)[1];
  const unions = [...eventCode.matchAll(/sql\+="( UNION ALL [^"]+)"/g)].map(m => m[1]);
  const owner = '00000000-0000-0000-0000-000000000001';
  const run = async (sql) => {
    const args=[];
    sql=sql.replace(/\?/g,()=>{args.push(owner);return `$${args.length}`;});
    const result=await db.query(sql,args);
    if(result.rows.length) throw new Error('Expected empty closure query fixture');
  };
  await run(`SELECT * FROM (${sources}) results WHERE EXTRACT(MONTH FROM event_date)=9 AND (lower(title) LIKE '%report%' ESCAPE '!' OR (kind='DOCUMENT' AND EXISTS(SELECT 1 FROM vault_document_tag t WHERE t.document_id=results.id AND lower(t.tag) LIKE '%cbc%' ESCAPE '!'))) ORDER BY event_date DESC NULLS LAST,kind,id LIMIT 20 OFFSET 0`);
  // Keep ownership placeholders; replace only LIKE-bound values for a syntax/type check.
  let events=first+'o.owner_id=?'+unions.join('');
  events=events.replaceAll("LIKE ? ESCAPE '!'", "LIKE '%' ESCAPE '!'");
  await run(`SELECT * FROM (${events}) ev ORDER BY event_date NULLS LAST,occurs_at NULLS LAST,kind,id LIMIT 20 OFFSET 0`);
  console.log('PASS: production search and five-type timeline SQL syntax/type checks (PGlite only)');
}
