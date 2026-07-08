package com.sb.sfrigola_core.domains.ingredients.repository;

import com.sb.sfrigola_core.domains.ingredients.entity.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IIngredientRepository extends JpaRepository<Ingredient, Long> {

    @Query(value = """
            SELECT i.id FROM Ingredient i
            LEFT JOIN i.translations tr ON tr.language.code = :locale
            JOIN tr.language l
            WHERE (:searchKey IS NULL OR LOWER(tr.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:foodGroup IS NULL OR CAST(i.foodGroup AS string) = :foodGroup)
                AND (:isVegetarian IS NULL OR i.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR i.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR i.isGlutenFree = :isGlutenFree)
                AND (:minCalories IS NULL OR i.caloriesPer100g >= :minCalories)
                AND (:maxCalories IS NULL OR i.caloriesPer100g <= :maxCalories)
            ORDER BY tr.name ASC
           """,
            countQuery = """
            SELECT COUNT(i.id) FROM Ingredient i
            LEFT JOIN i.translations tr ON tr.language.code = :locale
            JOIN tr.language l
            WHERE (:searchKey IS NULL OR LOWER(tr.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:foodGroup IS NULL OR CAST(i.foodGroup AS string) = :foodGroup)
                AND (:isVegetarian IS NULL OR i.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR i.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR i.isGlutenFree = :isGlutenFree)
                AND (:minCalories IS NULL OR i.caloriesPer100g >= :minCalories)
                AND (:maxCalories IS NULL OR i.caloriesPer100g <= :maxCalories)
           """)
    Page<Long> findIdsByFiltersAndLocaleAsc(
            @Param("locale") String locale,
            @Param("searchKey") String searchKey,
            @Param("foodGroup") String foodGroup,
            @Param("isVegetarian") Boolean isVegetarian,
            @Param("isVegan") Boolean isVegan,
            @Param("isGlutenFree") Boolean isGlutenFree,
            @Param("minCalories") Double minCalories,
            @Param("maxCalories") Double maxCalories,
            Pageable pageable
    );

    @Query(value = """
            SELECT i.id FROM Ingredient i
            JOIN i.translations tr ON tr.language.code = :locale
            JOIN tr.language l
            WHERE l.code = :locale
                AND (:searchKey IS NULL OR LOWER(tr.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:foodGroup IS NULL OR CAST(i.foodGroup AS string) = :foodGroup)
                AND (:isVegetarian IS NULL OR i.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR i.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR i.isGlutenFree = :isGlutenFree)
                AND (:minCalories IS NULL OR i.caloriesPer100g >= :minCalories)
                AND (:maxCalories IS NULL OR i.caloriesPer100g <= :maxCalories)
            ORDER BY tr.name DESC
           """,
            countQuery = """
            SELECT COUNT(i.id) FROM Ingredient i
            JOIN i.translations tr ON tr.language.code = :locale
            JOIN tr.language l
            WHERE l.code = :locale
                AND (:searchKey IS NULL OR LOWER(tr.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:foodGroup IS NULL OR CAST(i.foodGroup AS string) = :foodGroup)
                AND (:isVegetarian IS NULL OR i.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR i.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR i.isGlutenFree = :isGlutenFree)
                AND (:minCalories IS NULL OR i.caloriesPer100g >= :minCalories)
                AND (:maxCalories IS NULL OR i.caloriesPer100g <= :maxCalories)
           """)
    Page<Long> findIdsByFiltersAndLocaleDesc(
            @Param("locale") String locale,
            @Param("searchKey") String searchKey,
            @Param("foodGroup") String foodGroup,
            @Param("isVegetarian") Boolean isVegetarian,
            @Param("isVegan") Boolean isVegan,
            @Param("isGlutenFree") Boolean isGlutenFree,
            @Param("minCalories") Double minCalories,
            @Param("maxCalories") Double maxCalories,
            Pageable pageable
    );

    @Query(value = """
            SELECT i.id FROM Ingredient i
            JOIN i.translations tr ON tr.language.code = :locale
            JOIN tr.language l
            WHERE l.code = :locale
                AND (:searchKey IS NULL OR LOWER(tr.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:foodGroup IS NULL OR CAST(i.foodGroup AS string) = :foodGroup)
                AND (:isVegetarian IS NULL OR i.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR i.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR i.isGlutenFree = :isGlutenFree)
                AND (:minCalories IS NULL OR i.caloriesPer100g >= :minCalories)
                AND (:maxCalories IS NULL OR i.caloriesPer100g <= :maxCalories)
           """,
            countQuery = """
            SELECT COUNT(i.id) FROM Ingredient i
            JOIN i.translations tr ON tr.language.code = :locale
            JOIN tr.language l
            WHERE l.code = :locale
                AND (:searchKey IS NULL OR LOWER(tr.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:foodGroup IS NULL OR CAST(i.foodGroup AS string) = :foodGroup)
                AND (:isVegetarian IS NULL OR i.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR i.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR i.isGlutenFree = :isGlutenFree)
                AND (:minCalories IS NULL OR i.caloriesPer100g >= :minCalories)
                AND (:maxCalories IS NULL OR i.caloriesPer100g <= :maxCalories)
           """)
    Page<Long> findIdsByFiltersAndLocaleOtherSort(
            @Param("locale") String locale,
            @Param("searchKey") String searchKey,
            @Param("foodGroup") String foodGroup,
            @Param("isVegetarian") Boolean isVegetarian,
            @Param("isVegan") Boolean isVegan,
            @Param("isGlutenFree") Boolean isGlutenFree,
            @Param("minCalories") Double minCalories,
            @Param("maxCalories") Double maxCalories,
            Pageable pageable
    );


    @Query("""
            SELECT i FROM Ingredient i
            JOIN FETCH i.translations tr
            JOIN FETCH tr.language l
            WHERE i.id IN :ids AND l.code = :locale
            """)
    List<Ingredient> findByIdsWithSpecificTranslation(
            @Param("ids") List<Long> ids,
            @Param("locale") String locale
    );

    @Query("""
            SELECT DISTINCT i FROM Ingredient i
            LEFT JOIN FETCH i.translations tr
            LEFT JOIN FETCH tr.language l
            WHERE i.id IN :ids
            """)
    List<Ingredient> findByIdsWithAllTranslations(
            @Param("ids") List<Long> ids
    );


    Optional<Ingredient> findByPublicId(UUID publicId);

    @Query("""
            SELECT DISTINCT i FROM Ingredient i
            LEFT JOIN FETCH i.translations tr
            LEFT JOIN FETCH tr.language l
            WHERE i.publicId = :publicId
            """)
    Optional<Ingredient> findByPublicIdWithAllTranslation(@Param("publicId") UUID publicId);

    boolean existsBySlug(String slug);
}
