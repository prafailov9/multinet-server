# Multinet Server — Design Document

**Date:** 2026-03-10
**Scope:** Current-state architecture, known gaps, and evaluation of the proposed binary protocol

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Module Structure](#2-module-structure)
3. [Component Deep-Dive](#3-component-deep-dive)
4. [Wire Protocol — Current State](#4-wire-protocol--current-state)
5. [Known Gaps & Issues](#5-known-gaps--issues)
6. [Protocol Proposition Evaluation](#6-protocol-proposition-evaluation)
7. [Roadmap](#7-roadmap)

---

## 1. Architecture Overview

```
Client (TCP :5555)
    │  plain-text line: "JOIN player-1 cfgWorld-1\n"
    ▼
TcpServer
    │  one virtual thread per connected client
    ▼
ClientSession.start()   ← session loop
    │  connection.receive() → raw string
    ▼
RequestClientMessageProcessor
    │  MessageParser.parse()  → Message{CommandType, args[]}
    │  MessageDispatcher.dispatch() → Command.execute()
    ▼
Command implementations (JoinCommand, MoveCommand, ...)
    │  joinAsync() / storeMoveAsync() → CompletableFuture<CommandResult>
    ▼
ServerInstance  (one per cfgWorld)
    │  delegates to CommandActor
    ▼
CommandActor  (single-threaded executor per cfgWorld, named "actor-<worldName>-ctl")
    │  join()   → WorldConnector.apply(JoinOp)
    │  stageMove() → ConcurrentHashMap<playerId, Direction>  [last-write-wins]
    │  step()   → applyMoves() + cfgWorld.update() + onAfterUpdate.run()
    ▼
WorldConnector / GridWorldEngine
    │  mutates GridWorldState
    ▼
ServerInstance.broadcastWorldSnapshot()
    │  cfgWorld.snapshot() → Object
    │  JsonProtocolEncoder.encodeState(StateFrame) → JSON string
    ▼
BroadcastToAll → ClientSessionManager → Session.response() → Connection.send()
    │
Client receives state JSON push
```

**Tick & broadcast rates:** 120 Hz ticks / 70 Hz broadcasts (both hardcoded in `ServerBootstrap`).

---

## 2. Module Structure

### `domain/`
Pure game logic. Zero network dependencies.

| Package | Contents |
|---------|----------|
| `model.entity` | `Player`, `NPC`, `Direction`, movement strategies |
| `model.cfgWorld.engine` | `GridWorldEngine` (tick logic), `OpenWorldEngine` (stub) |
| `model.cfgWorld.state` | `GridWorldState` (occupancy, entity map) |
| `model.cfgWorld.connector` | `WorldConnector` interface; `GridWorldConnector`, `TrafficConnector` (stub), `OpenWorldConnector` (stub) |
| `model.cfgWorld.protocol` | `CommandType`, `Message`, `StateFrame`, `JsonProtocolEncoder`, `ServerResponse`, `CommandResult` |
| `model.entity.config.access` | `Settings`, `Role`, `Visibility` |

### `protocol/`
Network, session lifecycle, command dispatch, clock.

| Package | Contents |
|---------|----------|
| `connection` | `Connection` interface, `SocketConnection` |
| `command` | `CommandRegistry`, `CommandUtil`, `Command` interface + implementations |
| `dispatcher` | `MessageDispatcher` |
| `parser` | `MessageParser` |
| `lifecycle.clock` | `AbstractClock`, `PacedRateClock`, `FixedRateClock`, `FixedDelayClock` |
| `lifecycle.instance` | `Instance`, `AbstractInstance`, `ServerInstance`, `Instances`, `InstanceFactory` |
| `lifecycle.instance.actor` | `Actor`, `CommandActor`, `Actors` |
| `lifecycle.session` | `Session`, `ClientSession`, `SessionContext`, `SessionState` |
| `lifecycle.session.process` | `ClientMessageProcessor`, `ServerMessageProcessor`, their implementations |
| `event.broadcaster` | `Broadcaster`, `BroadcastToAll` |
| `event.sessionmanager` | `SessionManager`, `ClientSessionManager` |
| `message` | `SessionContext` |

### `server/`
Entry point and bootstrapping only.

| File | Role |
|------|------|
| `Main.java` | calls `ServerBootstrap.startServer()` |
| `ServerBootstrap.java` | creates cfgWorlds, clocks, instances, starts `TcpServer` |
| `TcpServer.java` | accepts TCP connections, spawns virtual threads |

---

## 3. Component Deep-Dive

### 3.1 Connection (`SocketConnection`)

Wraps one `java.net.Socket`. Responsibilities:

- **receive()** — byte-by-byte newline scan into a reused `ByteArrayOutputStream`. 8 KB line limit. Returns `"_TIMEOUT_"` on `SocketTimeoutException` (5 s).
- **send(String)** — enqueues into a `LinkedBlockingQueue<String>` (capacity 1024). Throws `RuntimeException("Backpressure")` when full. Schedules a drain task via `SEND_POOL` (fixed-thread-pool, `2 × availableProcessors`), gated by `AtomicBoolean` so at most one sender task runs per connection at a time.
- **receiveBytesExactly(int)** — reads exactly N bytes; implemented but unused.
- **sendFrame(String, byte[])** — declared in `Connection` interface, throws `UnsupportedOperationException`.

`Connection` interface already anticipates binary framing (`sendFrame`, `receiveBytesExactly`) but neither method is implemented or called.

### 3.2 Session (`ClientSession`)

Per-client state machine. States: `RUNNING → STOPPING → TERMINATED` (via `AtomicReference<SessionState>`).

Loop (on virtual thread):
1. `connection.receive()` → raw string
2. Skip empty / `_TIMEOUT_` frames
3. `clientMessageProcessor.process(raw, session)` → `ServerResponse`
4. `serverMessageProcessor.processResponse(response, session)` → calls `session.response()` to write back

On any uncaught exception: synthesizes a `SESSION_FAILED` message, processes it, which calls `session.stop()`.

`shutdown()` → `cleanupResources()`: if the session was authenticated and joined a cfgWorld, calls `instance.leaveAsync(session)` on the actor thread, then closes the connection.

`SessionContext` fields are `volatile` or `AtomicBoolean` — threading is correct.

### 3.3 Command Dispatch

```
MessageParser.parse(raw)  →  Message{ CommandType, List<String> args }
MessageDispatcher.dispatch(message, session)  →  CommandRegistry.get(type)  →  command.execute()
```

**Registered commands:**

| Token | Class | Status |
|-------|-------|--------|
| `JOIN` | `JoinCommand` | Implemented |
| `MOVE` | `MoveCommand` | Implemented |
| `DISCONNECT` | `DisconnectCommand` | Implemented |
| `WELCOME` | `WelcomeCommand` | Returns `Optional.empty()` — stub |
| `STATE` | `StateCommand` | Returns `Optional.empty()` — stub |
| `ERROR` | `ErrorCommand` | Returns `Optional.empty()` — stub |

**`AuthCommand`** exists and has a partial implementation (sets `authenticated = true`, no token/credential check) but is **not registered** in `CommandRegistry`.

**`OrchestratorCommand`** exists but is not registered.

`JoinCommand` now enforces:
- `Visibility.PRIVATE` guard (checks owner via `InstanceFactory.ownerOf`)
- Single-player cfgWorld busy check (`maxPlayers == 1`)
- 750 ms timeout on the join future

### 3.4 Actor (`CommandActor`)

Single-threaded `ExecutorService` named `actor-<worldName>-ctl`. All cfgWorld mutations are serialized through it.

- **`stageMove(cfgWorld, req)`** — does NOT submit to the executor; writes `playerId → Direction` into a `ConcurrentHashMap` directly and returns an already-completed future. Moves are coalesced (last-write-wins).
- **`step(cfgWorld, onAfterUpdate)`** — submits to executor: flush all staged moves, `cfgWorld.update()`, then calls `onAfterUpdate` (which is `broadcastWorldSnapshot()`). Broadcast therefore runs **on the actor thread** — noted with `// TODO: Broadcast off-actor thread` in `ServerInstance`.
- **`join/leave/remove`** — submitted to executor, return `CompletableFuture<CommandResult>`.
- **`drain()`** — submits a no-op, allows callers to await queue drain.
- **Shutdown** — flips `accepting` flag, `shutdown()` + `awaitTermination(5 s)`, then `shutdownNow()`.

### 3.5 Clock (`PacedRateClock`)

`scheduleAtFixedRate` + in-flight gate (`AtomicBoolean inFlight`).

- Scheduler thread fires at `intervalMs` cadence.
- If `inFlight` is already true → **drops the tick** (no queue buildup).
- If clear → sets `inFlight = true`, submits task to a dedicated `worker` thread, clears `inFlight` in `finally`.
- **Two threads per clock**: the `ScheduledExecutorService` scheduler, and a single-thread `worker`.
- `TickListener` callbacks (`onTickStart`, `onTickEnd`) are called by the `AbstractClock` wrapper. `ServerInstance` logs tick duration every 120 ticks.
- `clock.pause()` / `clock.resume()` are surfaced through `AbstractInstance`.

### 3.6 ServerInstance

Owns the per-cfgWorld lifecycle. Extends `AbstractInstance`.

- `start()` — begins clock ticking; idempotent via `clockTicking` CAS.
- `stop()` — 7-step graceful shutdown: stop clock → drain actor → shutdown sessions → drain again → reset cfgWorld → stop actor → shutdown clock.
- `registerSession` / `removeSession` — delegates to `SessionManager`; auto-starts or auto-stops the clock based on `settings.autoStartOnPlayerJoin()`.
- `broadcastWorldSnapshot()` — calls `cfgWorld.snapshot()`, encodes to JSON, publishes to all sessions. Runs on actor thread (see §3.4 TODO).

`PROTO_VER = 1` constant exists; version is included in each `StateFrame` but is not used for negotiation.

### 3.7 World Model

`GridWorldConnector` → `GridWorldEngine` → `GridWorldState`.

`WorldConnector` is the adapter between the domain model and the protocol layer. It exposes:
- `apply(WorldOp)` — sealed dispatch (JoinOp, MoveOp, RemoveOp)
- `update()` — advance the cfgWorld by one tick
- `snapshot()` — capture current state for broadcast
- `reset()` — wipe cfgWorld state on instance stop
- `getCurrentEntities()` — for active session count

---

## 4. Wire Protocol — Current State

### Client → Server (commands)

Plain UTF-8 text, newline-terminated:

```
JOIN <playerName> [worldName]\n
MOVE <direction>\n           direction: N, S, E, W (or UP/DOWN/LEFT/RIGHT)
DISCONNECT\n
AUTH <args>\n                parsed but not in CommandRegistry
```

### Server → Client (state push)

JSON string, newline-terminated, pushed at up to 70 Hz:

```json
{"version":1,"worldName":"cfgWorld-1","seq":42,"data":{ ...cfgWorld snapshot... }}
```

### Framing

- Receive: byte scan until `\n`, max 8 192 bytes.
- Send: string + `\n`, via async queue.
- No length prefix. No binary framing. No versioning handshake.

### Notable: `Connection` interface already has binary hooks

```java
void sendFrame(String headerLine, byte[] body);          // UnsupportedOperationException
byte[] receiveBytesExactly(int length) throws IOException;  // implemented, unused
```

This signals that binary framing has been anticipated but not yet wired up.

---

## 5. Known Gaps & Issues

### 5.1 Backpressure throws instead of dropping

```java
// SocketConnection.send()
if (!sendQueue.offer(message)) {
    throw new RuntimeException("Backpressure: client not reading data.");
}
```

State frames are superseded snapshots — dropping the oldest is safe. Throwing disconnects the client instead.

### 5.2 Broadcast on actor thread

`broadcastWorldSnapshot()` runs inside `actor.step()`, on the actor thread. I/O latency during broadcast blocks the next tick. The code itself has a `// TODO: Broadcast off-actor thread` comment acknowledging this.

### 5.3 `AuthCommand` not registered

`AuthCommand` exists with partial logic but is not in `CommandRegistry`. Clients cannot authenticate via the `AUTH` command.

### 5.4 Stub commands return `Optional.empty()`

`WelcomeCommand`, `StateCommand`, `ErrorCommand` return `Optional.empty()`. `MessageDispatcher` passes the empty Optional back to `RequestClientMessageProcessor`, which calls `.orElseThrow()` — this means any client message that routes to these commands throws, which is caught by the session loop, processed as `SESSION_FAILED`, and terminates the session.

### 5.5 Hardcoded configuration

Tick rate (120), broadcast rate (70), port (5555), and cfgWorld names are all literals in `ServerBootstrap`. No config file, no env-variable overrides.

### 5.6 No rate limiting

A client can flood the server with commands. Move coalescing absorbs duplicate MOVE commands, but parsing, dispatch, and the join path are all unprotected.

### 5.7 No authentication credential check

`AuthCommand` accepts any client immediately (`setAuthenticated(true)` with no verification). `JoinCommand` sets `authenticated = true` itself. Authentication is effectively bypassed.

### 5.8 Known domain bugs (from STORIES.md)

- `Velocity2D.normalize()` returns null
- `GridWorldState.getTileTypeAt()` returns null
- `ApplyMoveStrategy.move()` returns null

### 5.9 Stubs

- `OpenWorldEngine` — empty class
- `TrafficConnector` / `TrafficEngine` — stubs
- `AbstractDynamicEntity` methods — empty

---

## 6. Protocol Proposition Evaluation

The proposition defines a binary framing layer:

```
[ length:4 ][ version:1 ][ type:1 ][ flags:1 ][ reserved:1 ][ payload:N ]
```

With message types: WELCOME (0x01), MOVE (0x02), STATE (0x03), ERROR (0x04), PING (0x05), PONG (0x06), GOODBYE (0x07).

### 6.1 What the proposition gets right

**Length-prefixed framing.** Replacing newline scanning with a 4-byte length prefix is the correct upgrade. The current 8 KB line limit is arbitrary, the `\n` sentinel is fragile (any payload containing a newline breaks parsing), and byte-scanning is more CPU-intensive than a fixed-size read.

**Type byte for fast dispatch.** `switch (type)` on a single byte is cleaner and faster than `Map<String, Command>` keyed on uppercased strings. It also eliminates the parsing step entirely.

**Version byte.** Putting the version inside the frame (not just in a handshake) means every packet is self-describing. This is strictly more robust than a session-level negotiation for detecting misbehaving or stale clients.

**Compact MOVE.** 9 bytes total vs. `"MOVE UP\n"` (8 bytes) — comparable size, but the binary form is fully structured with no parsing ambiguity.

**PING/PONG with timestamp.** 8-byte timestamp payload enables round-trip latency measurement, which the current protocol lacks entirely.

**GOODBYE with reason code.** Allows graceful disconnect signalling from both sides, currently absent.

**Layering proposal (Connection / PacketCodec / Session).** The suggestion to keep `Connection` as raw-bytes transport and put codec logic in a separate `PacketCodec` is architecturally correct. It matches what `Connection` already exposes (`receiveBytesExactly`, `sendFrame`), and it keeps I/O concerns separated from protocol semantics.

### 6.2 Points to refine

**`length` field semantics.** The spec says:
> "If payload is 5 bytes, total length is: version(1) + type(1) + flags(1) + reserved(1) + payload(5) = 9"

So `length` counts the bytes *after the length field itself* (i.e., the rest of the frame). This is the conventional and correct choice — it makes the read loop trivial (`read 4 → readExactly(length)`). The example packet confirms this. Just be explicit in code comments so future maintainers do not confuse "length of payload" with "length of frame".

**STATE message type not in the payload section.** Type `0x03 = STATE` is listed in the type enum but has no payload definition. This is the highest-volume message (70 Hz). The payload layout should be defined alongside WELCOME, MOVE, ERROR, etc. The existing `StateFrame` JSON suggests the payload might remain JSON for now, but the spec should say so explicitly rather than leaving it undefined.

**`flags` byte future-use note.** Listing `0x02 = ack required` as a future flag implies a reliability layer. For a game server sending cfgWorld snapshots, lost frames are acceptable (client re-syncs on next tick). Implementing ACK required would add round-trip overhead on the hot path. If the flag is ever used, it should only apply to transactional messages (JOIN, AUTH), not state pushes.

**Rejection of bad frames.** The spec correctly notes that the server must reject:
- `length <= 0`
- `length > MAX_FRAME_SIZE`
- unknown `version`
- unknown `type`

This logic belongs in `PacketCodec.decode()` and must throw a typed `ProtocolException` (not return null) so the session can send an ERROR frame and close cleanly.

**Error packet `msgLen` field.** The ERROR payload uses `[ errorCode:2 ][ msgLen:2 ][ message:msgLen ]`. This is correct. Ensure that on decode, `msgLen` is validated before allocating the message byte array — a malicious client could send `msgLen = 65535` to force a large allocation.

### 6.3 Fit with the existing codebase

**`Connection` interface already has the right hooks.** `receiveBytesExactly(int)` is implemented in `SocketConnection`. `sendFrame` is declared but throws `UnsupportedOperationException`. Adopting the binary protocol means:

1. Implement `SocketConnection.sendFrame(byte type, byte[] payload)` — write the 8-byte header then payload to `output`.
2. Change `send(String)` usage in `ClientSession` → `connection.sendFrame(type, payload)`.
3. Change `receive()` usage in `ClientSession` → read 4-byte length header, then `receiveBytesExactly(length)`, then `PacketCodec.decode()`.

**`CommandRegistry` must change.** Currently keyed on `String` command tokens. With binary types, the registry key becomes `byte`. This is a localized change.

**`MessageParser` is replaced.** The parser that splits on spaces is no longer needed. `PacketCodec.decode(byte type, byte[] payload)` takes its role.

**`JsonProtocolEncoder` can stay for now.** The STATE payload can remain JSON in the first iteration — the binary framing layer is independent of payload encoding. Move to binary STATE encoding in a later iteration.

**`SessionContext.userId` and the auth flow.** `AuthCommand` currently accepts any client. With the binary protocol, an AUTH packet type (e.g., `0x08`) should be added. The session should reject JOIN/MOVE packets until AUTH succeeds.

### 6.4 Migration path

The `Connection` interface already contains both the old text API (`send`, `receive`) and the new binary API (`sendFrame`, `receiveBytesExactly`). This means the migration can be done incrementally:

1. **Implement `sendFrame` in `SocketConnection`** without removing `send`. No callers break.
2. **Add `PacketCodec`** with encode/decode for all defined types. Cover with unit tests.
3. **Switch `ClientSession`** to use the binary read/write path.
4. **Switch `ServerInstance.broadcastWorldSnapshot()`** to use `sendFrame` with the STATE type.
5. **Remove `send(String)` and `receive()` from `Connection`** once no callers remain.

This avoids a flag-day rewrite and keeps the server testable at each step.

### 6.5 Summary verdict

**Adopt it.** The proposition is sound. The framing, the type dispatch, the layering recommendation, and the concrete byte layout are all correct and fit the existing code well. The `Connection` interface already partially anticipates this design. The main work to add before implementation:

- Define the STATE (0x03) payload layout explicitly.
- Add an AUTH packet type.
- Document `length` semantics in one sentence in the codec.
- Add frame size rejection in `PacketCodec.decode()` before allocating anything.

---

## 7. Roadmap

Items ordered by impact/effort ratio. Each tier builds on the previous.

### Tier 1 — Correctness (do first)

| # | Item | Notes |
|---|------|-------|
| 1 | Fix staged-move stale state after player leaves | `stagedMoves.remove(entityId)` on leave is present; verify it fires before actor shuts down |
| 2 | Drop stale frames instead of throwing on full send queue | Change `send()` to `poll()` oldest, `offer()` new |
| 3 | Register `AuthCommand` in `CommandRegistry` | It exists; just add the entry |
| 4 | Make stub commands return proper errors | `WelcomeCommand`, `StateCommand`, `ErrorCommand` must not return `Optional.empty()` |
| 5 | Fix the three null-return domain bugs | `Velocity2D.normalize`, `GridWorldState.getTileTypeAt`, `ApplyMoveStrategy.move` |

### Tier 2 — Protocol

| # | Item | Notes |
|---|------|-------|
| 6 | Implement `SocketConnection.sendFrame` | ~20 lines |
| 7 | Add `PacketCodec` | encode/decode all 7 types + AUTH |
| 8 | Switch `ClientSession` to binary read path | replace `receive()` with length-prefix loop + `PacketCodec.decode()` |
| 9 | Switch broadcast to binary write path | replace `JsonProtocolEncoder` + `send(String)` with `sendFrame(STATE, payload)` |
| 10 | Remove text protocol dead code | remove `send(String)`, `receive()` from `Connection` |

### Tier 3 — Stability & Operations

| # | Item | Notes |
|---|------|-------|
| 11 | Decouple broadcast from actor thread | fire `broadcastExecutor.submit(...)` instead of calling inline in `step()` |
| 12 | Externalize configuration | `application.properties`: port, tick rate, broadcast rate, cfgWorld definitions |
| 13 | Rate limit per session | token bucket, ~200 cmd/s, enforced before dispatch |
| 14 | Proper AUTH credential check | at minimum: token issued on first connect, verified on reconnect |
| 15 | Graceful shutdown with GOODBYE packet | use type 0x06; broadcast before stopping |

### Tier 4 — Performance & Observability

| # | Item | Notes |
|---|------|-------|
| 16 | Pre-serialize STATE payload once per tick | reuse bytes across all sessions |
| 17 | Tick timing diagnostics in `PacedRateClock` | log warning when avg tick > 50% of budget |
| 18 | In-process metrics | `AtomicLong` counters: active sessions, ticks dropped, frames sent |
| 19 | Integration test with `TestClient` | TCP connect → AUTH → JOIN → MOVE → receive STATE |
| 20 | Implement `OpenWorldEngine` and `TrafficEngine` | currently stubs |

---

## Appendix — What to Leave Alone

- **Virtual threads per client** — correct for Java 21, do not change.
- **Actor-per-cfgWorld** — correct serialization model; do not add intra-cfgWorld parallelism.
- **`PacedRateClock` tick-dropping** — correct backpressure; do not switch to queuing.
- **Move coalescing (last-write-wins)** — correct for a physics-less grid; do not accumulate.
- **Module split (domain / protocol / server)** — clean boundary; domain has zero network deps.
- **`CommandRegistry` as static map** — adequate at this scale; no need for DI.

---

*Document reflects codebase state as of 2026-03-10.*
