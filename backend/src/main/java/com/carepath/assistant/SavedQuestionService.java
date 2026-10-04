package com.carepath.assistant;
import static com.carepath.assistant.AssistantDtos.*;
import static com.carepath.audit.AuditService.Action.*;
import java.util.*;
import java.sql.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.carepath.longitudinal.*;
import com.carepath.longitudinal.HistoryDtos.*;
import com.carepath.security.Ownership;
import com.carepath.audit.AuditService;
import com.carepath.foundation.ApiFailure;
@Service
@Transactional
public class SavedQuestionService {
 private final JdbcTemplate jdbc;private final Ownership ownership;private final HistoryRepository history;private final AuditService audit;
 public SavedQuestionService(JdbcTemplate j,Ownership o,HistoryRepository h,AuditService a){jdbc=j;ownership=o;history=h;audit=a;}
 public Page<Saved> list(int page){UUID owner=ownership.currentOwnerId();return new Page<>(jdbc.query("SELECT * FROM saved_question WHERE owner_id=? ORDER BY created_at DESC,id LIMIT 20 OFFSET ?",this::map,owner,page*20L),jdbc.queryForObject("SELECT count(*) FROM saved_question WHERE owner_id=?",Long.class,owner),page,20);}
 public Saved get(UUID id){return jdbc.query("SELECT * FROM saved_question WHERE id=? AND owner_id=?",this::map,id,ownership.currentOwnerId()).stream().findFirst().orElseThrow(HistoryService::missing);}
 private Saved map(ResultSet r,int n)throws SQLException {
  UUID id=r.getObject("id",UUID.class),owner=r.getObject("owner_id",UUID.class);var ids=jdbc.query("SELECT observation_id FROM saved_question_evidence WHERE question_id=? AND owner_id=? ORDER BY observation_id",(rs,i)->rs.getObject(1,UUID.class),id,owner);List<Point> points=new ArrayList<>();for(UUID oid:ids)history.find(oid,owner).ifPresent(points::add);
  return new Saved(id,r.getString("question_text"),r.getString("origin"),r.getObject("created_at",OffsetDateTime.class).toInstant(),r.getObject("updated_at",OffsetDateTime.class).toInstant(),r.getLong("version"),points.size()!=r.getInt("expected_evidence_count"),List.copyOf(points));
 }
 public Saved create(Save input){UUID owner=ownership.currentOwnerId(),id=UUID.randomUUID();var ids=new LinkedHashSet<>(input.observationIds());for(UUID oid:ids)if(history.find(oid,owner).isEmpty())throw HistoryService.missing();
  jdbc.update("INSERT INTO saved_question(id,owner_id,question_text,origin,expected_evidence_count) VALUES(?,?,?,'USER',?)",id,owner,input.text().strip(),ids.size());
  for(UUID oid:ids)jdbc.update("INSERT INTO saved_question_evidence(question_id,owner_id,observation_id) VALUES(?,?,?)",id,owner,oid);
  audit.assistant(owner,SAVED_QUESTION_CREATED,"SUCCESS",id);return get(id);
 }
 public Saved edit(UUID id,Edit input){get(id);if(jdbc.update("UPDATE saved_question SET question_text=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND owner_id=? AND version=?",input.text().strip(),id,ownership.currentOwnerId(),input.version())!=1)throw new ApiFailure(409,"VERSION_CONFLICT","This question changed. Reload before editing.");audit.assistant(ownership.currentOwnerId(),SAVED_QUESTION_UPDATED,"SUCCESS",id);return get(id);}
 public void delete(UUID id){if(jdbc.update("DELETE FROM saved_question WHERE id=? AND owner_id=?",id,ownership.currentOwnerId())!=1)throw HistoryService.missing();audit.assistant(ownership.currentOwnerId(),SAVED_QUESTION_DELETED,"SUCCESS",id);}
}
