package com.nmleytem.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Base64;

/**
 * Utility class for encoding and decoding pagination cursors.
 * Cursors are Base64 encoded JSON representations of {@link CursorData}.
 */
public class CursorUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Data structure representing the information stored within a cursor.
     *
     * @param startTime the start time of the last event in the current page
     * @param eventId the ID of the last event in the current page
     */
    public record CursorData(long startTime, String eventId) {
    }

    /**
     * Encodes pagination data into a Base64 string.
     *
     * @param startTime the start time to encode
     * @param eventId the event ID to encode
     * @return a Base64 encoded cursor string
     * @throws RuntimeException if encoding fails
     */
    public static String encode(long startTime, String eventId) {
        try {
            CursorData cursorData = new CursorData(startTime, eventId);
            var cursorBytes = objectMapper.writeValueAsBytes(cursorData);
            return Base64.getEncoder().encodeToString(cursorBytes);
        } catch (IOException e) {
            throw new RuntimeException("Error encoding cursor", e);
        }
    }

    /**
     * Decodes a Base64 encoded cursor string into {@link CursorData}.
     *
     * @param encodedCursor the Base64 encoded cursor string
     * @return the decoded CursorData
     * @throws IllegalArgumentException if the cursor is invalid or cannot be decoded
     */
    public static CursorData decode(String encodedCursor) {
        try {
            byte[] decodedCursor = Base64.getDecoder().decode(encodedCursor);
            return objectMapper.readValue(decodedCursor, CursorData.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor", e);
        }
    }
}
