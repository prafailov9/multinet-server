package com.ntros.model.world.engine.solid;

import static org.junit.jupiter.api.Assertions.*;

import com.ntros.model.entity.sequence.IdSequenceGenerator;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.state.solid.GridWorldState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GridWorldEngineTest {

  private final GridWorldEngine engine = new GridWorldEngine();


  @AfterEach
  public void tearDown() {
    // reset the id generator after each run
    IdSequenceGenerator.getInstance().resetAll();
  }

  @Test
  public void joinCommand_validJoinRequest_succeeds() {
    //setup
    String expectedPlayerName = "cl1";
    String expectedWorldName = "arena-x";
    long expectedPlayerId = 1;
    String expectedSuccessReason = String.format(
        "Player %s successfully joined world %s with ID: %d", expectedPlayerName, expectedWorldName,
        expectedPlayerId);

    JoinRequest request = new JoinRequest(expectedPlayerName);
    GridWorldState state = createWorldState(expectedWorldName, 10, 10);

    // act
    CommandResult result = engine.joinEntity(request, state);

    //verify
    assertEquals(
        CommandResult.succeeded(expectedPlayerName, expectedWorldName, expectedSuccessReason),
        result);
    assertEquals(1, state.takenPositions().size());
    assertEquals(1, state.entities().size());
    assertEquals(0, state.moveIntents().size());
  }

  @Test
  public void joinCommand_validJoinRequest_playerWithGivenNameAlreadyExistsError() {
    //setup
    // add existing player
    String existingPlayerName = "cl1";
    String expectedWorldName = "arena-x";
    String expectedErrorReason = String.format("Player with name %s already exists",
        existingPlayerName);
    JoinRequest request = new JoinRequest(existingPlayerName);
    GridWorldState state = createWorldState(expectedWorldName, 10, 10);

    CommandResult unused = engine.joinEntity(request, state);
    // add new player with same name(same join request)
    CommandResult result2 = engine.joinEntity(request, state);

    //verify
    assertEquals(
        CommandResult.failed(existingPlayerName, expectedWorldName, expectedErrorReason),
        result2);
    assertEquals(1, state.takenPositions().size());
    assertEquals(1, state.entities().size());
    assertEquals(0, state.moveIntents().size());
  }

  @Test
  public void joinCommand_validJoinRequest_worldFullError() {
    //setup
    // add existing player
    String existingPlayerName = "cl1";
    String newPlayerName = "cl2";
    String expectedWorldName = "arena-x";
    String expectedErrorReason = "Could not find free position in world.";

    // creating world with only one position
    GridWorldState state = createWorldState(expectedWorldName, 1, 1);

    CommandResult unused = engine.joinEntity(new JoinRequest(existingPlayerName), state);

    CommandResult result2 = engine.joinEntity(new JoinRequest(newPlayerName), state);

    //verify
    assertEquals(
        CommandResult.failed(newPlayerName, expectedWorldName, expectedErrorReason),
        result2);
    assertEquals(1, state.takenPositions().size());
    assertEquals(1, state.entities().size());
    assertEquals(0, state.moveIntents().size());
  }


  private GridWorldState createWorldState(String name, int width, int height) {
    return new GridWorldState(name, width, height);
  }

}