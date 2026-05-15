package com.nmleytem.dto;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Request DTO for the Events API.
 * Uses Java Record for immutability and brevity.
 *
 * @param startTime minimum start time filter (required)
 * @param endTime maximum start time filter (required)
 * @param limit maximum number of results per page (optional, defaults to 20)
 * @param cursor pagination cursor for fetching the next page (optional)
 * @param category category filter (optional)
 * @param city city filter (optional)
 * @param tags comma-separated list of tags to filter by (optional)
 */
public record EventRequest(
        Long startTime,
        Long endTime,
        Integer limit,
        String cursor,
        String category,
        String city,
        String tags
) {
    /**
     * Returns the decoded city filter.
     *
     * @return the decoded city string, or null if not provided
     */
    @Override
    public String city(){
        return decode(this.city);
    }

    /**
     * Returns the decoded category filter.
     *
     * @return the decoded category string, or null if not provided
     */
    @Override
    public String category(){
        return decode(this.category);
    }

    /**
     * Returns the decoded tags filter string.
     *
     * @return the decoded tags string, or null if not provided
     */
    @Override
    public String tags(){
        return decode(this.tags);
    }

    private static String decode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Fallback to original if decoding fails
            return value;
        }
    }
    /**
     * Returns tag list parsed from comma-separated string.
     *
     * @return a list of tags, or null if tags parameter is not provided
     */
    public List<String> getTagList() {
        if (tags == null || tags.trim().isEmpty()) {
            return null;
        }
        return List.of(this.tags().split(","));
    }

    /**
     * Validates the request parameters.
     * Ensures startTime and endTime are provided and startTime <= endTime.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime are required");
        }
        if (startTime > endTime) {
            throw new IllegalArgumentException("startTime must be before or equal to endTime");
        }
    }
}