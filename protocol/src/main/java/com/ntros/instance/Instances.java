package com.ntros.instance;

import com.ntros.instance.ins.Instance;
import com.ntros.model.entity.config.access.Settings;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Instances {

  /// Keyed by WorldName
  private static final Map<String, Instance> INSTANCES = new ConcurrentHashMap<>();
  private static final Map<String, Settings> CONFIG_MAP = new ConcurrentHashMap<>();

  public static void registerInstance(Instance instance) {
    INSTANCES.put(instance.getWorldName(), instance);
    CONFIG_MAP.put(instance.getWorldName(), instance.getSettings());
  }

  public static Instance getInstance(String worldName) {
    return INSTANCES.get(worldName);
  }

  public static Settings getInstanceConfigForWorld(String worldName) {
    return CONFIG_MAP.get(worldName);
  }

  public static Collection<Instance> getAll() {
    return INSTANCES.values();
  }

  public static void clear() {
    INSTANCES.clear();
  }
}
