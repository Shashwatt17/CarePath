package com.carepath.identity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SessionStore {
    public record Session(UUID id, UUID owner, Instant expires, Instant revoked) {}
    public record Token(UUID id, UUID owner, UUID family, Instant expires, Instant used, Instant revoked) {}
    private final JdbcTemplate jdbc;
    public SessionStore(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        Timestamp ts=rs.getTimestamp(column); return ts == null ? null : ts.toInstant();
    }
    public Optional<Token> find(String hash) {
        return jdbc.query("SELECT * FROM refresh_token WHERE token_hash=?", (rs,n) -> new Token(rs.getObject("id",UUID.class),
            rs.getObject("owner_id",UUID.class),rs.getObject("family_id",UUID.class),instant(rs,"expires_at"),instant(rs,"used_at"),instant(rs,"revoked_at")), hash).stream().findFirst();
    }
    public Optional<Session> lock(UUID id) {
        return jdbc.query("SELECT * FROM auth_session WHERE id=? FOR UPDATE",(rs,n) -> new Session(rs.getObject("id",UUID.class),
            rs.getObject("owner_id",UUID.class),instant(rs,"expires_at"),instant(rs,"revoked_at")), id).stream().findFirst();
    }
    public void create(UUID id, UUID owner, Instant expires, Instant now) {
        jdbc.update("INSERT INTO auth_session(id,owner_id,expires_at,created_at,updated_at) VALUES(?,?,?,?,?)",id,owner,Timestamp.from(expires),Timestamp.from(now),Timestamp.from(now));
    }
    public void token(UUID id, UUID owner, UUID family, UUID parent, String hash, Instant expires, Instant now) {
        jdbc.update("INSERT INTO refresh_token(id,owner_id,family_id,parent_id,token_hash,expires_at,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?)",
            id,owner,family,parent,hash,Timestamp.from(expires),Timestamp.from(now),Timestamp.from(now));
    }
    public void consume(UUID id, Instant now) {
        jdbc.update("UPDATE refresh_token SET used_at=?,updated_at=?,version=version+1 WHERE id=?",Timestamp.from(now),Timestamp.from(now),id);
    }
    public void revoke(UUID family, Instant now) {
        jdbc.update("UPDATE auth_session SET revoked_at=?,updated_at=?,version=version+1 WHERE id=? AND revoked_at IS NULL",Timestamp.from(now),Timestamp.from(now),family);
        jdbc.update("UPDATE refresh_token SET revoked_at=?,updated_at=?,version=version+1 WHERE family_id=? AND revoked_at IS NULL",Timestamp.from(now),Timestamp.from(now),family);
    }
    public boolean active(UUID owner, UUID session, Instant now) {
        Integer count=jdbc.queryForObject("SELECT count(*) FROM auth_session s JOIN app_user u ON u.id=s.owner_id WHERE s.id=? AND s.owner_id=? AND s.revoked_at IS NULL AND s.expires_at>? AND u.status='ACTIVE'",Integer.class,session,owner,Timestamp.from(now));
        return count != null && count == 1;
    }
}
