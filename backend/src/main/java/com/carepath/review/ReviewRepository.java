package com.carepath.review;
import static com.carepath.review.ReviewDtos.*;
import com.carepath.intelligence.*;
import com.carepath.terminology.*;
import java.util.*;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class ReviewRepository {
    public record Entry(UUID owner,UUID document,UUID extraction,UUID page,String filename,String state,long version,ExtractionData.Candidate candidate,UUID observation) {}
    private final JdbcTemplate jdbc;private final ExtractionRepository extraction;private final Terminology terms;
    public ReviewRepository(JdbcTemplate jdbc,ExtractionRepository extraction,Terminology terms) {this.jdbc=jdbc;this.extraction=extraction;this.terms=terms;}
    private static final String BASE="SELECT c.*,p.page_number,p.extraction_method,d.original_filename,(SELECT o.id FROM medical_observation o WHERE o.source_candidate_id=c.id AND o.owner_id=c.owner_id) AS observation_id FROM extraction_candidate c JOIN document_page p ON p.id=c.page_id AND p.owner_id=c.owner_id JOIN medical_document d ON d.id=c.document_id AND d.owner_id=c.owner_id ";
    private Entry row(java.sql.ResultSet r,int n) throws java.sql.SQLException { return new Entry(r.getObject("owner_id",UUID.class),r.getObject("document_id",UUID.class),r.getObject("extraction_id",UUID.class),r.getObject("page_id",UUID.class),r.getString("original_filename"),r.getString("review_state"),r.getLong("review_version"),extraction.candidate(r,n),r.getObject("observation_id",UUID.class)); }
    public Optional<Entry> find(UUID id,UUID owner) { return jdbc.query(BASE+"WHERE c.id=? AND c.owner_id=?",this::row,id,owner).stream().findFirst(); }
    public void lock(UUID id,UUID owner) { jdbc.queryForList("SELECT id FROM extraction_candidate WHERE id=? AND owner_id=? FOR UPDATE",id,owner); }
    public List<Entry> list(UUID owner,UUID document,String state,int page,int size) {
        String where=" WHERE c.owner_id=?";List<Object> args=new ArrayList<>(List.of(owner));
        if(document!=null) {where+=" AND c.document_id=?";args.add(document);}
        if(state!=null) {where+=" AND c.review_state=?";args.add(state);}
        args.add(size);args.add((long)page*size);
        return jdbc.query(BASE+where+" ORDER BY c.created_at,c.id LIMIT ? OFFSET ?",this::row,args.toArray());
    }
    public long count(UUID owner,UUID document,String state) {
        String where=" WHERE owner_id=?";List<Object> args=new ArrayList<>(List.of(owner));
        if(document!=null) {where+=" AND document_id=?";args.add(document);}
        if(state!=null) {where+=" AND review_state=?";args.add(state);}
        return jdbc.queryForObject("SELECT count(*) FROM extraction_candidate"+where,Long.class,args.toArray());
    }
    public void validateEvidence(Entry e) {
        var c=e.candidate();String text=jdbc.queryForObject("SELECT extracted_text FROM document_page WHERE id=? AND document_id=? AND owner_id=?",String.class,e.page(),e.document(),e.owner());var source=c.source();
        if(text==null || source.start()<0 || source.end()>text.length() || source.end()<=source.start() || !text.substring(source.start(),source.end()).equals(source.text()) || !source.text().contains(c.originalTestName()) || c.originalValue()!=null && !source.text().contains(c.originalValue()) || c.originalUnit()!=null && !source.text().contains(c.originalUnit()) || c.referenceText()!=null && !source.text().contains(c.referenceText())) throw new com.carepath.foundation.ApiFailure(409,"PROVENANCE_INVALID","The source evidence could not be validated.");
    }
    public Optional<String> previousHash(Entry e,String action) { return jdbc.query("SELECT request_hash FROM candidate_verification WHERE candidate_id=? AND owner_id=? AND action=?",(r,n)->r.getString(1),e.candidate().id(),e.owner(),action).stream().findFirst(); }
    public void resolve(Entry e,String state) { jdbc.update("UPDATE extraction_candidate SET review_state=?,review_version=review_version+1 WHERE id=? AND owner_id=?",state,e.candidate().id(),e.owner()); }
    public UUID insertObservation(Entry e,Fields f,Preview p,String action,Instant now) {
        UUID id=UUID.randomUUID();var c=e.candidate();var concept=p.mapping().concept();
        // Original fields are copied from immutable machine extraction, corrected fields kept separately.
        jdbc.update("INSERT INTO medical_observation(id,owner_id,document_id,extraction_id,source_candidate_id,original_test_name,original_value_text,original_numeric_value,original_unit,original_reference_text,original_reference_low,original_reference_high,original_abnormal_flag,value_comparator,concept_id,canonical_name_snapshot,normalization_version,normalized_value,normalized_unit,conversion_rule_id,observed_date,date_basis,laboratory,verification_status,verified_at,verified_by,verified_test_name,verified_value_text,verified_numeric_value,verified_unit,verified_reference_text,verified_reference_low,verified_reference_high,mapping_status,unit_status,derived_range_status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            id,e.owner(),e.document(),e.extraction(),c.id(),c.originalTestName(),c.originalValue(),c.numericValue(),c.originalUnit(),c.referenceText(),c.referenceLower(),c.referenceUpper(),c.abnormalFlag(),p.comparator(),concept==null?null:concept.id(),concept==null?null:concept.name(),"carepath-demo-v1",p.normalized().value(),p.normalized().unit(),p.normalized().ruleId(),f.date(),f.date()==null?"UNKNOWN":"USER_CONFIRMED",c.providerName(),action.equals("CONFIRM")?"CONFIRMED":"CORRECTED",now.atOffset(ZoneOffset.UTC),e.owner(),f.testName(),f.value(),p.number(),f.unit(),f.referenceRange(),p.referenceLow(),p.referenceHigh(),p.mapping().status(),p.normalized().status(),p.derivedRangeStatus());
        jdbc.update("INSERT INTO observation_source(id,owner_id,observation_id,page_id,extraction_id,document_id,evidence_text,char_start,char_end) VALUES(?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),e.owner(),id,e.page(),e.extraction(),e.document(),c.source().text(),c.source().start(),c.source().end());
        return id;
    }
    public void history(Entry e,UUID observation,String action,String reason,String fields,String hash,Instant now) {
        jdbc.update("INSERT INTO candidate_verification(id,candidate_id,owner_id,extraction_id,document_id,observation_id,actor_id,action,reason,changed_fields,request_hash,occurred_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),e.candidate().id(),e.owner(),e.extraction(),e.document(),observation,e.owner(),action,reason,fields,hash,now.atOffset(ZoneOffset.UTC));
    }
    public void updateDocument(Entry e) {
        if(count(e.owner(),e.document(),"PENDING_REVIEW")==0)
            jdbc.update("UPDATE medical_document SET status='COMPLETED',version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND owner_id=? AND status IN ('COMPLETED','NEEDS_REVIEW')",e.document(),e.owner());
    }
    public Optional<Observation> observation(UUID id,UUID owner) {
        return jdbc.query("SELECT o.*,v.reason FROM trusted_medical_observation o JOIN candidate_verification v ON v.observation_id=o.id AND v.owner_id=o.owner_id WHERE o.id=? AND o.owner_id=?",(r,n)->{
            var entry=find(r.getObject("source_candidate_id",UUID.class),owner).orElseThrow();var c=entry.candidate();
            UUID conceptId=r.getObject("concept_id",UUID.class);var concept=conceptId==null?null:new Terminology.Concept(conceptId,r.getString("canonical_name_snapshot"),r.getString("normalized_unit"),null,null,r.getString("normalization_version"),"Verified terminology snapshot; no standard identifier assigned");
            var mapping=new Terminology.Mapping(r.getString("verified_test_name"),r.getString("mapping_status"),concept,List.of(),"VERIFIED_SNAPSHOT_V1");
            var norm=new UnitNormalizer.Normalized(r.getBigDecimal("normalized_value"),r.getString("normalized_unit"),r.getString("unit_status"),r.getObject("conversion_rule_id",UUID.class),"carepath-units-v1");
            var preview=new Preview(mapping,norm,r.getString("value_comparator"),r.getBigDecimal("verified_numeric_value"),r.getBigDecimal("verified_reference_low"),r.getBigDecimal("verified_reference_high"),r.getString("derived_range_status"),List.of(),true);
            var fields=new Fields(r.getString("verified_test_name"),r.getString("verified_value_text"),r.getString("verified_unit"),r.getString("verified_reference_text"),r.getObject("observed_date",LocalDate.class),conceptId);
            return new Observation(id,entry.document(),c.id(),r.getString("verification_status"),r.getObject("verified_at",OffsetDateTime.class).toInstant(),c,fields,preview,c.source(),r.getString("reason"));
        },id,owner).stream().findFirst();
    }
}
