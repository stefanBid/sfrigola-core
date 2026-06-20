package com.sb.sfrigola_core.domains.users.repository;

import com.sb.sfrigola_core.domains.users.entity.SCUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ISCUserRepository extends JpaRepository<SCUser, Long> {

    boolean existsByEmail(String email);

    Optional<SCUser> findByPublicId(UUID publicId);

    @Query("SELECT u FROM SCUser u JOIN FETCH u.role WHERE u.email = :username")
    Optional<SCUser> findByEmailWithRole(String username);

    @Modifying
    @Query("UPDATE SCUser u SET u.preferredLang = :preferredLang, u.updatedAt = :updatedAt, u.updatedBy = :updatedBy WHERE u.publicId = :publicId")
    int updatePreferredLang(@Param("publicId") UUID publicId, @Param("preferredLang") String preferredLang, @Param("updatedAt") java.time.Instant updatedAt, @Param("updatedBy") String updatedBy);

}
