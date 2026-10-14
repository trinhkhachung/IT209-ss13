package vn.campushub.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void databaseFailureReturnsSafeServiceUnavailableResponse() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FailingDatabaseController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        MvcResult result = mockMvc.perform(get("/test/database-error"))
                .andExpect(status().isServiceUnavailable())
                .andReturn();

        JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(result.getResponse().getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
        assertThat(error.get("code").asText()).isEqualTo("SERVICE_UNAVAILABLE");
        assertThat(error.toString()).doesNotContain("connection refused", "SQLException", "stackTrace");
    }

    @RestController
    static class FailingDatabaseController {

        @GetMapping("/test/database-error")
        String fail() {
            throw new DataAccessResourceFailureException("connection refused: internal-db");
        }
    }
}
