package com.ntros.instance;

import com.ntros.instance.ins.Instance;
import com.ntros.session.Session;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InstanceRegistry {

  /// Keyed by WorldName
  private static final Map<String, Instance> INSTANCES = new ConcurrentHashMap<>();

  public static void register(Instance instance) {
    INSTANCES.put(instance.getWorldName(), instance);
  }

  public static Instance get(String worldName) {
    return INSTANCES.get(worldName);
  }

  public static Collection<Instance> getAll() {
    return INSTANCES.values();
  }

  public static Session getActiveSession(String worldName, Long sessionId) {
    Instance instance = INSTANCES.get(worldName);
    return instance.getSession(sessionId);
  }

  public static void clear() {
    INSTANCES.clear();
  }
}
