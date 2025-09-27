package com.ntros.model.world.connector;

import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.state.solid.GridWorldState;
import org.junit.jupiter.api.BeforeEach;

class GridWorldConnectorTest {

  private static final String DEFAULT_WORLD_NAME = "arena-x";
  private static final int DEFAULT_WORLD_WIDTH = 10;
  private static final int DEFAULT_WORLD_HEIGHT = 10;

  private GridWorldState state;
  private GridWorldEngine engine;
  private GridWorldConnector connector;


  @BeforeEach
  public void setUp() {
    engine = new GridWorldEngine();
    state = createWorldState(DEFAULT_WORLD_NAME, DEFAULT_WORLD_WIDTH, DEFAULT_WORLD_HEIGHT);
    connector = new GridWorldConnector(state, engine, new WorldCapabilities(true,
        true, true, true));
  }

//  @Test
//  public void joinPlayerTest() {
//    JoinRequest request = new JoinRequest("cl1");
//    CommandResult result = connector.joinPlayer(request);
//  }


  private GridWorldState createWorldState(String name, int width, int height) {
    return new GridWorldState(name, width, height);
  }

}