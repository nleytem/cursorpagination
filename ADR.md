# Architecture Decision Records (ADR)

## 1. Keyset Pagination (Cursor-based)
**Context**: The application needs to handle potentially large datasets (thousands of events) and provide a smooth infinite scroll or "next page" experience.
**Decision**: Use keyset pagination (also known as cursor-based pagination) instead of offset/limit pagination.
**Rationale**: Keyset pagination provides stable performance as it uses indexed columns (`start_time`, `id`) to find the next set of results, avoiding the performance degradation seen with high offsets. It also prevents "skipped" or "duplicated" items when rows are inserted or deleted between page requests.

## 2. Base64 JSON Cursor Encoding
**Context**: The pagination cursor needs to represent a specific position in the ordered result set.
**Decision**: Use Base64 encoded JSON strings to represent the cursor. The cursor contains the `start_time` and `id` of the last seen item.
**Rationale**: Opaque cursors hide internal implementation details while being easy to decode on the server. Using JSON within the Base64 string allows for easy extensibility if the ordering criteria change in the future.

## 3. Raw JDBC (JdbcTemplate) instead of ORM
**Context**: The exercise explicitly forbids Hibernate/JPA to ensure correct keyset pagination SQL generation.
**Decision**: Use Spring's `NamedParameterJdbcTemplate` for all database interactions.
**Rationale**: Keyset pagination depends heavily on the specific shape of the `WHERE` and `ORDER BY` clauses. Using raw SQL allows full control over these clauses and ensures that indices are used effectively. It also makes it easier to use PostgreSQL-specific features like the array overlap operator (`&&`).
 Additionally, Hikari is being used for connection pooling to boost performance.

## 4. Embedded PostgreSQL for Testing
**Context**: Integration tests need a real database to verify complex SQL (like array overlaps and keyset logic), but requiring the user to have Docker running adds friction.
**Decision**: Use `com.opentable.components:otj-pg-embedded` for integration testing.
**Rationale**: This library downloads and runs real PostgreSQL binaries for the host OS. This allows tests to run against a real PostgreSQL instance with full support for all features (including array types and operators) without requiring the user to install Docker or PostgreSQL manually.

## 5. Advanced Domain Model Mapping & Integrity
**Context**: The data includes nested `location` objects and `tags` arrays. We need to ensure data quality and high query performance.
**Decision**: 
- Store `location` fields as flattened columns (`location_venue`, `location_city`, `location_state`).
- Use a native PostgreSQL `ENUM` type for `category`.
- Use a native PostgreSQL `TEXT[]` array for `tags` with a `GIN` index.
- Implement strict database-level `CHECK` constraints for logical validation (e.g., `end_time >= start_time`, price non-negativity, and ID format).
**Rationale**: 
- Flattening locations allows for simpler, faster indexing on `location_city`.
- Native `ENUM` types are more storage-efficient than strings and provide strict type safety at the database layer.
- PostgreSQL arrays with `GIN` indices allow for extremely performant filtering using the overlap (`&&`) operator, avoiding the performance overhead of join tables for simple tags.
- Moving logical validation into `CHECK` constraints provides a "defense-in-depth" strategy, ensuring data integrity even if application-level validation is bypassed.
- Unflattening the Location object into a table could be a future consideration if more information were added for locations (address, websites, contact information, etc.)

## 6. Global Exception Handling
**Context**: The API should return user-friendly errors for invalid inputs (e.g., malformed cursors).
**Decision**: Implement a `@RestControllerAdvice` with a `GlobalExceptionHandler`.
**Rationale**: This provides a centralized way to map internal exceptions (like `IllegalArgumentException`) to appropriate HTTP status codes (like `400 Bad Request`), ensuring a consistent API contract.
**Future Consideration**: I would create a more robust model so errors had the same shape and were machine-readable and actionable. This implementation is more for informational or visibility purposes.


## 7. MDC-based Request Tracing
**Context**: In a production environment, multiple requests are handled concurrently. Log messages from different requests become interleaved, making it difficult to trace the lifecycle of a single request.
**Decision**: Implement a `OncePerRequestFilter` that generates a unique `requestId` (UUID) for every incoming request and stores it in the SLF4J Mapped Diagnostic Context (MDC).
**Rationale**: By including `%X{requestId}` in the logging pattern, all log messages generated during the execution of a request—from the controller to the service and repository layers—are tagged with the same ID. This significantly improves observability and simplifies debugging and log aggregation. They are formatted in JSON so they can be machine-readable and easily queried with something like SumoLogic.

## 8. Filtering
**Context**: The API requires startTime and endTime as parameters to the GET /events endpoint.
**Decision**: This range is being applied only to the start_time of the event.
**Rationale**: Generally date ranges like this are mostly useful for finding when events start within a range with the `end_time` being more informational

## 9. Data Seeding
**Context**: Database needs to be seeded with data in order for the API to return anything interesting.
**Decision**: On startup, the application loads the DB with data from the seed_data.jsonl file
**Rationale**: This is idempotent (if data exists, nothing is loaded) and ensures that the DB is seeded. This application is meant to be run with the docker-compose file, so if it runs outside of that context (or not under text), it will fail to start. 

