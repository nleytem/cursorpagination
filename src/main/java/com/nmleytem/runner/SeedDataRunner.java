package com.nmleytem.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmleytem.api.EventJson;
import com.nmleytem.domain.Event;
import com.nmleytem.domain.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads seed data from seed_data.jsonl into the events table on application startup.
 * Only runs if the table is empty (idempotent).
 */
@Component
public class SeedDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper(); // or inject it

    public SeedDataRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Check if already seeded
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events", Integer.class);
        if (count != null && count > 0) {
            log.info("Events table already contains {} records. Skipping seed.", count);
            return;
        }

        log.info("Seeding events table from seed_data.jsonl...");

        List<Event> events = new ArrayList<>();
        ClassLoader classLoader = this.getClass().getClassLoader();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(classLoader.getResourceAsStream("seed_data.jsonl")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                EventJson json = objectMapper.readValue(line, EventJson.class);
                events.add(mapToEventObject(json));
            }
        }

        insertBatch(events);
        log.info("Successfully seeded {} events.", events.size());
    }

    private void insertBatch(List<Event> events) {
        String sql = """
            INSERT INTO events (id, start_time, end_time, title, description, category,
                                location_venue, location_city, location_state, organizer, price_cents, is_free, tags)
            VALUES (?, ?, ?, ?, ?, ?::event_category, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.batchUpdate(sql, events, 100, (ps, event) -> {
            ps.setString(1, event.id());
            ps.setLong(2, event.startTime());
            ps.setLong(3, event.endTime());
            ps.setString(4, event.title());
            ps.setString(5, event.description());
            ps.setString(6, event.category());
            ps.setString(7, event.location().venue());
            ps.setString(8, event.location().city());
            ps.setString(9, event.location().state());
            ps.setString(10, event.organizer());
            ps.setInt(11, event.priceCents());
            ps.setBoolean(12, event.isFree());
            ps.setArray(13, ps.getConnection().createArrayOf("text", event.tags()));
        });
    }

    private Event mapToEventObject(EventJson json) {
        Location location = new Location(
                json.location().venue(),
                json.location().city(),
                json.location().state()
        );
        return new Event(
                json.id(),
                json.startTime(),
                json.endTime(),
                json.title(),
                json.description(),
                json.category(),
                location,
                json.organizer(),
                json.priceCents(),
                json.isFree(),
                json.tags() != null ? json.tags() : new String[]{}
        );
    }
}