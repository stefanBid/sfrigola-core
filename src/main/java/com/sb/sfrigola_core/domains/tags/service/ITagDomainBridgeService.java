package com.sb.sfrigola_core.domains.tags.service;

import com.sb.sfrigola_core.domains.tags.entity.Tag;

import java.util.List;
import java.util.UUID;

/**
 * Internal bridge contract for the tags' domain.
 * Does NOT read the security context; all required data is received explicitly.
 * Intended for cross-domain use by other services that need to resolve tag public IDs
 * to {@link Tag} entities while enforcing the {@code tag_scope} usage rules
 * ('ingredient'/'both' vs 'recipe'/'both') owned by this domain.
 */
public interface ITagDomainBridgeService {

    /**
     * Resolves the given tag public IDs to {@link Tag} entities usable on ingredients
     * (scope {@code ingredient} or {@code both}).
     *
     * @param publicIds the tag public IDs to resolve; never {@code null}
     * @return the resolved {@link java.util.List} of {@link Tag} entities, same size as {@code publicIds}
     * @throws com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException if any ID does not match an existing tag
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagScopeNotAllowedException if any resolved tag has scope {@code recipe}
     */
    List<Tag> getTagsUsableForIngredients(List<UUID> publicIds);

    /**
     * Resolves the given tag public IDs to {@link Tag} entities usable on recipes
     * (scope {@code recipe} or {@code both}).
     *
     * @param publicIds the tag public IDs to resolve; never {@code null}
     * @return the resolved {@link java.util.List} of {@link Tag} entities, same size as {@code publicIds}
     * @throws com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException if any ID does not match an existing tag
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagScopeNotAllowedException if any resolved tag has scope {@code ingredient}
     */
    List<Tag> getTagsUsableForRecipes(List<UUID> publicIds);
}