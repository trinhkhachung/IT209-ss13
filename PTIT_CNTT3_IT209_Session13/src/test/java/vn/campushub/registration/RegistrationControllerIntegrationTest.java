package vn.campushub.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID eventId;

    @BeforeEach
    void createSingleSeatEvent() {
        eventId = UUID.randomUUID();
        OffsetDateTime startsAt = OffsetDateTime.now().plusDays(2);
        jdbcTemplate.update("""
                INSERT INTO events (id, title, category, description, location, starts_at, ends_at, capacity, registration_open)
                VALUES (?, 'Kiểm thử API', 'Kiểm thử', 'Sự kiện dùng trong test', 'Phòng test', ?, ?, 1, TRUE)
                """, eventId, startsAt, startsAt.plusHours(2));
    }

    @AfterEach
    void removeEvent() {
        jdbcTemplate.update("DELETE FROM registrations WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM events WHERE id = ?", eventId);
    }

    @Test
    void validRegistrationReturnsCreatedConfirmationWithoutEmail() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content("""
                                {"fullName":"Nguyễn An","email":"an@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(response.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(response.has("registrationId")).isTrue();
        assertThat(response.has("email")).isFalse();

        MvcResult detailResult = mockMvc.perform(get("/api/events/{id}", eventId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode detail = objectMapper.readTree(detailResult.getResponse().getContentAsString());
        assertThat(detail.get("registeredCount").asLong()).isEqualTo(1);
        assertThat(detail.get("remainingSeats").asInt()).isZero();
    }

    @Test
    void invalidEmailReturnsSafeBadRequestWithoutSaving() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content("""
                                {"fullName":"Nguyễn An","email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(error.get("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registrations WHERE event_id = ?", Integer.class, eventId)).isZero();
    }

    @Test
    void duplicateEmailVariantReturnsConflict() throws Exception {
        submit("Nguyễn An", "an@example.com", 201);
        MvcResult result = submit("Nguyễn An", " AN@EXAMPLE.COM ", 409);

        JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(error.get("code").asText()).isEqualTo("REGISTRATION_DUPLICATE");
    }

    @Test
    void fullEventReturnsConflict() throws Exception {
        submit("Nguyễn An", "an@example.com", 201);
        MvcResult result = submit("Nguyễn Bình", "binh@example.com", 409);

        JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(error.get("code").asText()).isEqualTo("EVENT_FULL");
    }

    @Test
    void unknownEventReturnsNotFound() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/events/{id}/registrations", UUID.randomUUID())
                        .contentType("application/json")
                        .content("""
                                {"fullName":"Nguyễn An","email":"an@example.com"}
                                """))
                .andExpect(status().isNotFound())
                .andReturn();

        assertThat(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("code").asText()).isEqualTo("EVENT_NOT_FOUND");
    }

    private MvcResult submit(String fullName, String email, int expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/events/{id}/registrations", eventId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new Request(fullName, email))))
                .andExpect(status().is(expectedStatus))
                .andReturn();
    }

    private record Request(String fullName, String email) {
    }
}
