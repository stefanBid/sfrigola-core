package com.sb.sfrigola_core.domains.users.repository;

import com.sb.sfrigola_core.domains.users.entity.SCUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ISCUserRepository extends JpaRepository<SCUser, Long> {

    @Query("SELECT u FROM SCUser u JOIN FETCH u.role WHERE u.email = :username")
    Optional<SCUser> findByEmailWithRole(String username);

}
