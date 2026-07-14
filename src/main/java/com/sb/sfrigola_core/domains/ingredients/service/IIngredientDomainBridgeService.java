package com.sb.sfrigola_core.domains.ingredients.service;

import com.sb.sfrigola_core.domains.ingredients.entity.Ingredient;

import java.util.List;
import java.util.UUID;

/**
 * Internal bridge contract for the ingredients' domain.
 * Does NOT read the security context; all required data is received explicitly.
 * Intended for cross-domain use by other services that need to resolve ingredient
 * public IDs to {@link Ingredient} entities (e.g. to build {@code recipe_ingredients} rows).
 */
public interface IIngredientDomainBridgeService {

    /**
     * Resolves the given ingredient public IDs to {@link Ingredient} entities.
     *
     * @param publicIds the ingredient public IDs to resolve; never {@code null}
     * @return the resolved {@link java.util.List} of {@link Ingredient} entities, same size as {@code publicIds}
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException
     *         if any ID does not match an existing ingredient
     */
    List<Ingredient> getIngredientsByPublicIds(List<UUID> publicIds);
}
