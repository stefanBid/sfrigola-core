package com.sb.sfrigola_core.domains.recipes.repository;

import com.sb.sfrigola_core.domains.recipes.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRecipeRepository extends JpaRepository<Recipe, Long> {

    @Query(value = """
            SELECT r.id FROM Recipe r
            LEFT JOIN r.translations tr ON tr.language.code = :locale
            WHERE (:searchKey IS NULL OR LOWER(tr.title) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:isPublished IS NULL OR r.isPublished = :isPublished)
                AND (:difficulty IS NULL OR CAST(r.difficulty AS string) = :difficulty)
                AND (:mealType IS NULL OR CAST(r.mealType AS string) = :mealType)
                AND (:season IS NULL OR CAST(r.season AS string) = :season)
                AND (:isVegetarian IS NULL OR r.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR r.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR r.isGlutenFree = :isGlutenFree)
                AND (:categoryId IS NULL OR r.category.id = :categoryId)
            ORDER BY tr.title ASC
           """,
            countQuery = """
            SELECT COUNT(r.id) FROM Recipe r
            LEFT JOIN r.translations tr ON tr.language.code = :locale
            WHERE (:searchKey IS NULL OR LOWER(tr.title) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:isPublished IS NULL OR r.isPublished = :isPublished)
                AND (:difficulty IS NULL OR CAST(r.difficulty AS string) = :difficulty)
                AND (:mealType IS NULL OR CAST(r.mealType AS string) = :mealType)
                AND (:season IS NULL OR CAST(r.season AS string) = :season)
                AND (:isVegetarian IS NULL OR r.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR r.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR r.isGlutenFree = :isGlutenFree)
                AND (:categoryId IS NULL OR r.category.id = :categoryId)
           """)
    Page<Long> findIdsByFiltersAndLocaleAsc(
            @Param("locale") String locale,
            @Param("searchKey") String searchKey,
            @Param("isPublished") Boolean isPublished,
            @Param("difficulty") String difficulty,
            @Param("mealType") String mealType,
            @Param("season") String season,
            @Param("isVegetarian") Boolean isVegetarian,
            @Param("isVegan") Boolean isVegan,
            @Param("isGlutenFree") Boolean isGlutenFree,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @Query(value = """
            SELECT r.id FROM Recipe r
            JOIN r.translations tr ON tr.language.code = :locale
            WHERE (:searchKey IS NULL OR LOWER(tr.title) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:isPublished IS NULL OR r.isPublished = :isPublished)
                AND (:difficulty IS NULL OR CAST(r.difficulty AS string) = :difficulty)
                AND (:mealType IS NULL OR CAST(r.mealType AS string) = :mealType)
                AND (:season IS NULL OR CAST(r.season AS string) = :season)
                AND (:isVegetarian IS NULL OR r.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR r.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR r.isGlutenFree = :isGlutenFree)
                AND (:categoryId IS NULL OR r.category.id = :categoryId)
            ORDER BY tr.title DESC
           """,
            countQuery = """
            SELECT COUNT(r.id) FROM Recipe r
            JOIN r.translations tr ON tr.language.code = :locale
            WHERE (:searchKey IS NULL OR LOWER(tr.title) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:isPublished IS NULL OR r.isPublished = :isPublished)
                AND (:difficulty IS NULL OR CAST(r.difficulty AS string) = :difficulty)
                AND (:mealType IS NULL OR CAST(r.mealType AS string) = :mealType)
                AND (:season IS NULL OR CAST(r.season AS string) = :season)
                AND (:isVegetarian IS NULL OR r.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR r.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR r.isGlutenFree = :isGlutenFree)
                AND (:categoryId IS NULL OR r.category.id = :categoryId)
           """)
    Page<Long> findIdsByFiltersAndLocaleDesc(
            @Param("locale") String locale,
            @Param("searchKey") String searchKey,
            @Param("isPublished") Boolean isPublished,
            @Param("difficulty") String difficulty,
            @Param("mealType") String mealType,
            @Param("season") String season,
            @Param("isVegetarian") Boolean isVegetarian,
            @Param("isVegan") Boolean isVegan,
            @Param("isGlutenFree") Boolean isGlutenFree,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @Query(value = """
            SELECT r.id FROM Recipe r
            JOIN r.translations tr ON tr.language.code = :locale
            WHERE (:searchKey IS NULL OR LOWER(tr.title) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:isPublished IS NULL OR r.isPublished = :isPublished)
                AND (:difficulty IS NULL OR CAST(r.difficulty AS string) = :difficulty)
                AND (:mealType IS NULL OR CAST(r.mealType AS string) = :mealType)
                AND (:season IS NULL OR CAST(r.season AS string) = :season)
                AND (:isVegetarian IS NULL OR r.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR r.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR r.isGlutenFree = :isGlutenFree)
                AND (:categoryId IS NULL OR r.category.id = :categoryId)
           """,
            countQuery = """
            SELECT COUNT(r.id) FROM Recipe r
            JOIN r.translations tr ON tr.language.code = :locale
            WHERE (:searchKey IS NULL OR LOWER(tr.title) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:isPublished IS NULL OR r.isPublished = :isPublished)
                AND (:difficulty IS NULL OR CAST(r.difficulty AS string) = :difficulty)
                AND (:mealType IS NULL OR CAST(r.mealType AS string) = :mealType)
                AND (:season IS NULL OR CAST(r.season AS string) = :season)
                AND (:isVegetarian IS NULL OR r.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR r.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR r.isGlutenFree = :isGlutenFree)
                AND (:categoryId IS NULL OR r.category.id = :categoryId)
           """)
    Page<Long> findIdsByFiltersAndLocaleOtherSort(
            @Param("locale") String locale,
            @Param("searchKey") String searchKey,
            @Param("isPublished") Boolean isPublished,
            @Param("difficulty") String difficulty,
            @Param("mealType") String mealType,
            @Param("season") String season,
            @Param("isVegetarian") Boolean isVegetarian,
            @Param("isVegan") Boolean isVegan,
            @Param("isGlutenFree") Boolean isGlutenFree,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @Query(value = """
            SELECT r.id FROM Recipe r
            JOIN r.translations tr ON tr.language.code = :locale
            WHERE r.isPublished = true
                AND (:searchKey IS NULL
                    OR LOWER(tr.title) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%'))
                    OR LOWER(tr.description) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:categoryId IS NULL OR r.category.id = :categoryId)
           """,
            countQuery = """
            SELECT COUNT(r.id) FROM Recipe r
            JOIN r.translations tr ON tr.language.code = :locale
            WHERE r.isPublished = true
                AND (:searchKey IS NULL
                    OR LOWER(tr.title) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%'))
                    OR LOWER(tr.description) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:categoryId IS NULL OR r.category.id = :categoryId)
           """)
    Page<Long> findIdsBySearchKeyAndLocale(
            @Param("locale") String locale,
            @Param("searchKey") String searchKey,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @Query("""
            SELECT r FROM Recipe r
            JOIN FETCH r.author
            JOIN FETCH r.translations tr
            JOIN FETCH tr.language l
            WHERE r.id IN :ids AND l.code = :locale
            """)
    List<Recipe> findByIdsWithSpecificTranslation(
            @Param("ids") List<Long> ids,
            @Param("locale") String locale
    );

    @Query("""
            SELECT DISTINCT r FROM Recipe r
            LEFT JOIN FETCH r.translations tr
            LEFT JOIN FETCH tr.language l
            WHERE r.id IN :ids
            """)
    List<Recipe> findByIdsWithAllTranslations(@Param("ids") List<Long> ids);

    Optional<Recipe> findByPublicId(UUID publicId);

    @Query("""
            SELECT DISTINCT r FROM Recipe r
            LEFT JOIN FETCH r.translations tr
            LEFT JOIN FETCH tr.language l
            WHERE r.publicId = :publicId
            """)
    Optional<Recipe> findByPublicIdWithAllTranslation(@Param("publicId") UUID publicId);
}
