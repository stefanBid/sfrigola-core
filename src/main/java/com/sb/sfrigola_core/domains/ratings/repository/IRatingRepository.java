package com.sb.sfrigola_core.domains.ratings.repository;

import com.sb.sfrigola_core.domains.ratings.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IRatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByUser_PublicIdAndRecipe_PublicId(UUID userPublicId, UUID recipePublicId);

    Optional<Rating> findByUser_PublicIdAndRecipe_PublicId(UUID userPublicId, UUID recipePublicId);
}
