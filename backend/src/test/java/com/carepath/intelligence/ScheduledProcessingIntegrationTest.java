package com.carepath.intelligence;
import com.carepath.identity.AuthTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@TestPropertySource(properties={"carepath.processing.worker-enabled=true","carepath.processing.poll-ms=100"})
@DirtiesContext
class ScheduledProcessingIntegrationTest extends AuthTestSupport {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Test void actualSchedulerConsumesDurableJobAfterAcceptedResponse() throws Exception {
        mvc.perform(post("/api/v1/auth/register").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(Map.of("email","scheduler@example.invalid","password","Synthetic password 123!","displayName","Synthetic")))).andExpect(status().isCreated());
        String token=json.readTree(mvc.perform(post("/api/v1/auth/login").header("Origin","http://localhost:3000").header("X-CarePath-Client","web").contentType("application/json").content(json.writeValueAsString(Map.of("email","scheduler@example.invalid","password","Synthetic password 123!")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("accessToken").asText();
        String id=json.readTree(mvc.perform(multipart("/api/v1/documents").file(new MockMultipartFile("file","january.pdf","application/pdf",Files.readAllBytes(ExtractionRuntimeTest.sample("january-lab.pdf")))).file(new MockMultipartFile("metadata","","application/json","{\"documentType\":\"LAB_REPORT\",\"tags\":[]}".getBytes())).header("Authorization","Bearer "+token)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asText();
        mvc.perform(post("/api/v1/documents/"+id+"/process").header("Authorization","Bearer "+token)).andExpect(status().isAccepted()).andExpect(jsonPath("$.state").value("PROCESSING"));
        String state="PROCESSING";
        for(int i=0;i<150 && state.equals("PROCESSING");i++) {
            Thread.sleep(100);
            state=json.readTree(mvc.perform(get("/api/v1/documents/"+id+"/processing-status").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("state").asText();
        }
        assertEquals("NEEDS_REVIEW",state);
        mvc.perform(get("/api/v1/documents/"+id+"/extraction").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.result.candidates.length()").value(3));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='PROCESSING_SUCCEEDED' AND actor_kind='SYSTEM'",Integer.class));
    }
}
