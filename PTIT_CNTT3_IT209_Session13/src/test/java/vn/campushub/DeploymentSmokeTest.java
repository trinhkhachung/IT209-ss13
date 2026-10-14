package vn.campushub;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DeploymentSmokeTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void servesUiEventsAndReadinessFromOneApplication() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> home = client.send(HttpRequest.newBuilder(uri("/")).GET().build(), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> events = client.send(HttpRequest.newBuilder(uri("/api/events")).GET().build(), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> readiness = client.send(HttpRequest.newBuilder(uri("/actuator/health/readiness")).GET().build(), HttpResponse.BodyHandlers.ofString());

        assertThat(home.statusCode()).isEqualTo(200);
        assertThat(home.body()).contains("Campus Hub", "/assets/");
        assertThat(events.statusCode()).isEqualTo(200);
        assertThat(events.body()).contains("Ngày hội câu lạc bộ sinh viên");
        assertThat(readiness.statusCode()).isEqualTo(200);
        assertThat(readiness.body()).contains("\"status\":\"UP\"");
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
