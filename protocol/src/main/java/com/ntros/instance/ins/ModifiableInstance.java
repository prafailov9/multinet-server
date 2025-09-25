package com.ntros.instance.ins;

import com.ntros.event.listener.SessionManager;
import com.ntros.instance.runner.InstanceRunner;
import com.ntros.model.entity.config.access.WorldConfig;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.session.Session;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModifiableInstance extends AbstractInstance {

  private final AtomicBoolean tickerRunning = new AtomicBoolean(false);

  ModifiableInstance(WorldConnector worldConnector,
      InstanceRunner instanceRunner,
      SessionManager sessionManager) {
    super(worldConnector, instanceRunner, sessionManager);
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
  public WorldConfig getWorldPolicy() {
    return null;
  }

  @Override
  public String worldName() {
    return worldConnector.worldName();
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
