package com.sb.sfrigola_core.domains.ingredients.service.impl;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.domains.ingredients.dto.IngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.admin.IngredientDetailsAdminDto;
import com.sb.sfrigola_core.domains.ingredients.dto.admin.IngredientInputDto;
import com.sb.sfrigola_core.domains.ingredients.dto.admin.IngredientPreviewAdminDto;
import com.sb.sfrigola_core.domains.ingredients.dto.admin.IngredientTranslationDetailsAdminDto;
import com.sb.sfrigola_core.domains.ingredients.dto.admin.IngredientTranslationInputDto;
import com.sb.sfrigola_core.domains.ingredients.entity.Ingredient;
import com.sb.sfrigola_core.domains.ingredients.entity.IngredientTranslation;
import com.sb.sfrigola_core.domains.ingredients.exception.DuplicateIngredientLocaleException;
import com.sb.sfrigola_core.domains.ingredients.exception.IngredientLanguageNotActiveException;
import com.sb.sfrigola_core.domains.ingredients.exception.IngredientSlugAlreadyExistsException;
import com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException;
import com.sb.sfrigola_core.domains.ingredients.enums.IngredientSortField;
import com.sb.sfrigola_core.domains.ingredients.models.IngredientSpecificFilter;
import com.sb.sfrigola_core.domains.ingredients.repository.IIngredientRepository;
import com.sb.sfrigola_core.domains.ingredients.service.IIngredientService;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IngredientServiceImpl implements IIngredientService {

    private final IIngredientRepository ingredientRepository;
    private final ILanguageDomainBridgeService languageDomainBridgeService;

    @Override
    public SCPagedResult<IngredientDto> getAll(SCFilterQuery<Void> filterQuery, String locale) {
        var pageable = SCPaginationUtils.toPageable(filterQuery);

        var ingredientIds = ingredientRepository.findIdsByFiltersAndLocaleAsc(
                locale, filterQuery.searchKey(), null, null, null, null, null, null, pageable
        );

        if (ingredientIds.hasContent()) {
            var ids = ingredientIds.getContent();
            Map<Long, Ingredient> byId = ingredientRepository.findByIdsWithSpecificTranslation(ids, locale)
                    .stream().collect(Collectors.toMap(Ingredient::getId, i -> i));
            List<Ingredient> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
            return new SCPagedResult<>(
                    ordered.stream().map(this::toDto).toList(),
                    SCPaginationUtils.toPagedOption(ingredientIds)
            );
        }
        return SCPagedResult.empty();
    }

    @Override
    public SCPagedResult<IngredientPreviewAdminDto> getAllAdmin(SCFilterQuery<IngredientSpecificFilter> filterQuery, @Nullable String locale) {
        var totalActiveLanguages = languageDomainBridgeService.getAllActiveLanguages().size();

        // SORT BY SWITCHER
        // CASE: SortBy is null or is NAME → "name" lives on the translation join, not on Ingredient,
        // so ORDER BY is hardcoded in the dedicated Asc/Desc @Query methods.
        // CASE: SortBy is not null and not NAME → the field lives directly on Ingredient, resolved
        // natively by Spring Data via Pageable's Sort against the *OtherSort queries (no hardcoded ORDER BY there).
        boolean sortByName = filterQuery.sortBy() == null || filterQuery.sortBy() == IngredientSortField.NAME;

        // SORT SWITCHER
        // CASE: Sort is ASC call query with hardcoded ORDER BY ASC
        // CASE: Sort is DESC call query with hardcoded ORDER BY DESC
        boolean descending = filterQuery.sort() != null && !filterQuery.sort().isAsc();

        // LOCALE SWITCHER
        // CASE: Locale is null
        // RESULT: All ingredients returned; preview = first translation in collection
        // CASE: Locale has value
        // RESULT: Only ingredients that have a translation for the given locale; preview = that specific translation
        boolean hasLocale = locale != null && !locale.isBlank();

        // STEP 1: Obtain ids
        var filterOtherExtracted = filterQuery.other();
        var category = filterOtherExtracted != null ? filterOtherExtracted.category() : null;
        var isVegetarian = filterOtherExtracted != null ? filterOtherExtracted.isVegetarian() : null;
        var isVegan = filterOtherExtracted != null ? filterOtherExtracted.isVegan() : null;
        var isGlutenFree = filterOtherExtracted != null ? filterOtherExtracted.isGlutenFree() : null;
        var minCalories = filterOtherExtracted != null ? filterOtherExtracted.minCalories() : null;
        var maxCalories = filterOtherExtracted != null ? filterOtherExtracted.maxCalories() : null;

        var pageable = SCPaginationUtils.toPageable(filterQuery, sortByName);
        Page<Long> idsPage;

        if (sortByName) {
            if (descending)
                idsPage = hasLocale
                        ? ingredientRepository.findIdsByFiltersAndLocaleDesc(locale, filterQuery.searchKey(), category, isVegetarian, isVegan, isGlutenFree, minCalories, maxCalories, pageable)
                        : ingredientRepository.findIdsByFiltersDesc(filterQuery.searchKey(), category, isVegetarian, isVegan, isGlutenFree, minCalories, maxCalories, pageable);
            else
                idsPage = hasLocale
                        ? ingredientRepository.findIdsByFiltersAndLocaleAsc(locale, filterQuery.searchKey(), category, isVegetarian, isVegan, isGlutenFree, minCalories, maxCalories, pageable)
                        : ingredientRepository.findIdsByFiltersAsc(filterQuery.searchKey(), category, isVegetarian, isVegan, isGlutenFree, minCalories, maxCalories, pageable);
        } else {
            idsPage = hasLocale
                    ? ingredientRepository.findIdsByFiltersAndLocaleOtherSort(locale, filterQuery.searchKey(), category, isVegetarian, isVegan, isGlutenFree, minCalories, maxCalories, pageable)
                    : ingredientRepository.findIdsByFiltersOtherSort(filterQuery.searchKey(), category, isVegetarian, isVegan, isGlutenFree, minCalories, maxCalories, pageable);
        }

        if (idsPage.hasContent()) {
            var ids = idsPage.getContent();
            Map<Long, Ingredient> byId = ingredientRepository.findByIdsWithAllTranslations(ids)
                    .stream().collect(Collectors.toMap(Ingredient::getId, i -> i));
            List<Ingredient> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();

            return new SCPagedResult<>(
                    ordered.stream().map(ingredient -> toAdminDto(ingredient, resolveNamePreview(ingredient, locale), totalActiveLanguages)).toList(),
                    SCPaginationUtils.toPagedOption(idsPage)
            );
        }
        return SCPagedResult.empty();
    }

    private String resolveNamePreview(Ingredient ingredient, @Nullable String locale) {
        IngredientTranslation translation = locale != null
                ? ingredient.getTranslations().stream().filter(t -> t.getLanguage().getCode().equals(locale)).findFirst().orElse(null)
                : ingredient.getTranslations().stream().findFirst().orElse(null);
        return translation != null ? translation.getName() : null;
    }

    @Override
    public IngredientDetailsAdminDto getByPublicIdAdmin(UUID publicId) {
        var activeLanguages = new ArrayList<>(languageDomainBridgeService.getAllActiveLanguages());
        var ingredient = ingredientRepository.findByPublicIdWithAllTranslation(publicId).orElseThrow(
                () -> new NoIngredientFoundException(publicId)
        );
        // Preparing Details for this Ingredient
        int totalLocalization = ingredient.getTranslations().size();
        int totalMissingLocalization = activeLanguages.size() - totalLocalization;
        ArrayList<IngredientTranslationDetailsAdminDto> missingTranslation = new ArrayList<>();

        // Populate missing Translation array only if there are missing languages
        if (totalMissingLocalization > 0) {
            // Remove from the activeLanguages list all languages that already have a translation for this ingredient
            ingredient.getTranslations().forEach(t -> activeLanguages.removeIf(l -> l.code().equals(t.getLanguage().getCode())));
            activeLanguages.forEach(al -> missingTranslation.add(new IngredientTranslationDetailsAdminDto(al.code(), al.name(), null)));
        }
        return toAdminDetailsDto(ingredient, missingTranslation);
    }

    @Override
    @Transactional
    public IngredientPreviewAdminDto createIngredient(IngredientInputDto inputDto) {
        // Guard for existing slug
        if (ingredientRepository.existsBySlug(inputDto.slug()))
            throw new IngredientSlugAlreadyExistsException(inputDto.slug());

        // Guard for duplicate locale in input — fail fast before building entities
        Set<String> seenLocales = new HashSet<>();
        inputDto.translations().forEach(t -> {
            if (!seenLocales.add(t.langCode()))
                throw new DuplicateIngredientLocaleException(t.langCode());
        });

        Ingredient newIngredient = new Ingredient();

        var activeLanguageMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();
        List<IngredientTranslation> translations = inputDto.translations().stream()
                .map(t -> {
                    Language lang = activeLanguageMap.get(t.langCode());
                    if (lang == null) throw new IngredientLanguageNotActiveException(t.langCode());
                    return toIngredientTranslation(t, lang);
                })
                .collect(Collectors.toCollection(ArrayList::new));
        translations.forEach(t -> t.setIngredient(newIngredient));

        newIngredient.setSlug(inputDto.slug());
        newIngredient.setCategory(inputDto.category());
        newIngredient.setCaloriesPer100g(inputDto.caloriesPer100g());
        newIngredient.setAllergens(inputDto.allergens());
        newIngredient.setVegetarian(inputDto.isVegetarian());
        newIngredient.setVegan(inputDto.isVegan());
        newIngredient.setGlutenFree(inputDto.isGlutenFree());
        newIngredient.setTranslations(translations);

        ingredientRepository.save(newIngredient);

        var dataForTranslationPreview = inputDto.translations().stream().findFirst().orElse(null);
        var namePreview = dataForTranslationPreview != null ? dataForTranslationPreview.name() : null;

        return toAdminDto(newIngredient, namePreview, activeLanguageMap.size());
    }

    @Override
    @Transactional
    public IngredientPreviewAdminDto updateIngredient(UUID publicId, IngredientInputDto inputDto) {
        var ingredientToUpdate = ingredientRepository.findByPublicIdWithAllTranslation(publicId).orElseThrow(
                () -> new NoIngredientFoundException(publicId)
        );

        // Check: new slug not already used by a different ingredient
        if (!inputDto.slug().equals(ingredientToUpdate.getSlug()) && ingredientRepository.existsBySlug(inputDto.slug()))
            throw new IngredientSlugAlreadyExistsException(inputDto.slug());

        // Check: duplicate locale in input — fail fast before touching any translation
        Set<String> seenLocales = new HashSet<>();
        inputDto.translations().forEach(t -> {
            if (!seenLocales.add(t.langCode())) throw new DuplicateIngredientLocaleException(t.langCode());
        });

        // Prepare Translation update
        var activeLanguagesMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();
        Map<String, IngredientTranslation> ingredientToUpdateTranslationMapped = ingredientToUpdate.getTranslations().stream()
                .collect(Collectors.toMap(t -> t.getLanguage().getCode(), t -> t));

        List<IngredientTranslation> toRemove = new ArrayList<>();

        for (IngredientTranslationInputDto input : inputDto.translations()) {
            Language lang = activeLanguagesMap.get(input.langCode());
            if (lang == null) throw new IngredientLanguageNotActiveException(input.langCode());

            boolean deleteSignal = input.name() == null || input.name().isBlank();
            IngredientTranslation extractedIngredientTranslation = ingredientToUpdateTranslationMapped.get(input.langCode());
            if (deleteSignal) {
                if (extractedIngredientTranslation != null) toRemove.add(extractedIngredientTranslation);
            } else if (extractedIngredientTranslation != null) {
                // Update existing translation
                if (Objects.equals(extractedIngredientTranslation.getName(), input.name())) continue; // No change
                extractedIngredientTranslation.setName(input.name());
            } else {
                // Create new translation
                IngredientTranslation newTranslation = new IngredientTranslation();
                newTranslation.setIngredient(ingredientToUpdate);
                newTranslation.setLanguage(lang);
                newTranslation.setName(input.name());
                ingredientToUpdate.getTranslations().add(newTranslation);
            }
        }

        ingredientToUpdate.getTranslations().removeAll(toRemove);
        ingredientToUpdate.setSlug(inputDto.slug());
        ingredientToUpdate.setCategory(inputDto.category());
        ingredientToUpdate.setCaloriesPer100g(inputDto.caloriesPer100g());
        ingredientToUpdate.setAllergens(inputDto.allergens());
        ingredientToUpdate.setVegetarian(inputDto.isVegetarian());
        ingredientToUpdate.setVegan(inputDto.isVegan());
        ingredientToUpdate.setGlutenFree(inputDto.isGlutenFree());

        var namePreview = ingredientToUpdate.getTranslations().stream().findFirst().map(IngredientTranslation::getName).orElse(null);

        return toAdminDto(ingredientToUpdate, namePreview, activeLanguagesMap.size());
    }

    @Override
    @Transactional
    public IngredientPreviewAdminDto deleteIngredient(UUID publicId) {
        var ingredientToDelete = ingredientRepository.findByPublicIdWithAllTranslation(publicId).orElseThrow(
                () -> new NoIngredientFoundException(publicId)
        );

        var totalActiveLanguages = languageDomainBridgeService.getAllActiveLanguages().size();
        var namePreview = ingredientToDelete.getTranslations().stream().findFirst().map(IngredientTranslation::getName).orElse(null);

        ingredientRepository.delete(ingredientToDelete);
        return toAdminDto(ingredientToDelete, namePreview, totalActiveLanguages);
    }

    // =========================================================
    // PRIVATE
    // =========================================================

    private IngredientDto toDto(Ingredient ingredient) {
        // We have only one item in the list of translations — the first query filters by locale
        var translation = ingredient.getTranslations().getFirst();
        return new IngredientDto(
                ingredient.getPublicId(),
                ingredient.getSlug(),
                translation.getName(),
                ingredient.getCategory(),
                ingredient.getCaloriesPer100g(),
                ingredient.getAllergens(),
                ingredient.isVegetarian(),
                ingredient.isVegan(),
                ingredient.isGlutenFree()
        );
    }

    private IngredientPreviewAdminDto toAdminDto(Ingredient ingredient, String namePreview, int totalActiveLanguages) {
        var translationCount = ingredient.getTranslations().size();
        return new IngredientPreviewAdminDto(
                ingredient.getPublicId(),
                ingredient.getSlug(),
                ingredient.getCategory(),
                ingredient.getCaloriesPer100g(),
                ingredient.getAllergens(),
                ingredient.isVegetarian(),
                ingredient.isVegan(),
                ingredient.isGlutenFree(),
                namePreview,
                translationCount,
                totalActiveLanguages - translationCount
        );
    }

    private IngredientDetailsAdminDto toAdminDetailsDto(Ingredient ingredient, List<IngredientTranslationDetailsAdminDto> missingTranslation) {
        var extractedPreview = ingredient.getTranslations().stream().findFirst().orElse(null);
        return new IngredientDetailsAdminDto(
                ingredient.getPublicId(),
                ingredient.getSlug(),
                ingredient.getCategory(),
                ingredient.getCaloriesPer100g(),
                ingredient.getAllergens(),
                ingredient.isVegetarian(),
                ingredient.isVegan(),
                ingredient.isGlutenFree(),
                extractedPreview != null ? extractedPreview.getName() : null,
                ingredient.getTranslations().stream().map(this::toIngredientTranslationDetails).toList(),
                missingTranslation
        );
    }

    private IngredientTranslation toIngredientTranslation(IngredientTranslationInputDto translationInputDto, Language lang) {
        IngredientTranslation translation = new IngredientTranslation();
        translation.setLanguage(lang);
        translation.setName(translationInputDto.name());
        return translation;
    }

    private IngredientTranslationDetailsAdminDto toIngredientTranslationDetails(IngredientTranslation translation) {
        return new IngredientTranslationDetailsAdminDto(
                translation.getLanguage().getCode(),
                translation.getLanguage().getName(),
                translation.getName()
        );
    }
}
