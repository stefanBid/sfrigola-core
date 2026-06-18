package com.sb.sfrigola_core.domains.users.repository;

import com.sb.sfrigola_core.domains.users.entity.SCRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ISCRoleRepository extends JpaRepository<SCRole, Long> {
}
