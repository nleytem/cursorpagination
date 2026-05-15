package com.nmleytem.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nmleytem.domain.Event;

/**
 * JSON representation of an Event for API responses.
 *
 * @param id unique identifier of the event
 * @param startTime event start time (epoch seconds)
 * @param endTime event end time (epoch seconds)
 * @param title title of the event
 * @param description detailed description of the event
 * @param category category the event belongs to
 * @param location venue and address information
 * @param organizer organization or person hosting the event
 * @param priceCents price in cents
 * @param isFree whether the event is free of charge
 * @param tags list of tags associated with the event
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventJson(
        String id,
        @JsonProperty("start_time")
        long startTime,
        @JsonProperty("end_time")
        long endTime,
        String title,
        String description,
        String category,
        LocationJson location,
        String organizer,
        @JsonProperty("price_cents")
        int priceCents,
        @JsonProperty("is_free")
        boolean isFree,
        String[] tags
) {
    /**
     * JSON representation of a Location.
     *
     * @param venue name of the venue
     * @param city city where the event is located
     * @param state state or province where the event is located
     */
    public record LocationJson(
            String venue,
            String city,
            String state
    ) {}

    /**
     * Factory method to create an EventJson from a domain Event object.
     *
     * @param event the domain event to convert
     * @return the corresponding EventJson object
     */
    public static EventJson from(Event event) {
        LocationJson locationJson = new LocationJson(
                event.location().venue(),
                event.location().city(),
                event.location().state()
        );

        return new EventJson(
                event.id(),
                event.startTime(),
                event.endTime(),
                event.title(),
                event.description(),
                event.category(),
                locationJson,
                event.organizer(),
                event.priceCents(),
                event.isFree(),
                event.tags()
        );
    }
}
