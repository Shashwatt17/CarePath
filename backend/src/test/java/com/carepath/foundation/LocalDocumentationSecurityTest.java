package com.carepath.foundation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@ActiveProfiles({"test", "local"})
class LocalDocumentationSecurityTest extends com.carepath.identity.AuthTestSupport {
    @Autowired MockMvc mvc;
    @Test void documentationPolicyDoesNotLoosenProductPolicy() throws Exception {
        // Full application: OpenAPI JSON really exists under local profile.
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("script-src 'self'")));
        mvc.perform(get("/api/v1/documents")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/system/info"))
            .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"));
    }
}
