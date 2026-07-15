package com.sb.sfrigola_core.domains.ingredients.service;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.domains.ingredients.dto.input.UpdateIngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.view.IngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.view.IngredientDetailsAdminDto;
import com.sb.sfrigola_core.domains.ingredients.dto.input.AddIngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.view.IngredientPreviewAdminDto;
import com.sb.sfrigola_core.domains.ingredients.models.IngredientSpecificFilter;
import org.jspecify.annotations.NonNull;

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
    SCPagedResult<IngredientDto> getAll(SCFilterQuery<Void> filterQuery, @NonNull String locale);

    /**
     * Returns a paginated admin preview of ingredients, including localization coverage counts
     * (present and missing) to support CMS overviews.
     * Only ingredients that have a translation for {@code locale} are returned; the preview
     * uses that specific translation.
     *
     * @param filterQuery pagination, sorting, optional search key, and optional
     *                     {@link IngredientSpecificFilter} (category, dietary flags, calorie range)
     * @param locale      BCP-47 language code used to filter and select the preview translation; never {@code null}
     * @return a {@link SCPagedResult} of {@link IngredientPreviewAdminDto}; never {@code null}
     */
    SCPagedResult<IngredientPreviewAdminDto> getAllAdmin(SCFilterQuery<IngredientSpecificFilter> filterQuery, @NonNull String locale);

    /**
     * Returns full admin details for a single ingredient, including every existing translation,
     * the list of active languages still missing a translation, and a preview of the translation
     * for the requested locale.
     * <p>
     * {@code locale} is mandatory and never filters — it only selects which translation is
     * returned as the preview. If no translation exists for {@code locale}, the preview is {@code null}.
     *
     * @param publicId public identifier of the ingredient
     * @param locale   BCP-47 locale code used to select the translation for the preview; never {@code null}
     * @return {@link IngredientDetailsAdminDto} with translation coverage details
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException
     *         if no ingredient exists with the given public ID
     */
    IngredientDetailsAdminDto getByPublicIdAdmin(UUID publicId, @NonNull String locale);

    /**
     * Creates a new ingredient. Translations provided in {@code inputDto} are inserted as-is;
     * no merge occurs on create.
     *
     * @param addIngredientDto creation payload — slug, category, nutritional/dietary fields and at least one translation
     * @param locale           BCP-47 locale code used to select the translation for the preview; never {@code null}
     * @return admin preview of the newly created ingredient
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.IngredientSlugAlreadyExistsException
     *         if an ingredient with the same slug already exists
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.DuplicateIngredientLocaleException
     *         if the same locale appears more than once in {@code inputDto}
     * @throws com.sb.sfrigola_core.domains.languages.exception.LocaleNotActiveException
     *         if a translation references a language that is not active
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.MissingIngredientLocalesException
     *         if {@code addIngredientDto.translations()} does not cover all active languages
     */
    IngredientPreviewAdminDto createNewIngredient(AddIngredientDto addIngredientDto, @NonNull String locale);

    /**
     * Updates an existing ingredient's slug, category, nutritional/dietary fields and exactly one
     * translation: {@code updateIngredientDto.specificTranslation()} either relabels the translation
     * for that locale if one already exists, or adds a new one. Only one locale can be touched per
     * call — to update multiple translations, call this method once per locale.
     *
     * @param publicId public identifier of the ingredient to update
     * @param updateIngredientDto new slug, category, nutritional/dietary fields, and the single translation to upsert
     * @return admin preview of the updated ingredient, previewing the upserted translation
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException
     *         if no ingredient exists with the given public ID
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.IngredientSlugAlreadyExistsException
     *         if the new slug is already used by another ingredient
     * @throws com.sb.sfrigola_core.domains.languages.exception.LocaleNotActiveException
     *         if {@code updateIngredientDto.specificTranslation()} references a language that is not active
     */
    IngredientPreviewAdminDto updateIngredient(UUID publicId, UpdateIngredientDto updateIngredientDto);

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
     * @return public identifier of the deleted ingredient
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException
     *         if no ingredient exists with the given public ID
     */
    UUID deleteIngredient(UUID publicId);

}
