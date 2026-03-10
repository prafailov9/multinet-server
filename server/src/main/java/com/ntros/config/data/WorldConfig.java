package com.ntros.config.data;

import java.util.List;

public class WorldConfig {

  private List<CfgWorld> cfgWorlds;

  public List<CfgWorld> getWorlds() {
    return cfgWorlds;
  }

  public void setWorlds(List<CfgWorld> cfgWorlds) {
    this.cfgWorlds = cfgWorlds;
  }
}