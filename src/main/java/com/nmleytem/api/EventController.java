package com.nmleytem.api;

import com.nmleytem.dto.EventPageResponse;
import com.nmleytem.dto.EventRequest;
import com.nmleytem.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * REST controller for handling event-related requests.
 * Provides endpoints for retrieving events with cursor-based pagination and filtering.
 */
@RestController
@RequestMapping("/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);
    private final EventService eventService;

    /**
     * Constructs a new EventController with the specified EventService.
     *
     * @param eventService the service used to handle event-related business logic
     */
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Retrieves a paginated list of events based on the provided request parameters.
     * Supports filtering by time range, category, city, and tags.
     * Uses cursor-based pagination for consistent results.
     *
     * @param request the object containing all query parameters and filters
     * @return a ResponseEntity containing the paginated events and pagination metadata
     * @throws IllegalArgumentException if the request parameters fail validation
     */
    @GetMapping
    @Operation(summary = "Get events with cursor pagination and filters")
    public ResponseEntity<EventPageResponse> getEvents(@ModelAttribute EventRequest request) {
        log.info("Received request for events: {}", request);
        request.validate();
        
        EventPageResponse page = eventService.getEvents(
                request.startTime(),
                request.endTime(),
                request.limit() != null ? request.limit() : 20,
                request.cursor(),
                request.category(),
                request.city(),
                request.getTagList()
        );

        log.info("Returning {} events (total available: {})", page.events().size(), page.pagination().totalCount());
        return ResponseEntity.ok(page);
    }
}