package com.carepath.care;
import java.time.*;import java.net.URI;import java.util.*;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.stereotype.Component;import com.carepath.foundation.ApiFailure;import com.carepath.security.Ownership;
@Component
public class CareSupport {
 final JdbcTemplate db; final Ownership ownership; final Clock clock;
 public CareSupport(JdbcTemplate d,Ownership o,Clock c){db=d;ownership=o;clock=c;}
 UUID owner(){return ownership.currentOwnerId();}
 static ApiFailure missing(){return new ApiFailure(404,"NOT_FOUND","The requested item is not available.");}
 static ApiFailure invalid(){return new ApiFailure(400,"VALIDATION_FAILED","Check the dates, timezone and entered details.");}
 static ApiFailure conflict(){return new ApiFailure(409,"VERSION_CONFLICT","This item changed. Reload before editing.");}
 void own(String table,UUID id,UUID owner){if(db.queryForObject("SELECT count(*) FROM "+table+" WHERE id=? AND owner_id=?",Long.class,id,owner)!=1)throw missing();}
 void lock(String table,UUID id,UUID owner){if(db.query("SELECT id FROM "+table+" WHERE id=? AND owner_id=? FOR UPDATE",(r,n)->r.getObject(1),id,owner).isEmpty())throw missing();}
 static ZoneId zone(String s){try{return ZoneId.of(s);}catch(Exception e){throw invalid();}}
 static Instant instant(OffsetDateTime date,String zone){if(date.getYear()<1900||date.getYear()>2200)throw invalid();ZoneId z=zone(zone);if(!z.getRules().getValidOffsets(date.toLocalDateTime()).contains(date.getOffset()))throw invalid();return date.toInstant();}
 static void url(String text){if(text==null||text.isBlank())return;try{URI u=URI.create(text);if(!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw invalid();}catch(IllegalArgumentException e){throw invalid();}}
 static OffsetDateTime utc(Instant i){return i==null?null:i.atOffset(ZoneOffset.UTC);}
 static Instant time(java.sql.ResultSet r,String c)throws java.sql.SQLException{var t=r.getObject(c,OffsetDateTime.class);return t==null?null:t.toInstant();}
}
