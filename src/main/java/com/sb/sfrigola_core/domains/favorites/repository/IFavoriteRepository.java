package com.sb.sfrigola_core.domains.favorites.repository;

import com.sb.sfrigola_core.domains.favorites.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IFavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUser_PublicIdAndRecipe_PublicId(UUID userPublicId, UUID recipePublicId);

    boolean existsByUser_IdAndRecipe_Id(Long userId, Long recipeId);

    Optional<Favorite> findByUser_PublicIdAndRecipe_PublicId(UUID userPublicId, UUID recipePublicId);

    @Query(value = """
            SELECT f.recipe.id FROM Favorite f
            WHERE f.user.publicId = :userPublicId
            ORDER BY f.createdAt DESC
           """,
            countQuery = """
            SELECT COUNT(f) FROM Favorite f
            WHERE f.user.publicId = :userPublicId
           """)
    Page<Long> findRecipeIdsByUserPublicId(@Param("userPublicId") UUID userPublicId, Pageable pageable);

    @Query("""
            SELECT f.recipe.id FROM Favorite f
            WHERE f.user.id = :userId AND f.recipe.id IN :recipeIds
           """)
    Set<Long> findFavoritedRecipeIds(@Param("userId") Long userId, @Param("recipeIds") List<Long> recipeIds);
}
