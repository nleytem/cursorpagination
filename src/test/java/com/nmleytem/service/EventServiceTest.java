package com.nmleytem.service;

import com.nmleytem.util.CursorUtils;
import com.nmleytem.domain.Event;
import com.nmleytem.domain.Location;
import com.nmleytem.dto.EventPageResponse;
import com.nmleytem.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository);
    }

    @Test
    void getEvents_ShouldReturnEventsAndNextCursor_WhenLimitIsReached() {
        // Arrange
        long startTime = 1000L;
        long endTime = 5000L;
        int limit = 2;
        String cursor = null;
        String category = "music";
        String city = "New York";
        List<String> tags = List.of("rock");

        Location location = new Location("Venue", "City", "State");
        Event event1 = new Event("1", 1100L, 1200L, "Title 1", "Desc 1", "music", location, "Org", 100, false, new String[]{"rock"});
        Event event2 = new Event("2", 1300L, 1400L, "Title 2", "Desc 2", "music", location, "Org", 200, false, new String[]{"rock"});
        List<Event> events = List.of(event1, event2);

        when(eventRepository.findEvents(eq(startTime), eq(endTime), eq(category), eq(city), any(String[].class), any(), any(), eq(limit)))
                .thenReturn(events);
        when(eventRepository.countEvents(eq(startTime), eq(endTime), eq(category), eq(city), any(String[].class)))
                .thenReturn(10L);

        // Act
        EventPageResponse response = eventService.getEvents(startTime, endTime, limit, cursor, category, city, tags);

        // Assert
        assertThat(response.events()).hasSize(2);
        assertThat(response.pagination().totalCount()).isEqualTo(10L);
        
        String expectedNextCursor = CursorUtils.encode(event2.startTime(), event2.id());
        assertThat(response.pagination().nextCursor()).isEqualTo(expectedNextCursor);

        verify(eventRepository).findEvents(eq(startTime), eq(endTime), eq(category), eq(city), any(String[].class), any(), any(), eq(limit));
        verify(eventRepository).countEvents(eq(startTime), eq(endTime), eq(category), eq(city), any(String[].class));
    }

    @Test
    void getEvents_ShouldReturnEventsAndNullNextCursor_WhenLimitIsNotReached() {
        // Arrange
        int limit = 10;
        List<Event> events = List.of(
            new Event("1", 1100L, 1200L, "Title 1", "Desc 1", "music", new Location("V", "C", "S"), "O", 100, true, new String[]{})
        );

        when(eventRepository.findEvents(anyLong(), anyLong(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(events);
        when(eventRepository.countEvents(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(1L);

        // Act
        EventPageResponse response = eventService.getEvents(0, 0, limit, null, null, null, null);

        // Assert
        assertThat(response.events()).hasSize(1);
        assertThat(response.pagination().nextCursor()).isNull();
        assertThat(response.pagination().totalCount()).isEqualTo(1L);
    }

    @Test
    void getEvents_ShouldReturnEmptyList_WhenNoEventsFound() {
        // Arrange
        when(eventRepository.findEvents(anyLong(), anyLong(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(eventRepository.countEvents(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(0L);

        // Act
        EventPageResponse response = eventService.getEvents(0, 0, 10, null, null, null, null);

        // Assert
        assertThat(response.events()).isEmpty();
        assertThat(response.pagination().nextCursor()).isNull();
        assertThat(response.pagination().totalCount()).isEqualTo(0L);
    }

    @Test
    void getEvents_ShouldDecodeCursorAndPassToRepository() {
        // Arrange
        String cursor = CursorUtils.encode(1500L, "event-123");
        when(eventRepository.findEvents(anyLong(), anyLong(), any(), any(), any(), eq(1500L), eq("event-123"), anyInt()))
                .thenReturn(Collections.emptyList());
        when(eventRepository.countEvents(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(0L);

        // Act
        eventService.getEvents(0, 0, 10, cursor, null, null, null);

        // Assert
        verify(eventRepository).findEvents(anyLong(), anyLong(), any(), any(), any(), eq(1500L), eq("event-123"), anyInt());
    }
}
