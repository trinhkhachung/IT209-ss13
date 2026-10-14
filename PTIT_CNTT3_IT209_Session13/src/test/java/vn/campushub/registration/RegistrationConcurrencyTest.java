package vn.campushub.registration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RegistrationConcurrencyTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID eventId;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void createSingleSeatEvent() {
        eventId = UUID.randomUUID();
        OffsetDateTime startsAt = OffsetDateTime.now().plusDays(2);
        jdbcTemplate.update("""
                INSERT INTO events (id, title, category, description, location, starts_at, ends_at, capacity, registration_open)
                VALUES (?, 'Sự kiện đồng thời', 'Kiểm thử', 'Sự kiện dùng trong test', 'Phòng test', ?, ?, 1, TRUE)
                """, eventId, startsAt, startsAt.plusHours(2));
    }

    @AfterEach
    void removeEvent() {
        jdbcTemplate.update("DELETE FROM registrations WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM events WHERE id = ?", eventId);
    }

    @Test
    void concurrentRequestsForLastSeatNeverExceedCapacity() throws Exception {
        List<Integer> statuses = sendConcurrently(12, false);

        assertThat(statuses).containsOnly(201, 409);
        assertThat(statuses.stream().filter(status -> status == 201)).hasSize(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registrations WHERE event_id = ?", Integer.class, eventId)).isEqualTo(1);
    }

    @Test
    void concurrentDuplicateEmailCreatesOnlyOneRegistration() throws Exception {
        List<Integer> statuses = sendConcurrently(2, true);

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registrations WHERE event_id = ?", Integer.class, eventId)).isEqualTo(1);
    }

    private List<Integer> sendConcurrently(int requestCount, boolean sameEmail) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();

        try {
            for (int index = 0; index < requestCount; index++) {
                int requestIndex = index;
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent requests did not start in time.");
                    }
                    String email = sameEmail ? "same@example.com" : "student" + requestIndex + "@example.com";
                    String body = objectMapper.writeValueAsString(new Request("Sinh viên " + requestIndex, email));
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:" + port + "/api/events/" + eventId + "/registrations"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build();
                    return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get(15, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private record Request(String fullName, String email) {
    }
}
