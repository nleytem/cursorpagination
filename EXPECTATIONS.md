# Expectations & Clarifications

This document clarifies what we expect from your submission. It is referenced from the main [README](README.md).

---

## API type

The deliverable is a **remote API** that clients call over the network, not an in-process Java API. Concretely:

- Build a **single HTTP endpoint** (e.g. REST). Clients should be able to call it with `curl`, Postman, or any HTTP client.
- We are **not** asking for only a Java interface or a class that another service calls in-process. The endpoint must be runnable (e.g. start a server) and callable over HTTP.

---

## Database

You **must** use a database. Loading the seed data into an in-memory `List` or `Map` and querying it with Java streams is not acceptable. We want to see:

- A data model you designed yourself.
- Database-level pagination (e.g. queries with appropriate clauses for ordering and limiting), not post-fetch slicing in Java.
- A sensible approach to storing the nested `location` object and the `tags` array from the seed data.

You choose the database technology (PostgreSQL, MySQL, MongoDB, H2, etc.). Include setup instructions so we can run it locally.

---

## Tests: unit vs integration

Your tests should run via your build tool (e.g. Maven `mvn test`, Gradle `test`) with no manual steps. We do not prescribe unit-only or integration-only:

- **Unit tests** — Testing your pagination/cursor logic (e.g. a service or helper) in isolation, with the data layer stubbed or in-memory, is fine and often quick to run.
- **Integration tests** — Testing the full stack by issuing HTTP requests to your endpoint and asserting on the response are also fine and demonstrate end-to-end correctness.

Use whichever mix best proves that pagination is correct (totals match, no duplicates, no missing events). Both are valued.

**Filter correctness:** Your tests must also verify that the pagination invariants (no duplicates, no missing events, totals match) hold when `category`, `city`, and/or `tags` filters are applied — not just for unfiltered queries.

---

## Javadoc

Include **proper Javadoc for all public classes and all public methods**. We use this to assess how you document APIs and code design. Describe purpose, parameters, return values, and any notable behavior or constraints.

---

## Libraries and frameworks

- **Spring** — Allowed. Use it if you prefer.
- **Guava** — **Not allowed.** Do not add Guava as a dependency or use it in your solution. Use the JDK and other libraries instead.
- **Hibernate / Spring Data JPA / HQL / JPQL** — **Not allowed.** Hibernate is explicitly excluded because it abstracts away the SQL that matters most for this exercise — keyset pagination correctness depends on the exact query shape, index usage, and ordering clauses, which an ORM will obscure or generate incorrectly. Use raw JDBC, `JdbcTemplate`, `NamedParameterJdbcTemplate`, jOOQ, or direct SQL instead.

All other technical choices (build tool, test framework, DB driver, etc.) are up to you.

---

## Design rationale

Include an `ADR.md` that documents the significant design decisions you made and why. We are as interested in your reasoning as in the working endpoint — explain the trade-offs you considered, not just the choices you landed on.

---

## Observability

The endpoint must emit structured logs. What you log and how you structure it is up to you — we want to see that you thought about how this service would be operated, not just that it works in a test.
