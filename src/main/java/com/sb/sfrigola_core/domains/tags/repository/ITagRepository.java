package com.sb.sfrigola_core.domains.tags.repository;

import com.sb.sfrigola_core.domains.tags.entity.Tag;
import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
import com.sb.sfrigola_core.domains.tags.enums.TagType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ITagRepository extends JpaRepository<Tag, Long> {

    @Query("""
            SELECT t.id FROM Tag t
            JOIN t.translations tr
            JOIN tr.language l
            WHERE l.code = :locale
                AND (:searchKey IS NULL OR LOWER(tr.label) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:status IS NULL OR t.status = :status)
                AND (:scope IS NULL OR t.scope = :scope)
                AND (:type IS NULL OR t.type = :type)
            ORDER BY tr.label ASC
           """)
    Page<Long> findIdsByFiltersAndLocale(
            @Param("locale") String locale,
            @Param("searchKey") String searchKey,
            @Param("status") TagStatus status,
            @Param("scope") TagScope scope,
            @Param("type") TagType type,
            Pageable pageable
    );


    @Query(value = """
            SELECT t.id FROM Tag t
            JOIN t.translations tr
            WHERE (:searchKey IS NULL OR LOWER(tr.label) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:status IS NULL OR t.status = :status)
                AND (:scope IS NULL OR t.scope = :scope)
                AND (:type IS NULL OR t.type = :type)
            GROUP BY t.id
            ORDER BY MIN(tr.label) ASC
           """,
            countQuery = """
            SELECT COUNT(DISTINCT t.id) FROM Tag t
            JOIN t.translations tr
            WHERE (:searchKey IS NULL OR LOWER(tr.label) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:status IS NULL OR t.status = :status)
                AND (:scope IS NULL OR t.scope = :scope)
                AND (:type IS NULL OR t.type = :type)
           """
    )
    Page<Long> findIdsByFilters(
            @Param("searchKey") String searchKey,
            @Param("status") TagStatus status,
            @Param("scope") TagScope scope,
            @Param("type") TagType type,
            Pageable pageable
    );


    @Query("""
            SELECT t FROM Tag t
            JOIN FETCH t.translations tr
            JOIN FETCH tr.language l
            WHERE t.id IN :ids AND l.code = :locale
            """)
    List<Tag> findByIdsWithSpecificTranslation(
            @Param("ids") List<Long> ids,
            @Param("locale") String locale
    );

    @Query("""
            SELECT DISTINCT t FROM Tag t
            LEFT JOIN FETCH t.translations tr
            LEFT JOIN FETCH tr.language l
            WHERE t.id IN :ids
            """)
    List<Tag> findByIdsWithAllTranslations(@Param("ids") List<Long> ids);

    @Query("""
            SELECT DISTINCT t FROM Tag t
            LEFT JOIN FETCH t.translations tr
            LEFT JOIN FETCH tr.language l
            WHERE t.publicId = :publicId
            """)
    Optional<Tag> findByPublicIdWithAllTranslation(@Param("publicId") UUID publicId);

    Optional<Tag> findByPublicId(UUID publicId);

    boolean existsBySlug(String slug);
}
