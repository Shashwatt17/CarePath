package com.carepath.visitpack;

import static com.carepath.visitpack.PackDtos.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.carepath.longitudinal.HistoryDtos.Page;
import com.carepath.longitudinal.HistoryService;

@Repository
public class PackRepository {
    private final JdbcTemplate db;
    public PackRepository(JdbcTemplate db) {this.db=db;}
    static Instant instant(ResultSet r,String name)throws SQLException {var x=r.getObject(name,OffsetDateTime.class);return x==null?null:x.toInstant();}
    public Pack get(UUID id,UUID owner) {
        var p=db.query("SELECT * FROM visit_pack WHERE id=? AND owner_id=?",(r,n)->new Pack(id,r.getString("title"),r.getString("reason_for_visit"),r.getObject("appointment_id",UUID.class),r.getObject("pack_date",LocalDate.class),r.getString("status"),r.getInt("revision"),r.getLong("version"),instant(r,"created_at"),instant(r,"updated_at"),instant(r,"generated_at"),r.getObject("previous_pack_id",UUID.class),List.of(),null),id,owner).stream().findFirst().orElseThrow(HistoryService::missing);
        return new Pack(p.id(),p.title(),p.reasonForVisit(),p.appointmentId(),p.packDate(),p.status(),p.revision(),p.version(),p.createdAt(),p.updatedAt(),p.generatedAt(),p.previousPackId(),selections(id,owner),p.status().equals("GENERATED")?snapshot(p,owner):null);
    }
    public void lock(UUID id,UUID owner) {
        if(db.query("SELECT id FROM visit_pack WHERE id=? AND owner_id=? FOR UPDATE",(r,n)->r.getObject(1),id,owner).isEmpty())throw HistoryService.missing();
    }
    public boolean appointmentSelected(UUID id,UUID owner) {return Boolean.TRUE.equals(db.queryForObject("SELECT appointment_selected FROM visit_pack WHERE id=? AND owner_id=?",Boolean.class,id,owner));}
    public Page<Summary> list(UUID owner,int page) {
        return new Page<>(db.query("SELECT * FROM visit_pack WHERE owner_id=? ORDER BY created_at DESC,id LIMIT 20 OFFSET ?",(r,n)->new Summary(r.getObject("id",UUID.class),r.getString("title"),r.getString("status"),r.getInt("revision"),r.getLong("version"),instant(r,"created_at"),instant(r,"generated_at")),owner,page*20L),db.queryForObject("SELECT count(*) FROM visit_pack WHERE owner_id=?",Long.class,owner),page,20);
    }
    public void insert(UUID id,UUID owner,Input x,int revision,UUID previous) {
        db.update("INSERT INTO visit_pack(id,owner_id,title,reason_for_visit,appointment_id,appointment_selected,pack_date,revision,previous_pack_id) VALUES(?,?,?,?,?,?,?,?,?)",id,owner,x.title().strip(),x.reasonForVisit().strip(),x.appointmentId(),x.appointmentId()!=null,x.packDate(),revision,previous);
        selections(id,owner,x.items());
    }
    public void update(UUID id,UUID owner,Input x) {
        db.update("UPDATE visit_pack SET title=?,reason_for_visit=?,appointment_id=?,appointment_selected=?,pack_date=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND owner_id=?",x.title().strip(),x.reasonForVisit().strip(),x.appointmentId(),x.appointmentId()!=null,x.packDate(),id,owner);
        selections(id,owner,x.items());
    }
    public UUID child(UUID id,UUID owner) {return db.query("SELECT id FROM visit_pack WHERE previous_pack_id=? AND owner_id=?",(r,n)->r.getObject(1,UUID.class),id,owner).stream().findFirst().orElse(null);}
    private static String column(Type type) {
        return switch(type) {case SYMPTOM->"symptom_id";case OBSERVATION,CHANGE->"observation_id";case DOCUMENT->"document_id";case APPOINTMENT->"appointment_id";case FOLLOW_UP->"follow_up_id";case SAVED_QUESTION->"question_id";case MANUAL_QUESTION->null;};
    }
    private List<Selection> selections(UUID id,UUID owner) {
        return db.query("SELECT * FROM visit_pack_item WHERE visit_pack_id=? AND owner_id=? ORDER BY position",(r,n)->{
            Type t=Type.valueOf(r.getString("item_type"));String col=column(t);
            return new Selection(t,col==null?null:r.getObject(col,UUID.class),r.getObject("other_observation_id",UUID.class),r.getBoolean("include_notes"),r.getString("question_override"));
        },id,owner);
    }
    private void selections(UUID id,UUID owner,List<Selection> items) {
        db.update("DELETE FROM visit_pack_item WHERE visit_pack_id=? AND owner_id=?",id,owner);
        for(int i=0;i<items.size();i++) {
            var x=items.get(i);String col=column(x.type());
            List<Object> values=new ArrayList<>(Arrays.asList(UUID.randomUUID(),owner,id,i,x.type().name(),x.otherId(),x.includeNotes(),x.questionText()));
            if(col!=null)values.add(x.sourceId());
            db.update("INSERT INTO visit_pack_item(id,owner_id,visit_pack_id,position,item_type,other_observation_id,include_notes,question_override"+(col==null?"":","+col)+") VALUES(?,?,?,?,?,?,?,?"+(col==null?"":",?")+")",values.toArray());
        }
    }
    public void freeze(UUID id,UUID owner,Content content,Instant at) {
        for(int i=0;i<content.items().size();i++) {
            var item=content.items().get(i);UUID itemId=UUID.randomUUID();
            db.update("INSERT INTO visit_pack_snapshot_item(id,owner_id,visit_pack_id,position,item_type,heading) VALUES(?,?,?,?,?,?)",itemId,owner,id,i,item.type().name(),item.heading());
            for(int j=0;j<item.fields().size();j++) {var f=item.fields().get(j);db.update("INSERT INTO visit_pack_snapshot_field(item_id,owner_id,position,label,field_value) VALUES(?,?,?,?,?)",itemId,owner,j,f.label(),f.value());}
            for(int j=0;j<item.evidence().size();j++) {var e=item.evidence().get(j);db.update("INSERT INTO visit_pack_snapshot_evidence(item_id,owner_id,position,document_id,observation_id,filename,page_number,snippet) VALUES(?,?,?,?,?,?,?,?)",itemId,owner,j,e.documentId(),e.observationId(),e.filename(),e.page(),e.snippet());}
        }
        db.update("UPDATE visit_pack SET status='GENERATED',generated_at=?,renderer_version='PDFBOX_V1',version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND owner_id=?",at.atOffset(ZoneOffset.UTC),id,owner);
    }
    private Content snapshot(Pack p,UUID owner) {
        // Three batched queries; no per-snapshot-item read loop over the database.
        Map<UUID,List<Field>> fields=new HashMap<>();Map<UUID,List<Evidence>> evidence=new HashMap<>();
        db.query("SELECT f.* FROM visit_pack_snapshot_field f JOIN visit_pack_snapshot_item i ON i.id=f.item_id AND i.owner_id=f.owner_id WHERE i.visit_pack_id=? AND i.owner_id=? ORDER BY f.position",r->{fields.computeIfAbsent(r.getObject("item_id",UUID.class),k->new ArrayList<>()).add(new Field(r.getString("label"),r.getString("field_value")));},p.id(),owner);
        db.query("SELECT e.* FROM visit_pack_snapshot_evidence e JOIN visit_pack_snapshot_item i ON i.id=e.item_id AND i.owner_id=e.owner_id WHERE i.visit_pack_id=? AND i.owner_id=? ORDER BY e.position",r->{evidence.computeIfAbsent(r.getObject("item_id",UUID.class),k->new ArrayList<>()).add(new Evidence(r.getObject("document_id",UUID.class),r.getObject("observation_id",UUID.class),r.getString("filename"),r.getInt("page_number"),r.getString("snippet")));},p.id(),owner);
        var items=db.query("SELECT * FROM visit_pack_snapshot_item WHERE visit_pack_id=? AND owner_id=? ORDER BY position",(r,n)->new Item(Type.valueOf(r.getString("item_type")),r.getString("heading"),List.copyOf(fields.getOrDefault(r.getObject("id",UUID.class),List.of())),List.copyOf(evidence.getOrDefault(r.getObject("id",UUID.class),List.of()))),p.id(),owner);
        return new Content(p.title(),p.reasonForVisit(),p.packDate(),p.revision(),items);
    }
    public void delete(UUID id,UUID owner) {if(db.update("DELETE FROM visit_pack WHERE id=? AND owner_id=?",id,owner)!=1)throw HistoryService.missing();}
}
