package com.carepath.identity;

import java.nio.file.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.time.Clock;
import java.util.Base64;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class IdentityConfiguration {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean PasswordEncoder passwordEncoder(AuthProperties p) { return new BCryptPasswordEncoder(p.bcryptCost()); }
    @Bean KeyPair jwtKeys(AuthProperties p) throws Exception {
        var origin = java.net.URI.create(p.frontendOrigin());
        boolean loopback = "localhost".equals(origin.getHost()) || "127.0.0.1".equals(origin.getHost());
        if (origin.getHost() == null || origin.getRawQuery() != null || origin.getRawFragment() != null
            || origin.getUserInfo() != null || (origin.getPath() != null && !origin.getPath().isEmpty())
            || !("https".equals(origin.getScheme()) || ("http".equals(origin.getScheme()) && loopback))) {
            throw new IllegalStateException("Configure a single HTTPS origin (HTTP is allowed only for loopback development)");
        }
        if (!p.cookieSecure() && !loopback) throw new IllegalStateException("Secure cookies are required outside loopback development");
        KeyFactory factory = KeyFactory.getInstance("RSA");
        var privateKey = (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(pem(p.privateKeyPath(), "PRIVATE KEY")));
        var publicKey = (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(pem(p.publicKeyPath(), "PUBLIC KEY")));
        if (publicKey.getModulus().bitLength() < 2048 || !publicKey.getModulus().equals(privateKey.getModulus()))
            throw new IllegalStateException("JWT keys must be a matching RSA pair of at least 2048 bits");
        return new KeyPair(publicKey, privateKey);
    }
    private byte[] pem(String path, String label) throws Exception {
        return Base64.getMimeDecoder().decode(Files.readString(Path.of(path))
            .replace("-----BEGIN " + label + "-----", "").replace("-----END " + label + "-----", ""));
    }
}
