package vn.campushub.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventControllerIntegrationTest {

    private static final UUID CLUB_FAIR_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID UNKNOWN_ID = UUID.fromString("99999999-9999-4999-8999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listReturnsPublicUpcomingEventsWithCapacity() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode events = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(events).hasSizeGreaterThanOrEqualTo(2);
        assertThat(events.toString()).contains("Ngày hội câu lạc bộ sinh viên", "remainingSeats");
        assertThat(events.toString()).doesNotContain("email");
    }

    @Test
    void queryAndCategoryFiltersCanBeCombined() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/events")
                        .queryParam("query", "hackathon")
                        .queryParam("category", "CÔNG NGHỆ"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode events = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).get("title").asText()).isEqualTo("Hackathon Sáng tạo Campus");
    }

    @Test
    void detailSerializesVietnamTimezoneAsTheSameInstant() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/events/{id}", CLUB_FAIR_ID))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode event = objectMapper.readTree(result.getResponse().getContentAsString());
        Instant actual = OffsetDateTime.parse(event.get("startsAt").asText()).toInstant();
        assertThat(actual).isEqualTo(Instant.parse("2026-11-21T01:00:00Z"));
        assertThat(event.has("email")).isFalse();
    }

    @Test
    void unknownEventReturnsSafeNotFoundContract() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/events/{id}", UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(error.get("code").asText()).isEqualTo("EVENT_NOT_FOUND");
        assertThat(error.get("message").asText()).isNotBlank();
        assertThat(error.toString()).doesNotContain("stackTrace", "SQLException");
    }
}
