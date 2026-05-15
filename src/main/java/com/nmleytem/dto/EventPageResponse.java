package com.nmleytem.dto;

import com.nmleytem.api.EventJson;

import java.util.List;

/**
 * Response DTO for paginated events.
 *
 * @param events the list of events for the current page
 * @param pagination metadata about pagination, including the next cursor and total count
 */
public record EventPageResponse(
        List<EventJson> events,
        Pagination pagination
) {

}