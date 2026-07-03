package com.sb.sfrigola_core.domains.categories.repository;

import com.sb.sfrigola_core.domains.categories.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
            SELECT COALESCE(MAX(c.sortOrder), -1) FROM Category c
            WHERE (:parentId IS NULL AND c.parent IS NULL)
               OR (:parentId IS NOT NULL AND c.parent.id = :parentId)
            """)
    Integer findMaxSortOrderInGroup(@Param("parentId") Long parentId);

    @Query("""
            SELECT c.id FROM Category c
            LEFT JOIN c.parent p
            JOIN c.translations t
            JOIN t.language l
            WHERE (:isActive IS NULL OR c.isActive = :isActive)
              AND l.code = :locale
              AND (:searchKey IS NULL
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%'))
                   OR LOWER(t.description) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
            ORDER BY COALESCE(p.sortOrder, c.sortOrder) ASC,
                     CASE WHEN p IS NULL THEN 0 ELSE 1 END ASC,
                     c.sortOrder ASC
            """)
    Page<Long> findIdsByLocaleAndIsActiveAndSearchKeyAsc(
            @Param("locale") String locale,
            @Param("isActive") Boolean isActive,
            @Param("searchKey") String searchKey,
            Pageable pageable);

    @Query("""
            SELECT c.id FROM Category c
            LEFT JOIN c.parent p
            JOIN c.translations t
            JOIN t.language l
            WHERE (:isActive IS NULL OR c.isActive = :isActive)
              AND l.code = :locale
              AND (:searchKey IS NULL
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%'))
                   OR LOWER(t.description) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
            ORDER BY COALESCE(p.sortOrder, c.sortOrder) DESC,
                     CASE WHEN p IS NULL THEN 0 ELSE 1 END ASC,
                     c.sortOrder DESC
            """)
    Page<Long> findIdsByLocaleAndIsActiveAndSearchKeyDesc(
            @Param("locale") String locale,
            @Param("isActive") Boolean isActive,
            @Param("searchKey") String searchKey,
            Pageable pageable);

    @Query(value = """
            SELECT c.id FROM Category c
            LEFT JOIN c.parent p
            JOIN c.translations t
            WHERE (:isActive IS NULL OR c.isActive = :isActive)
              AND (:searchKey IS NULL
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%'))
                   OR LOWER(t.description) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
            GROUP BY c.id, p.id, p.sortOrder, c.sortOrder
            ORDER BY COALESCE(p.sortOrder, c.sortOrder) ASC,
                     CASE WHEN p IS NULL THEN 0 ELSE 1 END ASC,
                     c.sortOrder ASC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT c.id) FROM Category c
            JOIN c.translations t
            WHERE (:isActive IS NULL OR c.isActive = :isActive)
              AND (:searchKey IS NULL
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%'))
                   OR LOWER(t.description) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
            """)
    Page<Long> findIdsByIsActiveAndSearchKeyAsc(
            @Param("isActive") Boolean isActive,
            @Param("searchKey") String searchKey,
            Pageable pageable);

    @Query(value = """
            SELECT c.id FROM Category c
            LEFT JOIN c.parent p
            JOIN c.translations t
            WHERE (:isActive IS NULL OR c.isActive = :isActive)
              AND (:searchKey IS NULL
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%'))
                   OR LOWER(t.description) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
            GROUP BY c.id, p.id, p.sortOrder, c.sortOrder
            ORDER BY COALESCE(p.sortOrder, c.sortOrder) DESC,
                     CASE WHEN p IS NULL THEN 0 ELSE 1 END ASC,
                     c.sortOrder DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT c.id) FROM Category c
            JOIN c.translations t
            WHERE (:isActive IS NULL OR c.isActive = :isActive)
              AND (:searchKey IS NULL
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%'))
                   OR LOWER(t.description) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
            """)
    Page<Long> findIdsByIsActiveAndSearchKeyDesc(
            @Param("isActive") Boolean isActive,
            @Param("searchKey") String searchKey,
            Pageable pageable);

    @Query("""
            SELECT c FROM Category c
            LEFT JOIN FETCH c.parent p
            JOIN FETCH c.translations t
            JOIN FETCH t.language l
            WHERE c.id IN :ids AND l.code = :locale
            """)
    List<Category> findByIdsWithSpecificTranslation(@Param("ids") List<Long> ids, @Param("locale") String locale);

    @Query("""
            SELECT DISTINCT c FROM Category c
            LEFT JOIN FETCH c.parent p
            LEFT JOIN FETCH c.translations t
            LEFT JOIN FETCH t.language l
            WHERE c.id IN :ids
            """)
    List<Category> findByIdsWithAllTranslations(@Param("ids") List<Long> ids);

    @Query("""
            SELECT DISTINCT c FROM Category c
            LEFT JOIN FETCH c.parent
            LEFT JOIN FETCH c.translations t
            LEFT JOIN FETCH t.language l
            WHERE c.publicId = :publicId
            """)
    Optional<Category> findByPublicIdWithAllTranslation(@Param("publicId") UUID publicId);

    Optional<Category> findByPublicId(UUID publicId);

    boolean existsBySlug(String slug);
    boolean existsByParentId(Long parentId);
    List<Category> findByParentIsNull();
    List<Category> findByParentId(Long parentId);
}