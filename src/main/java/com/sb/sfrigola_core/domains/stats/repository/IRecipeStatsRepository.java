package com.sb.sfrigola_core.domains.stats.repository;

import com.sb.sfrigola_core.domains.stats.entity.RecipeStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IRecipeStatsRepository extends JpaRepository<RecipeStats, Long> {

    @Query("SELECT rs FROM RecipeStats rs WHERE (:categoryIds IS NULL OR rs.recipe.category.id IN :categoryIds)")
    Page<RecipeStats> findAllByCategoryIds(@Param("categoryIds") List<Long> categoryIds, Pageable pageable);

    @Query(value = """
            SELECT rs.recipeId FROM RecipeStats rs
            WHERE rs.recipe.isPublished = true
            ORDER BY rs.favoritesCount DESC
           """,
            countQuery = """
            SELECT COUNT(rs.recipeId) FROM RecipeStats rs
            WHERE rs.recipe.isPublished = true
           """)
    Page<Long> findPublishedRecipeIdsOrderByFavoritesDesc(Pageable pageable);
}
