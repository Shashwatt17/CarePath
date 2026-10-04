package com.carepath.identity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="app_user")
public class UserAccount {
    @Id private UUID id;
    @Column(nullable=false, unique=true, length=254) private String email;
    @Column(name="password_hash", nullable=false) private String passwordHash;
    @Column(name="display_name", nullable=false, length=120) private String displayName;
    @Column(nullable=false, length=20) private String status;
    @Column(name="last_login_at") private Instant lastLoginAt;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    @Version private long version;
    protected UserAccount() {}
    public UserAccount(UUID id, String email, String hash, String name, Instant now) {
        this.id=id; this.email=email; passwordHash=hash; displayName=name; status="ACTIVE"; createdAt=now; updatedAt=now;
    }
    public UUID id() { return id; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public String displayName() { return displayName; }
    public boolean active() { return "ACTIVE".equals(status); }
    public void loggedIn(Instant now) { lastLoginAt=now; updatedAt=now; }
}
