package com.carepath.sharing;

import static com.carepath.sharing.ShareDtos.*;
import java.time.*;
import java.util.*;
import java.sql.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carepath.audit.AuditService;
import com.carepath.audit.AuditService.Action;
import com.carepath.security.Ownership;
import com.carepath.visitpack.PackRepository;
import com.carepath.foundation.ApiFailure;
import com.carepath.longitudinal.HistoryDtos.Page;

@Service
@Transactional
public class ShareService {
    private final JdbcTemplate db; private final Ownership ownership; private final PackRepository packs;
    private final Clock clock; private final AuditService audit;
    public ShareService(JdbcTemplate db, Ownership ownership, PackRepository packs, Clock clock, AuditService audit) {
        this.db=db; this.ownership=ownership; this.packs=packs; this.clock=clock; this.audit=audit;
    }
    private static Instant time(ResultSet r,String c)throws SQLException {var v=r.getObject(c,OffsetDateTime.class);return v==null?null:v.toInstant();}
    private Summary summary(ResultSet r,int n)throws SQLException {
        Instant expiry=time(r,"expires_at"),revoked=time(r,"revoked_at");
        return new Summary(r.getObject("id",UUID.class),r.getObject("visit_pack_id",UUID.class),r.getInt("pack_revision"),time(r,"created_at"),expiry,revoked,revoked!=null?"REVOKED":!clock.instant().isBefore(expiry)?"EXPIRED":"ACTIVE",r.getLong("version"));
    }
    static ApiFailure unavailable(){return new ApiFailure(404,"SHARE_UNAVAILABLE","This shared Visit Pack is unavailable.");}
    public Created create(Create input) {
        if(!Set.of(15,30,60,1440).contains(input.expiryMinutes()))throw new ApiFailure(400,"INVALID_EXPIRY","Choose an available expiry.");
        UUID owner=ownership.currentOwnerId();packs.lock(input.packId(),owner);
        var pack=packs.get(input.packId(),owner);
        if(!pack.status().equals("GENERATED")||pack.snapshot()==null)throw new ApiFailure(409,"PACK_NOT_GENERATED","Generate the Visit Pack before sharing.");
        String raw=ShareTokens.create();UUID id=UUID.randomUUID();Instant now=clock.instant(),expiry=now.plusSeconds(input.expiryMinutes()*60L);
        db.update("INSERT INTO share_token(id,owner_id,visit_pack_id,pack_revision,token_hash,created_at,expires_at) VALUES(?,?,?,?,?,?,?)",id,owner,pack.id(),pack.revision(),ShareTokens.digest(raw),now.atOffset(ZoneOffset.UTC),expiry.atOffset(ZoneOffset.UTC));
        audit.care(owner,Action.SHARE_CREATED,"SHARE",id,false);
        return new Created(new Summary(id,pack.id(),pack.revision(),now,expiry,null,"ACTIVE",0),raw);
    }
    public Page<Summary> list(int page) {
        UUID owner=ownership.currentOwnerId();
        return new Page<>(db.query("SELECT * FROM share_token WHERE owner_id=? ORDER BY created_at DESC,id LIMIT 20 OFFSET ?",this::summary,owner,page*20L),db.queryForObject("SELECT count(*) FROM share_token WHERE owner_id=?",Long.class,owner),page,20);
    }
    public Summary revoke(UUID id) {
        UUID owner=ownership.currentOwnerId();
        var prior=db.query("SELECT * FROM share_token WHERE id=? AND owner_id=?",this::summary,id,owner).stream().findFirst().orElseThrow(ShareService::unavailable);
        // All share operations lock pack before token, matching pack deletion's FK lock order.
        packs.lock(prior.packId(),owner);
        var row=db.query("SELECT * FROM share_token WHERE id=? AND owner_id=? FOR UPDATE",this::summary,id,owner).stream().findFirst().orElseThrow(ShareService::unavailable);
        if(row.revokedAt()==null) {
            db.update("UPDATE share_token SET revoked_at=?,version=version+1,updated_at=? WHERE id=? AND owner_id=?",clock.instant().atOffset(ZoneOffset.UTC),clock.instant().atOffset(ZoneOffset.UTC),id,owner);
            audit.care(owner,Action.SHARE_REVOKED,"SHARE",id,false);
        }
        return db.queryForObject("SELECT * FROM share_token WHERE id=? AND owner_id=?",this::summary,id,owner);
    }
    public Snapshot access(String token) {
        if(!ShareTokens.valid(token))throw unavailable();
        String hash=ShareTokens.digest(token);
        var matches=db.query("SELECT id,owner_id,visit_pack_id FROM share_token WHERE token_hash=?",(r,n)->new UUID[]{r.getObject("id",UUID.class),r.getObject("owner_id",UUID.class),r.getObject("visit_pack_id",UUID.class)},hash);
        if(matches.isEmpty())throw unavailable();
        UUID[] key=matches.get(0);
        try {packs.lock(key[2],key[1]);} catch(ApiFailure e){throw unavailable();}
        var row=db.query("SELECT * FROM share_token WHERE id=? AND token_hash=? FOR UPDATE",this::summary,key[0],hash).stream().findFirst().orElseThrow(ShareService::unavailable);
        if(!row.status().equals("ACTIVE"))throw unavailable();
        var pack=packs.get(key[2],key[1]);
        if(!pack.status().equals("GENERATED")||pack.snapshot()==null||pack.revision()!=row.revision())throw unavailable();
        List<Item> items=new ArrayList<>();
        for(int i=0;i<pack.snapshot().items().size();i++) {
            var source=pack.snapshot().items().get(i);List<Citation> citations=new ArrayList<>();
            for(int j=0;j<source.evidence().size();j++){var e=source.evidence().get(j);citations.add(new Citation(e.filename(),e.page(),e.snippet()));}
            items.add(new Item(source.type(),source.heading(),source.fields(),List.copyOf(citations)));
        }
        // Recheck the actual clock after assembly; equality is expired. No cached authorization.
        Instant now=clock.instant();if(!now.isBefore(row.expiresAt()))throw unavailable();
        db.update("UPDATE share_token SET last_accessed_at=?,access_count=access_count+1,version=version+1,updated_at=? WHERE id=?",now.atOffset(ZoneOffset.UTC),now.atOffset(ZoneOffset.UTC),key[0]);
        audit.shareAccess(key[1],key[0]);
        var c=pack.snapshot();return new Snapshot(c.title(),c.reasonForVisit(),c.packDate(),c.revision(),pack.generatedAt(),row.expiresAt(),List.copyOf(items));
    }
}
