package com.nmleytem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Pagination metadata for the events response.
 *
 * @param nextCursor the cursor to be used for retrieving the next page of results. 
 *                   Null if there are no more pages.
 * @param totalCount the total number of events matching the filter criteria across all pages.
 */
public record Pagination(
        @JsonProperty("next_cursor")
        String nextCursor,
        @JsonProperty("total_count")
        long totalCount
) {
}