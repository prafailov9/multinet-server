package com.ntros.persistence.repository;

import com.ntros.persistence.model.InstanceRoleRecord;
import java.util.List;
import java.util.Optional;

public interface InstanceRoleRepository {

  List<InstanceRoleRecord> findAll();

  Optional<InstanceRoleRecord> findByName(String instanceRoleName);

}
