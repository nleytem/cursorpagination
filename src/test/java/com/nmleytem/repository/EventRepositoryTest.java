package com.nmleytem.repository;

import com.nmleytem.domain.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private EventRepository eventRepository;

    @BeforeEach
    void setUp() {
        eventRepository = new EventRepository(jdbcTemplate);
    }

    @Test
    void findEvents_ShouldCallJdbcTemplateWithCorrectSqlAndParams() {
        // Arrange
        long startTime = 1000L;
        long endTime = 2000L;
        String category = "music";
        String city = "New York";
        String[] tags = {"rock"};
        int limit = 10;

        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());

        // Act
        eventRepository.findEvents(startTime, endTime, category, city, tags, 1500L, "event-123", limit);

        // Assert
        ArgumentCaptor<MapSqlParameterSource> paramCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).query(contains("SELECT"), paramCaptor.capture(), any(RowMapper.class));

        MapSqlParameterSource params = paramCaptor.getValue();
        assertThat(params.getValue("startTime")).isEqualTo(startTime);
        assertThat(params.getValue("endTime")).isEqualTo(endTime);
        assertThat(params.getValue("category")).isEqualTo(category);
        assertThat(params.getValue("city")).isEqualTo(city);
        assertThat(params.getValue("tags")).isEqualTo(tags);
        assertThat(params.getValue("cursorStartTime")).isEqualTo(1500L);
        assertThat(params.getValue("cursorId")).isEqualTo("event-123");
        assertThat(params.getValue("limit")).isEqualTo(limit);
    }

    @Test
    void findEvents_ShouldHandleNullCursor() {
        // Arrange
        long startTime = 1000L;
        long endTime = 2000L;

        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());

        // Act
        eventRepository.findEvents(startTime, endTime, null, null, null, null, null,10);

        // Assert
        ArgumentCaptor<MapSqlParameterSource> paramCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).query(anyString(), paramCaptor.capture(), any(RowMapper.class));

        MapSqlParameterSource params = paramCaptor.getValue();
        assertThat(params.getValue("cursorStartTime")).isNull();
        assertThat(params.getValue("cursorId")).isNull();
    }

    @Test
    void findEvents_ShouldMapResultSetToEventList() throws SQLException {
        // Arrange
        long startTime = 1000L;
        long endTime = 2000L;

        ResultSet rs = mock(ResultSet.class);
        Array tagsArray = mock(Array.class);
        when(tagsArray.getArray()).thenReturn(new String[]{"rock", "pop"});

        when(rs.getString("id")).thenReturn("event-1");
        when(rs.getLong("start_time")).thenReturn(1100L);
        when(rs.getLong("end_time")).thenReturn(1200L);
        when(rs.getString("title")).thenReturn("Concert");
        when(rs.getString("description")).thenReturn("A rock concert");
        when(rs.getString("category")).thenReturn("music");
        when(rs.getString("location_venue")).thenReturn("Madison Square Garden");
        when(rs.getString("location_city")).thenReturn("New York");
        when(rs.getString("location_state")).thenReturn("NY");
        when(rs.getString("organizer")).thenReturn("Organizer X");
        when(rs.getInt("price_cents")).thenReturn(5000);
        when(rs.getBoolean("is_free")).thenReturn(false);
        when(rs.getArray("tags")).thenReturn(tagsArray);

        ArgumentCaptor<RowMapper<Event>> rowMapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), rowMapperCaptor.capture()))
                .thenReturn(Collections.emptyList()); // We'll manually call the mapper

        // Act
        eventRepository.findEvents(startTime, endTime, null, null, null, 1500L, "event-123", 10);
        Event event = rowMapperCaptor.getValue().mapRow(rs, 0);

        // Assert
        assertThat(event).isNotNull();
        assertThat(event.id()).isEqualTo("event-1");
        assertThat(event.startTime()).isEqualTo(1100L);
        assertThat(event.endTime()).isEqualTo(1200L);
        assertThat(event.title()).isEqualTo("Concert");
        assertThat(event.category()).isEqualTo("music");
        assertThat(event.location().venue()).isEqualTo("Madison Square Garden");
        assertThat(event.location().city()).isEqualTo("New York");
        assertThat(event.location().state()).isEqualTo("NY");
        assertThat(event.tags()).containsExactly("rock", "pop");
    }

    @Test
    void countEvents_ShouldCallJdbcTemplateWithCorrectSqlAndParams() {
        // Arrange
        long startTime = 1000L;
        long endTime = 2000L;
        String category = "music";
        String city = "New York";
        String[] tags = {"rock"};

        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(5L);

        // Act
        long count = eventRepository.countEvents(startTime, endTime, category, city, tags);

        // Assert
        assertThat(count).isEqualTo(5L);
        ArgumentCaptor<MapSqlParameterSource> paramCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).queryForObject(contains("SELECT count(*)"), paramCaptor.capture(), eq(Long.class));

        MapSqlParameterSource params = paramCaptor.getValue();
        assertThat(params.getValue("startTime")).isEqualTo(startTime);
        assertThat(params.getValue("endTime")).isEqualTo(endTime);
        assertThat(params.getValue("category")).isEqualTo(category);
        assertThat(params.getValue("city")).isEqualTo(city);
        assertThat(params.getValue("tags")).isEqualTo(tags);
    }
}
