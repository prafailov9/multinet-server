package com.ntros.lifecycle.clock.instance.actor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntros.lifecycle.sessionmanager.SessionManager;
import com.ntros.lifecycle.instance.actor.Actor;
import com.ntros.lifecycle.instance.actor.WorldActor;
import com.ntros.lifecycle.session.Session;
import com.ntros.lifecycle.session.SessionContext;
import com.ntros.model.entity.movement.MoveInput;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.connector.ops.MoveOp;
import com.ntros.model.world.connector.ops.RemoveOp;
import com.ntros.model.world.protocol.request.MoveRequest;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorldActorTest {

  ///  One-Step directional moves
  private static final MoveInput MOVE_UP = new MoveInput(0, 1, 0, 0);
  private static final MoveInput MOVE_DOWN = new MoveInput(0, -1, 0, 0);

  private static final MoveInput MOVE_RIGHT = new MoveInput(1, 0, 0, 0);
  private static final MoveInput MOVE_LEFT = new MoveInput(-1, 0, 0, 0);


  private final Runnable NO_OP_TASK = () -> {
  };

  @Mock
  private WorldConnector world;
  @Mock
  private SessionManager sessionManager;
  @Mock
  private Session session;

  private Actor actor;

  @BeforeEach
  void setUp() {
    actor = new WorldActor(true, "arena-1");
  }

  @AfterEach
  void tearDown() {
    actor.shutdown();
  }

  @Test
  void step_flushesMoves_updatesWorld_runsCallback_and_clearsStaged() {
    when(world.getWorldName()).thenReturn("arena-1"); // only where needed

    // setup
    // queue 2 moves for same client. Last write wins
    actor.stageMove(world, new MoveRequest("c1", new MoveInput(0, 1, 0, 0)));
    actor.stageMove(world, new MoveRequest("c1", new MoveInput(1, 0, 0, 0)));

    // act
    var onAfter = new AtomicBoolean(false);
    actor.step(world, () -> onAfter.set(true)).join();

    // verify
    // only last move should be applied
    ArgumentCaptor<MoveOp> moveOp = ArgumentCaptor.forClass(MoveOp.class);
    verify(world).apply(moveOp.capture());
    assertThat(moveOp.getValue().req().playerId()).isEqualTo("c1");
    assertThat(moveOp.getValue().req().moveInput()).isEqualTo(MOVE_RIGHT);

    // world updated and callback ran
    verify(world).update();
    assertThat(onAfter.get()).isEqualTo(true);

    // subsequent step should NOT re-apply old moves
    reset(world);
    // force all tasks executed with no-op and block with .join()
    actor.step(world, NO_OP_TASK).join();
    verify(world, never()).apply(any(MoveOp.class));
  }

  @Test
  void leave_removesEntity_dropsStagedMove_then_deregisters() {
    var ctx = mock(SessionContext.class);
    when(session.getSessionContext()).thenReturn(ctx);
    when(ctx.getEntityId()).thenReturn("e1");

    // stage a move for e1 and then leave
    actor.stageMove(world, new MoveRequest("e1", MOVE_DOWN));
    actor.leave(world, sessionManager, session).join();

    InOrder order = inOrder(world, sessionManager);
    order.verify(world).apply(argThat(op -> op instanceof RemoveOp));
    order.verify(sessionManager).remove(session);

    // ensure staged move dropped (no lingering move if we step)
    reset(world);
    actor.step(world, NO_OP_TASK).join();
    verify(world, never()).apply(any(MoveOp.class));
  }

  @Test
  void execute_propagatesExceptions() {
    var fut = actor.tell(() -> {
      throw new RuntimeException("boom");
    });
    assertThatThrownBy(fut::join).hasCauseInstanceOf(RuntimeException.class);
  }

}