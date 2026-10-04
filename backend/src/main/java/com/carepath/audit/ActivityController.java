package com.carepath.audit;

import com.carepath.security.Ownership;
import com.carepath.longitudinal.HistoryDtos.Page;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @Validated @RequestMapping("/api/v1/activity")
public class ActivityController {
    public enum Category { ALL, SECURITY, DOCUMENT, CARE, VISIT_PACK, SHARE }
    public record Event(String action, String outcome, String actor, Instant occurredAt) {}
    private final JdbcTemplate db;
    private final Ownership ownership;
    public ActivityController(JdbcTemplate db, Ownership ownership) { this.db=db; this.ownership=ownership; }
    @GetMapping @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Page<Event> list(@RequestParam(defaultValue="ALL") Category category,
            @RequestParam(defaultValue="0") @Min(0) @Max(10000) int page) {
        String filter=switch(category) {
            case ALL -> "";
            case SECURITY -> " AND resource_type='AUTH_SESSION'";
            case DOCUMENT -> " AND resource_type='MEDICAL_DOCUMENT'";
            case CARE -> " AND resource_type IN ('SYMPTOM','APPOINTMENT','FOLLOW_UP','REMINDER')";
            case VISIT_PACK -> " AND resource_type='VISIT_PACK'";
            case SHARE -> " AND resource_type='SHARE'";
        };
        UUID owner=ownership.currentOwnerId();
        long total=db.queryForObject("SELECT count(*) FROM audit_event WHERE owner_id=?"+filter,Long.class,owner);
        // Deliberately omit resource/session/request IDs and reason/security metadata.
        var events=db.query("SELECT action,outcome,actor_kind,occurred_at FROM audit_event WHERE owner_id=?"+filter+
            " ORDER BY occurred_at DESC,id DESC LIMIT 20 OFFSET ?",(r,n)->new Event(r.getString(1),r.getString(2),r.getString(3),r.getObject(4,OffsetDateTime.class).toInstant()),owner,page*20L);
        return new Page<>(events,total,page,20);
    }
}
