package com.carepath.identity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;import java.net.http.*;import java.time.Duration;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@TestPropertySource(properties={"carepath.auth.cookie-secure=true","carepath.frontend-origin=https://carepath.example","carepath.auth.frontend-origin=https://carepath.example"})
class HttpSecurityIntegrationTest extends AuthTestSupport {
 @LocalServerPort int port;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;@Autowired TestLimits limits;
 HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
 HttpRequest.Builder request(String path){return HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(15));}
 HttpResponse<String> send(HttpRequest.Builder r)throws Exception{return client.send(r.build(),HttpResponse.BodyHandlers.ofString());}
 HttpRequest.Builder post(String path,String body){return request(path).header("Origin","https://carepath.example").header("X-CarePath-Client","web").header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body));}
 @Test void secureCookieRotationLogoutOverActualHttp()throws Exception{
  limits.clear();String email=UUID.randomUUID()+"@example.invalid";String login=json.writeValueAsString(Map.of("email",email,"password","Synthetic password 123!"));
  assertEquals(201,send(post("/api/v1/auth/register",json.writeValueAsString(Map.of("email",email,"password","Synthetic password 123!","displayName","Synthetic")))).statusCode());
  var signed=send(post("/api/v1/auth/login",login));assertEquals(200,signed.statusCode());String set=signed.headers().firstValue("set-cookie").orElseThrow();assertTrue(set.startsWith("__Host-carepath_refresh="));for(String flag:List.of("Secure","HttpOnly","SameSite=Lax","Path=/"))assertTrue(set.contains(flag));assertFalse(set.contains("Domain="));String cookie=set.split(";",2)[0];
  var refreshed=send(post("/api/v1/auth/refresh","{}").header("Cookie",cookie));assertEquals(200,refreshed.statusCode());String rotated=refreshed.headers().firstValue("set-cookie").orElseThrow().split(";",2)[0];assertNotEquals(cookie,rotated);
  String access=json.readTree(refreshed.body()).path("accessToken").asText();assertEquals(200,send(request("/api/v1/auth/me").header("Authorization","Bearer "+access).GET()).statusCode());
  assertEquals(204,send(post("/api/v1/auth/logout","{}").header("Cookie",rotated)).statusCode());assertEquals(401,send(post("/api/v1/auth/refresh","{}").header("Cookie",rotated)).statusCode());assertEquals(401,send(request("/api/v1/auth/me").header("Authorization","Bearer "+access).GET()).statusCode());
 }
 @Test void csrfAndCorsOverActualHttp()throws Exception{
  assertEquals(403,send(request("/api/v1/auth/logout").header("Origin","https://evil.example").header("X-CarePath-Client","web").POST(HttpRequest.BodyPublishers.noBody())).statusCode());
  assertEquals(403,send(request("/api/v1/auth/logout").header("Origin","https://carepath.example").POST(HttpRequest.BodyPublishers.noBody())).statusCode());
  var good=send(request("/api/v1/auth/me").header("Origin","https://carepath.example").header("Access-Control-Request-Method","GET").method("OPTIONS",HttpRequest.BodyPublishers.noBody()));assertEquals(200,good.statusCode());assertEquals("https://carepath.example",good.headers().firstValue("access-control-allow-origin").orElseThrow());
  var bad=send(request("/api/v1/auth/me").header("Origin","https://evil.example").header("Access-Control-Request-Method","GET").method("OPTIONS",HttpRequest.BodyPublishers.noBody()));assertEquals(403,bad.statusCode());assertTrue(bad.headers().firstValue("access-control-allow-origin").isEmpty());
 }
 @Test void publicShareFailureAndProtectedApiHeaders()throws Exception{
  var r=send(post("/api/v1/public/share/access","{\"token\":\"invalid\"}"));assertEquals(404,r.statusCode());assertEquals("no-store",r.headers().firstValue("cache-control").orElseThrow());assertEquals("no-referrer",r.headers().firstValue("referrer-policy").orElseThrow());assertTrue(r.headers().firstValue("x-robots-tag").orElseThrow().contains("noindex"));assertFalse(r.body().contains("Exception"));
  assertEquals(401,send(request("/api/v1/visit-packs").GET()).statusCode());
 }
 @Test void jdbcStatementTimeoutIsConfigured(){assertEquals(20,jdbc.getQueryTimeout());}
}
