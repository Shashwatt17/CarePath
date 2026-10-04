package com.carepath.vault;
import static com.carepath.vault.DocumentDtos.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class DocumentRepository {
    private final JdbcTemplate jdbc;
    public DocumentRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private MedicalDocument map(ResultSet r,int row) throws SQLException {
        UUID id=r.getObject("id",UUID.class);
        return new MedicalDocument(r.getObject("owner_id",UUID.class),r.getString("storage_key"),new View(id,r.getString("original_filename"),
            Category.valueOf(r.getString("document_type")),r.getObject("document_date",LocalDate.class),r.getString("provider_name"),r.getString("mime_type"),
            r.getLong("byte_size"),r.getString("sha256"),State.valueOf(r.getString("status")),r.getTimestamp("uploaded_at").toInstant(),
            r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant(),r.getLong("version"),r.getInt("page_count"),
            jdbc.queryForList("SELECT tag FROM vault_document_tag WHERE document_id=? ORDER BY tag",String.class,id)));
    }
    public Optional<MedicalDocument> findByIdAndOwnerId(UUID id,UUID owner) {
        return jdbc.query("SELECT * FROM medical_document WHERE id=? AND owner_id=?",this::map,id,owner).stream().findFirst();
    }
    public Optional<MedicalDocument> lockByIdAndOwnerId(UUID id,UUID owner) {
        return jdbc.query("SELECT * FROM medical_document WHERE id=? AND owner_id=? FOR UPDATE",this::map,id,owner).stream().findFirst();
    }
    public void insert(UUID id,UUID owner,String key,FileValidation.Validated file,String hash,Metadata data) {
        jdbc.update("INSERT INTO medical_document(id,owner_id,original_filename,storage_key,document_type,document_date,provider_name,mime_type,byte_size,sha256,page_count,date_precision) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
            id,owner,file.filename(),key,data.documentType().name(),data.documentDate(),data.providerName(),file.mime(),file.bytes().length,hash,file.pages(),data.documentDate()==null?"UNKNOWN":"DAY");
        tags(id,data.tags());
    }
    private void tags(UUID id,List<String> tags) {
        jdbc.update("DELETE FROM vault_document_tag WHERE document_id=?",id);
        for(String tag:new TreeSet<>(tags.stream().map(String::strip).toList())) jdbc.update("INSERT INTO vault_document_tag(document_id,tag) VALUES(?,?)",id,tag.strip());
    }
    public boolean update(UUID id,UUID owner,Update update) {
        Metadata m=update.metadata();
        int n=jdbc.update("UPDATE medical_document SET document_type=?,document_date=?,provider_name=?,date_precision=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND owner_id=? AND version=?",
            m.documentType().name(),m.documentDate(),m.providerName(),m.documentDate()==null?"UNKNOWN":"DAY",id,owner,update.version());
        if(n==1) tags(id,m.tags()); return n==1;
    }
    public int delete(UUID id,UUID owner) { return jdbc.update("DELETE FROM medical_document WHERE id=? AND owner_id=?",id,owner); }
    public Page search(UUID owner,Filters f) {
        StringBuilder where=new StringBuilder(" WHERE owner_id=?"); List<Object> args=new ArrayList<>();args.add(owner);
        if(f.type()!=null) { where.append(" AND document_type=?");args.add(f.type().name()); }
        if(f.status()!=null) { where.append(" AND status=?");args.add(f.status().name()); }
        like(where,args,"original_filename",f.filename());like(where,args,"provider_name",f.provider());
        if(f.from()!=null) { where.append(" AND document_date>=?");args.add(f.from()); }
        if(f.to()!=null) { where.append(" AND document_date<=?");args.add(f.to()); }
        if(f.uploadedFrom()!=null) { where.append(" AND uploaded_at>=?");args.add(f.uploadedFrom().atStartOfDay().atOffset(ZoneOffset.UTC)); }
        if(f.uploadedTo()!=null) { where.append(" AND uploaded_at<?");args.add(f.uploadedTo().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)); }
        long total=jdbc.queryForObject("SELECT count(*) FROM medical_document"+where,Long.class,args.toArray());
        args.add(f.size());args.add((long)f.page()*f.size());
        var items=jdbc.query("SELECT * FROM medical_document"+where+" ORDER BY uploaded_at DESC,id DESC LIMIT ? OFFSET ?",this::map,args.toArray());
        return new Page(items.stream().map(MedicalDocument::view).toList(),total,f.page(),f.size());
    }
    private void like(StringBuilder sql,List<Object> args,String column,String text) {
        if(text!=null && !text.isBlank()) {
            sql.append(" AND lower(").append(column).append(") LIKE ? ESCAPE '!'");
            args.add("%"+text.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");
        }
    }
}
