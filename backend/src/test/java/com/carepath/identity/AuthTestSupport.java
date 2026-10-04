package com.carepath.identity;

import com.carepath.security.RateLimitStore;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthTestSupport.Support.class)
public abstract class AuthTestSupport {
    private static final Path[] KEYS=keys();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("carepath.processing.temp-root",()->KEYS[0].getParent().resolve("processing").toString());
        r.add("carepath.vault.root",()->KEYS[0].getParent().resolve("vault").toString());
        r.add("carepath.auth.private-key-path",()->KEYS[0].toString());
        r.add("carepath.auth.public-key-path",()->KEYS[1].toString());
        r.add("carepath.auth.rate-key",()->"test-only-"+UUID.randomUUID()+UUID.randomUUID());
    }
    private static Path[] keys() {
        try {
            KeyPairGenerator gen=KeyPairGenerator.getInstance("RSA");gen.initialize(2048);KeyPair key=gen.generateKeyPair();
            Path dir=Files.createTempDirectory("carepath-test-keys");
            Path a=dir.resolve("private.pem"), b=dir.resolve("public.pem");
            Files.writeString(a,"-----BEGIN PRIVATE KEY-----\n"+Base64.getMimeEncoder().encodeToString(key.getPrivate().getEncoded())+"\n-----END PRIVATE KEY-----");
            Files.writeString(b,"-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder().encodeToString(key.getPublic().getEncoded())+"\n-----END PUBLIC KEY-----");
            a.toFile().deleteOnExit();b.toFile().deleteOnExit();dir.toFile().deleteOnExit();return new Path[]{a,b};
        } catch(Exception e) { throw new IllegalStateException(e); }
    }
    public static class TestClock extends Clock {
        private Instant value=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        public void reset() { value=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS); }
        public void advance(long seconds) { value=value.plusSeconds(seconds); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return value; }
    }
    public static class TestLimits implements RateLimitStore {
        record Bucket(int count, Instant until) {}
        private final Map<String,Bucket> counts=new ConcurrentHashMap<>();
        private final TestClock clock;
        public boolean unavailable=false;
        public TestLimits(TestClock clock) { this.clock=clock; }
        public void clear() { counts.clear();unavailable=false; }
        @Override public synchronized boolean allow(String key,int limit,int window) {
            if(unavailable) throw new IllegalStateException("simulated store outage");
            Bucket b=counts.get(key);
            if(b==null || !b.until().isAfter(clock.instant())) b=new Bucket(0,clock.instant().plusSeconds(window));
            b=new Bucket(b.count()+1,b.until());counts.put(key,b);return b.count()<=limit;
        }
    }
    @TestConfiguration public static class Support {
        @Bean @Primary TestClock testClock() { return new TestClock(); }
        @Bean @Primary TestLimits testLimits(TestClock clock) { return new TestLimits(clock); }
    }
}
