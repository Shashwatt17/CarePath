package com.carepath.identity;

import com.carepath.audit.AuditService;
import com.carepath.audit.AuditService.*;
import com.carepath.foundation.ApiFailure;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserAccounts users; private final SessionStore sessions; private final PasswordEncoder passwords;
    private final JwtService jwt; private final AuditService audit; private final AuthProperties properties; private final Clock clock;
    private final String dummyHash;
    public AuthService(UserAccounts users, SessionStore sessions, PasswordEncoder passwords, JwtService jwt,
            AuditService audit, AuthProperties properties, Clock clock) {
        this.users=users;this.sessions=sessions;this.passwords=passwords;this.jwt=jwt;this.audit=audit;this.properties=properties;this.clock=clock;
        dummyHash=passwords.encode(Tokens.random());
    }
    public static String email(String value) { return value.strip().toLowerCase(Locale.ROOT); }
    public static void passwordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length>72) throw new ApiFailure(400,"VALIDATION_FAILED","Password must fit within 72 UTF-8 bytes.");
    }
    @Transactional
    public void register(AuthDtos.Register request) {
        passwordLength(request.password());
        // Hash even duplicates to avoid the obvious fast-path account existence timing signal.
        String hash=passwords.encode(request.password());
        if (users.findByEmail(email(request.email())).isPresent()) throw new ApiFailure(409,"REGISTRATION_UNAVAILABLE","Registration could not be completed. Try signing in or use different details.");
        UserAccount user=new UserAccount(UUID.randomUUID(),email(request.email()),hash,request.displayName().strip(),clock.instant());
        users.saveAndFlush(user);
        audit.record(user.id(),Action.USER_REGISTERED,"SUCCESS",null,null);
    }
    @Transactional
    public AuthDtos.Outcome login(AuthDtos.Login request) {
        passwordLength(request.password());
        var found=users.findByEmail(email(request.email()));
        boolean matches=passwords.matches(request.password(),found.map(UserAccount::passwordHash).orElse(dummyHash));
        if (!matches || found.isEmpty() || !found.get().active()) {
            audit.record(found.map(UserAccount::id).orElse(null),Action.LOGIN_FAILED,"FAILURE",null,Reason.INVALID_CREDENTIALS);
            return AuthDtos.Outcome.denied();
        }
        UserAccount user=found.get(); Instant now=clock.instant(); UUID session=UUID.randomUUID();
        Instant expires=now.plusSeconds(properties.refreshSeconds());
        sessions.create(session,user.id(),expires,now); user.loggedIn(now);
        var grant=grant(user,session,null,expires,now);
        audit.record(user.id(),Action.LOGIN_SUCCEEDED,"SUCCESS",session,null);
        return AuthDtos.Outcome.success(grant);
    }
    @Transactional
    public AuthDtos.Outcome refresh(String raw) {
        if (!Tokens.validFormat(raw)) return AuthDtos.Outcome.denied();
        String hash=Tokens.hash(raw);
        var first=sessions.find(hash); if (first.isEmpty()) return AuthDtos.Outcome.denied();
        var locked=sessions.lock(first.get().family()); if (locked.isEmpty()) return AuthDtos.Outcome.denied();
        // Re-read after family lock, so simultaneous rotation cannot mint two descendants.
        var token=sessions.find(hash).orElseThrow(); var session=locked.get(); Instant now=clock.instant();
        if (token.used()!=null) {
            sessions.revoke(session.id(),now);
            audit.record(session.owner(),Action.TOKEN_REUSE_DETECTED,"DENIED",session.id(),Reason.REFRESH_REUSE);
            return AuthDtos.Outcome.denied(); // Commit revocation, do not throw inside the transaction.
        }
        var user=users.findById(session.owner());
        if (session.revoked()!=null || token.revoked()!=null || !token.expires().isAfter(now)
            || !session.expires().isAfter(now) || user.isEmpty() || !user.get().active()) return AuthDtos.Outcome.denied();
        sessions.consume(token.id(),now);
        var grant=grant(user.get(),session.id(),token.id(),session.expires(),now);
        audit.record(user.get().id(),Action.TOKEN_REFRESHED,"SUCCESS",session.id(),null);
        return AuthDtos.Outcome.success(grant);
    }
    @Transactional
    public void logout(String raw) {
        if (!Tokens.validFormat(raw)) return;
        var token=sessions.find(Tokens.hash(raw)); if (token.isEmpty()) return;
        var session=sessions.lock(token.get().family());
        if (session.isPresent() && session.get().revoked()==null) {
            sessions.revoke(session.get().id(),clock.instant());
            audit.record(session.get().owner(),Action.LOGOUT,"SUCCESS",session.get().id(),Reason.USER_REQUEST);
        }
    }
    @Transactional(readOnly=true)
    public AuthDtos.User me(CarePrincipal principal) {
        return users.findById(principal.userId()).filter(UserAccount::active).map(AuthDtos.User::from).orElseThrow(ApiFailure::session);
    }
    private AuthDtos.Grant grant(UserAccount user, UUID session, UUID parent, Instant expires, Instant now) {
        String raw=Tokens.random(); sessions.token(UUID.randomUUID(),user.id(),session,parent,Tokens.hash(raw),expires,now);
        return new AuthDtos.Grant(new AuthDtos.Access(jwt.issue(user.id(),session),properties.accessSeconds(),AuthDtos.User.from(user)), raw,Duration.between(now,expires).getSeconds());
    }
}
