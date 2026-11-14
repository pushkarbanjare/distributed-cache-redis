# Distributed Cache with Write-Through / Write-Behind (Redis)

A Redis-backed caching layer in front of PostgreSQL, implementing cache-aside
reads and two write-consistency strategies — Write-Through and Write-Behind —
along with eviction policy tuning and concurrency-safe writes.

## Why this exists

High-traffic systems can't serve every read from the database directly — Redis
caches hot data in memory instead. But caching introduces a real problem: how
do writes stay in sync between the fast cache and the durable database? This
project implements and compares the two standard answers to that problem.

## Strategies implemented

- **Cache-Aside (read path)** — check Redis first; on a miss, read from Postgres
  and populate the cache for next time
- **Write-Through** — every write updates Postgres AND Redis together,
  synchronously, before returning to the caller. Guarantees the cache is never
  stale, at the cost of extra write latency.
- **Write-Behind** — writes update Redis immediately and return fast; the
  actual database write is queued and flushed asynchronously in batches every
  5 seconds. Faster writes, but introduces a real (small) data-loss risk if the
  app crashes before a queued write flushes, and a brief eventual-consistency
  window.

## Concurrency & reliability safeguards

- **Per-key write locking** — concurrent writes to the *same* product are
  serialized (via a per-ID lock), preventing the cache and DB from ending up
  with different values after a race; writes to *different* products are
  completely unaffected and run in parallel
- **Bounded write-behind queue** — capped at 1000 pending writes; once full,
  further writes are explicitly dropped (and logged) rather than growing
  memory unboundedly — a deliberate backpressure decision
- **Redis eviction policy** — `allkeys-lru`, with a memory cap, so Redis
  gracefully evicts least-recently-used entries instead of erroring out once
  full

## Tech stack

Java 21, Spring Boot 4.0, Redis (Lettuce), PostgreSQL, Spring Data JPA, Lombok,
JUnit 5, Awaitility

## API

| Endpoint | Description |
|---|---|
| `GET /products/{id}` | Cache-aside read |
| `POST /products` | Create/update via Write-Through |
| `PUT /products/{id}` | Update via Write-Through |
| `DELETE /products/{id}` | Delete from both DB and cache |
| `POST /products/write-behind` | Create via Write-Behind (async DB flush) |

## Testing

- `WriteThroughConsistencyTest` — 20 concurrent writes to the same product;
  asserts cache and DB always agree afterward (proves the per-key lock works)
- `WriteBehindBehaviorTest` — asserts the DB is NOT updated immediately after
  a write-behind call, but IS updated once the flush interval passes
- Manual eviction test — seeding past the memory cap triggers non-zero
  `evicted_keys`
- Manual overflow test — 1500 concurrent write-behind calls against a
  1000-capacity queue triggers explicit drop logging

## Design trade-offs

Write-Through favors **consistency over latency** — correct for data where
staleness is unacceptable (e.g. account balances, inventory counts).
Write-Behind favors **latency over durability/consistency** — correct for
high-volume, loss-tolerant data (e.g. view counts, analytics events). This
mirrors the write-through/write-back policies used in CPU and OS-level caches
— the same fundamental trade-off appears at every layer of a computing system,
not just application-level caching.