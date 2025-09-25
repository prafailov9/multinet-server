package com.ntros.command.impl;

import com.ntros.instance.ins.Instance;
import com.ntros.instance.InstanceFactory;
import com.ntros.instance.InstanceRegistry;
import com.ntros.message.ClientProfile;
import com.ntros.model.world.WorldConnectorHolder;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;

import com.ntros.model.world.protocol.WorldType;
import com.ntros.model.world.state.WorldStateFactory;
import com.ntros.session.Session;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to create worlds, or objects within world
 */
@Slf4j
public class CreateCommand extends AbstractCommand {

  private static final int WORLD_NAME = 1;
  private static final int IS_WORLD_SHARED = 2;
  private static final int WORLD_WIDTH = 3;

  @Override
  public Optional<ServerResponse> execute(Message message, Session session) {
    ClientProfile clientProfile = session.getProtocolContext();

    if (!clientProfile.isAuthenticated()) {
      return Optional.of(ServerResponse.ofError(message, "User not authenticated"));
    }

    ServerResponse serverResponse;
    try {
      // Create a world and instance and register them in their in-memory caches.
      WorldConnector worldConnector = createWorld(message);
      WorldConnectorHolder.register(worldConnector);

      boolean isShared = parseSharedFlag(message);
      Instance instance = InstanceFactory.createInstance(session, isShared, worldConnector);
      InstanceRegistry.register(instance);

      serverResponse = ServerResponse.ofSuccess(
          String.valueOf(clientProfile.getSessionId()),
          worldConnector.worldName(), "World Created");
    } catch (IllegalArgumentException ex) {
      log.error("Command failed. Could not create world", ex);
      serverResponse = ServerResponse.ofError(message, ex.getMessage());
    }

    return Optional.of(serverResponse);
  }

  private WorldConnector createWorld(Message message) {
    // COMMAND: CREATE;
    // ARGS: type, name, isMultiplayer, width, height
    String worldType = Optional.ofNullable(message.args().getFirst())
        .orElseThrow(() -> new IllegalArgumentException("No world type provided"))
        .trim().toUpperCase();

    if (!worldType.equals(WorldType.GRID.name())) {
      throw new IllegalArgumentException("Unsupported world type: " + worldType);
    }

    String worldName = message.args().get(WORLD_NAME);
    int width = Integer.parseInt(message.args().get(WORLD_WIDTH));
    int height = Integer.parseInt(message.args().getLast());

    return new GridWorldConnector(WorldStateFactory.createGridWorld(worldName, width, height),
        new GridWorldEngine());
  }

  private boolean parseSharedFlag(Message message) {
    String s = Optional.ofNullable(message.args().get(IS_WORLD_SHARED))
        .orElseThrow(() -> new IllegalArgumentException("Missing shared flag (index 2)."))
        .trim().toUpperCase();

    return switch (s) {
      case "TRUE", "1", "MULTI", "PUBLIC", "SHARED" -> true;
      case "FALSE", "0", "SOLO", "PRIVATE" -> false;
      default -> throw new IllegalArgumentException(
          "Invalid shared flag. Use TRUE/FALSE, 1/0, SOLO/PRIVATE/PUBLIC.");
    };
  }
}
