package com.ntros.ticker;

public class ManualTicker implements Ticker {

  @Override
  public void tick(Runnable task) {

  }

  @Override
  public void stop() {

  }

  @Override
  public void shutdown() {

  }

  @Override
  public void updateTickRate(int ticksPerSecond) {

  }

  @Override
  public int getTickRate() {
    return 0;
  }

  @Override
  public void pause() {

  }

  @Override
  public void resume() {

  }

  @Override
  public boolean isPaused() {
    return false;
  }

  @Override
  public void updateTask(Runnable task) {

  }

  @Override
  public void setListener(TickListener listener) {

  }
}
