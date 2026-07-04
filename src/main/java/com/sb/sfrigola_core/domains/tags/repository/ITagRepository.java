package com.sb.sfrigola_core.domains.tags.repository;

import com.sb.sfrigola_core.domains.tags.entity.Tag;
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
                AND (:status IS NULL OR CAST(t.status AS string) = :status)
                AND (:scope IS NULL OR CAST(t.scope AS string) = :scope)
                AND (:type IS NULL OR CAST(t.type AS string) = :type)
            ORDER BY tr.label ASC
           """)
    Page<Long> findIdsByFiltersAndLocaleAsc(
            @Param("locale") String locale,
            @Param("searchKey") String searchKey,
            @Param("status") String status,
            @Param("scope") String scope,
            @Param("type") String type,
            Pageable pageable
    );

    @Query("""
            SELECT t.id FROM Tag t
            JOIN t.translations tr
            JOIN tr.language l
            WHERE l.code = :locale
                AND (:searchKey IS NULL OR LOWER(tr.label) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:status IS NULL OR CAST(t.status AS string) = :status)
                AND (:scope IS NULL OR CAST(t.scope AS string) = :scope)
                AND (:type IS NULL OR CAST(t.type AS string) = :type)
            ORDER BY tr.label DESC
           """)
    Page<Long> findIdsByFiltersAndLocaleDesc(
            @Param("locale") String locale,
            @Param("searchKey") String searchKey,
            @Param("status") String status,
            @Param("scope") String scope,
            @Param("type") String type,
            Pageable pageable
    );


    @Query(value = """
            SELECT t.id FROM Tag t
            JOIN t.translations tr
            WHERE (:searchKey IS NULL OR LOWER(tr.label) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:status IS NULL OR CAST(t.status AS string) = :status)
                AND (:scope IS NULL OR CAST(t.scope AS string) = :scope)
                AND (:type IS NULL OR CAST(t.type AS string) = :type)
            GROUP BY t.id
            ORDER BY MIN(tr.label) ASC
           """,
            countQuery = """
            SELECT COUNT(DISTINCT t.id) FROM Tag t
            JOIN t.translations tr
            WHERE (:searchKey IS NULL OR LOWER(tr.label) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:status IS NULL OR CAST(t.status AS string) = :status)
                AND (:scope IS NULL OR CAST(t.scope AS string) = :scope)
                AND (:type IS NULL OR CAST(t.type AS string) = :type)
           """
    )
    Page<Long> findIdsByFiltersAsc(
            @Param("searchKey") String searchKey,
            @Param("status") String status,
            @Param("scope") String scope,
            @Param("type") String type,
            Pageable pageable
    );

    @Query(value = """
            SELECT t.id FROM Tag t
            JOIN t.translations tr
            WHERE (:searchKey IS NULL OR LOWER(tr.label) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:status IS NULL OR CAST(t.status AS string) = :status)
                AND (:scope IS NULL OR CAST(t.scope AS string) = :scope)
                AND (:type IS NULL OR CAST(t.type AS string) = :type)
            GROUP BY t.id
            ORDER BY MIN(tr.label) DESC
           """,
            countQuery = """
            SELECT COUNT(DISTINCT t.id) FROM Tag t
            JOIN t.translations tr
            WHERE (:searchKey IS NULL OR LOWER(tr.label) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:status IS NULL OR CAST(t.status AS string) = :status)
                AND (:scope IS NULL OR CAST(t.scope AS string) = :scope)
                AND (:type IS NULL OR CAST(t.type AS string) = :type)
           """
    )
    Page<Long> findIdsByFiltersDesc(
            @Param("searchKey") String searchKey,
            @Param("status") String status,
            @Param("scope") String scope,
            @Param("type") String type,
            Pageable pageable
    );

    @Query("""
            SELECT t.id FROM Tag t
            JOIN t.translations tr
            JOIN tr.language l
            WHERE l.code = :locale
                AND (:searchKey IS NULL OR LOWER(tr.label) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:status IS NULL OR CAST(t.status AS string) = :status)
                AND (:scope IS NULL OR CAST(t.scope AS string) = :scope)
                AND (:type IS NULL OR CAST(t.type AS string) = :type)
           """)
    Page<Long> findIdsByFiltersAndLocaleOtherSort(
            @Param("locale") String locale,
            @Param("searchKey") String searchKey,
            @Param("status") String status,
            @Param("scope") String scope,
            @Param("type") String type,
            Pageable pageable
    );

    @Query(value = """
            SELECT t.id FROM Tag t
            JOIN t.translations tr
            WHERE (:searchKey IS NULL OR LOWER(tr.label) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:status IS NULL OR CAST(t.status AS string) = :status)
                AND (:scope IS NULL OR CAST(t.scope AS string) = :scope)
                AND (:type IS NULL OR CAST(t.type AS string) = :type)
            GROUP BY t.id
           """,
            countQuery = """
            SELECT COUNT(DISTINCT t.id) FROM Tag t
            JOIN t.translations tr
            WHERE (:searchKey IS NULL OR LOWER(tr.label) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:status IS NULL OR CAST(t.status AS string) = :status)
                AND (:scope IS NULL OR CAST(t.scope AS string) = :scope)
                AND (:type IS NULL OR CAST(t.type AS string) = :type)
           """
    )
    Page<Long> findIdsByFiltersOtherSort(
            @Param("searchKey") String searchKey,
            @Param("status") String status,
            @Param("scope") String scope,
            @Param("type") String type,
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

    List<Tag> findByPublicIdIn(List<UUID> publicIds);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT COUNT(tr) > 0 FROM Tag t
            JOIN t.translations tr
            JOIN tr.language l
            WHERE LOWER(tr.label) = LOWER(:label) AND l.code = :locale
           """)
    boolean existsByLabelAndLanguage(@Param("label") String label, @Param("locale") String locale);
}
