package com.carepath.identity;
import java.time.Clock;
import java.util.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
@Component
public class SessionAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final SessionStore sessions; private final Clock clock;
    public SessionAuthenticationConverter(SessionStore sessions, Clock clock) { this.sessions=sessions;this.clock=clock; }
    @Override public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID owner=UUID.fromString(jwt.getSubject()), session=UUID.fromString(jwt.getClaimAsString("sid"));
        try {
            if (!sessions.active(owner,session,clock.instant())) throw new OAuth2AuthenticationException("invalid_token");
        } catch (org.springframework.dao.DataAccessException e) {
            throw new OAuth2AuthenticationException("invalid_token"); // Store outage must never authenticate.
        }
        return UsernamePasswordAuthenticationToken.authenticated(new CarePrincipal(owner,session), null, List.of());
    }
}
