package com.carepath.nearby;

import com.carepath.identity.AuthTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NearbyIntegrationTest extends AuthTestSupport {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TestLimits limits;

    @MockitoBean
    NearbyCareProvider provider;

    String token;

    @BeforeEach
    void setup() throws Exception {

        limits.clear();

        when(provider.available()).thenReturn(true);
        when(provider.search(any(NearbyCareProvider.Search.class)))
                .thenReturn(List.of());

        String email =
                UUID.randomUUID() + "@example.invalid";

        var body =
                Map.of(
                        "email", email,
                        "password", "Synthetic password 123!",
                        "displayName", "Synthetic"
                );

        mvc.perform(
                post("/api/v1/auth/register")
                        .header("Origin", "http://localhost:3000")
                        .header("X-CarePath-Client", "web")
                        .contentType("application/json")
                        .content(json.writeValueAsString(body))
        ).andExpect(status().isCreated());

        token =
                json.readTree(
                        mvc.perform(
                                post("/api/v1/auth/login")
                                        .header("Origin", "http://localhost:3000")
                                        .header("X-CarePath-Client", "web")
                                        .contentType("application/json")
                                        .content(
                                                json.writeValueAsString(
                                                        Map.of(
                                                                "email", email,
                                                                "password",
                                                                "Synthetic password 123!"
                                                        )
                                                )
                                        )
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
                )
                .path("accessToken")
                .asText();
    }

    ResultActions search(String body) throws Exception {

        return mvc.perform(
                post("/api/v1/nearby-care/search")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
                        .contentType("application/json")
                        .content(body)
        );
    }

    String valid =
            """
            {
              "latitude":0,
              "longitude":0,
              "radiusMeters":1000,
              "category":"HOSPITAL",
              "locationConsent":true
            }
            """;

    @Test
    void authenticationRequired() throws Exception {

        mvc.perform(
                get("/api/v1/nearby-care/config")
        ).andExpect(
                status().isUnauthorized()
        );

        mvc.perform(
                post("/api/v1/nearby-care/search")
                        .contentType("application/json")
                        .content(valid)
        ).andExpect(
                status().isUnauthorized()
        );
    }

    @Test
    void availableConfigurationAndNoStore() throws Exception {

        mvc.perform(
                get("/api/v1/nearby-care/config")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
        )
        .andExpect(status().isOk())
        .andExpect(
                header().string(
                        "Cache-Control",
                        "no-store"
                )
        )
        .andExpect(
                jsonPath("$.available")
                        .value(true)
        )
        .andExpect(
                jsonPath("$.provider")
                        .value("OpenStreetMap")
        );

        search(valid)
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                "Cache-Control",
                                "no-store"
                        )
                )
                .andExpect(
                        jsonPath("$.provider")
                                .value("OpenStreetMap")
                )
                .andExpect(
                        jsonPath("$.results")
                                .isArray()
                );
    }

    @Test
    void invalidAndForgedInputs() throws Exception {

        for (String value :
                List.of(
                        valid.replace(
                                "\"latitude\":0",
                                "\"latitude\":91"
                        ),
                        valid.replace(
                                "\"longitude\":0",
                                "\"longitude\":181"
                        ),
                        valid.replace(
                                "1000",
                                "50001"
                        ),
                        valid.replace(
                                "true",
                                "false"
                        ),
                        valid.replace(
                                "HOSPITAL",
                                "invented"
                        ),
                        valid.replace(
                                "{",
                                "{\"owner\":\"forged\","
                        )
                )) {

            search(value)
                    .andExpect(
                            status().isBadRequest()
                    );
        }
    }

    @Test
    void rateLimit() throws Exception {

        for (int i = 0; i < 10; i++) {
            search(valid)
                    .andExpect(
                            status().isOk()
                    );
        }

        search(valid)
                .andExpect(
                        status().isTooManyRequests()
                );
    }
}