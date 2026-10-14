package vn.campushub.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import vn.campushub.event.EventNotFoundException;
import vn.campushub.registration.dto.RegistrationConfirmationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RegistrationServiceTest {

    @Autowired
    private RegistrationService registrationService;

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
                VALUES (?, 'Kiểm thử đăng ký', 'Kiểm thử', 'Sự kiện dùng trong test', 'Phòng test', ?, ?, 1, TRUE)
                """, eventId, startsAt, startsAt.plusHours(2));
    }

    @AfterEach
    void removeEvent() {
        jdbcTemplate.update("DELETE FROM registrations WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM events WHERE id = ?", eventId);
    }

    @Test
    void registerTrimsNameNormalizesEmailAndDoesNotReturnEmail() throws Exception {
        RegistrationConfirmationResponse response = registrationService.register(
                eventId, "  Nguyễn An  ", "  An.Example@Email.COM ");

        String storedEmail = jdbcTemplate.queryForObject(
                "SELECT email_normalized FROM registrations WHERE event_id = ?", String.class, eventId);
        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.registrationId()).isNotNull();
        assertThat(storedEmail).isEqualTo("an.example@email.com");
        assertThat(objectMapper.writeValueAsString(response)).doesNotContain("email");
    }

    @Test
    void duplicateRegistrationUsesNormalizedEmailAndReturnsConflict() {
        registrationService.register(eventId, "Nguyễn An", "an@example.com");

        assertThatThrownBy(() -> registrationService.register(eventId, "Nguyễn An", " AN@EXAMPLE.COM "))
                .isInstanceOf(RegistrationConflictException.class)
                .hasMessageContaining("đăng ký");
    }

    @Test
    void fullEventRejectsAnotherRegistration() {
        registrationService.register(eventId, "Nguyễn An", "an@example.com");

        assertThatThrownBy(() -> registrationService.register(eventId, "Nguyễn Bình", "binh@example.com"))
                .isInstanceOf(RegistrationConflictException.class)
                .hasMessageContaining("hết chỗ");
    }

    @Test
    void closedEventRejectsRegistration() {
        jdbcTemplate.update("UPDATE events SET registration_open = FALSE WHERE id = ?", eventId);

        assertThatThrownBy(() -> registrationService.register(eventId, "Nguyễn An", "an@example.com"))
                .isInstanceOf(RegistrationConflictException.class)
                .hasMessageContaining("đóng");
    }

    @Test
    void unknownEventIsNotFound() {
        assertThatThrownBy(() -> registrationService.register(
                UUID.randomUUID(), "Nguyễn An", "an@example.com"))
                .isInstanceOf(EventNotFoundException.class);
    }
}
