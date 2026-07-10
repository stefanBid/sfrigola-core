package com.sb.sfrigola_core.domains.ingredients.service.impl;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.domains.ingredients.dto.view.IngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.view.IngredientDetailsAdminDto;
import com.sb.sfrigola_core.domains.ingredients.dto.input.AddIngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.input.UpdateIngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.view.IngredientPreviewAdminDto;
import com.sb.sfrigola_core.domains.ingredients.dto.input.IngredientTranslationInputDto;
import com.sb.sfrigola_core.domains.ingredients.entity.Ingredient;
import com.sb.sfrigola_core.domains.ingredients.entity.IngredientTag;
import com.sb.sfrigola_core.domains.ingredients.entity.IngredientTranslation;
import com.sb.sfrigola_core.domains.ingredients.exception.DuplicateIngredientLocaleException;
import com.sb.sfrigola_core.domains.ingredients.exception.IngredientLanguageNotActiveException;
import com.sb.sfrigola_core.domains.ingredients.exception.IngredientSlugAlreadyExistsException;
import com.sb.sfrigola_core.domains.ingredients.exception.MissingIngredientLocalesException;
import com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException;
import com.sb.sfrigola_core.domains.ingredients.enums.IngredientSortField;
import com.sb.sfrigola_core.domains.ingredients.models.IngredientSpecificFilter;
import com.sb.sfrigola_core.domains.ingredients.repository.IIngredientRepository;
import com.sb.sfrigola_core.domains.ingredients.service.IIngredientService;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import com.sb.sfrigola_core.domains.tags.dto.view.TagDto;
import com.sb.sfrigola_core.domains.tags.entity.Tag;
import com.sb.sfrigola_core.domains.tags.service.ITagDomainBridgeService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
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
    private final ITagDomainBridgeService tagDomainBridgeService;

    @Override
    public SCPagedResult<IngredientDto> getAll(SCFilterQuery<Void> filterQuery, @NonNull String locale) {
        // LOCALE CHECK
        languageDomainBridgeService.validateLocaleIsActiveOrThrow(locale);

        var pageable = SCPaginationUtils.toPageable(filterQuery);

        // Step 1: Fetch IDs of Ingredients for the given locale
        var ingredientIds = ingredientRepository.findIdsByFiltersAndLocaleAsc(
                locale, filterQuery.searchKey(), null, null, null, null, null, null, pageable
        );

        // Step 2: Fetch and restore the ordered ID sequence from step 1
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
    public SCPagedResult<IngredientPreviewAdminDto> getAllAdmin(SCFilterQuery<IngredientSpecificFilter> filterQuery, @NonNull String locale) {
        var activeLanguagesSimpleMap = languageDomainBridgeService.getAllActiveLanguagesSimpleMap();

        // LOCALE CHECK
        languageDomainBridgeService.validateLocaleIsActiveByActiveLanguagesMapKeysOrThrow(activeLanguagesSimpleMap.keySet(), locale);

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

        // STEP 1: Obtain ids
        var filterOtherExtracted = filterQuery.other();
        var foodGroup = filterOtherExtracted != null && filterOtherExtracted.foodGroup() != null ? filterOtherExtracted.foodGroup().getValue() : null;
        var isVegetarian = filterOtherExtracted != null ? filterOtherExtracted.isVegetarian() : null;
        var isVegan = filterOtherExtracted != null ? filterOtherExtracted.isVegan() : null;
        var isGlutenFree = filterOtherExtracted != null ? filterOtherExtracted.isGlutenFree() : null;
        var minCalories = filterOtherExtracted != null ? filterOtherExtracted.minCalories() : null;
        var maxCalories = filterOtherExtracted != null ? filterOtherExtracted.maxCalories() : null;

        var pageable = SCPaginationUtils.toPageable(filterQuery, sortByName);
        Page<Long> idsPage;

        if (sortByName) {
                idsPage = descending
                        ? ingredientRepository.findIdsByFiltersAndLocaleDesc(locale, filterQuery.searchKey(), foodGroup, isVegetarian, isVegan, isGlutenFree, minCalories, maxCalories, pageable)
                        : ingredientRepository.findIdsByFiltersAndLocaleAsc(locale, filterQuery.searchKey(), foodGroup, isVegetarian, isVegan, isGlutenFree, minCalories, maxCalories, pageable);
        } else {
            idsPage = ingredientRepository.findIdsByFiltersAndLocaleOtherSort(locale, filterQuery.searchKey(), foodGroup, isVegetarian, isVegan, isGlutenFree, minCalories, maxCalories, pageable);
        }

        if (idsPage.hasContent()) {
            var ids = idsPage.getContent();
            Map<Long, Ingredient> byId = ingredientRepository.findByIdsWithAllTranslations(ids)
                    .stream().collect(Collectors.toMap(Ingredient::getId, i -> i));
            List<Ingredient> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();

            return new SCPagedResult<>(
                    ordered.stream().map(ingredient -> {
                        IngredientTranslation ingredientTranslation = ingredient.getTranslations().stream().filter(t -> t.getLanguage().getCode().equals(locale)).findFirst().orElse(null);
                        return toAdminDto (ingredient, ingredientTranslation, activeLanguagesSimpleMap);
                    }).toList(),
                    SCPaginationUtils.toPagedOption(idsPage)
            );
        }
        return SCPagedResult.empty();
    }

    @Override
    public IngredientDetailsAdminDto getByPublicIdAdmin(UUID publicId, @NonNull String locale) {
        languageDomainBridgeService.validateLocaleIsActiveOrThrow(locale);

        // ID CHECK
        var ingredient = ingredientRepository.findByPublicId(publicId).orElseThrow(
                () -> new NoIngredientFoundException(publicId)
        );

        var ingredientTranslation = ingredient.getTranslations().stream()
                .filter(t -> t.getLanguage().getCode().equals(locale))
                .findFirst().orElse(null);


        return toAdminDetailsDto(ingredient, ingredientTranslation, locale);
    }

    @Override
    @Transactional
    public IngredientPreviewAdminDto createNewIngredient(AddIngredientDto addIngredientDto, @NonNull String locale) {
        // Guard for existing slug
        if (ingredientRepository.existsBySlug(addIngredientDto.slug()))
            throw new IngredientSlugAlreadyExistsException(addIngredientDto.slug());


        // TRANSLATION CHECKS:
        // 1) No duplicated translation
        // 2) A new ingredient must have all active languages covered in translation, otherwise it is not valid
        var activeLanguageMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();
        var activeCodeSet = activeLanguageMap.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());

        Set<String> seenLocales = new HashSet<>();
        addIngredientDto.translations().forEach(t -> {
            if (!seenLocales.add(t.langCode()))
                throw new DuplicateIngredientLocaleException(t.langCode());
        });

        if (!activeCodeSet.containsAll(seenLocales) || activeCodeSet.size() != seenLocales.size())
            throw new MissingIngredientLocalesException();

        Ingredient newIngredient = new Ingredient();

        List<IngredientTranslation> translations = addIngredientDto.translations().stream()
                .map(t -> {
                    Language lang = languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguageMap, t.langCode());
                    return toIngredientTranslation(t, lang);
                })
                .collect(Collectors.toCollection(ArrayList::new));
        translations.forEach(t -> t.setIngredient(newIngredient));

        newIngredient.setSlug(addIngredientDto.slug());
        newIngredient.setFoodGroup(addIngredientDto.foodGroup());
        newIngredient.setCaloriesPer100g(addIngredientDto.caloriesPer100g());
        newIngredient.setAllergens(addIngredientDto.allergens());
        newIngredient.setVegetarian(addIngredientDto.isVegetarian());
        newIngredient.setVegan(addIngredientDto.isVegan());
        newIngredient.setGlutenFree(addIngredientDto.isGlutenFree());
        newIngredient.setTranslations(translations);

        // TAGS MANAGEMENT
        List<Tag> tagsForIngredient = tagDomainBridgeService.getTagsUsableForIngredients(addIngredientDto.ingredientTagsIds());
        List<IngredientTag> ingredientTags = toIngredientTags(tagsForIngredient, newIngredient);
        newIngredient.setIngredientTags(ingredientTags);

        ingredientRepository.save(newIngredient);

        return toAdminDto(
                newIngredient,
                translations.stream().filter(t -> t.getLanguage().getCode().equals(locale)).findFirst().orElse(null),
                languageDomainBridgeService.toSimpleLanguagesMap(activeLanguageMap));
    }

    @Override
    @Transactional
    public IngredientPreviewAdminDto updateIngredient(UUID publicId, UpdateIngredientDto updateIngredientDto) {
        var ingredientToUpdate = ingredientRepository.findByPublicIdWithAllTranslation(publicId).orElseThrow(
                () -> new NoIngredientFoundException(publicId)
        );

        // SLUG CHECK:
        if (!updateIngredientDto.slug().equals(ingredientToUpdate.getSlug()) && ingredientRepository.existsBySlug(updateIngredientDto.slug()))
            throw new IngredientSlugAlreadyExistsException(updateIngredientDto.slug());

        // TRANSLATION CHECK: Update translation is of an active locale
        var activeLangMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();
        Language lang = activeLangMap.get(updateIngredientDto.specificTranslation().langCode());
        if (lang == null) throw new IngredientLanguageNotActiveException(updateIngredientDto.specificTranslation().langCode());

        var ingredientTranslationToUpdate = ingredientToUpdate.getTranslations().stream()
                .filter(t -> t.getLanguage().getCode().equals(updateIngredientDto.specificTranslation().langCode()))
                .findFirst().orElse(null);

        if (ingredientTranslationToUpdate == null) {
            // New Translation
            ingredientTranslationToUpdate = new IngredientTranslation();
            ingredientTranslationToUpdate.setIngredient(ingredientToUpdate);
            ingredientTranslationToUpdate.setLanguage(lang);
            ingredientTranslationToUpdate.setName(updateIngredientDto.specificTranslation().name());
            ingredientToUpdate.getTranslations().add(ingredientTranslationToUpdate);
        } else if (!ingredientTranslationToUpdate.getName().equals(updateIngredientDto.specificTranslation().name())) {
            // Update existing translation — only touch fields that actually changed
            ingredientTranslationToUpdate.setName(updateIngredientDto.specificTranslation().name());
        }

        if (!ingredientToUpdate.getSlug().equals(updateIngredientDto.slug()))
            ingredientToUpdate.setSlug(updateIngredientDto.slug());
        if (!Objects.equals(ingredientToUpdate.getFoodGroup(), updateIngredientDto.foodGroup()))
            ingredientToUpdate.setFoodGroup(updateIngredientDto.foodGroup());
        if (!Objects.equals(ingredientToUpdate.getCaloriesPer100g(), updateIngredientDto.caloriesPer100g()))
            ingredientToUpdate.setCaloriesPer100g(updateIngredientDto.caloriesPer100g());
        if (!Arrays.equals(ingredientToUpdate.getAllergens(), updateIngredientDto.allergens()))
            ingredientToUpdate.setAllergens(updateIngredientDto.allergens());
        if (ingredientToUpdate.isVegetarian() != updateIngredientDto.isVegetarian())
            ingredientToUpdate.setVegetarian(updateIngredientDto.isVegetarian());
        if (ingredientToUpdate.isVegan() != updateIngredientDto.isVegan())
            ingredientToUpdate.setVegan(updateIngredientDto.isVegan());
        if (ingredientToUpdate.isGlutenFree() != updateIngredientDto.isGlutenFree())
            ingredientToUpdate.setGlutenFree(updateIngredientDto.isGlutenFree());

        // TAGS MANAGEMENT — full replace of the ingredient's tag set
        List<Tag> tagsForIngredient = tagDomainBridgeService.getTagsUsableForIngredients(updateIngredientDto.ingredientTagsIds());
        List<IngredientTag> newIngredientTags = toIngredientTags(tagsForIngredient, ingredientToUpdate);
        ingredientToUpdate.getIngredientTags().clear();
        ingredientToUpdate.getIngredientTags().addAll(newIngredientTags);

        return toAdminDto(ingredientToUpdate, ingredientTranslationToUpdate, languageDomainBridgeService.toSimpleLanguagesMap(activeLangMap));
    }

    @Override
    @Transactional
    public UUID deleteIngredient(UUID publicId) {
        var ingredientToDelete = ingredientRepository.findByPublicId(publicId).orElseThrow(
                () -> new NoIngredientFoundException(publicId)
        );

        ingredientRepository.delete(ingredientToDelete);
        return ingredientToDelete.getPublicId();
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
                ingredient.getFoodGroup(),
                ingredient.getCaloriesPer100g(),
                ingredient.getAllergens(),
                ingredient.isVegetarian(),
                ingredient.isVegan(),
                ingredient.isGlutenFree()
        );
    }

    private IngredientPreviewAdminDto toAdminDto(Ingredient ingredient, IngredientTranslation ingredientTranslation, Map<String, String> activeLanguageMap) {
        Map<String, String> translatedLanguages = languageDomainBridgeService.buildTranslatedLanguagesMap(ingredient.getTranslations(), activeLanguageMap);

        String namePreview = ingredientTranslation != null ? ingredientTranslation.getName() : null;
        return new IngredientPreviewAdminDto(
                ingredient.getPublicId(),
                ingredient.getSlug(),
                ingredient.getFoodGroup(),
                ingredient.getCaloriesPer100g(),
                ingredient.getAllergens(),
                ingredient.isVegetarian(),
                ingredient.isVegan(),
                ingredient.isGlutenFree(),
                namePreview,
                translatedLanguages
        );
    }

    private IngredientDetailsAdminDto toAdminDetailsDto(Ingredient ingredient, @Nullable IngredientTranslation specificTranslation, @NonNull String locale) {
        var tagList = ingredient.getIngredientTags().stream()
                .map(IngredientTag::getTag)
                .map(tag -> {
                    var tagTranslation = tag.getTranslations().stream()
                            .filter(t -> t.getLanguage().getCode().equals(locale))
                            .findFirst().orElse(null);
                    return new TagDto(
                            tag.getPublicId(),
                            tag.getSlug(),
                            tagTranslation != null ? tagTranslation.getLabel() : null
                    );
                })
                .toList();

        return new IngredientDetailsAdminDto(
                ingredient.getPublicId(),
                ingredient.getSlug(),
                ingredient.getFoodGroup(),
                ingredient.getCaloriesPer100g(),
                ingredient.getAllergens(),
                ingredient.isVegetarian(),
                ingredient.isVegan(),
                ingredient.isGlutenFree(),
                specificTranslation != null ? specificTranslation.getName() : null,
                tagList
        );
    }

    private List<IngredientTag> toIngredientTags(List<Tag> tags, Ingredient ingredient) {
        return tags.stream()
                .map(tag -> {
                    IngredientTag ingredientTag = new IngredientTag();
                    ingredientTag.setIngredient(ingredient);
                    ingredientTag.setTag(tag);
                    return ingredientTag;
                })
                .collect(Collectors.toList());
    }

    private IngredientTranslation toIngredientTranslation(IngredientTranslationInputDto translationInputDto, Language lang) {
        IngredientTranslation translation = new IngredientTranslation();
        translation.setLanguage(lang);
        translation.setName(translationInputDto.name());
        return translation;
    }
}
