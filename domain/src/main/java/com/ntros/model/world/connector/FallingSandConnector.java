package com.ntros.model.world.connector;

import com.ntros.model.entity.Entity;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.ops.WorldOp;
import com.ntros.model.world.protocol.result.WorldResult;
import java.util.List;

public class FallingSandConnector implements WorldConnector {

  @Override
  public WorldResult apply(WorldOp op) {
    return null;
  }

  @Override
  public void update() {

  }

  @Override
  public Object snapshot() {
    return null;
  }

  @Override
  public String snapshot(boolean oneLine) {
    return "";
  }

  @Override
  public String getWorldName() {
    return "";
  }

  @Override
  public String getWorldType() {
    return "";
  }

  @Override
  public List<Entity> getCurrentEntities() {
    return List.of();
  }

  @Override
  public WorldCapabilities getCapabilities() {
    return null;
  }

  @Override
  public void reset() {

  }
}
