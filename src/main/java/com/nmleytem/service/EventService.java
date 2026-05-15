package com.nmleytem.service;

import com.nmleytem.util.CursorUtils;
import com.nmleytem.api.EventJson;
import com.nmleytem.domain.Event;
import com.nmleytem.dto.EventPageResponse;
import com.nmleytem.dto.Pagination;
import com.nmleytem.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service class for managing events.
 * Handles the business logic for retrieving and paginating events.
 */
@Service
public class EventService {

    private final EventRepository eventRepository;

    /**
     * Constructs a new EventService with the specified EventRepository.
     *
     * @param eventRepository the repository used for data access
     */
    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Retrieves a paginated list of events based on filters and cursor.
     *
     * @param startTime minimum start time of events (epoch seconds)
     * @param endTime maximum start time of events (epoch seconds)
     * @param limit maximum number of events to return
     * @param cursor optional pagination cursor for fetching the next page
     * @param category optional filter for event category
     * @param city optional filter for event city
     * @param tags optional list of tags to filter by (events matching any tag will be included)
     * @return an EventPageResponse containing the list of events and pagination metadata
     */
    public EventPageResponse getEvents(
            long startTime,
            long endTime,
            int limit,
            String cursor,
            String category,
            String city,
            List<String> tags) {

        CursorUtils.CursorData cursorData = cursor != null && !cursor.isEmpty() ? CursorUtils.decode(cursor) : null;

        List<Event> events = eventRepository.findEvents(
                startTime,
                endTime,
                category,
                city,
                tags != null ? tags.toArray(new String[0]) : null,
                cursorData != null ? cursorData.startTime() : null,
                cursorData != null ? cursorData.eventId() : null,
                limit);

        long totalCount = eventRepository.countEvents(startTime, endTime, category, city, tags != null ? tags.toArray(new String[0]) : null);

        String nextCursor = null;
        if (!events.isEmpty() && events.size() == limit) {
            Event last = events.getLast();
            nextCursor = CursorUtils.encode(last.startTime(), last.id());
        }

        Pagination pagination = new Pagination(nextCursor, totalCount);
        return new EventPageResponse(events.stream().map(EventJson::from).toList(), pagination);
    }
}