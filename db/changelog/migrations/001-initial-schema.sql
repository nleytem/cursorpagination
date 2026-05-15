--liquibase formatted sql

--changeset nleytem:1
CREATE TABLE events (
    id                VARCHAR(50) PRIMARY KEY,
    start_time        BIGINT NOT NULL,
    end_time          BIGINT NOT NULL,
    title             TEXT,
    description       TEXT,
    category          VARCHAR(20) NOT NULL CHECK (category IN ('music','tech','food','sports','arts','comedy','wellness','community')),
    location_venue    VARCHAR(255),
    location_city     VARCHAR(100),
    location_state    VARCHAR(50),
    organizer         TEXT,
    price_cents       INTEGER,
    is_free           BOOLEAN,
    tags              TEXT[]
);

--changeset nleytem:2
CREATE INDEX idx_events_start_time_id ON events (start_time, id);
CREATE INDEX idx_events_category       ON events (category);
CREATE INDEX idx_events_location_city  ON events (location_city);
CREATE INDEX idx_events_tags           ON events USING GIN (tags);