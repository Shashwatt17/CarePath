package com.carepath.longitudinal;
import static com.carepath.longitudinal.HistoryDtos.*;
import java.util.*;
import java.time.*;
import java.sql.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class HistoryRepository {
 private final JdbcTemplate jdbc;
 public HistoryRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 private static final String BASE="SELECT o.*,c.source_text,c.source_start,c.source_end,p.page_number,p.extraction_method,d.original_filename FROM trusted_medical_observation o JOIN extraction_candidate c ON c.id=o.source_candidate_id AND c.owner_id=o.owner_id JOIN document_page p ON p.id=c.page_id AND p.owner_id=o.owner_id JOIN medical_document d ON d.id=o.document_id AND d.owner_id=o.owner_id ";
 private Point point(ResultSet r,int n)throws SQLException {
  var original=new Reading(r.getString("original_value_text"),r.getBigDecimal("original_numeric_value"),r.getString("original_unit"),r.getString("original_reference_text"),r.getBigDecimal("original_reference_low"),r.getBigDecimal("original_reference_high"),r.getString("original_abnormal_flag"));
  var effective=new Reading(r.getString("verified_value_text"),r.getBigDecimal("verified_numeric_value"),r.getString("verified_unit"),r.getString("verified_reference_text"),r.getBigDecimal("verified_reference_low"),r.getBigDecimal("verified_reference_high"),r.getString("original_abnormal_flag"));
  var evidence=new Evidence(r.getObject("document_id",UUID.class),r.getObject("source_candidate_id",UUID.class),r.getString("original_filename"),r.getInt("page_number"),r.getString("source_text"),r.getInt("source_start"),r.getInt("source_end"),r.getString("extraction_method"));
  return new Point(r.getObject("id",UUID.class),r.getObject("concept_id",UUID.class),Objects.toString(r.getString("canonical_name_snapshot"),r.getString("verified_test_name")),r.getObject("observed_date",LocalDate.class),r.getString("laboratory"),r.getString("specimen"),r.getString("method"),r.getString("normalization_version"),r.getString("mapping_status"),r.getString("unit_status"),r.getString("value_comparator"),r.getString("verification_status"),r.getObject("verified_at",OffsetDateTime.class).toInstant(),original,effective,r.getBigDecimal("normalized_value"),r.getString("normalized_unit"),evidence);
 }
 public Optional<Point> find(UUID id,UUID owner){return jdbc.query(BASE+"WHERE o.id=? AND o.owner_id=?",this::point,id,owner).stream().findFirst();}
 public boolean ownsConcept(UUID id,UUID owner){return jdbc.queryForObject("SELECT count(*) FROM trusted_medical_observation WHERE concept_id=? AND owner_id=?",Long.class,id,owner)>0;}
 public Page<Concept> concepts(UUID owner,int page,int size){
  var items=jdbc.query("SELECT concept_id,MIN(canonical_name_snapshot) AS name,count(*) AS n FROM trusted_medical_observation WHERE owner_id=? AND concept_id IS NOT NULL GROUP BY concept_id ORDER BY name,concept_id LIMIT ? OFFSET ?",(r,n)->new Concept(r.getObject("concept_id",UUID.class),r.getString("name"),r.getLong("n")),owner,size,(long)page*size);
  long total=jdbc.queryForObject("SELECT count(DISTINCT concept_id) FROM trusted_medical_observation WHERE owner_id=?",Long.class,owner);return new Page<>(items,total,page,size);
 }
 public Page<Point> history(UUID owner,UUID concept,int page,int size){return new Page<>(jdbc.query(BASE+"WHERE o.owner_id=? AND o.concept_id=? ORDER BY o.observed_date NULLS LAST,o.id LIMIT ? OFFSET ?",this::point,owner,concept,size,(long)page*size),jdbc.queryForObject("SELECT count(*) FROM trusted_medical_observation WHERE owner_id=? AND concept_id=?",Long.class,owner,concept),page,size);}
 public List<Point> window(UUID owner,UUID concept){return jdbc.query(BASE+"WHERE o.owner_id=? AND o.concept_id=? ORDER BY o.observed_date DESC NULLS LAST,o.id DESC LIMIT 501",this::point,owner,concept);}
 public List<Point> reportPoints(UUID owner,UUID document){return jdbc.query(BASE+"WHERE o.owner_id=? AND o.document_id=? ORDER BY o.concept_id,o.id LIMIT 501",this::point,owner,document);}
 public Report report(UUID owner,UUID id){
  return jdbc.query("SELECT d.id,d.original_filename,d.document_type,d.status,(SELECT MIN(o.observed_date) FROM trusted_medical_observation o WHERE o.owner_id=d.owner_id AND o.document_id=d.id) AS report_date,(SELECT MIN(o.laboratory) FROM trusted_medical_observation o WHERE o.owner_id=d.owner_id AND o.document_id=d.id) AS laboratory,NOT EXISTS(SELECT 1 FROM extraction_candidate c WHERE c.owner_id=d.owner_id AND c.document_id=d.id AND (c.review_state NOT IN ('CONFIRMED','CORRECTED_AND_CONFIRMED') OR NOT EXISTS(SELECT 1 FROM trusted_medical_observation o WHERE o.source_candidate_id=c.id AND o.concept_id IS NOT NULL))) AS fully_reviewed FROM medical_document d WHERE d.owner_id=? AND d.id=?",(r,n)->new Report(id,r.getString("original_filename"),r.getString("document_type"),r.getString("status"),r.getObject("report_date",LocalDate.class),r.getString("laboratory"),null,r.getBoolean("fully_reviewed")),owner,id).stream().findFirst().orElseThrow(HistoryService::missing);
 }
 public String panel(UUID owner,UUID document){
  var pages=jdbc.query("SELECT substring(extracted_text,1,2000) FROM document_page WHERE owner_id=? AND document_id=? ORDER BY page_number LIMIT 20",(r,n)->r.getString(1),owner,document);
  Set<String> matches=new HashSet<>();for(var text:pages)for(String line:text.split("\\R")){var heading=line.strip().toUpperCase(Locale.ROOT);if(Set.of("CBC","COMPLETE BLOOD COUNT").contains(heading))matches.add("CBC");if(heading.equals("COMPREHENSIVE METABOLIC PANEL"))matches.add("CMP");}
  return matches.size()==1?matches.iterator().next():null;
 }
 public Page<Event> events(UUID owner,UUID concept,UUID document,String search,int page,int size){
  String where="o.owner_id=?";List<Object> args=new ArrayList<>();args.add(owner);
  if(concept!=null){where+=" AND o.concept_id=?";args.add(concept);}if(document!=null){where+=" AND o.document_id=?";args.add(document);}
  String like="%"+search.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
  where+=" AND (lower(coalesce(o.canonical_name_snapshot,o.verified_test_name)) LIKE ? ESCAPE '!' OR lower(d.original_filename) LIKE ? ESCAPE '!')";args.add(like);args.add(like);
  String sql="SELECT 'OBSERVATION' AS kind,o.id,o.observed_date AS event_date,coalesce(o.canonical_name_snapshot,o.verified_test_name) AS title,o.document_id,CAST(NULL AS integer) AS source_page,CAST(NULL AS varchar) AS event_status,CAST(NULL AS timestamp with time zone) AS occurs_at FROM trusted_medical_observation o JOIN medical_document d ON d.id=o.document_id AND d.owner_id=o.owner_id WHERE "+where;
  if(concept==null){sql+=" UNION ALL SELECT 'DOCUMENT',d.id,d.document_date,d.original_filename,d.id,CAST(NULL AS integer),d.document_type,CAST(NULL AS timestamp with time zone) FROM medical_document d WHERE d.owner_id=? AND lower(d.original_filename) LIKE ? ESCAPE '!'";args.add(owner);args.add(like);if(document!=null){sql+=" AND d.id=?";args.add(document);}}
  if(concept==null && document==null){sql+=" UNION ALL SELECT 'SYMPTOM',s.id,s.start_date,s.name,CAST(NULL AS UUID),CAST(NULL AS integer),CASE WHEN s.resolved_at IS NULL THEN 'ACTIVE' ELSE 'RESOLVED' END,s.started_at FROM symptom s WHERE s.owner_id=? AND lower(s.name) LIKE ? ESCAPE '!'";args.add(owner);args.add(like);}
  if(concept==null && document==null){
   sql+=" UNION ALL SELECT 'APPOINTMENT',a.id,CAST(a.starts_at AT TIME ZONE 'UTC' AS date),a.provider_name,CAST(NULL AS UUID),CAST(NULL AS integer),a.status,a.starts_at FROM appointment a WHERE a.owner_id=? AND (lower(a.provider_name) LIKE ? ESCAPE '!' OR lower(coalesce(a.specialty,'')) LIKE ? ESCAPE '!')";args.add(owner);args.add(like);args.add(like);
  }
  if(concept==null){
   sql+=" UNION ALL SELECT 'FOLLOW_UP',f.id,f.confirmed_date,'Confirmed follow-up',f.document_id,p.page_number,f.status,f.confirmed_at_time FROM follow_up f JOIN document_page p ON p.id=f.page_id AND p.owner_id=f.owner_id AND p.document_id=f.document_id JOIN medical_document d ON d.id=f.document_id AND d.owner_id=f.owner_id WHERE f.owner_id=? AND f.status IN ('CONFIRMED','EDITED') AND (lower(f.source_text) LIKE ? ESCAPE '!' OR lower(d.original_filename) LIKE ? ESCAPE '!')";args.add(owner);args.add(like);args.add(like);
   if(document!=null){sql+=" AND f.document_id=?";args.add(document);}
  }
  long total=jdbc.queryForObject("SELECT count(*) FROM ("+sql+") ev",Long.class,args.toArray());args.add(size);args.add((long)page*size);
  var rows=jdbc.query("SELECT * FROM ("+sql+") ev ORDER BY event_date NULLS LAST,occurs_at NULLS LAST,kind,id LIMIT ? OFFSET ?",(r,n)->new Event(r.getString("kind"),r.getObject("id",UUID.class),r.getObject("event_date",LocalDate.class),r.getString("title"),null,r.getObject("document_id",UUID.class),(Integer)r.getObject("source_page"),r.getString("event_status"),r.getObject("occurs_at",OffsetDateTime.class)==null?null:r.getObject("occurs_at",OffsetDateTime.class).toInstant()),args.toArray());
  var ids=rows.stream().filter(e->e.type().equals("OBSERVATION")).map(Event::id).toList();Map<UUID,Point> points=new HashMap<>();
  if(!ids.isEmpty()){List<Object> params=new ArrayList<>();params.add(owner);params.addAll(ids);for(var p:jdbc.query(BASE+"WHERE o.owner_id=? AND o.id IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+")",this::point,params.toArray()))points.put(p.id(),p);}
  return new Page<>(rows.stream().map(e->new Event(e.type(),e.id(),e.date(),e.title(),points.get(e.id()),e.documentId(),e.sourcePage(),e.status(),e.occursAt())).toList(),total,page,size);
 }
}
