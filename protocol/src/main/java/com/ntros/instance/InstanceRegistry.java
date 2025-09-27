package com.ntros.instance;

import com.ntros.instance.ins.Instance;
import com.ntros.model.entity.config.access.InstanceConfig;
import com.ntros.session.Session;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InstanceRegistry {

  /// Keyed by WorldName
  private static final Map<String, Instance> INSTANCES = new ConcurrentHashMap<>();
  private static final Map<String, InstanceConfig> CONFIG_MAP = new ConcurrentHashMap<>();

  public static void registerInstance(Instance instance) {
    INSTANCES.put(instance.getWorldName(), instance);
    CONFIG_MAP.put(instance.getWorldName(), instance.getConfig());
  }

  public static Instance getInstance(String worldName) {
    return INSTANCES.get(worldName);
  }

  public static InstanceConfig getInstanceConfigForWorld(String worldName) {
    return CONFIG_MAP.get(worldName);
  }

  public static Collection<Instance> getAll() {
    return INSTANCES.values();
  }

  public static void clear() {
    INSTANCES.clear();
  }
}
