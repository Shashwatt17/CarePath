package com.carepath.foundation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;



class FoundationSecurityTest extends com.carepath.identity.AuthTestSupport {
    @Autowired MockMvc mvc;

    @Test void publicInfoIsHonestAndHasSecurityHeaders() throws Exception {
        mvc.perform(get("/api/v1/system/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productFeaturesAvailable").value(false))
            .andExpect(header().exists("X-Request-ID"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"));
    }
    @Test void productRoutesAreClosed() throws Exception {
        mvc.perform(get("/api/v1/documents")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/documents").with(csrf())).andExpect(status().isUnauthorized());
    }
    @Test void swaggerIsClosedByDefault() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
    }
    @Test void foreignOriginIsRejected() throws Exception {
        mvc.perform(options("/api/v1/system/info").header("Origin", "https://untrusted.example")
            .header("Access-Control-Request-Method", "GET")).andExpect(status().isForbidden());
    }
    @Test void configuredOriginIsAllowed() throws Exception {
        mvc.perform(options("/api/v1/system/info").header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
    @Test void clientCannotChooseRequestId() throws Exception {
        mvc.perform(get("/api/v1/system/info").header("X-Request-ID", "untrusted"))
            .andExpect(header().string("X-Request-ID", org.hamcrest.Matchers.not("untrusted")));
    }
}
