# Take-Home: Events API with Cursor-Based Pagination

**Candidate:** _Your Name Here_

**Role:** Backend Java Developer
**Focus:** Pagination correctness, API design, data modeling
**Tech:** Your choice of framework and database. **You must use a database** (see below). **Spring is fine.** **Do not use Guava.** The provided `pom.xml` targets Java 8 — if you prefer a newer JDK, feel free to update the pom and Dockerfile. You make all other technical decisions.

For more detail on what we expect (API type, tests, documentation), see [Expectations](EXPECTATIONS.md).

---

## The Setup

You are building an **Events API**. Your task is to build a **single remote endpoint** (e.g. REST over HTTP) that returns events for a **date range** using **cursor-based pagination**, with support for **filtering**. The deliverable is a **callable HTTP API**, not an in-process Java interface — clients should be able to hit the endpoint with a tool like `curl` or a REST client.

- **Input:** Start date, end date (for the range), optional `limit`, optional `cursor`, optional filters (see below).
- **Output:** A page of events (as structured JSON) plus a `next_cursor` when more results exist. Results are ordered by `start_time` (ascending) with a stable tie-breaker (e.g. `id`).
- You must use a **database** — in-memory data structures (e.g. loading everything into a `List`) are not acceptable. You choose the technology (PostgreSQL, MySQL, MongoDB, H2, etc.).

The repo includes `seed_data.jsonl` — approximately 2,000 events with structured JSON payloads. You must ingest this data into your database. You own the schema design and the loading mechanism.

---

## The Assignment

### 1. Implementation

Build the API endpoint with correct cursor-based pagination so that:

- Each event (by `id`) appears **at most once** across all pages.
- The **total number of events** returned when paginating through all pages equals the total for the same query without pagination (no inflation, no missing events).
- Cursor semantics are stable and unambiguous (e.g. "start after this position" or "start at this position" — clearly defined).
- **Filtering** works correctly with pagination (see required filters below).

You own the choice of cursor format (opaque string, base64-encoded tuple, etc.) and the exact request/response shape.

#### Required filters

| Parameter  | Behavior |
|------------|----------|
| `category` | Exact match on the event's category (e.g. `?category=music`). |
| `city`     | Exact match on the event's city (e.g. `?city=New+York`). |
| `tags`     | Filter events matching any of the given tags (e.g. `?tags=jazz,nightlife`). |

These filters are **required**, not optional. Pagination must remain correct when filters are applied.

#### Data modeling

You decide how to model the data in your database. The seed data contains nested objects (`location`) and arrays (`tags`). How you store and query these is up to you — this is part of the evaluation.

---

### 2. Testing

Write **unit and/or integration tests** that prove pagination is correct. Unit tests (e.g. testing your pagination/cursor logic in isolation) and integration tests (e.g. calling the HTTP endpoint) are both acceptable — use whatever mix best demonstrates correctness. The test suite must be runnable without manual steps (e.g. via your build tool). At minimum, your tests must verify:

- **Totals match:** The count of events over all paginated pages equals the total event count for the same query without pagination.
- **No duplicates:** No event `id` appears more than once across all pages.
- **No missing events:** Every event matching the query appears in exactly one page.
- **Filter correctness:** The above invariants hold when `category`, `city`, and/or `tags` filters are applied.

Feel free to add more tests (e.g. empty range, single page, cursor stability, boundary times, filter that matches zero events).

---

### 3. API Contract

Document how clients should consume this API:

- Request parameters (e.g. `start_time`, `end_time`, `limit`, `cursor`, `category`, `city`, `tags`).
- Response shape (e.g. `events`, `next_cursor`, and when `next_cursor` is absent or null).
- How to use `next_cursor` to request the next page and how to know when pagination is complete.
- How filters interact with pagination.

Put this in your README or a dedicated API doc so that an integration engineer could implement a client without reading your source code.

---

## Stretch goals (optional)

If you have time and want to go further, here are ideas. We do not expect you to do all of these — pick whichever interest you.

- **Configurable sort order** — Support `sort_by` (e.g. `start_time`, `price_cents`) with `asc` / `desc`.
- **Price range** — Filter by `min_price` and/or `max_price`.
- **Full-text search** — Search across `title` and `description` fields.
- **Write endpoint** — `POST /events` to create a new event.
- **Error handling** — Return meaningful error responses (e.g. 400 with a message) for bad inputs: invalid cursors, missing required parameters, unknown filter values, etc.
- **Dockerized setup** — A `docker-compose.yml` (or similar) that starts both the application and the database with a single command.
- **Database migrations** — Versioned, repeatable schema management (e.g. Flyway, Liquibase) rather than ad-hoc DDL in application startup code.

These are not required. The core assignment is date-range + filter + cursor-based pagination with correct cursor logic.

---

## Deliverables Checklist

- [ ] **Working API** — One HTTP endpoint: events for a date range with cursor-based pagination, `category`, `city`, and `tags` filters, and correct cursor logic (callable via REST client).
- [ ] **Database** — A candidate-chosen database with your own data model. Data ingested from `seed_data.jsonl`.
- [ ] **Tests** — Unit and/or integration tests, runnable via your build tool, that prove paginated totals match non-paginated totals with no missing or duplicate events — both with and without filters.
- [ ] **API contract** — Clear docs for request/response, cursor usage, and filter behavior.
- [ ] **Javadoc** — Proper Javadoc for all public classes and public methods.
- [ ] **Setup/run instructions** — How to install dependencies, set up the database, load seed data, run the server, and run tests (so we can run your solution locally).

---

## Data: `seed_data.jsonl`

The seed file contains approximately 2,000 events in [JSON Lines](https://jsonlines.org/) format (one JSON object per line). Each event looks like:

```json
{
  "id": "evt-0042",
  "start_time": 1708646400,
  "end_time": 1708657200,
  "title": "Jazz Night at Blue Note",
  "description": "Live jazz performance featuring the house quartet and guest musicians.",
  "category": "music",
  "location": {
    "venue": "Blue Note",
    "city": "New York",
    "state": "NY"
  },
  "organizer": "Blue Note NYC",
  "price_cents": 3500,
  "is_free": false,
  "tags": ["jazz", "live-music", "nightlife"]
}
```

**Fields:**

| Field | Type | Notes |
|-------|------|-------|
| `id` | string | Unique event identifier (e.g. `evt-0042`). |
| `start_time` | integer | Unix timestamp (seconds). Many events share the same `start_time`. |
| `end_time` | integer | Unix timestamp (seconds). Always after `start_time`. |
| `title` | string | Short event title. |
| `description` | string | Longer description (1-2 sentences). |
| `category` | string | One of: `music`, `tech`, `food`, `sports`, `arts`, `comedy`, `wellness`, `community`. |
| `location` | object | Nested object with `venue` (string), `city` (string), `state` (string). |
| `organizer` | string | Name of the organizer. |
| `price_cents` | integer | Price in cents. `0` for free events. |
| `is_free` | boolean | `true` when `price_cents` is `0`. |
| `tags` | array of strings | 2-4 tags per event from a pool of ~30 tags. |

**Design note:** The `location` (nested object) and `tags` (array) fields require you to make data modeling decisions. There is no single right answer — we are interested in your approach.

Good luck. We're interested in your reasoning, code structure, and tests as much as in a working endpoint.
