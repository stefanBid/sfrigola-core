package com.sb.sfrigola_core.domains.ingredients.repository;

import com.sb.sfrigola_core.domains.ingredients.entity.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IIngredientRepository extends JpaRepository<Ingredient, Long> {

    @Query("""
            SELECT i.id FROM Ingredient i
            JOIN i.translations tr
            JOIN tr.language l
            WHERE l.code = :locale
                AND (:searchKey IS NULL OR LOWER(tr.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:category IS NULL OR i.category = :category)
                AND (:isVegetarian IS NULL OR i.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR i.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR i.isGlutenFree = :isGlutenFree)
                AND (:minCalories IS NULL OR i.caloriesPer100g >= :minCalories)
                AND (:maxCalories IS NULL OR i.caloriesPer100g <= :maxCalories)
            ORDER BY tr.name ASC
           """)
    Page<Long> findIdsByFiltersAndLocale(
            @Param("locale") String locale,
            @Param("searchKey") String searchKey,
            @Param("category") String category,
            @Param("isVegetarian") Boolean isVegetarian,
            @Param("isVegan") Boolean isVegan,
            @Param("isGlutenFree") Boolean isGlutenFree,
            @Param("minCalories") Double minCalories,
            @Param("maxCalories") Double maxCalories,
            Pageable pageable
    );

    @Query("""
            SELECT i.id FROM Ingredient i
            JOIN i.translations tr
            JOIN tr.language l
            WHERE l.code = :locale
                AND (:searchKey IS NULL OR LOWER(tr.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:category IS NULL OR i.category = :category)
                AND (:isVegetarian IS NULL OR i.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR i.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR i.isGlutenFree = :isGlutenFree)
                AND (:minCalories IS NULL OR i.caloriesPer100g >= :minCalories)
                AND (:maxCalories IS NULL OR i.caloriesPer100g <= :maxCalories)
            ORDER BY tr.name DESC
           """)
    Page<Long> findIdsByFiltersAndLocaleNameDesc(
            @Param("locale") String locale,
            @Param("searchKey") String searchKey,
            @Param("category") String category,
            @Param("isVegetarian") Boolean isVegetarian,
            @Param("isVegan") Boolean isVegan,
            @Param("isGlutenFree") Boolean isGlutenFree,
            @Param("minCalories") Double minCalories,
            @Param("maxCalories") Double maxCalories,
            Pageable pageable
    );

    /**
     * No hardcoded ORDER BY — the caller supplies it via {@code pageable}'s {@link Sort}. Spring Data
     * detects the {@code i} alias from the FROM clause and appends the requested field (e.g.
     * {@code i.caloriesPer100g}) after GROUP BY. Only for sorting by a plain Ingredient property
     * (never "name", which lives on the translations join — see {@link #findIdsByFiltersAndLocale}
     * / {@link #findIdsByFiltersAndLocaleNameDesc} for that case).
     * {@code locale} is nullable here (unlike the two methods above) since sorting by a root property
     * doesn't depend on translation uniqueness; GROUP BY collapses the duplicate rows a null-locale
     * LEFT JOIN can produce when an ingredient has more than one translation.
     */
    @Query(value = """
            SELECT i.id FROM Ingredient i
            LEFT JOIN i.translations tr
            LEFT JOIN tr.language l
            WHERE (:locale IS NULL OR l.code = :locale)
                AND (:searchKey IS NULL OR LOWER(tr.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:category IS NULL OR i.category = :category)
                AND (:isVegetarian IS NULL OR i.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR i.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR i.isGlutenFree = :isGlutenFree)
                AND (:minCalories IS NULL OR i.caloriesPer100g >= :minCalories)
                AND (:maxCalories IS NULL OR i.caloriesPer100g <= :maxCalories)
            GROUP BY i.id
           """,
            countQuery = """
            SELECT COUNT(DISTINCT i.id) FROM Ingredient i
            LEFT JOIN i.translations tr
            LEFT JOIN tr.language l
            WHERE (:locale IS NULL OR l.code = :locale)
                AND (:searchKey IS NULL OR LOWER(tr.name) LIKE LOWER(CONCAT('%', CAST(:searchKey AS string), '%')))
                AND (:category IS NULL OR i.category = :category)
                AND (:isVegetarian IS NULL OR i.isVegetarian = :isVegetarian)
                AND (:isVegan IS NULL OR i.isVegan = :isVegan)
                AND (:isGlutenFree IS NULL OR i.isGlutenFree = :isGlutenFree)
                AND (:minCalories IS NULL OR i.caloriesPer100g >= :minCalories)
                AND (:maxCalories IS NULL OR i.caloriesPer100g <= :maxCalories)
           """
    )
    Page<Long> findIdsByFiltersCustomSort(
            @Param("locale") String locale,
            @Param("searchKey") String searchKey,
            @Param("category") String category,
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
