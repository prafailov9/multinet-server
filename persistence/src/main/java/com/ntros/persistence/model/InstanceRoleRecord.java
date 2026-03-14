package com.ntros.persistence.model;

/**
 * DTO for a row in {@code clients_instance_roles} joined with {@code instance_roles}.
 * <p>
 * A role is always world-scoped: a client can be ADMIN in world "arena-1" and PLAYER in
 * "arena-2" simultaneously. {@code worldName} carries that scope so callers never need
 * a second lookup.
 */
public class InstanceRoleRecord {

  private long instanceRoleId;
  private String instanceRoleName;
  private String worldName;

  /** Convenience constructor — id left as 0 (not yet persisted or not needed). */
  public InstanceRoleRecord(String instanceRoleName, String worldName) {
    this.instanceRoleName = instanceRoleName;
    this.worldName = worldName;
  }

  public InstanceRoleRecord(long instanceRoleId, String instanceRoleName, String worldName) {
    this.instanceRoleId = instanceRoleId;
    this.instanceRoleName = instanceRoleName;
    this.worldName = worldName;
  }

  public long getInstanceRoleId() {
    return instanceRoleId;
  }

  public void setInstanceRoleId(long instanceRoleId) {
    this.instanceRoleId = instanceRoleId;
  }

  public String getInstanceRoleName() {
    return instanceRoleName;
  }

  public void setInstanceRoleName(String instanceRoleName) {
    this.instanceRoleName = instanceRoleName;
  }

  public String getWorldName() {
    return worldName;
  }

  public void setWorldName(String worldName) {
    this.worldName = worldName;
  }

  @Override
  public String toString() {
    return "InstanceRoleRecord{world='" + worldName + "', role='" + instanceRoleName + "'}";
  }
}
