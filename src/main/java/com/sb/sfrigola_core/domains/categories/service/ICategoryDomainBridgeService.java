package com.sb.sfrigola_core.domains.categories.service;

import com.sb.sfrigola_core.domains.categories.entity.Category;

import java.util.UUID;

/**
 * Internal bridge contract for the categories' domain.
 * Does NOT read the security context; all required data is received explicitly.
 * Intended for cross-domain use by other services that need to resolve a category
 * public ID to a {@link Category} entity (e.g. to build a {@code @ManyToOne}
 * relationship such as {@code recipes.category}).
 */
public interface ICategoryDomainBridgeService {

    /**
     * Resolves the given category public ID to a {@link Category} entity.
     *
     * @param publicId the category public ID to resolve; never {@code null}
     * @return the resolved {@link Category} entity
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if no category exists with the given public ID
     */
    Category getCategoryEntityByPublicIdOrThrow(UUID publicId);
}
