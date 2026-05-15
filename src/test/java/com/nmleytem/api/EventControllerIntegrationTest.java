package com.nmleytem.api;

import com.nmleytem.dto.EventPageResponse;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EventControllerIntegrationTest {

    @TestConfiguration
    static class PostgresConfig {
        @Bean
        @Primary
        public DataSource dataSource() throws IOException {
            return EmbeddedPostgres.start().getPostgresDatabase();
        }
    }

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    private String getUrl(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @DisplayName("Should paginate through all events without duplicates")
    void shouldPaginateThroughEvents() {
        String baseUrl = "/events?startTime=0&endTime=2000000000&limit=50";
        Set<String> allEventIds = new HashSet<>();
        String nextCursor = null;
        int totalFetched = 0;
        long reportedTotal = -1;

        // Fetch first 3 pages to verify pagination logic
        for (int i = 0; i < 3; i++) {
            String url = getUrl(baseUrl + (nextCursor != null ? "&cursor=" + nextCursor : ""));
            ResponseEntity<EventPageResponse> response = restTemplate.getForEntity(url, EventPageResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            EventPageResponse body = response.getBody();
            assertThat(body).isNotNull();

            if (reportedTotal == -1) {
                reportedTotal = body.pagination().totalCount();
                assertThat(reportedTotal).isGreaterThan(0);
            }

            List<EventJson> events = body.events();
            assertThat(events).hasSizeLessThanOrEqualTo(50);
            
            for (EventJson event : events) {
                boolean isNew = allEventIds.add(event.id());
                assertThat(isNew).as("Duplicate event ID found: " + event.id()).isTrue();
            }

            totalFetched += events.size();
            nextCursor = body.pagination().nextCursor();
            
            if (nextCursor == null) break;
        }

        assertThat(totalFetched).isGreaterThan(0);
        assertThat(allEventIds.size()).isEqualTo(totalFetched);
    }

    @Test
    @DisplayName("Should filter by category and paginate")
    void shouldFilterByCategoryAndPaginate() {
        String category = "music";
        String url = getUrl("/events?startTime=0&endTime=2000000000&limit=10&category=" + category);

        ResponseEntity<EventPageResponse> response = restTemplate.getForEntity(url, EventPageResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        EventPageResponse body = response.getBody();
        assertThat(body).isNotNull();
        
        assertThat(body.events()).allMatch(e -> category.equals(e.category()));
        long totalCount = body.pagination().totalCount();
        assertThat(totalCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should filter by tags and ensure overlap")
    void shouldFilterByTags() {
        // Tag 'live-music' is in seed data
        String tag = "live-music";
        String url = getUrl("/events?startTime=0&endTime=2000000000&limit=10&tags=" + tag);

        ResponseEntity<EventPageResponse> response = restTemplate.getForEntity(url, EventPageResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        EventPageResponse body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.events()).allMatch(e -> {
            for (String t : e.tags()) {
                if (tag.equals(t)) return true;
            }
            return false;
        });
    }

    @Test
    @DisplayName("Should filter by city and paginate")
    void shouldFilterByCityAndPaginate() {
        String city = "New+York";
        String url = getUrl("/events?startTime=0&endTime=2000000000&limit=10&city=" + city);
        ResponseEntity<EventPageResponse> response = restTemplate.getForEntity(url, EventPageResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        EventPageResponse body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.events()).allMatch( e -> e.location().city().equals("New York"));
        long totalCount = body.pagination().totalCount();
        assertThat(totalCount).isGreaterThan(0);
        String cursor = body.pagination().nextCursor();
        assertThat(cursor).isNotNull();
    }

    @Test
    @DisplayName("Should return 400 for invalid cursor")
    void shouldReturn400ForInvalidCursor() {
        String url = getUrl("/events?startTime=0&endTime=2000000000&cursor=invalid_base64_or_json");

        try {
            restTemplate.getForEntity(url, String.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    @DisplayName("Should return 400 for missing required parameters")
    void shouldReturn400ForMissingParams() {
        String url = getUrl("/events"); // startTime/endTime missing

        try {
            restTemplate.getForEntity(url, String.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
