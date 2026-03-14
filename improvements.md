# Multinet Server — Improvement Stories

> Generated: 2026-03-14
> Session: Interface-segregation refactor (PlayerGridEngine / SimulationGridEngine split)
> Stories below capture follow-on work identified during that session.
> See also `STORIES.md` for the earlier backlog of 30 cross-cutting stories.

---

## EPIC-A · Role & Authorisation System

The current role model is incomplete: `SessionContext` only tracks `InstanceRole` (world-scoped),
there is no system-level role, the role enum constants are ordered incorrectly, and
`OrchestratorCommand` unconditionally promotes any client to `ADMIN` — a security hole that
bypasses the entire permission system.

---

### IMP-A01 · Add `SystemRole` to `SessionContext`

**Type:** Story
**Priority:** High
**Module:** runtime · domain
**Files:**
- `runtime/.../lifecycle/session/SessionContext.java`
- `domain/.../model/entity/config/access/SystemRole.java` *(new)*

**Description:**
`SessionContext` currently only records `InstanceRole` (the player's role within a specific world
instance). There is no concept of a server-wide privilege level. A `SUPERUSER` or `ROOT` account
should be able to perform administrative actions on any world — inspecting state, forcing resets,
shutting down instances — regardless of whether they have joined that world.

**Acceptance Criteria:**
- [ ] `SystemRole` enum created with constants in ascending privilege order: `USER`, `SUPERUSER`, `ROOT`
- [ ] `SessionContext` gains a `volatile SystemRole systemRole` field, defaulting to `USER` on construction
- [ ] `SessionContext` exposes `getSystemRole()` and `setSystemRole(SystemRole)` (called during AUTH)
- [ ] Existing tests compile and pass unchanged — `instanceRole` field is not removed
- [ ] Javadoc on `SessionContext` describes the two-axis role model (system vs instance)

**Implementation Notes:**
Place `SystemRole` alongside `InstanceRole` in the `config.access` package. During the AUTH
handshake, look up the persisted system role for the user and call `ctx.setSystemRole(...)`.
This paves the way for IMP-A03 (`InstanceAccessController`) to use both axes for decisions.

---

### IMP-A02 · Fix `InstanceRole` enum: correct ordering and add `OBSERVER`

**Type:** Bug
**Priority:** High
**Module:** domain
**File:** `domain/.../model/entity/config/access/InstanceRole.java`

**Description:**
`InstanceRole` currently declares constants in the order `OWNER, ADMIN, MODERATOR, PLAYER,
GAME_MASTER`. This ordering implies `OWNER` is the lowest ordinal (`0`) and `GAME_MASTER` is
the highest (`4`), which is the opposite of intent. Code that compares roles by ordinal (e.g.
`role.ordinal() >= minimumRole.ordinal()`) silently produces inverted results.

`OBSERVER` (a read-only viewer, below `PLAYER`) is absent, which forces simulation worlds to
treat observers as `PLAYER` or leave them untracked.

**Acceptance Criteria:**
- [ ] Constants ordered ascending by privilege: `OBSERVER(0) < PLAYER(1) < GAME_MASTER(2) < MODERATOR(3) < ADMIN(4) < OWNER(5)`
- [ ] `OBSERVER` is added as the lowest-privilege role
- [ ] A helper `boolean atLeast(InstanceRole minimum)` is added for safe comparison without relying on ordinal
- [ ] All usages of the enum in the codebase are updated to the new constant names / ordering
- [ ] Unit test asserts: `OBSERVER.atLeast(OBSERVER)`, `PLAYER.atLeast(OBSERVER)`, `!PLAYER.atLeast(ADMIN)`, etc.

**Implementation Notes:**
Existing switch expressions on `InstanceRole` (e.g. in command classes) must be reviewed — some
may be exhaustive and will fail to compile after `OBSERVER` is added.

---

### IMP-A03 · Introduce `InstanceAccessController`

**Type:** Story
**Priority:** High
**Module:** runtime
**Files:**
- `runtime/.../command/access/InstanceAccessController.java` *(new)*
- `runtime/.../command/AbstractCommand.java` *(modified)*

**Description:**
Authorization logic is currently scattered across individual command classes (or absent entirely).
`OrchestratorCommand` grants ADMIN to anyone who sends a valid ORCHESTRATE message. `JoinCommand`
does not check world visibility. `MoveCommand` does not verify the sender has at least `PLAYER`
role in the joined world.

A single `InstanceAccessController` should be the authoritative gate between a parsed command
and the `Instance.apply(WorldOp)` call, making the access rules explicit, testable, and not
duplicated.

**Acceptance Criteria:**
- [ ] `InstanceAccessController` has a `check(SessionContext ctx, WorldOp op, WorldCapabilities caps)` method returning a `Result<Void, String>` (or throws `AccessDeniedException`)
- [ ] Controller enforces:
  - World visibility: `PRIVATE` → only the owner may join; `JOINABLE` → invite-only; `PUBLIC` → anyone authenticated
  - Capacity: `ctx.playerCount() < caps.maxPlayers()` (or similar via `InstanceSettings`)
  - Op-level role: `JoinOp` requires at least `OBSERVER`, `MoveOp` requires at least `PLAYER`, `OrchestrateOp` requires at least `caps.minimumOrchestratorRole()`
  - System override: `SystemRole.ROOT` bypasses all instance-level checks
- [ ] `AbstractCommand` (or each command's `execute`) delegates to the controller before calling the instance
- [ ] Unit tests cover: public world join, private world rejected, capacity exceeded, role insufficient, ROOT override

**Implementation Notes:**
Depends on IMP-A01 (`SystemRole`) and IMP-A02 (corrected `InstanceRole` ordering).
Depends on IMP-C04 (`minimumOrchestratorRole` on `WorldCapabilities`).
The controller should receive `WorldCapabilities` via `instance.getConnector().getCapabilities()`,
not hard-code anything world-type-specific.

---

### IMP-A04 · Remove unconditional ADMIN promotion from `OrchestratorCommand`

**Type:** Bug
**Priority:** Critical
**Module:** runtime
**File:** `runtime/.../command/OrchestratorCommand.java`

**Description:**
`OrchestratorCommand.execute()` contains:

```java
ctx.setInstanceRole(ADMIN);
PersistenceContext.clients().updateRole(ctx.getUsername(), ADMIN.name());
```

This runs unconditionally for *any* client that sends a syntactically valid `ORCHESTRATE`
message — before any authorisation check. A `PLAYER`-role user can permanently escalate
their own persisted role to `ADMIN` with a single command.

**Acceptance Criteria:**
- [ ] Both lines above are removed from `OrchestratorCommand`
- [ ] The `ADMIN` static import is removed (no longer used)
- [ ] If an orchestrator role assignment is genuinely needed on first use, it happens through `InstanceAccessController` (IMP-A03) and only after verifying `SystemRole >= SUPERUSER`
- [ ] Regression test: a `PLAYER`-role client sending ORCHESTRATE to a world where `minimumOrchestratorRole = ADMIN` receives an access-denied response and their role remains `PLAYER`

---

## EPIC-B · Command Segregation

`OrchestratorCommand` currently contains world-type-specific parse logic (GOL subcommands:
SEED, RANDOM, TOGGLE, CLEAR; FS subcommand: PLACE). The command layer should be agnostic to
world type — it should parse a generic `OrchestrateRequest` and let the engine dispatch handle
the rest.

---

### IMP-B01 · Extract world-type parse logic out of `OrchestratorCommand`

**Type:** Story
**Priority:** Medium
**Module:** runtime
**Files:**
- `runtime/.../command/OrchestratorCommand.java`
- `runtime/.../command/orchestrate/OrchestrateParser.java` *(new)*

**Description:**
`OrchestratorCommand.parseRequest()` is a flat switch over subcommand strings that knows about
both GoL verbs (SEED, RANDOM, TOGGLE, CLEAR) and FS verbs (PLACE). When a new world type
with its own subcommands is added, this switch must grow. The parser belongs in its own class
(or a per-engine strategy) so commands stay thin.

**Acceptance Criteria:**
- [ ] `OrchestrateParser` is an interface (or sealed hierarchy) with implementations per action family: `GolOrchestrateParser`, `FsOrchestrateParser`
- [ ] `OrchestratorCommand` looks up the correct parser by `WorldCapabilities.orchestrateParser()` (a `Class<? extends OrchestrateParser>`) or equivalent capability field
- [ ] The `parseRequest()` method in `OrchestratorCommand` is deleted; its logic moves to the respective parser
- [ ] Unknown subcommand errors propagate identically to the current behaviour
- [ ] Unit tests cover each parser: valid parse, unknown subcommand, malformed args (bad int, missing material)

**Implementation Notes:**
The simplest wiring: `WorldCapabilities` gains a `String orchestrateParserClass` (or a
`Class<? extends OrchestrateParser>`) that the connector can provide.
Alternative: the connector itself exposes a `parseOrchestrate(List<String> args): OrchestrateRequest`
method so the engine, which owns the knowledge of valid sub-commands, handles parsing too.
The second approach keeps `OrchestrateParser` out of domain and avoids coupling runtime to a
world-type enum — preferred.

---

### IMP-B02 · Add capability guard before executing ORCHESTRATE

**Type:** Story
**Priority:** High
**Module:** runtime
**File:** `runtime/.../command/OrchestratorCommand.java`

**Description:**
`OrchestratorCommand` calls `instance.orchestrateAsync(req)` without first checking whether
the instance's `WorldCapabilities.supportsOrchestrator()` is true. Sending ORCHESTRATE to an
Arena world (which has no `SimulationGridEngine`) currently falls through to a
`WorldResult.failed(...)` deep in the connector, with a generic error message. The guard should
fire before any work is done, at the command layer, with a clear user-facing message.

**Acceptance Criteria:**
- [ ] `OrchestratorCommand` reads `instance.getConnector().getCapabilities().supportsOrchestrator()` before dispatching
- [ ] If false, immediately returns `errorMsg("World '<name>' does not support ORCHESTRATE.")` without touching the instance
- [ ] `WorldCapabilities(supportsOrchestrator=false)` worlds never see an `OrchestrateOp` reach the connector
- [ ] Test: sending ORCHESTRATE to an Arena instance returns an error without a `WorldResult` object being created

---

## EPIC-C · WorldCapabilities & InstanceSettings Cleanup

`InstanceSettings` has accumulated fields that are intrinsic to a world type (not to a runtime
configuration): `requiresOrchestrator`, `autoStartOnPlayerJoin`, `deterministic`, `seed`.
These should move to `WorldCapabilities`, which describes what a world *is*, not what a
particular instance of it is *configured as*.

---

### IMP-C01 · Move `requiresOrchestrator` → `WorldCapabilities`

**Type:** Story
**Priority:** High
**Module:** domain · runtime
**Files:**
- `domain/.../model/entity/config/WorldCapabilities.java`
- `domain/.../model/entity/config/access/InstanceSettings.java`
- `runtime/.../lifecycle/instance/InstanceFactory.java`

**Description:**
`InstanceSettings.requiresOrchestrator` is read to decide whether to start the instance clock
on player join or only on ORCHESTRATE. This is a property of the world type (GOL *always*
requires an orchestrator; an Arena *never* does), not a per-instance configuration.

**Acceptance Criteria:**
- [ ] `WorldCapabilities` gains `boolean requiresOrchestrator`
- [ ] `InstanceSettings.requiresOrchestrator` field is removed
- [ ] All factory methods on `InstanceSettings` that set `requiresOrchestrator` are updated to remove the field
- [ ] `ServerInstance` (or wherever the field is consumed) reads from `WorldCapabilities` via the connector
- [ ] All existing `InstanceFactory` call sites compile and behave identically
- [ ] Unit test: `ServerInstance` with `WorldCapabilities(requiresOrchestrator=true)` does not start the clock on `JOIN`; does start it on `ORCHESTRATE`

---

### IMP-C02 · Move `autoStartOnPlayerJoin` → `WorldCapabilities`

**Type:** Story
**Priority:** Medium
**Module:** domain · runtime
**Files:** same as IMP-C01

**Description:**
`autoStartOnPlayerJoin` is always `true` for multiplayer worlds and `false` for autonomous
simulations. Like `requiresOrchestrator`, this encodes the world type's lifecycle policy, not
a tunable per-instance setting.

**Acceptance Criteria:**
- [ ] `WorldCapabilities` gains `boolean autoStartOnPlayerJoin`
- [ ] `InstanceSettings.autoStartOnPlayerJoin` field is removed
- [ ] Behaviour is unchanged for all existing world types

---

### IMP-C03 · Move `deterministic` and `seed` → `WorldCapabilities`; introduce `LifecyclePolicy`

**Type:** Story
**Priority:** Medium
**Module:** domain
**Files:**
- `domain/.../model/entity/config/WorldCapabilities.java`
- `domain/.../model/entity/config/access/InstanceSettings.java`
- `domain/.../model/entity/config/LifecyclePolicy.java` *(new)*

**Description:**
`InstanceSettings` carries a `TODO` comment noting that `deterministic` and `seed` do not belong
there. They describe world simulation characteristics, not instance runtime settings.

Additionally, the interaction between `requiresOrchestrator` and `autoStartOnPlayerJoin` can be
collapsed into a single `LifecyclePolicy` enum, which is easier to reason about and extend.

**`LifecyclePolicy` values:**

| Value | Meaning |
|---|---|
| `PLAYER_DRIVEN` | Clock starts when the first player joins; stops when all leave (Arena) |
| `ORCHESTRATION_DRIVEN` | Clock starts only on first ORCHESTRATE command; players join as observers (GoL, FS) |
| `AUTONOMOUS` | Clock runs from server boot regardless of players (future: Wa-Tor, traffic sim) |

**Acceptance Criteria:**
- [ ] `LifecyclePolicy` enum created with the three values above
- [ ] `WorldCapabilities` gains `LifecyclePolicy lifecyclePolicy`, replacing the two booleans from IMP-C01/C02
- [ ] `WorldCapabilities` gains `boolean deterministic` and `Long seed` (nullable), replacing the `InstanceSettings` fields
- [ ] `InstanceSettings` loses `requiresOrchestrator`, `autoStartOnPlayerJoin`, `deterministic`, `seed`
- [ ] `ServerInstance` clock-start logic switches on `lifecyclePolicy` instead of two booleans
- [ ] Existing world types are updated: Arena → `PLAYER_DRIVEN`, GoL/FS → `ORCHESTRATION_DRIVEN`
- [ ] Unit tests cover each policy's clock-start behaviour

---

### IMP-C04 · Add `minimumOrchestratorRole` to `WorldCapabilities`

**Type:** Story
**Priority:** Medium
**Module:** domain
**File:** `domain/.../model/entity/config/WorldCapabilities.java`

**Description:**
Who can send ORCHESTRATE commands is currently hard-coded in `OrchestratorCommand` (and
accidentally anyone, due to IMP-A04). The minimum role required to orchestrate a world should
be declared in `WorldCapabilities` so that `InstanceAccessController` (IMP-A03) can enforce it
without world-type-specific conditionals.

**Acceptance Criteria:**
- [ ] `WorldCapabilities` gains `InstanceRole minimumOrchestratorRole` (defaults to `ADMIN` for existing worlds)
- [ ] `InstanceAccessController` (IMP-A03) reads this field when checking `OrchestrateOp` permission
- [ ] `InstanceFactory` sets `minimumOrchestratorRole` explicitly for each world type:
  - Arena: `ADMIN` (only admins can orchestrate an Arena — it's a player world, not a simulation)
  - GoL / FS: `PLAYER` (any joined player can seed/toggle a simulation)
- [ ] Unit test: `PLAYER`-role session can send ORCHESTRATE to a GoL world; same session is rejected for an Arena world

---

## EPIC-D · Instance Factory & Wiring Fixes

These are concrete bugs in `InstanceFactory` introduced by the interface-segregation refactor.
They will cause `ClassCastException` or silent `WorldResult.failed` at runtime.

---

### IMP-D01 · Fix `createGameOfLifeWorld` — wrong state type and wrong capabilities

**Type:** Bug
**Priority:** Critical
**Module:** runtime
**File:** `runtime/.../lifecycle/instance/InstanceFactory.java`

**Description:**
`InstanceFactory.createGameOfLifeWorld` creates:
```java
WorldConnector connector = new GridWorldConnector(
    ArenaGridState.blank(name, 256, 256),   // ← PlayerGridState, NOT SimulationGridState
    new GameOfLifeEngine(),                  // ← SimulationGridEngine
    new WorldCapabilities(true, true, true, true));  // ← supportsPlayers=true for GoL?
```

After the interface-segregation refactor, `GameOfLifeEngine` is a `SimulationGridEngine` and
requires a `SimulationGridState`. `GridWorldConnector.apply(JoinOp)` checks
`engine instanceof PlayerGridEngine` — which is false for GOL — so `JoinOp` silently returns
`WorldResult.failed`. Meanwhile `OrchestrateOp` checks
`state instanceof SimulationGridState` — which is false for `ArenaGridState` (it's a
`PlayerGridState`) — so ORCHESTRATE also silently fails.

**Acceptance Criteria:**
- [ ] State is changed to `new GameOfLifeState(name, 256, 256)`
- [ ] `WorldCapabilities` is corrected: `supportsPlayers=false`, `supportsOrchestrator=true`, `hasAIEntities=false`, `isDeterministic=true` (or uses post-IMP-C03 `LifecyclePolicy`)
- [ ] Integration test (or `ServerBootstrapWiringTest`): a GOL world created via `InstanceFactory` accepts ORCHESTRATE RANDOM and rejects JOIN as unsupported
- [ ] `FallingSandEngine` worlds have a matching factory method that also uses the correct `FallingSandState` rather than `ArenaGridState`

---

### IMP-D02 · Add `createFallingSandWorld` factory method

**Type:** Story
**Priority:** High
**Module:** runtime
**File:** `runtime/.../lifecycle/instance/InstanceFactory.java`

**Description:**
There is no factory method for Falling Sand worlds in `InstanceFactory`. The bootstrap presumably
creates them directly inline, bypassing the factory's registry (`INST` map). This means
`InstanceFactory.get(name)` returns `null` for FS worlds, causing `OrchestratorCommand` to
return `World not found` even for valid FS worlds.

**Acceptance Criteria:**
- [ ] `createFallingSandWorld(String name, SessionManager sessions)` is added
- [ ] Uses `FallingSandState` (not `ArenaGridState`) and `FallingSandEngine`
- [ ] `WorldCapabilities`: `supportsPlayers=false`, `supportsOrchestrator=true`
- [ ] World is registered in `INST` map
- [ ] `ServerBootstrap` uses this method for all FS world construction

---

## EPIC-E · ECS Framework & Boids Simulation

*(From the architectural plan agreed in this session — see plan file for full detail.)*

---

### IMP-E01 · Implement ECS core framework

**Type:** Story
**Priority:** Medium
**Module:** domain
**Package:** `domain/.../ecs/core/`

**Description:**
Introduce a minimal Entity-Component-System framework inside the `domain` module. This provides
a clean, allocation-free foundation for future simulation worlds (Boids, Wa-Tor, traffic) without
leaking entity management concerns into the engine/state hierarchy that was just cleaned up.

**Components to create:**

| Class | Role |
|---|---|
| `EntityId` | `record(int id)` — lightweight entity handle |
| `Component` | Marker interface |
| `ComponentStore<T>` | Interface: `set`, `get`, `has`, `remove`, `forEach` |
| `DenseStore<T>` | `Object[]` indexed by `EntityId.id()` — O(1), cache-sequential. Best for position/velocity (all entities have it) |
| `SparseSet<T>` | `int[] sparse` + `int[] dense` + `Object[] values` — O(1), dense iteration. Best for rare tags |
| `EcsSystem` | `@FunctionalInterface`: `void update(EcsWorld world, float dt)` |
| `EcsWorld` | Central registry: create entity, register stores, query two components, tick all systems |

**Acceptance Criteria:**
- [ ] All classes above are implemented and unit-tested
- [ ] `DenseStore` and `SparseSet` are interchangeable via `ComponentStore<T>`
- [ ] `EcsWorld.query(Class<A>, Class<B>, TriConsumer<EntityId,A,B>)` iterates the smaller store and checks membership in the larger — O(min(|A|, |B|))
- [ ] `EcsWorld.tick(float dt)` calls each registered `EcsSystem` in registration order
- [ ] No allocation occurs inside `tick()` for worlds with a stable entity count (verified by a gc-alloc test or profiling note)
- [ ] Javadoc explains the DenseStore-vs-SparseSet trade-off

---

### IMP-E02 · Implement Boids flocking simulation using ECS

**Type:** Story
**Priority:** Medium
**Module:** domain
**Package:** `domain/.../ecs/sim/boids/`
**Depends on:** IMP-E01

**Description:**
Build a Boids flocking simulation (`Reynolds 1987`) as the first concrete ECS world. Boids
demonstrate that the ECS + `SimulationGridEngine` design scales beyond cellular automata.

**Components:**

| Component | Fields |
|---|---|
| `PositionComp` | `float x, y` |
| `VelocityComp` | `float vx, vy` |
| `BoidComp` | `float sepRadius, alignRadius, cohRadius` |

**Systems (in tick order):**
1. `SpatialHashRebuildSystem` — clears and re-inserts all boids into a fixed-bucket `SpatialHash`
2. `SeparationSystem` — steer away from neighbours within `sepRadius`
3. `AlignmentSystem` — steer toward average heading of neighbours within `alignRadius`
4. `CohesionSystem` — steer toward centre of mass within `cohRadius`
5. `MovementSystem` — `pos += vel * dt`; clamp speed
6. `BoundsSystem` — wrap or bounce at world edges

**`BoidsEngine` (`implements SimulationGridEngine`):**
- Holds `EcsWorld` + `SpatialHash`, spawns N boids in constructor
- `applyIntents`: rebuild spatial hash, tick ECS world, sync positions into (conceptual) entity list for serialisation
- `orchestrate`: handle `RANDOM_SEED` to respawn boids at random positions
- `snapshot` / `serialize`: emit entity positions as JSON (tiles section empty)

**Acceptance Criteria:**
- [ ] 200 boids at 10 TPS produce observable flocking on the client canvas
- [ ] `SpatialHash` bucket size is configurable; default cell size = `max(sepRadius, alignRadius, cohRadius)`
- [ ] No `new` allocations inside `applyIntents` at steady state (SpatialHash reuses internal arrays)
- [ ] Unit test: after ≥ 10 ticks with default parameters, average inter-boid distance is less than `cohRadius` (emergent clustering)
- [ ] `BoidsEngine` rejects `JOIN` and `MOVE` ops (returns `WorldResult.failed` — it is a simulation)

---

### IMP-E03 · Register boids world in `ServerBootstrap` and expose in client

**Type:** Story
**Priority:** Low
**Module:** server · client
**Files:**
- `server/.../server/ServerBootstrap.java`
- `multinet-client/index.html`
**Depends on:** IMP-E02, IMP-D01 (factory pattern)

**Description:**
Wire the Boids world into the server's startup sequence and add it to the client's world-selector
dropdown so it is reachable without code changes.

**Acceptance Criteria:**
- [ ] `ServerBootstrap` creates a `boids-small` world (100×100, 200 boids) using the same factory pattern as GoL/FS
- [ ] `InstanceFactory.createBoidsWorld(String name, SessionManager sessions)` is added
- [ ] `index.html` world-selector has `<option value="boids-small">boids-small · BOIDS 100×100</option>` with correct `data-w`/`data-h` attributes
- [ ] The client renders boids as entity dots on a dark grid with no tile colours (tiles section is empty)
- [ ] ORCHESTRATE RANDOM_SEED respawns all boids at new random positions visible to connected clients

---

## EPIC-F · Protocol Stubs — Minimum Viable Implementations

These stubs have been known for some time (see `STORIES.md` EPIC-2). They are listed here as a
reminder that the interface-segregation work has not resolved them.

---

### IMP-F01 · Implement `StateCommand`

**Type:** Story
**Priority:** Medium
**Module:** runtime
**File:** `runtime/.../command/StateCommand.java`

**Description:**
`StateCommand.execute()` returns an empty `Optional`. A client sending `STATE` should receive a
serialised snapshot of the world it is currently joined to, allowing it to request a full
state resync without waiting for the next broadcast cycle.

**Acceptance Criteria:**
- [ ] `StateCommand` reads `ctx.getWorldName()`, looks up the instance, calls `connector.snapshot(false)`, and returns the JSON as a `Message`
- [ ] If the client is not joined to a world, returns `errorMsg("Not joined to any world.")`
- [ ] Response format is identical to a broadcast frame so the client's existing deserialization handles it

---

### IMP-F02 · Implement `WelcomeCommand` and `ErrorCommand`

**Type:** Story
**Priority:** Low
**Module:** runtime
**Files:**
- `runtime/.../command/WelcomeCommand.java`
- `runtime/.../command/ErrorCommand.java`

**Description:**
Both commands return empty `Optional`s. `WelcomeCommand` should send the server version and
available world list to a newly connected client. `ErrorCommand` should format and forward an
error message back to the session.

**Acceptance Criteria:**
- [ ] `WelcomeCommand` returns a JSON message containing server version and a list of public world names from `InstanceFactory.all()`
- [ ] `ErrorCommand` returns `errorMsg(args.get(0))` (simple passthrough)
- [ ] Unit tests cover both

---

### IMP-F03 · Implement `AbstractDynamicEntity` movement methods

**Type:** Story
**Priority:** Medium
**Module:** domain
**File:** `domain/.../model/entity/AbstractDynamicEntity.java`

**Description:**
`AbstractDynamicEntity` contains empty `move()`, `stop()`, and `accelerate()` stubs. These are
called by `ApplyMoveStrategy` and silently do nothing. The entity never changes position.

**Acceptance Criteria:**
- [ ] `move(Vector direction, float speed)` updates the entity's position by `direction * speed`
- [ ] `stop()` sets velocity to zero
- [ ] `accelerate(Vector delta)` adds `delta` to current velocity
- [ ] The existing `Velocity2D.normalize()` bug (STORY-101) must be fixed before these methods are useful — flag dependency
- [ ] Unit tests confirm position changes after `move()` and reset after `stop()`
