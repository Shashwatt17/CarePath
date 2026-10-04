package com.carepath.search;

import static com.carepath.search.SearchController.*;
import com.carepath.foundation.ApiFailure;
import com.carepath.longitudinal.HistoryDtos.Page;
import com.carepath.security.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class SearchService {
    private final JdbcTemplate db;
    private final Ownership ownership;
    private final AuthRateLimiter limits;
    public SearchService(JdbcTemplate db, Ownership ownership, AuthRateLimiter limits) {
        this.db=db; this.ownership=ownership; this.limits=limits;
    }
    private static final String SOURCES="""
        SELECT 'DOCUMENT' AS kind,d.id,d.original_filename AS title,d.document_date AS event_date,
          d.provider_name AS provider,replace(d.document_type,'_',' ') AS summary,d.id AS document_id,
          CAST(NULL AS UUID) AS candidate_id FROM medical_document d WHERE d.owner_id=?
        UNION ALL SELECT 'OBSERVATION',o.id,coalesce(o.canonical_name_snapshot,o.verified_test_name),
          o.observed_date,o.laboratory,coalesce(o.verified_value_text,'') || ' ' || coalesce(o.verified_unit,''),
          o.document_id,o.source_candidate_id FROM trusted_medical_observation o WHERE o.owner_id=?
        UNION ALL SELECT 'SYMPTOM',s.id,s.name,s.start_date,CAST(NULL AS varchar),
          'User-reported symptom',CAST(NULL AS UUID),CAST(NULL AS UUID) FROM symptom s WHERE s.owner_id=?
        UNION ALL SELECT 'APPOINTMENT',a.id,a.provider_name,CAST(a.starts_at AT TIME ZONE 'UTC' AS date),
          a.provider_name,coalesce(a.specialty,'') || ' ' || a.status,CAST(NULL AS UUID),CAST(NULL AS UUID)
          FROM appointment a WHERE a.owner_id=?
        """;
    static String literal(String value) { return "%"+value.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%"; }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Page<Result> search(Query q) {
        if(q.from()!=null && q.to()!=null && q.from().isAfter(q.to())) throw invalid();
        String[] words=q.q().strip().isEmpty()?new String[0]:q.q().strip().toLowerCase(Locale.ROOT).split("\\s+");
        if(words.length>8) throw invalid();
        UUID owner=ownership.currentOwnerId(); limits.search(owner.toString());
        List<Object> args=new ArrayList<>(List.of(owner,owner,owner,owner));
        StringBuilder where=new StringBuilder(" WHERE 1=1");
        if(q.kind()!=Kind.ALL){where.append(" AND kind=?");args.add(q.kind().name());}
        if(q.from()!=null){where.append(" AND event_date>=?");args.add(q.from());}
        if(q.to()!=null){where.append(" AND event_date<=?");args.add(q.to());}
        if(q.provider()!=null&&!q.provider().isBlank()){where.append(" AND lower(provider) LIKE ? ESCAPE '!'");args.add(literal(q.provider().strip()));}
        for(int i=0;i<words.length;i++) {
            String word=words[i]; Integer month=month(word);
            if(month!=null){where.append(" AND EXTRACT(MONTH FROM event_date)=?");args.add(month);}
            else {where.append(" AND (lower(title) LIKE ? ESCAPE '!' OR lower(coalesce(provider,'')) LIKE ? ESCAPE '!' OR lower(summary) LIKE ? ESCAPE '!' OR (kind='DOCUMENT' AND EXISTS(SELECT 1 FROM vault_document_tag t WHERE t.document_id=results.id AND lower(t.tag) LIKE ? ESCAPE '!')))");for(int j=0;j<4;j++)args.add(literal(word));}
        }
        String source=" FROM ("+SOURCES+") results"+where;
        long total=db.queryForObject("SELECT count(*)"+source,Long.class,args.toArray());
        args.add(q.page()*20L);
        var items=db.query("SELECT *"+source+" ORDER BY event_date DESC NULLS LAST,kind,id LIMIT 20 OFFSET ?",(r,n)->
            new Result(Kind.valueOf(r.getString("kind")),r.getObject("id",UUID.class),r.getString("title"),r.getObject("event_date",LocalDate.class),r.getString("provider"),r.getString("summary"),r.getObject("document_id",UUID.class),r.getObject("candidate_id",UUID.class)),args.toArray());
        return new Page<>(items,total,q.page(),20);
    }
    private static Integer month(String word) {
        for(int i=1;i<=12;i++) { var m=Month.of(i); if(m.getDisplayName(TextStyle.FULL,Locale.ENGLISH).equalsIgnoreCase(word)||m.getDisplayName(TextStyle.SHORT,Locale.ENGLISH).equalsIgnoreCase(word))return i; }
        return null;
    }
    private static ApiFailure invalid(){return new ApiFailure(400,"VALIDATION_FAILED","Check the search terms and date range.");}
}
