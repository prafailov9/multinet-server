package com.ntros.persistence.model;

public class SystemRoleRecord {

  private long systemRoleId;
  private String systemRoleName;

  public SystemRoleRecord(String systemRoleName) {
    this.systemRoleName = systemRoleName;
  }

  public SystemRoleRecord(long systemRoleId, String systemRoleName) {
    this.systemRoleId = systemRoleId;
    this.systemRoleName = systemRoleName;
  }

  public long getSystemRoleId() {
    return systemRoleId;
  }

  public void setSystemRoleId(long systemRoleId) {
    this.systemRoleId = systemRoleId;
  }

  public String getSystemRoleName() {
    return systemRoleName;
  }

  public void setSystemRoleName(String systemRoleName) {
    this.systemRoleName = systemRoleName;
  }
}
