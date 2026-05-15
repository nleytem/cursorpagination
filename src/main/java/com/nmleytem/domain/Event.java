package com.nmleytem.domain;

public record Event(
        String id,
        long startTime,
        long endTime,
        String title,
        String description,
        String category,
        Location location,
        String organizer,
        int priceCents,
        boolean isFree,
        String[] tags
) {
}
