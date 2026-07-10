package com.sb.sfrigola_core.domains.recipes.service;

import com.sb.sfrigola_core.domains.recipes.entity.Recipe;

import java.util.List;
import java.util.UUID;

/**
 * Internal bridge contract for the recipes' domain.
 * Does NOT read the security context; all required data is received explicitly.
 * Intended for cross-domain use by other services that need to resolve a recipe
 * public ID to a {@link Recipe} entity (e.g. to build a {@code @ManyToOne}
 * relationship such as {@code favorites.recipe} or {@code ratings.recipe}).
 */
public interface IRecipeDomainBridgeService {

    /**
     * Resolves the given recipe public ID to a {@link Recipe} entity.
     *
     * @param publicId the recipe public ID to resolve; never {@code null}
     * @return the resolved {@link Recipe} entity
     * @throws com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException
     *         if no recipe exists with the given public ID
     */
    Recipe getRecipeEntityByPublicIdOrThrow(UUID publicId);

    /**
     * Resolves the given recipe internal IDs to {@link Recipe} entities, each with its
     * translation for {@code locale} eagerly loaded (or none, if the recipe has no
     * translation for that locale). Used by other domains that already hold a page of
     * recipe IDs (e.g. favourites) and need to localize them without depending on the
     * recipes' repository directly.
     *
     * @param ids    internal recipe IDs to resolve; never {@code null}
     * @param locale BCP-47 language code used to select each recipe's translation
     * @return the resolved {@link java.util.List} of {@link Recipe} entities, in no
     *         guaranteed order — callers must re-order by {@code ids} themselves
     */
    List<Recipe> getRecipesByIdsWithLocale(List<Long> ids, String locale);
}
