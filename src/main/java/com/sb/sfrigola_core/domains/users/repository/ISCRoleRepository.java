package com.sb.sfrigola_core.domains.users.repository;

import com.sb.sfrigola_core.domains.users.entity.SCRole;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ISCRoleRepository extends JpaRepository<SCRole, Long> {

    @Cacheable(value = "roles", key = "'roleByName_' + #name")
    Optional<SCRole> findByName(String name);
}
