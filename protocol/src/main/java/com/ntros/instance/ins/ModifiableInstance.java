package com.ntros.instance.ins;

import com.ntros.event.sessionmanager.SessionManager;
import com.ntros.instance.runner.InstanceRunner;
import com.ntros.model.entity.config.access.InstanceConfig;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.MoveRequest;
import com.ntros.session.Session;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModifiableInstance extends AbstractInstance {

  private final AtomicBoolean tickerRunning = new AtomicBoolean(false);

  ModifiableInstance(WorldConnector worldConnector,
      InstanceRunner instanceRunner,
      SessionManager sessionManager) {
    super(worldConnector, instanceRunner, sessionManager);
  }

  @Override
  public void startIfNeededForJoin() {

  }

  @Override
  public CompletableFuture<CommandResult> joinAsync(JoinRequest req) {
    return null;
  }

  @Override
  public CommandResult move(MoveRequest req) {
    return null;
  }

  @Override
  public void removeEntity(String entityId) {

  }

  @Override
  public void onWelcomeSent(Session session) {

  }

  @Override
  public void run() {

//    if (!tickerRunning.get()) {
//      instanceRunner.getTicker().tick(() -> {
//        instanceRunner.getBroadcaster().publish(worldConnector.serialize(), sessionManager);
//      });
//    }


  }

  @Override
  public InstanceConfig getConfig() {
    return null;
  }

  @Override
  public WorldConnector getWorldConnector() {
    return null;
  }

  @Override
  public String getWorldName() {
    return worldConnector.getWorldName();
  }

  @Override
  public void reset() {

  }

  @Override
  public void registerSession(Session session) {

  }

  @Override
  public void removeSession(Session session) {

  }

  @Override
  public Session getSession(Long sessionId) {
    return null;
  }

  @Override
  public int getActiveSessionsCount() {
    return 0;
  }

  @Override
  public int getEntityCount() {
    return 0;
  }

  @Override
  public boolean isRunning() {
    return false;
  }

  @Override
  public void pause() {

  }

  @Override
  public void resume() {

  }

  @Override
  public void updateTickRate(int ticksPerSecond) {

  }

  @Override
  public void updateWorldState(Map<Boolean, Boolean> worldStateUpdates) {

  }
}
