# Pagination Invariants and Verification

This document details the core invariants of our keyset pagination implementation and provides concrete examples of the API behavior.

## Core Invariants

This pagination logic is built on **Keyset Pagination** (also known as the "Seek Method"). This approach guarantees the following properties:

### 1. Total Count Invariance
The sum of the number of events returned across all pages of a paginated query must exactly equal the total number of matching events reported by the database for that same query without pagination.
*   **Verification**: Integration tests verify that `sum(page_sizes) == total_count`.

### 2. Uniqueness (No Duplicates)
No event ID shall appear more than once when paginating through a result set, even if the result set is thousands of items long.
*   **Verification**: Integration tests use a `Set` to collect all IDs across pages and assert that every ID added is "new" (not already present).

### 3. Completeness (No Missing Events)
Every event that satisfies the query filters must appear in exactly one page of the result set.
*   **Verification**: Verified by combining Invariants 1 and 2.

### 4. Order Stability
Results are strictly ordered by `(start_time ASC, id ASC)`. The ID tie-breaker is essential because many events share the exact same start time.
*   **Verification**: The SQL `ORDER BY start_time ASC, id ASC` ensures this at the database level.

---

## Concrete Examples

### Example 1: Initial Page Request
**Request**: `GET /events?startTime=0&endTime=2000000000&limit=2`

**Expected Behavior**: Returns the first 2 events in chronological order.

**Response Body Snippet**:
```json
{
  "events": [
    {
      "id": "evt-0001",
      "start_time": 1722096000,
      "title": "Painting Workshop at House of Blues Boston",
      ...
    },
    {
      "id": "evt-0002",
      "start_time": 1722096000,
      "title": "Film Screening at The Sinclair",
      ...
    }
  ],
  "pagination": {
    "next_cursor": "eyJzdGFydFRpbWUiOjE3MjIwOTYwMDAsImV2ZW50SWQiOiJldnQtMDAwMiJ9",
    "total_count": 2000
  }
}
```

### Example 2: Following the Cursor
**Request**: `GET /events?startTime=0&endTime=2000000000&limit=2&cursor=eyJzdGFydFRpbWUiOjE3MjIwOTYwMDAsImV2ZW50SWQiOiJldnQtMDAwMiJ9`

**Expected Behavior**: Returns the *next* 2 events (evt-0003, evt-0004) without repeating evt-0002.

**Response Body Snippet**:
```json
{
  "events": [
    {
      "id": "evt-0003",
      "start_time": 1722096000,
      "title": "Painting Workshop at Brighton Music Hall",
      ...
    },
    {
      "id": "evt-0004",
      "start_time": 1722096000,
      "title": "Theater Performance at House of Blues Boston",
      ...
    }
  ],
  "pagination": {
    "next_cursor": "eyJzdGFydFRpbWUiOjE3MjIwOTYwMDAsImV2ZW50SWQiOiJldnQtMDAwNCJ9",
    "total_count": 2000
  }
}
```

### Example 3: Filtered Pagination
**Request**: `GET /events?startTime=0&endTime=2000000000&limit=2&category=music`

**Expected Behavior**: Returns the first 2 events that match the `music` category. The `total_count` will reflect only matching events.

**Response Body Snippet**:
```json
{
  "events": [...],
  "pagination": {
    "next_cursor": "...",
    "total_count": 255
  }
}
```

---

## Error Handling

| Case | Status | Reason |
|------|--------|--------|
| Malformed Cursor | `400 Bad Request` | Cursor is not valid Base64 or contains invalid JSON data. |
| Missing `startTime` | `400 Bad Request` | Required range parameter missing. |
| `startTime > endTime` | `400 Bad Request` | Invalid logical time range. |
