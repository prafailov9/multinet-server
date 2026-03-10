# Multinet Server — Upgrade Stories

> Generated: 2026-03-10
> Project: multinet-server (Java 21 multi-module TCP game server)
> Modules: domain · protocol · server

---

## EPIC-1 · Correctness & Bug Fixes

---

### STORY-101 · Fix `Velocity2D.normalize()` returning null

**Type:** Bug
**Priority:** Critical
**Module:** domain
**File:** `domain/src/main/java/com/ntros/model/entity/movement/Velocity2D.java`

**Description:**
`Velocity2D.normalize()` unconditionally returns `null`. Any caller treating the return value as non-null will throw a `NullPointerException` at runtime.

**Acceptance Criteria:**
- [ ] `normalize()` returns a `Velocity2D` whose magnitude is within `±0.001` of `1.0` for non-zero vectors
- [ ] `normalize()` returns a zero-velocity for a zero-magnitude vector (no division-by-zero)
- [ ] Unit test covers both cases (normal vector + zero vector)

**Notes:**
Compare with `Vector2D.normalize()` to confirm expected behaviour and reuse the pattern.

---

### STORY-102 · Fix `GridWorldState.getTileTypeAt()` returning null

**Type:** Bug
**Priority:** Critical
**Module:** domain
**File:** `domain/src/main/java/com/ntros/model/cfgWorld/state/GridWorldState.java`

**Description:**
`getTileTypeAt(x, y)` returns `null` unconditionally. Callers in `GridWorldEngine` that check tile type for collision/movement validity will silently fail or throw NPEs.

**Acceptance Criteria:**
- [ ] Returns the correct `TileType` for coordinates that exist in the tile map
- [ ] Returns `TileType.EMPTY` (or throws `IllegalArgumentException`) for out-of-bounds coordinates — behaviour documented in Javadoc
- [ ] Unit test covers valid coords, missing tile key, and out-of-bounds coords

---

### STORY-103 · Fix `ApplyMoveStrategy.move()` returning null

**Type:** Bug
**Priority:** High
**Module:** protocol
**File:** `protocol/src/main/java/com/ntros/lifecycle/instance/actor/movestrategy/ApplyMoveStrategy.java`

**Description:**
`ApplyMoveStrategy.move()` returns `null` instead of an `Optional<ServerResponse>`. This breaks the `MoveStrategy` contract and will NPE anywhere the strategy is selected.

**Acceptance Criteria:**
- [ ] Returns `Optional.empty()` or a populated `Optional<ServerResponse>` — never `null`
- [ ] The immediate-apply logic is actually implemented (apply move to cfgWorld state synchronously)
- [ ] Unit test verifies both the success path and invalid-move path

---

### STORY-104 · Fix `SessionContext` field visibility under concurrent access

**Type:** Bug
**Priority:** High
**Module:** protocol
**File:** `protocol/src/main/java/com/ntros/message/SessionContext.java`

**Description:**
`SessionContext` fields (`worldId`, `entityId`, `role`, `joinedAt`, etc.) are written from the Actor thread and read from the session/clock threads. Without `volatile` or synchronization they are subject to stale-read visibility bugs on multi-core JVMs. Only `authenticated` appears to be atomic; the rest are plain fields.

**Acceptance Criteria:**
- [ ] All mutable `SessionContext` fields that are written by one thread and read by another are either `volatile`, backed by `AtomicReference`, or the class is immutable with copy-on-write semantics
- [ ] An explanatory comment states the threading model (writer = Actor, reader = session/clock)
- [ ] No existing tests regress

---

### STORY-105 · Enforce `MoveStrategy` null-safety contract via interface default

**Type:** Bug / Improvement
**Priority:** Medium
**Module:** protocol

**Description:**
The `MoveStrategy` interface does not specify that `move()` must not return `null`. Add a default wrapper or a test utility that validates this contract so future implementors get an early failure rather than a downstream NPE.

**Acceptance Criteria:**
- [ ] Javadoc on `MoveStrategy.move()` explicitly states "must not return null; return `Optional.empty()` for no-op moves"
- [ ] A `MoveStrategyContract` test base class validates the non-null postcondition for all implementations

---

## EPIC-2 · Incomplete Implementations

---

### STORY-201 · Implement `StateCommand`

**Type:** Feature
**Priority:** High
**Module:** protocol
**File:** `protocol/src/main/java/com/ntros/command/impl/StateCommand.java`

**Description:**
`StateCommand.execute()` returns `Optional.empty()`. Clients that send a `STATE` command receive no response, leaving them with no on-demand way to poll current cfgWorld state.

**Acceptance Criteria:**
- [ ] Calling `STATE <worldId>` returns a `ServerResponse` whose payload is the current serialised `GridSnapshot` for that cfgWorld
- [ ] Command validates that the session is authenticated and has a valid `worldId`
- [ ] Unit test covers: authenticated + valid cfgWorld → state returned; unauthenticated → error response

---

### STORY-202 · Implement `WelcomeCommand` and `ErrorCommand`

**Type:** Feature
**Priority:** Medium
**Module:** protocol
**Files:** `WelcomeCommand.java`, `ErrorCommand.java`

**Description:**
Both command handlers return `Optional.empty()`. They are registered in `CommandRegistry` but do nothing. `WelcomeCommand` should return a welcome banner and server metadata; `ErrorCommand` should echo a structured error response.

**Acceptance Criteria:**
- [ ] `WelcomeCommand` returns a `ServerResponse` containing server version, available cfgWorlds, and accepted commands
- [ ] `ErrorCommand` returns a `ServerResponse` with `success=false` and a human-readable reason derived from the message args
- [ ] Unit tests for each

---

### STORY-203 · Implement `AbstractDynamicEntity` body methods

**Type:** Feature
**Priority:** Medium
**Module:** domain
**File:** `domain/src/main/java/com/ntros/model/entity/dynamic/AbstractDynamicEntity.java`

**Description:**
All `DynamicEntity` interface methods on `AbstractDynamicEntity` have empty bodies (return defaults / no-ops). The class is the intended base for physics-capable entities (players in open-cfgWorld mode, vehicles).

**Acceptance Criteria:**
- [ ] `getPosition()`, `getVelocity()`, `getAcceleration()`, `getMaxSpeed()`, `getRotation()` return stored state
- [ ] `setPosition()`, `setVelocity()`, `setAcceleration()`, `setRotation()` update stored state
- [ ] All fields are thread-safe (volatile or synchronized) consistent with the concurrency model
- [ ] Unit test validates get/set round-trip for each property

---

### STORY-204 · Complete `CreateRequest` fields

**Type:** Feature / Cleanup
**Priority:** Medium
**Module:** domain
**File:** `domain/src/main/java/com/ntros/model/cfgWorld/protocol/request/CreateRequest.java`

**Description:**
`CreateRequest` has a `// TODO` comment noting missing fields. `CreateCommand` currently only supports `GRID` cfgWorld type and ignores many possible creation parameters. The request model should capture the full intent.

**Acceptance Criteria:**
- [ ] `CreateRequest` includes: `worldName`, `worldType`, `width`, `height`, `maxPlayers`, `visibility`, `shared` (boolean), `tps`, `broadcastHz`
- [ ] `CreateCommand` parses all supported fields from the message args
- [ ] Unsupported `worldType` values return a structured error instead of silently defaulting
- [ ] Unit test for each supported field combination

---

### STORY-205 · Implement `OpenWorldEngine`

**Type:** Feature
**Priority:** Low
**Module:** domain
**File:** `domain/src/main/java/com/ntros/model/cfgWorld/engine/OpenWorldEngine.java`

**Description:**
`OpenWorldEngine` is an empty class implementing `DynamicWorldEngine` (which is itself an empty interface). The open-cfgWorld type is reserved but non-functional.

**Acceptance Criteria:**
- [ ] `DynamicWorldEngine` defines: `applyIntents()`, `joinEntity()`, `removeEntity()`, `serialize()` (mirror of `WorldEngine`)
- [ ] `OpenWorldEngine` implements these using continuous-space (float) positions rather than grid cells
- [ ] Collision detection uses bounding-circle or AABB checks against static obstacles
- [ ] `OpenWorldState` is populated with entity positions and serialised to JSON
- [ ] Unit tests for entity joining, movement, collision, and removal

---

### STORY-206 · Implement `TrafficConnector` and `TrafficEngine`

**Type:** Feature
**Priority:** Low
**Module:** domain

**Description:**
The traffic simulation domain model (`Vehicle`, `Signal`, `RoadNode`, `RoadLink`, intents) is fully defined but the `TrafficConnector` and `TrafficEngine` are stubs. A functioning traffic sim would validate the domain model design.

**Acceptance Criteria:**
- [ ] `TrafficEngine.step()` advances vehicle positions along road links
- [ ] `TrafficEngine.enqueueIntent()` accepts and applies `SpawnVehicle`, `DespawnVehicle`, `SetAcceleration`
- [ ] `TrafficConnector.apply(WorldOp)` converts `JoinOp/MoveOp/RemoveOp` to traffic intents
- [ ] `TrafficConnector.snapshot()` returns a serialisable `TrafficState`
- [ ] Unit tests for vehicle spawn, movement step, and despawn

---

### STORY-207 · Implement Game of Life cfgWorld type

**Type:** Feature
**Priority:** Low
**Module:** domain / protocol

**Description:**
`WorldType.GAME_OF_LIFE` is defined and three `gol-*` cfgWorlds are registered in `ServerBootstrap`, but no engine or connector implements the cellular-automaton rules.

**Acceptance Criteria:**
- [ ] A `GameOfLifeEngine` applies Conway's rules each tick
- [ ] A `GameOfLifeConnector` wraps the engine and exposes the `WorldConnector` interface
- [ ] Players can "plant" cells via `JoinOp` (place a live cell at their spawn position)
- [ ] State is serialised as a flat tile map (`EMPTY`/`WALL` encoding for dead/alive)
- [ ] Unit tests for a known initial pattern (e.g., blinker, glider)

---

## EPIC-3 · Architecture & Design

---

### STORY-301 · Extract `Constants` class (resolve AuthCommand TODO)

**Type:** Improvement
**Priority:** Medium
**Module:** protocol
**File:** `protocol/src/main/java/com/ntros/command/impl/AuthCommand.java`

**Description:**
`AuthCommand` has a `// TODO: add Constants class` comment. Magic strings like `"CLIENT_AUTHENTICATED"` and `"NOT_IN_WORLD"` are scattered across commands and tests.

**Acceptance Criteria:**
- [ ] A `ProtocolConstants` class (or interface with constants) centralises all protocol-level magic strings
- [ ] `AuthCommand`, `JoinCommand`, `DisconnectCommand`, and any other callers reference `ProtocolConstants.*` instead of literals
- [ ] No existing tests regress

---

### STORY-302 · Replace static mutable singletons with dependency injection

**Type:** Refactor
**Priority:** Medium
**Module:** domain / protocol

**Description:**
`Connectors`, `Instances`, `CommandRegistry`, and `IdSequenceGenerator` are static mutable singletons. This makes unit testing difficult (state leaks between tests) and prevents multiple server instances in the same JVM.

**Acceptance Criteria:**
- [ ] Each registry is an injectable, instantiable class (e.g., constructed with `new`)
- [ ] `ServerBootstrap` constructs and wires instances instead of relying on static initialisers
- [ ] All unit tests that currently depend on static state use local instances or proper `@BeforeEach` / `@AfterEach` reset
- [ ] The Singleton `IdSequenceGenerator` becomes a scoped bean (per-server or per-test)

---

### STORY-303 · Document the wire protocol

**Type:** Documentation
**Priority:** Medium
**Module:** protocol

**Description:**
The text-based protocol is implied by the code but never formally documented. New contributors and client authors must reverse-engineer it from `MessageParser`, `CommandRegistry`, and `SocketConnection`.

**Acceptance Criteria:**
- [ ] A `PROTOCOL.md` at repo root documents:
  - Message format: `COMMAND arg1 arg2\n`
  - All client commands with args and expected server responses
  - All server push messages (STATE frame format, JSON schema)
  - Authentication flow (sequence diagram)
  - Error codes / error response format
  - Backpressure limits (8 KB line limit, 1024 queue depth)
- [ ] The document is accurate against the current implementation

---

### STORY-304 · Introduce a `WorldRegistry` interface to replace `Connectors` static map

**Type:** Refactor
**Priority:** Medium
**Module:** domain

**Description:**
`Connectors` is a class with a static `HashMap<String, WorldConnector>` and static methods. There is no interface, making it impossible to mock in tests or swap implementations.

**Acceptance Criteria:**
- [ ] `WorldRegistry` interface with `register(name, connector)`, `get(name)`, `list()`, `remove(name)`
- [ ] `InMemoryWorldRegistry` is the default implementation backed by `ConcurrentHashMap`
- [ ] `Connectors` becomes a deprecated thin shim delegating to a default `InMemoryWorldRegistry`, removed in the next sprint
- [ ] All call sites migrated

---

### STORY-305 · Remove commented-out Quake 3 inverse sqrt code

**Type:** Cleanup
**Priority:** Low
**Module:** domain
**File:** `domain/src/main/java/com/ntros/model/entity/movement/Velocity2D.java`

**Description:**
`Velocity2D` contains a large block of commented-out Quake 3 inverse square root code with humorous annotation. The actual `normalize()` returns null (see STORY-101). The commented code is misleading, adds noise, and is not used.

**Acceptance Criteria:**
- [ ] Commented-out block removed
- [ ] If the fast-inverse-sqrt trick is genuinely desired for performance, it is implemented correctly with a unit test — otherwise `1.0 / Math.sqrt(magnitudeSquared)` is sufficient
- [ ] File has no other commented-out code blocks

---

### STORY-306 · Add structured logging context (MDC) per session

**Type:** Improvement
**Priority:** Medium
**Module:** protocol

**Description:**
Log statements throughout `ClientSession`, `CommandActor`, and command handlers lack a shared session/cfgWorld identifier. Correlating logs for a single client across threads is difficult.

**Acceptance Criteria:**
- [ ] `ClientSession` sets `MDC.put("sessionId", ...)` and `MDC.put("worldId", ...)` at session start
- [ ] MDC is cleared in a `finally` block when the session ends
- [ ] Log4j2 pattern includes `%X{sessionId}` and `%X{worldId}`
- [ ] Virtual-thread-safe: MDC cleared per thread context (Log4j2's `CloseableThreadContext` preferred)

---

### STORY-307 · Model server-to-client error responses consistently

**Type:** Improvement
**Priority:** Medium
**Module:** protocol

**Description:**
Error responses are ad hoc: some commands return `Optional.empty()`, some return a `ServerResponse` with `success=false`, some throw exceptions caught upstream. There is no uniform error envelope.

**Acceptance Criteria:**
- [ ] All command `execute()` methods return `Optional<ServerResponse>` where `Optional.empty()` means "no response needed" (valid for fire-and-forget) and a populated `Optional` carries either a success or structured error
- [ ] `ServerResponse` or a new `ErrorResponse` includes a machine-readable `errorCode` enum (e.g., `NOT_AUTHENTICATED`, `WORLD_FULL`, `INVALID_DIRECTION`, `UNKNOWN_COMMAND`)
- [ ] Clients receive a parseable error they can act on rather than a raw exception message string

---

## EPIC-4 · Testing

---

### STORY-401 · Increase unit test coverage for command handlers

**Type:** Testing
**Priority:** High
**Module:** protocol

**Description:**
Only `AuthCommand` has a meaningful unit test. `JoinCommand`, `MoveCommand`, `DisconnectCommand`, `CreateCommand` are exercised only through integration or not at all.

**Acceptance Criteria:**
- [ ] Unit tests exist for `JoinCommand`: cfgWorld-not-found, cfgWorld-full, join-timeout, successful join
- [ ] Unit tests exist for `MoveCommand`: unauthenticated, invalid direction, successful stage
- [ ] Unit tests exist for `DisconnectCommand`: clean disconnect removes session and stops cfgWorld when empty
- [ ] Unit tests exist for `CreateCommand`: GRID creation with valid args, invalid cfgWorld type
- [ ] All tests use Mockito mocks for `Session`, `WorldConnector`, and `Instances`

---

### STORY-402 · Add `GridWorldEngine` edge-case tests

**Type:** Testing
**Priority:** High
**Module:** domain

**Description:**
`GridWorldEngine` tests cover the happy path but miss boundary and error cases that matter for game correctness.

**Acceptance Criteria:**
- [ ] Test: player moves into a `WALL` tile → position unchanged
- [ ] Test: player moves into a `WATER` tile → position unchanged (if water is blocking)
- [ ] Test: player moves to grid boundary → position clamped / unchanged
- [ ] Test: two players attempt to move to the same cell in the same tick → one or both denied (define and document the policy)
- [ ] Test: entity removed mid-tick does not appear in snapshot

---

### STORY-403 · Add `SocketConnection` unit tests

**Type:** Testing
**Priority:** Medium
**Module:** protocol
**File:** `protocol/src/main/java/com/ntros/connection/SocketConnection.java`

**Description:**
`SocketConnection` has zero unit tests. Its logic for async send queue, backpressure, timeout, and CR-stripping has no regression coverage.

**Acceptance Criteria:**
- [ ] Test: `receive()` strips `\r` from lines
- [ ] Test: `receive()` returns `"_TIMEOUT_"` when socket read times out
- [ ] Test: `send()` rejects messages when queue exceeds 1024 items (backpressure)
- [ ] Test: `close()` drains the send queue (or documents that it doesn't) and closes the socket
- [ ] Use in-memory socket pairs (`ServerSocket` + `Socket` on loopback) or a custom `Socket` stub

---

### STORY-404 · Add integration test for full client session lifecycle

**Type:** Testing
**Priority:** Medium
**Module:** server

**Description:**
`ServerBootstrapTest` and `TestClient` exist but the test coverage of the full AUTHENTICATE → JOIN → MOVE → DISCONNECT flow is unclear.

**Acceptance Criteria:**
- [ ] Integration test: `TestClient` sends `AUTHENTICATE`, `JOIN cfgWorld-1`, `MOVE UP` × 3, `DISCONNECT`
- [ ] Server responds with `AUTH_SUCCESS`, `WELCOME`, `ACK`, `ACK`×3, and session is cleanly removed
- [ ] Test verifies the server can handle 10 concurrent `TestClient` sessions without deadlock (use a `CountDownLatch`)
- [ ] Test runs in < 5 s (use `PacedRateClock` with reduced TPS for speed)

---

### STORY-405 · Add clock determinism tests for all three clock types

**Type:** Testing
**Priority:** Medium
**Module:** protocol

**Description:**
`FixedDelayClockTest` exists; `FixedRateClock` and `PacedRateClock` lack equivalent deterministic tests.

**Acceptance Criteria:**
- [ ] `FixedRateClockTest` verifies N ticks fire, listener callbacks are called, pause/resume works
- [ ] `PacedRateClockTest` verifies in-flight gating: a slow tick does not cause a second tick to start concurrently
- [ ] All new clock tests use a mocked/manual scheduler to avoid wall-clock flakiness
- [ ] `TickListener.onTickStart`/`onTickEnd` assertions included

---

## EPIC-5 · Performance & Reliability

---

### STORY-501 · Add graceful shutdown timeout to `TcpServer`

**Type:** Improvement
**Priority:** High
**Module:** server
**File:** `server/src/main/java/com/ntros/server/TcpServer.java`

**Description:**
`TcpServer.stop()` iterates sessions and calls cleanup, but it is unclear whether it waits for all virtual threads to terminate before returning. Abrupt JVM shutdown may corrupt in-flight writes.

**Acceptance Criteria:**
- [ ] `stop()` signals all sessions to shut down and awaits their completion up to a configurable timeout (default 5 s)
- [ ] After the timeout, remaining sessions are forcibly closed and a warning is logged
- [ ] `ServerBootstrap` registers a JVM shutdown hook that calls `TcpServer.stop()`
- [ ] Integration test verifies that mid-write sessions do not leave partial frames in the output

---

### STORY-502 · Add dead-letter handling to `CommandActor`

**Type:** Improvement
**Priority:** Medium
**Module:** protocol
**File:** `protocol/src/main/java/com/ntros/lifecycle/instance/actor/CommandActor.java`

**Description:**
`CommandActor.ask()` submits tasks to a single-threaded executor and wraps results in `CompletableFuture`. If the executor is shut down or the task throws, the future completes exceptionally but the caller may not handle it. Dead-letter tasks are silently dropped.

**Acceptance Criteria:**
- [ ] Uncaught exceptions in actor tasks are logged at `ERROR` with session/cfgWorld context
- [ ] A dead-letter counter metric (via Micrometer or a simple `AtomicLong`) tracks dropped tasks
- [ ] `CommandActor.shutdown()` drains the queue and logs how many pending tasks were discarded
- [ ] Unit test: submitting a task to a shut-down actor throws or returns a failed future — not a silent no-op

---

### STORY-503 · Replace `JoinCommand` hard-coded 750 ms timeout with configuration

**Type:** Improvement
**Priority:** Low
**Module:** protocol
**File:** `protocol/src/main/java/com/ntros/command/impl/JoinCommand.java`

**Description:**
The join timeout is hard-coded as `750` milliseconds. Under heavy load, 750 ms may be too short; for tests it is too long.

**Acceptance Criteria:**
- [ ] Timeout is configurable via `Settings` or a `ProtocolConfig` record passed to `JoinCommand`
- [ ] Default value remains 750 ms
- [ ] Test uses a small timeout (e.g., 50 ms) to keep suite fast

---

### STORY-504 · Add health-check endpoint or heartbeat command

**Type:** Feature
**Priority:** Low
**Module:** protocol / server

**Description:**
There is no way for a client or monitoring system to confirm the server is alive without a full session handshake. A lightweight `PING` / `PONG` command would allow load balancers and health checks to operate without authentication.

**Acceptance Criteria:**
- [ ] `PING` command is registered and requires no authentication
- [ ] Server responds with `PONG <serverTimestampMs>`
- [ ] `PING` does not create a session context or affect any cfgWorld state
- [ ] Unit test verifies `PONG` response and timestamp is within ±1 s of wall clock

---

## EPIC-6 · Observability

---

### STORY-601 · Expose per-cfgWorld metrics (TPS, player count, tick latency)

**Type:** Feature
**Priority:** Medium
**Module:** protocol

**Description:**
There is no way to observe runtime behaviour (actual TPS, broadcast rate, session count) without attaching a debugger or reading logs.

**Acceptance Criteria:**
- [ ] `AbstractClock` records measured tick duration via `TickListener.onTickEnd(tickNumber, durationNs)`
- [ ] `ServerInstance` tracks player count and exposes it via a `getMetrics()` method returning a simple record
- [ ] A `STATS` admin command (requires `Role.ORCHESTRATOR`) returns a JSON snapshot of all cfgWorld metrics
- [ ] Metrics are logged at `INFO` level every 30 s in a structured format

---

### STORY-602 · Add `log4j2.xml` configuration file

**Type:** Improvement
**Priority:** Low
**Module:** server

**Description:**
No `log4j2.xml` configuration file is present in the repository. Log4j2 falls back to a default configuration that logs to the console at `ERROR` level only, hiding all `INFO`/`DEBUG` output in production and tests.

**Acceptance Criteria:**
- [ ] `server/src/main/resources/log4j2.xml` configures:
  - Console appender at `INFO` (or configurable via system property)
  - Rolling-file appender at `DEBUG` writing to `logs/multinet.log` with 10 MB rotation
  - Pattern includes `%d{ISO8601} %-5level [%t] %X{sessionId} %X{worldId} %logger{36} - %msg%n`
- [ ] `protocol/src/test/resources/log4j2-test.xml` sets level to `WARN` so tests are quiet by default

---

## Story Index

| ID | Title | Priority | Epic |
|----|-------|----------|------|
| STORY-101 | Fix `Velocity2D.normalize()` returning null | Critical | EPIC-1 |
| STORY-102 | Fix `GridWorldState.getTileTypeAt()` returning null | Critical | EPIC-1 |
| STORY-103 | Fix `ApplyMoveStrategy.move()` returning null | High | EPIC-1 |
| STORY-104 | Fix `SessionContext` field visibility | High | EPIC-1 |
| STORY-105 | Enforce `MoveStrategy` null-safety contract | Medium | EPIC-1 |
| STORY-201 | Implement `StateCommand` | High | EPIC-2 |
| STORY-202 | Implement `WelcomeCommand` and `ErrorCommand` | Medium | EPIC-2 |
| STORY-203 | Implement `AbstractDynamicEntity` body methods | Medium | EPIC-2 |
| STORY-204 | Complete `CreateRequest` fields | Medium | EPIC-2 |
| STORY-205 | Implement `OpenWorldEngine` | Low | EPIC-2 |
| STORY-206 | Implement `TrafficConnector` and `TrafficEngine` | Low | EPIC-2 |
| STORY-207 | Implement Game of Life cfgWorld type | Low | EPIC-2 |
| STORY-301 | Extract `ProtocolConstants` class | Medium | EPIC-3 |
| STORY-302 | Replace static singletons with DI | Medium | EPIC-3 |
| STORY-303 | Document the wire protocol | Medium | EPIC-3 |
| STORY-304 | Introduce `WorldRegistry` interface | Medium | EPIC-3 |
| STORY-305 | Remove commented-out Quake 3 code | Low | EPIC-3 |
| STORY-306 | Add MDC structured logging per session | Medium | EPIC-3 |
| STORY-307 | Model server-to-client errors consistently | Medium | EPIC-3 |
| STORY-401 | Increase command handler unit test coverage | High | EPIC-4 |
| STORY-402 | Add `GridWorldEngine` edge-case tests | High | EPIC-4 |
| STORY-403 | Add `SocketConnection` unit tests | Medium | EPIC-4 |
| STORY-404 | Add full client session lifecycle integration test | Medium | EPIC-4 |
| STORY-405 | Add clock determinism tests for all clock types | Medium | EPIC-4 |
| STORY-501 | Add graceful shutdown timeout to `TcpServer` | High | EPIC-5 |
| STORY-502 | Add dead-letter handling to `CommandActor` | Medium | EPIC-5 |
| STORY-503 | Replace hard-coded 750 ms join timeout | Low | EPIC-5 |
| STORY-504 | Add `PING`/`PONG` health-check command | Low | EPIC-5 |
| STORY-601 | Expose per-cfgWorld metrics | Medium | EPIC-6 |
| STORY-602 | Add `log4j2.xml` configuration file | Low | EPIC-6 |
