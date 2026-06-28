package com.sb.sfrigola_core.domains.users.repository;

import com.sb.sfrigola_core.domains.users.entity.SCUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ISCUserRepository extends JpaRepository<SCUser, Long> {

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    Optional<SCUser> findByPublicId(UUID publicId);

    @Query("""
            SELECT u FROM SCUser u
            WHERE (:searchKey IS NULL OR LOWER(u.username)  LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%'))
                                     OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%'))
                                     OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%'))
                                     OR LOWER(u.email)     LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
            AND   (:active IS NULL OR u.isActive = :active)
    """)
    Page<SCUser> findAllUsersByParams(
            Pageable pageable,
            @Param("searchKey") String searchKey,
            @Param("active") Boolean active
    );

    @Query("SELECT u FROM SCUser u JOIN FETCH u.role WHERE u.email = :username")
    Optional<SCUser> findByEmailWithRole(String username);

    @Modifying
    @Query("UPDATE SCUser u SET u.preferredLang = :preferredLang, u.updatedAt = :updatedAt, u.updatedBy = :updatedBy WHERE u.publicId = :publicId")
    int updatePreferredLang(@Param("publicId") UUID publicId,
                            @Param("preferredLang") String preferredLang,
                            @Param("updatedAt") Instant updatedAt,
                            @Param("updatedBy") String updatedBy);


    @Modifying
    @Query("UPDATE SCUser u SET u.email = :email, u.updatedAt = :updatedAt, u.updatedBy = :updatedBy WHERE u.publicId = :publicId")
    int updateEmail(@Param("publicId") UUID publicId,
                    @Param("email") String email,
                    @Param("updatedAt") Instant updatedAt,
                    @Param("updatedBy") String updatedBy);

    @Modifying
    @Query("UPDATE SCUser u SET u.passwordHash = :newHashedPassword, u.updatedAt = :updatedAt, u.updatedBy = :updatedBy WHERE u.publicId = :publicId")
    int updatePasswordByPublicId(@Param("publicId") UUID publicId,
                                 @Param("newHashedPassword") String newHashedPassword,
                                 @Param("updatedAt") Instant updatedAt,
                                 @Param("updatedBy") String updatedBy);

    @Modifying
    @Query("UPDATE SCUser u SET u.isActive = :active, u.updatedAt = :updatedAt, u.updatedBy = :updatedBy WHERE u.publicId = :publicId")
    int updateActiveByPublicId(@Param("publicId") UUID publicId,
                               @Param("active") boolean active,
                               @Param("updatedAt") Instant updatedAt,
                               @Param("updatedBy") String updatedBy);

    @Modifying
    @Query("""
            UPDATE SCUser u SET u.role = (SELECT r FROM SCRole r WHERE r.name = :roleName),
            u.updatedAt = :updatedAt, u.updatedBy = :updatedBy
            WHERE u.publicId = :publicId
    """)
    int updateRoleByPublicId(@Param("publicId")
                             UUID publicId,
                             @Param("roleName")
                             String roleName,
                             @Param("updatedAt")
                             Instant updatedAt,
                             @Param("updatedBy")
                             String updatedBy);

}
