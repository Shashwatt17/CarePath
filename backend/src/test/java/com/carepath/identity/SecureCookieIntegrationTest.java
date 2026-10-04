package com.carepath.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource(properties="carepath.auth.cookie-secure=true")
class SecureCookieIntegrationTest extends AuthTestSupport {
    @Autowired MockMvc mvc;
    @Test void productionCookieUsesHostPrefixSecureHttpOnlyAndSameSite() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").header("Origin","http://localhost:3000").header("X-CarePath-Client","web"))
            .andExpect(status().isNoContent())
            .andExpect(cookie().secure("__Host-carepath_refresh",true))
            .andExpect(cookie().httpOnly("__Host-carepath_refresh",true))
            .andExpect(cookie().path("__Host-carepath_refresh","/"))
            .andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("SameSite=Lax")));
    }
}
