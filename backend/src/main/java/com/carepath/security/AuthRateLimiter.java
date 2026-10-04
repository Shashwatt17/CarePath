package com.carepath.security;
import com.carepath.foundation.ApiFailure;
import com.carepath.identity.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
@Service
public class AuthRateLimiter {
    private final RateLimitStore store; private final AuthProperties properties;
    public AuthRateLimiter(RateLimitStore store,AuthProperties properties) { this.store=store;this.properties=properties; }
    public void ip(String value) { check("ip",value,properties.ipLimit()); }
    public void search(String value) { check("search",value,60); }
    public void nearby(String value) { check("nearby",value,10); }
    public void share(String value) { check("share",value,60); }
    public void account(String value) { check("account",value,properties.accountLimit()); }
    private void check(String kind,String value,int limit) {
        boolean allowed;
        try {
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.rateKey().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            String key="carepath:auth:"+kind+":"+HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
            allowed=store.allow(key,limit,properties.rateWindowSeconds());
        } catch(Exception e) { throw new ApiFailure(503,"AUTH_UNAVAILABLE","Authentication is temporarily unavailable. Please try again."); }
        if (!allowed) throw new ApiFailure(429,"RATE_LIMITED","Too many attempts. Please wait and try again.");
    }
}
