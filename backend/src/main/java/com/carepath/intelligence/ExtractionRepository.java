package com.carepath.intelligence;
import static com.carepath.intelligence.ExtractionData.*;
import java.util.*;
import java.time.*;
import java.sql.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class ExtractionRepository {
    private final JdbcTemplate jdbc;private final ObjectMapper mapper;
    public ExtractionRepository(JdbcTemplate jdbc,ObjectMapper mapper) { this.jdbc=jdbc;this.mapper=mapper; }
    private String json(Object value) { try { return mapper.writeValueAsString(value); } catch(Exception e) { throw new IllegalStateException("Extraction serialization failed"); } }
    private <T> T read(String value,Class<T> type) { try { return mapper.readValue(value,type); } catch(Exception e) { throw new IllegalStateException("Extraction persistence invalid"); } }
    public void save(UUID owner,UUID document,UUID job,Result result) {
        new ProvenanceValidator().validate(result,200,500000);
        UUID extraction=UUID.randomUUID();int revision=jdbc.queryForObject("SELECT coalesce(max(revision),0)+1 FROM document_extraction WHERE document_id=? AND owner_id=?",Integer.class,document,owner);
        jdbc.update("INSERT INTO document_extraction(id,owner_id,document_id,job_id,revision,pipeline_version,extractor,ocr_used,classification,confidence_band,classification_method,classification_evidence,document_information,needs_review,completed_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)",
            extraction,owner,document,job,revision,result.pipelineVersion(),"PDFBOX_TESSERACT_RULES",result.ocrUsed(),result.classification().category(),result.classification().confidence().name(),result.classification().method(),json(result.classification().evidence()),json(result.information()),result.needsReview());
        Map<Integer,UUID> pages=new HashMap<>();
        for(Page page:result.pages()) {
            UUID id=UUID.randomUUID();pages.put(page.number(),id);
            jdbc.update("INSERT INTO document_page(id,owner_id,document_id,extraction_id,page_number,extracted_text,confidence,extraction_method,line_layout) VALUES(?,?,?,?,?,?,?,?,?)",
                id,owner,document,extraction,page.number(),page.text(),page.ocrConfidence(),page.method().name(),json(page.lines()));
        }
        for(Candidate c:result.candidates()) jdbc.update("INSERT INTO extraction_candidate(id,owner_id,document_id,extraction_id,page_id,original_test_name,original_value,numeric_value,value_comparator,original_unit,reference_lower,reference_upper,reference_text,abnormal_flag,report_date,provider_name,confidence_band,confidence_reasons,source_text,source_start,source_end,source_box) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            c.id(),owner,document,extraction,pages.get(c.source().page()),c.originalTestName(),c.originalValue(),c.numericValue(),c.comparator(),c.originalUnit(),c.referenceLower(),c.referenceUpper(),c.referenceText(),c.abnormalFlag(),c.reportDate(),c.providerName(),c.confidence().name(),String.join(",",c.reasons()),c.source().text(),c.source().start(),c.source().end(),c.source().box()==null?null:json(c.source().box()));
    }
    public Optional<View> latest(UUID document,UUID owner) {
        var results=jdbc.query("SELECT * FROM document_extraction WHERE document_id=? AND owner_id=? ORDER BY revision DESC LIMIT 1",(r,n)-> {
            UUID id=r.getObject("id",UUID.class);
            var pages=jdbc.query("SELECT * FROM document_page WHERE extraction_id=? AND document_id=? AND owner_id=? ORDER BY page_number",(p,i)->new Page(p.getInt("page_number"),p.getString("extracted_text"),Method.valueOf(p.getString("extraction_method")),p.getObject("confidence")==null?null:p.getDouble("confidence"),Arrays.asList(read(p.getString("line_layout"),Line[].class))),id,document,owner);
            var candidates=jdbc.query("SELECT c.*,p.page_number,p.extraction_method FROM extraction_candidate c JOIN document_page p ON p.id=c.page_id AND p.owner_id=c.owner_id WHERE c.extraction_id=? AND c.document_id=? AND c.owner_id=? ORDER BY p.page_number,c.source_start,c.id",this::candidate,id,document,owner);
            var classification=new Classification(r.getString("classification"),Band.valueOf(r.getString("confidence_band")),r.getString("classification_method"),Arrays.asList(read(r.getString("classification_evidence"),Source[].class)));
            return new View(id,document,r.getInt("revision"),new Result(pages,classification,read(r.getString("document_information"),Information.class),candidates,r.getBoolean("needs_review"),r.getBoolean("ocr_used"),r.getString("pipeline_version")));
        },document,owner);return results.stream().findFirst();
    }
    public Optional<Source> evidence(UUID document,UUID owner,UUID candidate) {
        return jdbc.query("SELECT c.*,p.page_number,p.extraction_method FROM extraction_candidate c JOIN document_page p ON p.id=c.page_id AND p.owner_id=c.owner_id WHERE c.document_id=? AND c.owner_id=? AND c.id=?",this::candidate,document,owner,candidate).stream().map(Candidate::source).findFirst();
    }
    public Candidate candidate(ResultSet r,int n) throws SQLException {
        String reasons=r.getString("confidence_reasons"),box=r.getString("source_box");
        return new Candidate(r.getObject("id",UUID.class),r.getString("original_test_name"),r.getString("original_value"),r.getBigDecimal("numeric_value"),r.getString("value_comparator"),r.getString("original_unit"),r.getBigDecimal("reference_lower"),r.getBigDecimal("reference_upper"),r.getString("reference_text"),r.getString("abnormal_flag"),r.getObject("report_date",LocalDate.class),r.getString("provider_name"),Band.valueOf(r.getString("confidence_band")),reasons.isBlank()?List.of():List.of(reasons.split(",")),
            new Source(r.getInt("page_number"),r.getInt("source_start"),r.getInt("source_end"),r.getString("source_text"),Method.valueOf(r.getString("extraction_method")),box==null?null:read(box,Box.class)));
    }
}
