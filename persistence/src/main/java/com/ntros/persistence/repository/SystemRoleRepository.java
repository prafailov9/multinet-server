package com.ntros.persistence.repository;

import com.ntros.persistence.model.SystemRoleRecord;
import java.util.List;
import java.util.Optional;

public interface SystemRoleRepository {

  List<SystemRoleRecord> findAll();
  Optional<SystemRoleRecord> findByName(String systemRoleName);

}
