package com.sb.sfrigola_core.domains.ingredients.service;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.domains.ingredients.dto.IngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.admin.IngredientDetailsAdminDto;
import com.sb.sfrigola_core.domains.ingredients.dto.admin.IngredientInputDto;
import com.sb.sfrigola_core.domains.ingredients.dto.admin.IngredientPreviewAdminDto;
import com.sb.sfrigola_core.domains.ingredients.models.IngredientSpecificFilter;
import jakarta.annotation.Nullable;

import java.util.UUID;

/**
 * Controller-facing contract for the ingredients' domain.
 * All ingredient management operations are admin-only; this service does not read the
 * security context — authorization is enforced upstream at the security-filter level
 * (see {@code SecurityBeansConfig}), not inside this contract.
 * All methods succeed or throw a subclass of {@link com.sb.sfrigola_core.common.exception.ex.SCGeneralException}.
 */
public interface IIngredientService {

    /**
     * Returns a paginated list of ingredients localized for the requested locale.
     * Only ingredients that have a translation for {@code locale} are included.
     *
     * @param filterQuery pagination and sorting parameters, with optional {@code searchKey}
     *                     matched case-insensitively against the translated name
     * @param locale      BCP-47 language code used to filter and localize results
     * @return a {@link SCPagedResult} of {@link IngredientDto}; never {@code null}
     */
    SCPagedResult<IngredientDto> getAll(SCFilterQuery<Void> filterQuery, String locale);

    /**
     * Returns a paginated admin preview of ingredients, including localization coverage counts
     * (present and missing) to support CMS overviews.
     * <p>
     * When {@code locale} is {@code null}, all ingredients are returned and the name preview
     * is taken from the first available translation in the collection.
     * When {@code locale} is provided, only ingredients that have a translation for that locale
     * are returned and the preview uses that specific translation.
     *
     * @param filterQuery pagination, sorting, optional search key, and optional
     *                     {@link IngredientSpecificFilter} (category, dietary flags, calorie range)
     * @param locale      BCP-47 language code for filtering and preview selection; {@code null} returns all ingredients
     * @return a {@link SCPagedResult} of {@link IngredientPreviewAdminDto}; never {@code null}
     */
    SCPagedResult<IngredientPreviewAdminDto> getAllAdmin(SCFilterQuery<IngredientSpecificFilter> filterQuery, @Nullable String locale);

    /**
     * Returns full admin details for a single ingredient, including every existing translation
     * and the list of active languages still missing a translation.
     *
     * @param publicId public identifier of the ingredient
     * @return {@link IngredientDetailsAdminDto} with translation coverage details
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException
     *         if no ingredient exists with the given public ID
     */
    IngredientDetailsAdminDto getByPublicIdAdmin(UUID publicId);

    /**
     * Creates a new ingredient. Translations provided in {@code inputDto} are inserted as-is;
     * no merge occurs on create.
     *
     * @param inputDto creation payload — slug, category, nutritional/dietary fields and at least one translation
     * @return admin preview of the newly created ingredient
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.IngredientSlugAlreadyExistsException
     *         if an ingredient with the same slug already exists
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.DuplicateIngredientLocaleException
     *         if the same locale appears more than once in {@code inputDto}
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.IngredientLanguageNotActiveException
     *         if a translation references a language that is not active
     */
    IngredientPreviewAdminDto createIngredient(IngredientInputDto inputDto);

    /**
     * Updates an existing ingredient's slug, category, nutritional/dietary fields and translations,
     * merging the translation set per locale rather than replacing it wholesale: a locale already
     * present is relabeled, a new locale is added, and a locale sent with a blank/{@code null} name
     * is removed.
     *
     * @param publicId public identifier of the ingredient to update
     * @param inputDto new slug, category, nutritional/dietary fields and the per-locale translation changes to apply
     * @return admin preview of the updated ingredient
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException
     *         if no ingredient exists with the given public ID
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.IngredientSlugAlreadyExistsException
     *         if the new slug is already used by another ingredient
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.DuplicateIngredientLocaleException
     *         if the same locale appears more than once in {@code inputDto}
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.IngredientLanguageNotActiveException
     *         if a translation references a language that is not active
     */
    IngredientPreviewAdminDto updateIngredient(UUID publicId, IngredientInputDto inputDto);

    /**
     * Permanently deletes an ingredient and all of its translations (cascade). Rows in the
     * {@code ingredient_tags} bridge table referencing this ingredient are removed as well,
     * via the database's {@code ON DELETE CASCADE}.
     * <p>
     * {@code recipe_ingredients.ingredient_id} has no {@code ON DELETE CASCADE}: deleting an
     * ingredient still referenced by a recipe fails at the database level with a foreign key
     * violation, surfaced as an unchecked {@link org.springframework.dao.DataIntegrityViolationException}.
     * No pre-check against {@code recipe_ingredients} is performed here — that table has no
     * entity/repository yet ({@code recipes} domain not implemented).
     *
     * @param publicId public identifier of the ingredient to delete
     * @return admin preview snapshot of the ingredient as it was right before deletion
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException
     *         if no ingredient exists with the given public ID
     */
    IngredientPreviewAdminDto deleteIngredient(UUID publicId);

}
