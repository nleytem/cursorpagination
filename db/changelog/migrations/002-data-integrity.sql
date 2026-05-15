--liquibase formatted sql

--changeset nleytem:3
-- Using a native ENUM type for categories is more efficient and cleaner than a VARCHAR with a CHECK constraint.
CREATE TYPE event_category AS ENUM ('music', 'tech', 'food', 'sports', 'arts', 'comedy', 'wellness', 'community');

-- Temporarily drop the constraint and change the column type
ALTER TABLE events ALTER COLUMN category DROP DEFAULT;
ALTER TABLE events ALTER COLUMN category TYPE event_category USING category::event_category;

-- Add strict integrity constraints to ensure data quality
ALTER TABLE events ADD CONSTRAINT check_times_logical CHECK (end_time >= start_time);
ALTER TABLE events ADD CONSTRAINT check_price_positive CHECK (price_cents >= 0);
ALTER TABLE events ADD CONSTRAINT check_id_format CHECK (id ~ '^evt-[0-9]+$');
