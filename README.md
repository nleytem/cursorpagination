# Events API with Keyset Pagination

**Candidate:** [Nick Leytem](https://linkedin.com/in/leytem)

**Tech Stack:** Java 25, Spring Boot 4.0.6, PostgreSQL, JDBC Template

This repository contains a high-performance Events API featuring **stable keyset (cursor-based) pagination** and flexible filtering. It is designed to handle large datasets efficiently by avoiding the performance pitfalls of traditional offset/limit pagination.

---

## 🚀 Getting Started

### Prerequisites
- **Java 25** (or compatible JDK)
- **Maven** (optional, the project includes `./mvnw`)
- **Docker** (optional, only needed for production-like run via `compose.yaml`. Tests run with an embedded database.)

### Build and Run Tests
The test suite includes full end-to-end integration tests that spin up an ephemeral PostgreSQL instance. No manual database setup is required to run the tests.
```bash
./mvnw clean test
```

### Run Locally
The application automatically seeds 2,000 events from `seed_data.jsonl` on startup.
```bash
./mvnw spring-boot:run
```
The API will be available at `http://localhost:8080`.

---

## 📖 API Documentation

### `GET /events`
Retrieves a paginated list of events within a specific time range, with optional filters.

#### Request Parameters
| Parameter   | Type    | Required | Description |
|-------------|---------|----------|-------------|
| `startTime` | Long    | Yes      | Unix timestamp (seconds) for range start. |
| `endTime`   | Long    | Yes      | Unix timestamp (seconds) for range end. |
| `limit`     | Integer | No       | Max items per page (default: 20). |
| `cursor`    | String  | No       | Opaque pointer for the next page. |
| `category`  | String  | No       | Exact match (e.g., `music`, `tech`). |
| `city`      | String  | No       | Exact match (e.g., `New York`). |
| `tags`      | String  | No       | Comma-separated list of tags. Returns events matching *any* of these tags. |

#### Response Shape
```json
{
  "events": [...],
  "pagination": {
    "next_cursor": "eyJzdGFydFRpbWUiOjE3MjIwOTYwMDAsImV2ZW50SWQiOiJldnQtMDAwMSJ9",
    "total_count": 2000
  }
}
```

#### Example cURL Requests

**Basic Pagination**
```bash
curl "http://localhost:8080/events?startTime=1700000000&endTime=1800000000&limit=5"
```

**Filtering by City (Handling spaces)**
```bash
curl "http://localhost:8080/events?startTime=1700000000&endTime=1800000000&city=New+York"
```

**Filtering by Multiple Tags**
```bash
curl "http://localhost:8080/events?startTime=1700000000&endTime=1800000000&tags=jazz,live-music"
```

### How to Paginate
1.  Make an initial request with `startTime` and `endTime`.
2.  If the response contains a non-null `next_cursor`, there are more results available.
3.  To fetch the next page, include the `cursor` value in your next request. **Important:** All filter parameters must remain identical across paginated requests to ensure consistency.
4.  Pagination is complete when `next_cursor` is `null`.

---

## 🛠 Architectural Highlights

- **Keyset Pagination**: Uses `(start_time, id)` as a unique, indexed position marker. This ensures $O(1)$ lookup for any page and prevents duplicates/skips if data changes between requests.
- **Base64 Cursors**: Cursors are Base64-encoded JSON tuples, providing an opaque but extensible API contract.
- **No ORM**: Implemented using raw SQL and `JdbcTemplate` to ensure full control over query performance and the use of PostgreSQL-specific operators (like the `&&` overlap operator for tags).
- **Observability**: Every request is assigned a unique `UUID` (Request ID) generated in a Servlet Filter and stored in the **MDC**. This ID is tagged on every log message throughout the request lifecycle.
- **Embedded Database for Testing**: Uses `otj-pg-embedded` to run tests against a real PostgreSQL binary without requiring Docker on the host machine.

For detailed design trade-offs, see [ADR.md](ADR.md).

For formal verification of pagination correctness and concrete examples, see [PAGINATION_INVARIANTS.md](PAGINATION_INVARIANTS.md).
