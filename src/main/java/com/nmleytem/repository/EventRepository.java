package com.nmleytem.repository;

import com.nmleytem.domain.Event;
import com.nmleytem.domain.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * Repository for accessing event data from the database.
 * Uses raw SQL and JdbcTemplate to ensure performance and correctness of keyset pagination.
 */
@Repository
public class EventRepository {

    private static final Logger log = LoggerFactory.getLogger(EventRepository.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Constructs a new EventRepository with the specified NamedParameterJdbcTemplate.
     *
     * @param jdbcTemplate the JDBC template used for database operations
     */
    public EventRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds events based on the provided filters and pagination parameters.
     * Implements keyset pagination using a combination of start_time and id.
     *
     * @param startTime minimum start time filter
     * @param endTime maximum start time filter
     * @param category optional category filter
     * @param city optional city filter
     * @param tags optional array of tags to filter by (uses Postgres overlap operator &&)
     * @param cursorStartTime start time from the cursor for pagination
     * @param cursorId event ID from the cursor for pagination
     * @param limit maximum number of results to return
     * @return a list of events matching the criteria
     */
    public List<Event> findEvents(
            long startTime,
            long endTime,
            String category,
            String city,
            String[] tags,
            Long cursorStartTime,
            String cursorId,
            int limit
    ) {
        String sql = """
                SELECT id, start_time, end_time, title, description, category,
                    location_venue, location_city, location_state, organizer, price_cents, is_free, tags
                FROM events
                WHERE start_time >= :startTime AND start_time <= :endTime
                    AND (:category::text IS NULL OR category = :category::event_category)
                    AND (:city IS NULL OR location_city = :city)
                    AND (:tags::text[] IS NULL OR tags && :tags::text[])
                    AND (:cursorStartTime IS NULL\s
                        OR start_time > :cursorStartTime\s
                        OR (start_time = :cursorStartTime AND id > :cursorId))
                ORDER BY start_time ASC, id ASC
                LIMIT :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("startTime", startTime)
                .addValue("endTime", endTime)
                .addValue("category", category, Types.VARCHAR)
                .addValue("city", city, Types.VARCHAR)
                .addValue("tags", tags != null && tags.length > 0
                        ? tags : null, Types.ARRAY)
                .addValue("cursorStartTime", cursorStartTime, Types.BIGINT)
                .addValue("cursorId", cursorId, Types.VARCHAR)
                .addValue("limit", limit, Types.INTEGER);

        log.debug("Executing event search: startTime={}, endTime={}, category={}, city={}, limit={}", 
                startTime, endTime, category, city, limit);
        return jdbcTemplate.query(sql, params, this::mapRow);
    }

    /**
     * Counts the total number of events matching the provided filters.
     *
     * @param startTime minimum start time filter
     * @param endTime maximum start time filter
     * @param category optional category filter
     * @param city optional city filter
     * @param tags optional array of tags to filter by
     * @return the total count of matching events
     */
    public long countEvents(
            long startTime,
            long endTime,
            String category,
            String city,
            String[] tags
    ) {
        String sql = """
                SELECT count(*) FROM events
                WHERE start_time >= :startTime AND start_time <= :endTime
                    AND (:category::text IS NULL OR category = :category::event_category)
                    AND (:city IS NULL OR location_city = :city)
                    AND (:tags::text[] IS NULL or tags && :tags::text[])
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("startTime", startTime)
                .addValue("endTime", endTime)
                .addValue("category", category, Types.VARCHAR)
                .addValue("city", city, Types.VARCHAR)
                .addValue("tags", tags != null && tags.length > 0
                        ? tags : null, Types.ARRAY);

        log.debug("Executing event count: startTime={}, endTime={}, category={}, city={}", 
                startTime, endTime, category, city);
        return jdbcTemplate.queryForObject(sql, params, Long.class);
    }

    private Event mapRow(ResultSet rs, int rowNum) throws SQLException {
        Location location = new Location(
                rs.getString("location_venue"),
                rs.getString("location_city"),
                rs.getString("location_state")
        );
        return new Event(
                rs.getString("id"),
                rs.getLong("start_time"),
                rs.getLong("end_time"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("category"),
                location,
                rs.getString("organizer"),
                rs.getInt("price_cents"),
                rs.getBoolean("is_free"),
                (String[]) rs.getArray("tags").getArray()
        );
    }
}
