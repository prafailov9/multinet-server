package com.ntros.model.world.wator;

import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.wator.system.DeathSystem;
import com.ntros.model.world.wator.WaTorSnapshot.AgentView;
import com.ntros.model.world.wator.WaTorSnapshot.FoodView;
import com.ntros.model.world.wator.WaTorSnapshot.PlantView;
import com.ntros.model.world.wator.component.EnergyComponent;
import com.ntros.model.world.wator.component.HealthComponent;
import com.ntros.model.world.wator.component.PlantComponent;
import com.ntros.model.world.wator.component.Position2f;
import com.ntros.model.world.wator.component.VelocityComponent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Production implementation of {@link WaTorEngine}.
 *
 * <p>Thin orchestration layer — delegates all simulation logic to {@link WaTorWorld#tick(float)}.
 * Maintains an observer registry so sessions can join/leave without affecting agents.
 */
@Slf4j
public class WaTorEngineImpl implements WaTorEngine {

  /** Names of currently connected observer sessions. */
  private final Set<String> observers = new HashSet<>();

  // ── WaTorEngine ───────────────────────────────────────────────────────────

  @Override
  public void tick(WaTorWorld world, float dt) {
    world.tick(dt);
  }

  @Override
  public WorldResult joinObserver(JoinRequest req, WaTorWorld world) {
    String name = req.playerName();
    if (observers.contains(name)) {
      return WorldResult.failed(name, world.getState().worldName(),
          "Observer '" + name + "' is already connected.");
    }
    observers.add(name);
    log.info("[WaTor] Observer '{}' joined '{}'.", name, world.getState().worldName());
    return WorldResult.succeeded(name, world.getState().worldName(),
        "Joined Wa-Tor simulation as observer.");
  }

  @Override
  public void removeObserver(String observerName, WaTorWorld world) {
    if (observers.remove(observerName)) {
      log.info("[WaTor] Observer '{}' left '{}'.", observerName, world.getState().worldName());
    }
  }

  @Override
  public Object snapshot(WaTorWorld world) {
    WaTorWorldState state = world.getState();
    int size = state.positions.size();

    List<AgentView> predators = new ArrayList<>();
    List<AgentView> prey      = new ArrayList<>();
    List<PlantView> plants    = new ArrayList<>();
    List<FoodView>  food      = new ArrayList<>();

    for (int id = 0; id < size; id++) {
      if (!state.alive.get(id)) continue;
      Position2f pos = state.positions.get(id);
      if (pos == null) continue;

      EntityType type = state.types.get(id);
      switch (type) {
        case PREDATOR, PREY -> {
          VelocityComponent vel = state.velocities.get(id);
          EnergyComponent   eng = state.energies.get(id);
          HealthComponent   hp  = state.healths.get(id);
          float angle      = vel  != null ? vel.angle        : 0f;
          float normEnergy = eng  != null ? eng.current / eng.max : 0f;
          float normHp     = hp   != null ? hp.current  / hp.max  : 0f;
          AgentView view   = new AgentView(id, pos.x, pos.y, angle, normHp, normEnergy);
          if (type == EntityType.PREDATOR) predators.add(view);
          else                             prey.add(view);
        }
        case PLANT -> {
          PlantComponent plant = state.plants.get(id);
          float sz = plant != null ? plant.size : 1f;
          plants.add(new PlantView(id, pos.x, pos.y, sz));
        }
        case FOOD -> {
          EnergyComponent eng = state.energies.get(id);
          // energy.current = TTL remaining; energy.max = initial TTL
          float ttlFrac = eng != null ? eng.current / DeathSystem.FOOD_TTL_TICKS : 0f;
          food.add(new FoodView(id, pos.x, pos.y, Math.max(0f, ttlFrac)));
        }
      }
    }

    return new WaTorSnapshot(predators, prey, plants, food);
  }

  @Override
  public void reset(WaTorWorld world) {
    log.info("[WaTor] Resetting world '{}'.", world.getState().worldName());
    // Re-bootstrap: clear all component arrays and re-seed
    // Full implementation deferred — bootstrap populates from ServerBootstrap
    observers.clear();
  }
}
