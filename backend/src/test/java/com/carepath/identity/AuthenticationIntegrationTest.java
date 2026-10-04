package com.carepath.identity;

import com.carepath.security.Ownership;
import com.carepath.foundation.ApiFailure;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.Cookie;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.net.http.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthenticationIntegrationTest extends AuthTestSupport {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords; @Autowired TestClock clock; @Autowired TestLimits limits;
    @Autowired Ownership ownership;
    @LocalServerPort int port;
    private static final String EMAIL="synthetic@example.invalid", PASSWORD="A long synthetic password 123!";
    @BeforeEach void reset() {
        clock.reset();limits.clear();
        jdbc.update("DELETE FROM audit_event");jdbc.update("DELETE FROM refresh_token");jdbc.update("DELETE FROM auth_session");jdbc.update("DELETE FROM app_user");
    }
    MockHttpServletRequestBuilder postAuth(String path) {
        return post("/api/v1/auth/"+path).header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json");
    }
    String body(String email,String password) throws Exception { return json.writeValueAsString(Map.of("email",email,"password",password)); }
    void register() throws Exception {
        mvc.perform(postAuth("register").content(json.writeValueAsString(Map.of("email",EMAIL,"password",PASSWORD,"displayName","Synthetic User")))).andExpect(status().isCreated());
    }
    record Login(String access, Cookie refresh) {}
    Login login() throws Exception {
        var r=mvc.perform(postAuth("login").content(body(EMAIL,PASSWORD))).andExpect(status().isOk()).andReturn();
        return new Login(json.readTree(r.getResponse().getContentAsString()).get("accessToken").asText(),r.getResponse().getCookie(AuthController.COOKIE));
    }
    @Test void registrationHashDuplicateAndAudit() throws Exception {
        register();String hash=jdbc.queryForObject("SELECT password_hash FROM app_user",String.class);
        assertNotEquals(PASSWORD,hash);assertTrue(passwords.matches(PASSWORD,hash));assertTrue(hash.startsWith("$2a$10$"));
        mvc.perform(postAuth("register").content(json.writeValueAsString(Map.of("email",EMAIL.toUpperCase(),"password",PASSWORD,"displayName","Someone"))))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REGISTRATION_UNAVAILABLE"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM app_user",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='USER_REGISTERED'",Integer.class));
    }
    @Test void concurrentDuplicateRegistrationCreatesExactlyOneAccount() throws Exception {
        String request=json.writeValueAsString(Map.of("email",EMAIL,"password",PASSWORD,"displayName","Synthetic"));
        try(var pool=Executors.newFixedThreadPool(2)) {
            var gate=new CountDownLatch(1);
            Callable<Integer> action=()->{gate.await();return mvc.perform(postAuth("register").content(request)).andReturn().getResponse().getStatus();};
            var a=pool.submit(action);var b=pool.submit(action);gate.countDown();
            var results=new ArrayList<>(List.of(a.get(),b.get()));Collections.sort(results);assertEquals(List.of(201,409),results);
        }
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM app_user",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='USER_REGISTERED'",Integer.class));
    }
    @Test void loginProtectedIdentityAndHashedRefresh() throws Exception {
        register();var result=login();
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+result.access())).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(EMAIL));
        assertTrue(result.refresh().isHttpOnly());assertEquals("/api/v1/auth",result.refresh().getPath());
        assertEquals(Tokens.hash(result.refresh().getValue()),jdbc.queryForObject("SELECT token_hash FROM refresh_token",String.class));
        String audit=jdbc.queryForList("SELECT * FROM audit_event").toString();
        assertFalse(audit.contains(result.refresh().getValue()));assertFalse(audit.contains(PASSWORD));assertFalse(audit.contains(result.access()));
        mvc.perform(get("/api/v1/observations").header("Authorization","Bearer "+result.access())).andExpect(status().isForbidden());
        mvc.perform(post("/logout").header("Authorization","Bearer "+result.access())).andExpect(status().isForbidden());
    }
    @Test void invalidCredentialsAreGenericAndAuditedIncludingUnknownAccounts() throws Exception {
        register();
        var one=mvc.perform(postAuth("login").content(body(EMAIL,"wrong-password"))).andExpect(status().isUnauthorized()).andReturn();
        var two=mvc.perform(postAuth("login").content(body("unknown@example.invalid","wrong-password"))).andExpect(status().isUnauthorized()).andReturn();
        assertEquals(json.readTree(one.getResponse().getContentAsString()).get("message"),json.readTree(two.getResponse().getContentAsString()).get("message"));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='LOGIN_FAILED'",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='LOGIN_FAILED' AND owner_id IS NULL",Integer.class));
    }
    @Test void expiredAndTamperedAccessRejected() throws Exception {
        register();var result=login();clock.advance(61);
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+result.access())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+result.access()+"x")).andExpect(status().isUnauthorized());
    }
    @Test void rotationReuseCommitsFamilyRevocation() throws Exception {
        register();var first=login();
        var result=mvc.perform(postAuth("refresh").cookie(first.refresh())).andExpect(status().isOk()).andReturn();
        Cookie next=result.getResponse().getCookie(AuthController.COOKIE);
        assertNotEquals(first.refresh().getValue(),next.getValue());
        mvc.perform(postAuth("refresh").cookie(first.refresh())).andExpect(status().isUnauthorized());
        mvc.perform(postAuth("refresh").cookie(next)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+first.access())).andExpect(status().isUnauthorized());
        assertNotNull(jdbc.queryForObject("SELECT revoked_at FROM auth_session",java.sql.Timestamp.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='TOKEN_REFRESHED'",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='TOKEN_REUSE_DETECTED'",Integer.class));
    }
    @Test void concurrentRefreshCannotCreateTwoChildren() throws Exception {
        register();var first=login();
        try(var pool=Executors.newFixedThreadPool(2)) {
            var gate=new CountDownLatch(1);
            Callable<Integer> request=()->{gate.await();return mvc.perform(postAuth("refresh").cookie(first.refresh())).andReturn().getResponse().getStatus();};
            var a=pool.submit(request);var b=pool.submit(request);gate.countDown();
            var results=new ArrayList<>(List.of(a.get(),b.get()));Collections.sort(results);assertEquals(List.of(200,401),results);
        }
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM refresh_token",Integer.class));
        assertNotNull(jdbc.queryForObject("SELECT revoked_at FROM auth_session",java.sql.Timestamp.class));
    }
    @Test void invalidExpiredAndDisabledRefreshRejected() throws Exception {
        mvc.perform(postAuth("refresh").cookie(new Cookie(AuthController.COOKIE,"invalid"))).andExpect(status().isUnauthorized());
        register();var result=login();clock.advance(3601);
        mvc.perform(postAuth("refresh").cookie(result.refresh())).andExpect(status().isUnauthorized());
        clock.reset();limits.clear();var another=login();jdbc.update("UPDATE app_user SET status='DISABLED'");
        mvc.perform(postAuth("refresh").cookie(another.refresh())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+another.access())).andExpect(status().isUnauthorized());
        mvc.perform(postAuth("login").content(body(EMAIL,PASSWORD))).andExpect(status().isUnauthorized());
    }
    @Test void logoutRevokesAccessAndRefreshAndClearsCookie() throws Exception {
        register();var result=login();
        mvc.perform(postAuth("logout").cookie(result.refresh())).andExpect(status().isNoContent())
            .andExpect(cookie().maxAge(AuthController.COOKIE,0));
        mvc.perform(postAuth("refresh").cookie(result.refresh())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+result.access())).andExpect(status().isUnauthorized());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='LOGOUT'",Integer.class));
        mvc.perform(postAuth("logout")).andExpect(status().isNoContent());
    }
    @Test void malformedValidationAndMassAssignmentAreRejected() throws Exception {
        mvc.perform(postAuth("register").content("{}" )).andExpect(status().isBadRequest());
        mvc.perform(postAuth("login").content("{" )).andExpect(status().isBadRequest());
        mvc.perform(postAuth("register").content(json.writeValueAsString(Map.of("email",EMAIL,"password",PASSWORD,"displayName","User","id",UUID.randomUUID())))).andExpect(status().isBadRequest());
        mvc.perform(postAuth("register").content(json.writeValueAsString(Map.of("email",EMAIL,"password","é".repeat(40),"displayName","User")))).andExpect(status().isBadRequest());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM app_user",Integer.class));
    }
    @Test void originCsrfAndCookiesCannotAuthenticateNormalApi() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(body(EMAIL,PASSWORD))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/refresh").header("X-CarePath-Client","web").header("Origin","https://evil.example")).andExpect(status().isForbidden());
        register();var session=login();mvc.perform(get("/api/v1/auth/me").cookie(session.refresh())).andExpect(status().isUnauthorized());
        mvc.perform(options("/api/v1/auth/refresh").header("Origin","https://evil.example").header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden());
    }
    @Test void accountRateLimitExpiresAndOutageFailsClosed() throws Exception {
        for(int i=0;i<4;i++) mvc.perform(postAuth("login").content(body(EMAIL,"wrong"))).andExpect(status().isUnauthorized());
        mvc.perform(postAuth("login").content(body(EMAIL,"wrong"))).andExpect(status().isTooManyRequests());
        clock.advance(61);mvc.perform(postAuth("login").content(body(EMAIL,"wrong"))).andExpect(status().isUnauthorized());
        limits.unavailable=true;mvc.perform(postAuth("refresh")).andExpect(status().isServiceUnavailable());
    }
    @Test void ipRateLimitProtectsRefresh() throws Exception {
        for(int i=0;i<100;i++) mvc.perform(postAuth("refresh")).andExpect(status().isUnauthorized());
        mvc.perform(postAuth("refresh")).andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
    }
    @Test void ownerComesFromValidatedPrincipalNotCallerInput() {
        UUID a=UUID.randomUUID(),b=UUID.randomUUID(),record=UUID.randomUUID();
        try {
            SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(new CarePrincipal(a,UUID.randomUUID()),null,List.of()));
            assertEquals(a,ownership.currentOwnerId());
            ApiFailure error=assertThrows(ApiFailure.class,()->ownership.require(record,(id,owner)->owner.equals(b)?Optional.of("private B"):Optional.empty()));
            assertEquals(404,error.status());
        } finally { SecurityContextHolder.clearContext(); }
        assertThrows(ApiFailure.class,()->ownership.currentOwnerId());
    }
    @Autowired java.security.KeyPair keys;
    @Test void signedJwtWithWrongIssuerAudienceOrMissingExpiryIsRejected() throws Exception {
        register();var login=login();
        UUID owner=jdbc.queryForObject("SELECT id FROM app_user",UUID.class);
        UUID session=jdbc.queryForObject("SELECT id FROM auth_session",UUID.class);
        for(int i=0;i<3;i++) {
            var builder=new com.nimbusds.jwt.JWTClaimsSet.Builder().subject(owner.toString()).claim("sid",session.toString())
                .issuer(i==0?"https://wrong.example":"https://carepath.test")
                .audience(i==1?"wrong-api":"carepath-api").issueTime(Date.from(clock.instant())).notBeforeTime(Date.from(clock.instant()));
            if(i!=2) builder.expirationTime(Date.from(clock.instant().plusSeconds(60)));
            var token=new com.nimbusds.jwt.SignedJWT(new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.RS256),builder.build());
            token.sign(new com.nimbusds.jose.crypto.RSASSASigner(keys.getPrivate()));
            mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+token.serialize())).andExpect(status().isUnauthorized());
        }
        var unsigned=new com.nimbusds.jwt.PlainJWT(new com.nimbusds.jwt.JWTClaimsSet.Builder().subject(owner.toString()).build());
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+unsigned.serialize())).andExpect(status().isUnauthorized());
    }
    @Test void frontendUserIdCannotChangeAuthenticatedIdentity() throws Exception {
        register();var login=login();
        mvc.perform(get("/api/v1/auth/me").param("userId",UUID.randomUUID().toString()).header("Authorization","Bearer "+login.access()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(EMAIL));
    }
    @Test void customHeaderRequiredAndOversizedAuthBodiesRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh").header("Origin","http://localhost:3000")).andExpect(status().isForbidden());
        mvc.perform(postAuth("register").content("x".repeat(8193))).andExpect(status().isPayloadTooLarge());
    }
    @Test void realHttpRegisterLoginRefreshLogoutFlow() throws Exception {
        var client=HttpClient.newHttpClient();String root="http://127.0.0.1:"+port+"/api/v1/auth/";
        java.util.function.BiFunction<String,String,HttpRequest.Builder> request=(path,body)->HttpRequest.newBuilder(URI.create(root+path)).header("Origin","http://localhost:3000")
            .header("X-CarePath-Client","web").header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        var registration=client.send(request.apply("register",json.writeValueAsString(Map.of("email",EMAIL,"password",PASSWORD,"displayName","Synthetic HTTP"))).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(201,registration.statusCode());
        var login=client.send(request.apply("login",body(EMAIL,PASSWORD)).build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,login.statusCode());
        String access=json.readTree(login.body()).get("accessToken").asText();String cookie=login.headers().firstValue("set-cookie").orElseThrow().split(";",2)[0];
        assertEquals(200,client.send(HttpRequest.newBuilder(URI.create(root+"me")).header("Authorization","Bearer "+access).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
        var refresh=client.send(request.apply("refresh","").header("Cookie",cookie).build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,refresh.statusCode());
        String rotated=refresh.headers().firstValue("set-cookie").orElseThrow().split(";",2)[0];
        assertEquals(204,client.send(request.apply("logout","").header("Cookie",rotated).build(),HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals(401,client.send(request.apply("refresh","").header("Cookie",rotated).build(),HttpResponse.BodyHandlers.ofString()).statusCode());
    }
}
