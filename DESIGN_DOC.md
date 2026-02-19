# Multinet Server — Design & Optimization Document

**Date:** 2026-02-19
**Author:** Claude (analysis-driven)
**Scope:** Current-state critique, targeted optimizations, and forward-looking improvements

---

## Table of Contents

1. [Current Architecture Summary](#1-current-architecture-summary)
2. [What Works Well](#2-what-works-well)
3. [Critical Issues](#3-critical-issues)
4. [Protocol Redesign](#4-protocol-redesign)
5. [Concurrency & Performance](#5-concurrency--performance)
6. [Authentication & Security](#6-authentication--security)
7. [Persistence Layer](#7-persistence-layer)
8. [Configuration & Deployment](#8-configuration--deployment)
9. [Observability](#9-observability)
10. [Testing Strategy](#10-testing-strategy)
11. [Prioritized Roadmap](#11-prioritized-roadmap)

---

## 1. Current Architecture Summary

```
Client (TCP)
    ↓ text line
TcpServer (virtual thread per client)
    ↓
ClientSession → MessageParser → CommandRegistry → Command.execute()
    ↓ (for mutations)
CommandActor (single-threaded executor per world)
    ↓ 120 Hz ticks
GridWorldEngine.applyIntents() → snapshot()
    ↓ 70 Hz broadcasts
BroadcastToAll → SessionManager → SocketConnection send queue
    ↓
Client (JSON state frames)
```

**Stack:** Java 21, Maven, TCP sockets, virtual threads, Jackson JSON, Log4j2

---

## 2. What Works Well

- **Virtual threads** — correct and modern choice for I/O-bound client connections. No need to change.
- **Actor-per-world** — eliminates lock contention across worlds. Clean model.
- **Move coalescing** — staging only the last move direction per tick avoids redundant work under fast input.
- **Clock abstraction** — `PacedRateClock` dropping ticks instead of queuing them is the right backpressure strategy.
- **Module separation** — `domain / protocol / server` is a reasonable layering. Domain has zero network dependencies.
- **Command pattern** — `CommandRegistry` + `Command` interface is easy to extend.

---

## 3. Critical Issues

### 3.1 No Authentication

**Current:** `AuthCommand` is a stub. Any string is accepted as a player name. Players can impersonate each other by reusing the same name.

**Impact:** Cannot identify a player across reconnects, cannot ban, cannot persist progress.

**Fix:** See [Section 6](#6-authentication--security).

---

### 3.2 No Persistence

**Current:** All state is in-memory. A server restart wipes every player's position.

**Impact:** Zero durability. Incompatible with stateful features (inventory, scores, progression).

**Fix:** See [Section 7](#7-persistence-layer).

---

### 3.3 Hardcoded Configuration

**Current:** Worlds, ports, tick rates, and broadcast rates are literals in `ServerBootstrap` and `Connectors`.

```java
// ServerBootstrap.java — today
new PacedRateClock(120, TimeUnit.MILLISECONDS)
new TcpServer(5555, instances)
```

**Impact:** Impossible to run different environments (dev/staging/prod) without recompiling.

**Fix:** See [Section 8](#8-configuration--deployment).

---

### 3.4 Text Protocol — Fragile and Inefficient

**Current:** Space-delimited plaintext. An 8 KB line limit is the only framing. Binary data, Unicode names with spaces, and versioning are all unsupported.

**Impact:** Hard to evolve the protocol without breaking clients. No compression.

**Fix:** See [Section 4](#4-protocol-redesign).

---

### 3.5 No Rate Limiting

**Current:** A client can flood the server with thousands of MOVE commands per second. Move coalescing in the actor helps, but the parsing and dispatch work still happens on the session thread.

**Impact:** A single misbehaving (or buggy) client can saturate a core.

**Fix:** Token-bucket rate limiter per session, enforced at `ClientSession` before dispatch.

```java
// Proposed addition to ClientSession
private final RateLimiter commandRateLimiter = RateLimiter.of(200); // 200 cmds/sec max

private void processIncoming(String raw) {
    if (!commandRateLimiter.tryAcquire()) {
        log.warn("Rate limit exceeded for session {}", sessionId);
        return; // silently drop or send error
    }
    dispatcher.dispatch(raw);
}
```

---

### 3.6 Unbounded Queue Risk in SocketConnection

**Current:** `SocketConnection` throws an exception when the send queue exceeds 1024 items — this disconnects the client. A slow client on a congested network will always lose.

**Impact:** Legitimate players on slow connections are ejected instead of throttled.

**Fix:** Drop the oldest state frames (they are superseded by newer ones anyway), not the client.

```java
// Instead of throwing when queue is full:
if (sendQueue.size() >= MAX_QUEUE) {
    sendQueue.poll(); // drop oldest (it's stale state anyway)
}
sendQueue.offer(message);
```

This is safe because state frames are not transactional — each is a full snapshot.

---

### 3.7 Broad Exception Swallowing

**Current:**
```java
// TcpServer.startSession()
} catch (Exception e) {
    log.error("session error", e);
}
```

**Impact:** Specific exceptions (auth failure, world-full, malformed message) are all treated identically. No structured error reporting to the client.

**Fix:** Define a typed exception hierarchy and handle each case explicitly.

```
MultinetException
├── ProtocolException       (malformed message)
├── WorldException
│   ├── WorldFullException
│   └── WorldNotFoundException
├── AuthException
└── SessionException
```

---

## 4. Protocol Redesign

### 4.1 The Problem with the Current Protocol

The current protocol is a good starting point but has fundamental limitations:

| Issue | Detail |
|-------|--------|
| No versioning | Can't evolve without breaking existing clients |
| Space-delimited | Player names with spaces break parsing |
| Line-based | Arbitrary 8KB limit; no binary data |
| Text JSON state | ~300–600 bytes per frame, all entities, every tick |
| No request IDs | Can't correlate response to request |
| No heartbeat | Server can't detect silent client disconnects |

### 4.2 Recommended: Length-Prefixed Binary Frames

Switch to a simple binary framing layer that wraps the existing JSON payload. This is the minimal change with maximum compatibility.

```
┌──────────────┬──────────────┬───────────────────────┐
│  Magic (2B)  │  Length (4B) │  Payload (N bytes)    │
│  0xMN 0xET  │  big-endian  │  JSON or binary body  │
└──────────────┴──────────────┴───────────────────────┘
```

This fixes:
- Arbitrary message sizes (no 8KB line limit)
- Binary payloads if needed in the future
- Unambiguous message boundaries

### 4.3 Protocol Versioning

Add a version handshake on connect:

```
Server → Client:  { "proto": 2, "features": ["zstd", "delta"] }
Client → Server:  { "proto": 2, "features": ["zstd"] }
```

Both sides negotiate the minimum common feature set. Old clients can still connect at `proto: 1`.

### 4.4 Delta State Frames

**Current:** Every broadcast sends the full world state (all entities, all positions).

**Proposed:** Only send what changed since the last acknowledged frame.

```json
// Full frame (on join or when client requests resync)
{ "seq": 1, "full": true, "entities": [...all...] }

// Delta frame (normal tick)
{ "seq": 2, "delta": { "moved": [{"id":3,"x":5,"y":2}], "left": [7] } }
```

**Estimated bandwidth reduction:** 60–80% for worlds with 10+ entities where most are stationary.

The `seq` field enables client-side recovery: if a delta is missed, the client sends `{ "cmd": "resync" }` and the server sends a full frame.

### 4.5 Compression

For large worlds or many entities, add optional per-message zstd compression (negotiated during handshake). At 70 Hz, even 10% compression saves CPU on the send path.

### 4.6 Heartbeat / Keep-alive

Add a server-sent PING every 5 seconds. If no PONG is received within 10 seconds, close the session. This frees resources from ghost connections (client crashed without FIN).

```java
// ServerInstance tick listener
if (tick % (TICK_RATE * 5) == 0) {
    sessionManager.broadcastPing();
}
```

---

## 5. Concurrency & Performance

### 5.1 Actor Throughput

**Current:** `CommandActor` uses a single-threaded `ExecutorService`. All world mutations are serialized. This is correct but means throughput is bounded by the actor thread.

**Observation:** At 120 Hz, the actor has ~8.3ms per tick. If `applyIntents()` + `snapshot()` + `broadcast()` takes >8ms, ticks will be dropped by `PacedRateClock`.

**Optimization: Separate the broadcast from the tick**

Currently `CommandActor.step()` does:
1. Flush staged moves
2. Apply intents (world mutation)
3. Take snapshot
4. Broadcast to all sessions

Steps 1–3 must be serialized (they mutate world state). Step 4 (broadcast) does not need to hold the actor thread — the snapshot is immutable.

```java
// Proposed: fire broadcast off-actor
StateFrame snapshot = connector.snapshot();
broadcastExecutor.submit(() -> broadcaster.publish(snapshot, sessionManager));
```

This frees the actor to start the next tick immediately, decoupling simulation rate from I/O rate.

### 5.2 JSON Serialization Cost

**Current:** `JsonProtocolEncoder` serializes a full `StateFrame` to JSON string on every broadcast (70 Hz × number of worlds).

**Optimization A:** Cache the serialized form when the state hasn't changed. If no entity moved this tick, the snapshot is identical to the previous one — reuse the bytes.

```java
private byte[] cachedFrame;
private long cachedFrameSeq = -1;

public byte[] encode(StateFrame frame) {
    if (frame.seq() == cachedFrameSeq) return cachedFrame;
    cachedFrame = mapper.writeValueAsBytes(frame);
    cachedFrameSeq = frame.seq();
    return cachedFrame;
}
```

**Optimization B:** Pre-serialize to `byte[]` once per tick, then write the same byte array to all client sockets. Current code serializes `toString()` per session — if there are 50 clients, the same JSON is built 50 times.

### 5.3 Position Lookup — takenPositions Map

**Current:** `GridWorldState.takenPositions` is a `ConcurrentHashMap<Position, String>`. Every move intent checks this map.

**Alternative:** Since grids are small (max 10×10 = 100 cells), a flat `int[]` array indexed by `(y * width + x)` would be faster than hashing and more cache-friendly. Entity IDs fit in an int.

```java
// Replace ConcurrentHashMap<Position, EntityId> with:
private final int[] occupancy = new int[width * height]; // 0 = empty, else entityId
```

This trades generality for performance on small grids. For larger worlds (future), the map is fine.

### 5.4 findRandomFreePosition — O(grid) Worst Case

**Current:** Repeatedly picks random positions until a free one is found. In a nearly-full grid this is unbounded.

**Fix:** Maintain a `LinkedList<Position> freePositions` in `GridWorldState`, updated on join/leave. `O(1)` to pick a free position, `O(1)` to restore on leave.

### 5.5 Virtual Thread Pinning Risk

**Current:** `SocketConnection.receive()` calls `BufferedReader.readLine()` which is a blocking I/O call. Virtual threads handle this correctly — the carrier thread is not pinned.

**Risk:** If any lock inside the session path is a `synchronized` block (not `ReentrantLock`), the virtual thread will pin its carrier. Audit all `synchronized` usages in hot paths.

```bash
# Check for synchronized blocks in hot-path classes
grep -r "synchronized" protocol/src/main/java/com/ntros/connection/
grep -r "synchronized" protocol/src/main/java/com/ntros/lifecycle/session/
```

Replace any found with `java.util.concurrent.locks.ReentrantLock` or use lock-free data structures.

---

## 6. Authentication & Security

### 6.1 Minimal Auth Flow

The current `AuthCommand` stub can be completed with a token-based scheme requiring zero external dependencies:

```
Client → Server:  AUTH <username> <token>
Server → Client:  AUTH_OK <session-token> | AUTH_FAIL <reason>
```

On first connection (new user), the server generates a random session token and returns it. The client stores it locally. On reconnect, the client presents the token to restore their identity.

This is "poor man's auth" — not cryptographically robust but sufficient to:
- Associate a name with a session
- Detect reconnects
- Enable persistence keyed on player identity

For a real auth layer, add HMAC-signed tokens or delegate to an auth service.

### 6.2 World Access Control

`WorldCapabilities` and `Visibility` already exist but appear unused. Activate them:

- `PUBLIC` — anyone can join
- `PRIVATE` — requires invite token
- `SPECTATE_ONLY` — can observe but not join

Enforce in `JoinCommand` via the existing `CapabilityGuard`.

### 6.3 Input Validation

**Current:** `MessageParser` splits on spaces and passes raw strings to commands. Commands do minimal validation.

**Required additions:**
- Maximum player name length (e.g., 32 characters)
- Allowlist of valid characters in names (alphanumeric + underscore)
- Reject unknown command types with a typed error response (not an exception)

```java
private static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9_]{1,32}$");

if (!VALID_NAME.matcher(playerName).matches()) {
    return Optional.of(ServerResponse.error("INVALID_NAME"));
}
```

---

## 7. Persistence Layer

### 7.1 What Needs to Be Persisted

| Data | Frequency | Storage Need |
|------|-----------|--------------|
| Player identity (name → token) | On auth | Low-write key-value |
| Player last position | On disconnect | Low-write key-value |
| World configuration | On startup | Static config file |
| Player scores/stats | Per game event | Append-only log |

### 7.2 Recommended: Redis for Hot State

Redis is ideal for game servers — in-memory, sub-millisecond, supports TTL for session expiry.

```java
// On player disconnect
redis.hset("player:" + playerId, "world", worldName, "x", x, "y", y);
redis.expire("player:" + playerId, 3600); // 1hr TTL

// On player reconnect
Map<String, String> state = redis.hgetAll("player:" + playerId);
```

**Alternative (zero new dependencies):** Use a simple `players.json` file written on disconnect, read on join. Good enough for single-instance deployments.

### 7.3 World State Snapshots

For persistence of ongoing world state (e.g., game-of-life worlds), periodically snapshot to disk:

```
worlds/
  arena-x/
    snapshot-2026021900.json
    snapshot-2026021912.json
  gol-x/
    snapshot-2026021900.json
```

Load on startup, fall back to defaults if no snapshot found.

---

## 8. Configuration & Deployment

### 8.1 Externalize All Configuration

Replace hardcoded values with an `application.properties` file:

```properties
server.port=5555
server.tick-rate-hz=120
server.broadcast-rate-hz=70
server.max-clients=1000

world.arena-x.width=10
world.arena-x.height=10
world.arena-x.visibility=PUBLIC
world.arena-x.max-players=20

world.gol-x.width=50
world.gol-x.height=50
world.gol-x.visibility=PUBLIC
```

Load via a `ConfigLoader` that reads from the file, with env-variable overrides for containerized deployments:

```java
int port = Integer.parseInt(
    System.getenv().getOrDefault("SERVER_PORT",
        props.getProperty("server.port", "5555"))
);
```

### 8.2 Graceful Shutdown

**Current:** `TcpServer.stop()` stops instances but does not notify connected clients.

**Add:** A shutdown sequence:
1. Stop accepting new connections
2. Broadcast `SERVER_SHUTDOWN 30` to all clients (give them time to save/react)
3. Wait up to 30 seconds
4. Close all sessions
5. Flush persistence

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    sessionManager.broadcastAll("SERVER_SHUTDOWN 30");
    Thread.sleep(5000); // short grace period
    server.stop();
}));
```

### 8.3 Docker / Container Readiness

Add:
- `Dockerfile` with a minimal JRE 21 base image
- Health check endpoint (even a simple TCP ping-responder on a separate port)
- Structured JSON logging (Log4j2 `JsonLayout`) for log aggregation

---

## 9. Observability

### 9.1 Metrics (No External Dependency)

Add lightweight in-process metrics using JDK's `java.lang.management` APIs or a small custom counter:

```java
public class ServerMetrics {
    public static final AtomicLong totalConnections = new AtomicLong();
    public static final AtomicLong activeConnections = new AtomicLong();
    public static final AtomicLong ticksDropped = new AtomicLong();
    public static final AtomicLong commandsProcessed = new AtomicLong();
    public static final AtomicLong broadcastsSent = new AtomicLong();
}
```

Expose via a `MetricsCommand` (telnet-friendly) or a minimal HTTP server on a separate port.

If external metrics are acceptable, Micrometer with a Prometheus registry adds these with one dependency and zero code changes to the hot path.

### 9.2 Structured Logging

**Current:** Log messages are unstructured strings. Hard to query in production.

**Proposed:** Switch to structured key-value logging:

```java
log.info("player.joined world={} player={} position={},{} total_in_world={}",
    worldName, playerName, x, y, playerCount);
```

Or use Log4j2's `MapMessage` for true structured output.

### 9.3 Tick Timing Diagnostics

Add a moving average of tick execution time in `PacedRateClock`. Log a warning if the average exceeds 50% of the tick interval:

```java
if (avgTickMs > tickIntervalMs * 0.5) {
    log.warn("Tick execution is slow: avg={}ms budget={}ms", avgTickMs, tickIntervalMs);
}
```

This surfaces performance regressions before they cause visible lag.

---

## 10. Testing Strategy

### 10.1 Current Gaps

- No integration test that exercises the full TCP stack
- No load test
- Clock tests are thorough; command/session tests are thin
- `TestClient` exists but is not wired into the test suite

### 10.2 Integration Test Pattern

```java
@Test
void twoPlayersCanMoveAndSeeEachOther() throws Exception {
    try (TestClient alice = TestClient.connect("localhost", 5555);
         TestClient bob   = TestClient.connect("localhost", 5555)) {

        alice.send("JOIN world-1 alice");
        bob.send("JOIN world-1 bob");

        alice.send("MOVE N");

        StateFrame aliceView = alice.awaitFrame();
        StateFrame bobView   = bob.awaitFrame();

        assertThat(aliceView.entities()).contains(entityAt("alice", 0, -1));
        assertThat(bobView.entities()).contains(entityAt("alice", 0, -1));
    }
}
```

### 10.3 Load Test

Use a single test that spawns N virtual threads, each running a `TestClient` in a tight move loop for 10 seconds. Assert:
- Server stays alive
- No ticks dropped (or < 1%)
- No `OutOfMemoryError`
- All clients disconnected cleanly

```java
@Test
void serverHandles100ConcurrentPlayers() throws Exception {
    int players = 100;
    CountDownLatch ready = new CountDownLatch(players);
    CountDownLatch done  = new CountDownLatch(players);

    for (int i = 0; i < players; i++) {
        int id = i;
        Thread.startVirtualThread(() -> {
            try (TestClient c = TestClient.connect("localhost", 5555)) {
                c.send("JOIN load-world player-" + id);
                ready.countDown();
                ready.await();
                for (int t = 0; t < 1000; t++) {
                    c.send("MOVE " + randomDirection());
                    Thread.sleep(10);
                }
            } finally {
                done.countDown();
            }
        });
    }
    done.await(30, TimeUnit.SECONDS);
    assertThat(ServerMetrics.ticksDropped.get()).isLessThan(100);
}
```

### 10.4 Chaos Tests

- Kill the socket mid-write → verify server recovers, session cleaned up
- Send a 9KB line → verify it's rejected without crashing
- Send 10,000 commands in 1ms → verify rate limiter triggers

---

## 11. Prioritized Roadmap

Items are sorted by **impact / effort ratio**. Do these in order.

### Tier 1 — Quick Wins (Low Effort, High Impact)

| # | Change | Effort | Impact |
|---|--------|--------|--------|
| 1 | Drop stale frames instead of disconnecting slow clients | 1 hour | Stability |
| 2 | Pre-serialize state frame once per tick, reuse bytes for all sessions | 2 hours | CPU |
| 3 | Rate-limit commands per session (token bucket, 200/sec) | 2 hours | Security/Stability |
| 4 | Externalize config to `application.properties` | 3 hours | Ops |
| 5 | Add typed exception hierarchy, remove broad catches | 3 hours | Reliability |
| 6 | Input validation on player names | 1 hour | Security |
| 7 | Heartbeat / keep-alive (detect ghost connections) | 2 hours | Stability |

### Tier 2 — Protocol & Persistence (Medium Effort, High Impact)

| # | Change | Effort | Impact |
|---|--------|--------|--------|
| 8 | Complete `AuthCommand` with token-based identity | 1 day | Feature |
| 9 | Length-prefixed binary framing | 1 day | Protocol |
| 10 | Delta state frames | 2–3 days | Bandwidth |
| 11 | Simple file-based persistence (player last position) | 1 day | Feature |
| 12 | Graceful shutdown with client notification | 1 day | Ops |

### Tier 3 — Observability & Testing (Medium Effort, High Value)

| # | Change | Effort | Impact |
|---|--------|--------|--------|
| 13 | Integration test suite with `TestClient` | 2 days | Quality |
| 14 | Load test (100 concurrent players) | 1 day | Quality |
| 15 | Structured logging + tick timing diagnostics | 1 day | Ops |
| 16 | In-process metrics (active sessions, ticks dropped) | 1 day | Ops |

### Tier 4 — Architectural (High Effort, Future-Proofing)

| # | Change | Effort | Impact |
|---|--------|--------|--------|
| 17 | Decouple broadcast from actor thread | 1–2 days | Performance |
| 18 | Redis-backed persistence | 2–3 days | Feature |
| 19 | Flat array occupancy grid (replace Position map) | 1 day | CPU |
| 20 | Activate `WorldCapabilities` / access control | 2 days | Feature |
| 21 | Protocol version negotiation handshake | 2 days | Protocol |

---

## Appendix A — What NOT to Change

These are things that look like targets for refactoring but should be left alone:

- **Virtual threads per client** — correct and idiomatic for Java 21
- **Actor-per-world** — the right model; adding parallelism inside a world would require complex conflict resolution
- **Move coalescing (last-write-wins)** — correct for a physics-less grid game; don't accumulate moves
- **`PacedRateClock` tick-dropping** — the right backpressure strategy; don't switch to queuing
- **Module structure (domain/protocol/server)** — clean; no need to merge or split further
- **`CommandRegistry` as static map** — fine for this scale; don't over-engineer with a DI framework

---

*This document reflects the codebase state as of 2026-02-19. Revisit Tier 4 items after Tier 1–2 are complete.*
