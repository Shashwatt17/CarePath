package com.carepath.identity;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
    private final JwtEncoder encoder;
    private final KeyPair keys;
    private final AuthProperties properties;
    private final Clock clock;
    public JwtService(KeyPair keys, AuthProperties properties, Clock clock) {
        this.keys=keys; this.properties=properties; this.clock=clock;
        RSAKey key=new RSAKey.Builder((RSAPublicKey)keys.getPublic()).privateKey((RSAPrivateKey)keys.getPrivate()).keyID("carepath-rsa").build();
        encoder=new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }
    public String issue(UUID user, UUID session) {
        Instant now=clock.instant();
        var claims=JwtClaimsSet.builder().issuer(properties.issuer()).subject(user.toString())
            .audience(List.of(properties.audience())).issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(properties.accessSeconds()))
            .id(UUID.randomUUID().toString()).claim("sid",session.toString()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build(), claims)).getTokenValue();
    }
    @Bean
    JwtDecoder jwtDecoder() {
        var decoder=NimbusJwtDecoder.withPublicKey((RSAPublicKey)keys.getPublic()).signatureAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(jwt -> {
            try {
                Instant now=clock.instant();
                UUID.fromString(jwt.getSubject()); UUID.fromString(jwt.getClaimAsString("sid"));
                boolean valid=properties.issuer().equals(jwt.getIssuer().toString()) && jwt.getAudience().contains(properties.audience())
                    && jwt.getExpiresAt()!=null && jwt.getExpiresAt().isAfter(now)
                    && jwt.getIssuedAt()!=null && !jwt.getIssuedAt().isAfter(now.plusSeconds(5))
                    && jwt.getNotBefore()!=null && !jwt.getNotBefore().isAfter(now)
                    && jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                    && Duration.between(jwt.getIssuedAt(),jwt.getExpiresAt()).getSeconds()<=properties.accessSeconds();
                if (valid) return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException ignored) { /* Untrusted claims fail closed; never log token content. */ }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid access token", null));
        });
        return decoder;
    }
}
