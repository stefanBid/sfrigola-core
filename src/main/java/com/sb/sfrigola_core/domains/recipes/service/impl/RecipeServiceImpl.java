package com.sb.sfrigola_core.domains.recipes.service.impl;

import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.util.SCAuthenticationUtils;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.domains.categories.entity.Category;
import com.sb.sfrigola_core.domains.categories.service.ICategoryDomainBridgeService;
import com.sb.sfrigola_core.domains.ingredients.entity.Ingredient;
import com.sb.sfrigola_core.domains.ingredients.service.IIngredientDomainBridgeService;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import com.sb.sfrigola_core.domains.recipes.dto.input.AddRecipeDto;
import com.sb.sfrigola_core.domains.recipes.dto.input.RecipeIngredientInputDto;
import com.sb.sfrigola_core.domains.recipes.dto.input.RecipeTranslationInputDto;
import com.sb.sfrigola_core.domains.recipes.dto.input.UpdateRecipeDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.*;
import com.sb.sfrigola_core.domains.recipes.entity.Recipe;
import com.sb.sfrigola_core.domains.recipes.entity.RecipeIngredient;
import com.sb.sfrigola_core.domains.recipes.entity.RecipeTag;
import com.sb.sfrigola_core.domains.recipes.entity.RecipeTranslation;
import com.sb.sfrigola_core.domains.recipes.enums.DifficultyLevel;
import com.sb.sfrigola_core.domains.recipes.enums.FeedType;
import com.sb.sfrigola_core.domains.recipes.enums.RecipeSortField;
import com.sb.sfrigola_core.domains.recipes.exception.DuplicateRecipeLocaleException;
import com.sb.sfrigola_core.domains.recipes.exception.MissingRecipeLocalesException;
import com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException;
import com.sb.sfrigola_core.domains.recipes.exception.RecipeAuthorMismatchException;
import com.sb.sfrigola_core.domains.recipes.models.RecipeSpecificFilter;
import com.sb.sfrigola_core.domains.recipes.repository.IRecipeRepository;
import com.sb.sfrigola_core.domains.recipes.service.IRecipeService;
import com.sb.sfrigola_core.domains.tags.dto.view.TagDto;
import com.sb.sfrigola_core.domains.tags.entity.Tag;
import com.sb.sfrigola_core.domains.tags.service.ITagDomainBridgeService;
import com.sb.sfrigola_core.domains.users.entity.SCUser;
import com.sb.sfrigola_core.domains.users.service.ISCUserDomainBridgeService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecipeServiceImpl implements IRecipeService {

    private final IRecipeRepository recipeRepository;
    private final ILanguageDomainBridgeService languageDomainBridgeService;
    private final ITagDomainBridgeService tagDomainBridgeService;
    private final IIngredientDomainBridgeService ingredientDomainBridgeService;
    private final ICategoryDomainBridgeService categoryDomainBridgeService;
    private final ISCUserDomainBridgeService userDomainBridgeService;

    @Override
    public Map<FeedType, RecipesFeedDto> getAllHomeFeed(@NonNull UUID categoryId, @NonNull String locale) {
        // Check Locale is active
        languageDomainBridgeService.validateLocaleIsActiveOrThrow(locale);

        // Check Category exist
        Category category = categoryDomainBridgeService.getCategoryEntityByPublicIdOrThrow(categoryId);

        // VIRAL FEED (Not Implemented yet)
        // FAVOURITE FEED (Not Implemented yet)

        Map<FeedType, RecipesFeedDto> homeFeed = new EnumMap<>(FeedType.class);

        // QUICK FEED — shortest prep + cook time first
        var quickFilter = new RecipeSpecificFilter(null, null, null, null, null, null, true, category.getId());
        var quickFilterQuery = SCFilterQuery.powerful(null, RecipeSortField.TOTAL_TIME_MIN, SortDirection.ASC, 10, 0, quickFilter);
        homeFeed.put(FeedType.QUICK, new RecipesFeedDto(getFilteredPublished(quickFilterQuery, locale), (short) 0));

        // LIKE_A_CHEF FEED — hard difficulty, longest prep + cook time first
        var gourmetFilter = new RecipeSpecificFilter(DifficultyLevel.HARD, null, null, null, null, null, true, category.getId());
        var gourmetFilterQuery = SCFilterQuery.powerful(null, RecipeSortField.TOTAL_TIME_MIN, SortDirection.DESC, 10, 0, gourmetFilter);
        homeFeed.put(FeedType.LIKE_A_CHEF, new RecipesFeedDto(getFilteredPublished(gourmetFilterQuery, locale), (short) 1));

        // ECONOMICAL FEED — lowest ingredient-count-to-servings ratio first
        var economicalFilter = new RecipeSpecificFilter(null, null, null, null, null, null, true, category.getId());
        var economicalFilterQuery = SCFilterQuery.powerful(null, RecipeSortField.ECONOMICAL_RATIO, SortDirection.ASC, 10, 0, economicalFilter);
        homeFeed.put(FeedType.ECONOMICAL, new RecipesFeedDto(getFilteredPublished(economicalFilterQuery, locale), (short) 2));

        return homeFeed;
    }

    @Override
    public SCPagedResult<RecipePreviewAdminDto> getAllAdmin(SCFilterQuery<RecipeSpecificFilter> filterQuery, UUID categoryId, @NonNull String locale) {
        var activeLanguagesSimpleMap = languageDomainBridgeService.getAllActiveLanguagesSimpleMap();

        languageDomainBridgeService.validateLocaleIsActiveByActiveLanguagesMapKeysOrThrow(activeLanguagesSimpleMap.keySet(), locale);

        Long categoryDbId = categoryId != null ? categoryDomainBridgeService.getCategoryEntityByPublicIdOrThrow(categoryId).getId() : null;

        boolean sortByTitle = filterQuery.sortBy() == null || filterQuery.sortBy() == RecipeSortField.TITLE;
        boolean descending = filterQuery.sort() != null && !filterQuery.sort().isAsc();

        var filterOtherExtracted = filterQuery.other();
        var isPublished = filterOtherExtracted != null ? filterOtherExtracted.isPublished() : null;
        var difficulty = filterOtherExtracted != null && filterOtherExtracted.difficulty() != null ? filterOtherExtracted.difficulty().getValue() : null;
        var mealType = filterOtherExtracted != null && filterOtherExtracted.mealType() != null ? filterOtherExtracted.mealType().getValue() : null;
        var season = filterOtherExtracted != null && filterOtherExtracted.season() != null ? filterOtherExtracted.season().getValue() : null;
        var isVegetarian = filterOtherExtracted != null ? filterOtherExtracted.isVegetarian() : null;
        var isVegan = filterOtherExtracted != null ? filterOtherExtracted.isVegan() : null;
        var isGlutenFree = filterOtherExtracted != null ? filterOtherExtracted.isGlutenFree() : null;

        var pageable = SCPaginationUtils.toPageable(filterQuery, sortByTitle);
        Page<Long> idsPage;

        if (sortByTitle) {
            idsPage = descending
                    ? recipeRepository.findIdsByFiltersAndLocaleDesc(locale, filterQuery.searchKey(), isPublished, difficulty, mealType, season, isVegetarian, isVegan, isGlutenFree, categoryDbId, pageable)
                    : recipeRepository.findIdsByFiltersAndLocaleAsc(locale, filterQuery.searchKey(), isPublished, difficulty, mealType, season, isVegetarian, isVegan, isGlutenFree, categoryDbId, pageable);
        } else {
            idsPage = recipeRepository.findIdsByFiltersAndLocaleOtherSort(locale, filterQuery.searchKey(), isPublished, difficulty, mealType, season, isVegetarian, isVegan, isGlutenFree, categoryDbId, pageable);
        }

        if (idsPage.hasContent()) {
            var ids = idsPage.getContent();
            Map<Long, Recipe> byId = recipeRepository.findByIdsWithAllTranslations(ids)
                    .stream().collect(Collectors.toMap(Recipe::getId, r -> r));
            List<Recipe> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();

            return new SCPagedResult<>(
                    ordered.stream().map(recipe -> {
                        RecipeTranslation recipeTranslation = recipe.getTranslations().stream()
                                .filter(t -> t.getLanguage().getCode().equals(locale))
                                .findFirst().orElse(null);
                        return toAdminDto(recipe, recipeTranslation, activeLanguagesSimpleMap);
                    }).toList(),
                    SCPaginationUtils.toPagedOption(idsPage)
            );
        }
        return SCPagedResult.empty();
    }

    @Override
    public SCPagedResult<RecipeDto> searchRecipes(SCFilterQuery<Void> filterQuery, UUID categoryId, @NonNull String locale) {
        languageDomainBridgeService.validateLocaleIsActiveOrThrow(locale);

        Long categoryDbId = categoryId != null ? categoryDomainBridgeService.getCategoryEntityByPublicIdOrThrow(categoryId).getId() : null;

        var pageable = SCPaginationUtils.toPageable(filterQuery);

        var recipeIds = recipeRepository.findIdsBySearchKeyAndLocale(locale, filterQuery.searchKey(), categoryDbId, pageable);

        if (recipeIds.hasContent()) {
            var ids = recipeIds.getContent();
            Map<Long, Recipe> byId = recipeRepository.findByIdsWithSpecificTranslation(ids, locale)
                    .stream().collect(Collectors.toMap(Recipe::getId, r -> r));
            List<Recipe> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
            return new SCPagedResult<>(
                    ordered.stream().map(this::toDto).toList(),
                    SCPaginationUtils.toPagedOption(recipeIds)
            );
        }
        return SCPagedResult.empty();
    }

    @Override
    public RecipeDetailsAdminDto getByPublicIdAdmin(UUID publicId, @NonNull String locale) {
        languageDomainBridgeService.validateLocaleIsActiveOrThrow(locale);

        var recipe = recipeRepository.findByPublicId(publicId).orElseThrow(
                () -> new NoRecipeFoundException(publicId)
        );

        var recipeTranslation = recipe.getTranslations().stream()
                .filter(t -> t.getLanguage().getCode().equals(locale))
                .findFirst().orElse(null);

        return toAdminDetailsDto(recipe, recipeTranslation, locale);
    }

    @Override
    public RecipeDetailsDto getByPublicId(UUID publicId, @NonNull String locale) {
        languageDomainBridgeService.validateLocaleIsActiveOrThrow(locale);

        var recipe = recipeRepository.findByPublicId(publicId)
                .filter(Recipe::isPublished)
                .orElseThrow(() -> new NoRecipeFoundException(publicId));

        var recipeTranslation = recipe.getTranslations().stream()
                .filter(t -> t.getLanguage().getCode().equals(locale))
                .findFirst().orElse(null);

        return toDetailsDto(recipe, recipeTranslation, locale);
    }

    @Override
    @Transactional
    public RecipePreviewAdminDto createRecipe(AddRecipeDto addRecipeDto, @NonNull String locale) {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();
        SCUser author = userDomainBridgeService.getUserEntityByPublicIdOrThrow(authUser.publicId());

        // TRANSLATION CHECKS:
        // 1) No duplicated translation
        // 2) A new recipe must have all active languages covered in translation, otherwise it is not valid
        var activeLanguageMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();
        var activeCodeSet = activeLanguageMap.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());

        Set<String> seenLocales = new HashSet<>();
        addRecipeDto.translations().forEach(t -> {
            if (!seenLocales.add(t.langCode()))
                throw new DuplicateRecipeLocaleException(t.langCode());
        });

        if (!activeCodeSet.containsAll(seenLocales) || activeCodeSet.size() != seenLocales.size())
            throw new MissingRecipeLocalesException();

        Recipe newRecipe = new Recipe();

        List<RecipeTranslation> translations = addRecipeDto.translations().stream()
                .map(t -> {
                    Language lang = languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguageMap, t.langCode());
                    return toRecipeTranslation(t, lang);
                })
                .collect(Collectors.toCollection(ArrayList::new));
        translations.forEach(t -> t.setRecipe(newRecipe));

        newRecipe.setAuthor(author);
        newRecipe.setCategory(resolveCategoryOrNull(addRecipeDto.categoryPublicId()));
        newRecipe.setDifficulty(addRecipeDto.difficulty());
        newRecipe.setMealType(addRecipeDto.mealType());
        newRecipe.setSeason(addRecipeDto.season());
        newRecipe.setPrepTimeMin(addRecipeDto.prepTimeMin());
        newRecipe.setCookTimeMin(addRecipeDto.cookTimeMin());
        newRecipe.setServings(addRecipeDto.servings());
        newRecipe.setVegetarian(addRecipeDto.isVegetarian());
        newRecipe.setVegan(addRecipeDto.isVegan());
        newRecipe.setGlutenFree(addRecipeDto.isGlutenFree());
        newRecipe.setPublished(addRecipeDto.isPublished());
        newRecipe.setTranslations(translations);

        // TAGS MANAGEMENT
        List<Tag> tagsForRecipe = tagDomainBridgeService.getTagsUsableForRecipes(nullSafe(addRecipeDto.recipeTagsIds()));
        newRecipe.setRecipeTags(toRecipeTags(tagsForRecipe, newRecipe));

        // INGREDIENTS MANAGEMENT
        newRecipe.setRecipeIngredients(toRecipeIngredients(nullSafe(addRecipeDto.ingredients()), newRecipe));

        // MATERIALIZED FEED-SORTING FIELDS
        recomputeFeedSortingFields(newRecipe);

        recipeRepository.save(newRecipe);

        return toAdminDto(
                newRecipe,
                translations.stream().filter(t -> t.getLanguage().getCode().equals(locale)).findFirst().orElse(null),
                toSimpleLanguagesMap(activeLanguageMap));
    }

    @Override
    @Transactional
    public RecipePreviewAdminDto updateRecipe(UUID publicId, UpdateRecipeDto updateRecipeDto) {
        var recipeToUpdate = recipeRepository.findByPublicIdWithAllTranslation(publicId).orElseThrow(
                () -> new NoRecipeFoundException(publicId)
        );

        assertAuthorOrAdmin(recipeToUpdate, publicId);

        // TRANSLATION CHECK: Update translation is of an active locale
        var activeLangMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();
        Language lang = languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLangMap, updateRecipeDto.specificTranslation().langCode());

        var recipeTranslationToUpdate = recipeToUpdate.getTranslations().stream()
                .filter(t -> t.getLanguage().getCode().equals(updateRecipeDto.specificTranslation().langCode()))
                .findFirst().orElse(null);

        if (recipeTranslationToUpdate == null) {
            // New Translation
            recipeTranslationToUpdate = toRecipeTranslation(updateRecipeDto.specificTranslation(), lang);
            recipeTranslationToUpdate.setRecipe(recipeToUpdate);
            recipeToUpdate.getTranslations().add(recipeTranslationToUpdate);
        } else {
            if (!recipeTranslationToUpdate.getTitle().equals(updateRecipeDto.specificTranslation().title()))
                recipeTranslationToUpdate.setTitle(updateRecipeDto.specificTranslation().title());
            if (!Objects.equals(recipeTranslationToUpdate.getDescription(), updateRecipeDto.specificTranslation().description()))
                recipeTranslationToUpdate.setDescription(updateRecipeDto.specificTranslation().description());
            if (!recipeTranslationToUpdate.getInstructions().equals(updateRecipeDto.specificTranslation().instructions()))
                recipeTranslationToUpdate.setInstructions(updateRecipeDto.specificTranslation().instructions());
        }

        recipeToUpdate.setCategory(resolveCategoryOrNull(updateRecipeDto.categoryPublicId()));
        if (!recipeToUpdate.getDifficulty().equals(updateRecipeDto.difficulty()))
            recipeToUpdate.setDifficulty(updateRecipeDto.difficulty());
        if (!Objects.equals(recipeToUpdate.getMealType(), updateRecipeDto.mealType()))
            recipeToUpdate.setMealType(updateRecipeDto.mealType());
        if (!recipeToUpdate.getSeason().equals(updateRecipeDto.season()))
            recipeToUpdate.setSeason(updateRecipeDto.season());
        if (!Objects.equals(recipeToUpdate.getPrepTimeMin(), updateRecipeDto.prepTimeMin()))
            recipeToUpdate.setPrepTimeMin(updateRecipeDto.prepTimeMin());
        if (!Objects.equals(recipeToUpdate.getCookTimeMin(), updateRecipeDto.cookTimeMin()))
            recipeToUpdate.setCookTimeMin(updateRecipeDto.cookTimeMin());
        if (!Objects.equals(recipeToUpdate.getServings(), updateRecipeDto.servings()))
            recipeToUpdate.setServings(updateRecipeDto.servings());
        if (recipeToUpdate.isVegetarian() != updateRecipeDto.isVegetarian())
            recipeToUpdate.setVegetarian(updateRecipeDto.isVegetarian());
        if (recipeToUpdate.isVegan() != updateRecipeDto.isVegan())
            recipeToUpdate.setVegan(updateRecipeDto.isVegan());
        if (recipeToUpdate.isGlutenFree() != updateRecipeDto.isGlutenFree())
            recipeToUpdate.setGlutenFree(updateRecipeDto.isGlutenFree());
        if (recipeToUpdate.isPublished() != updateRecipeDto.isPublished())
            recipeToUpdate.setPublished(updateRecipeDto.isPublished());

        // TAGS MANAGEMENT — full replace of the recipe's tag set
        List<Tag> tagsForRecipe = tagDomainBridgeService.getTagsUsableForRecipes(nullSafe(updateRecipeDto.recipeTagsIds()));
        recipeToUpdate.getRecipeTags().clear();
        recipeToUpdate.getRecipeTags().addAll(toRecipeTags(tagsForRecipe, recipeToUpdate));

        // INGREDIENTS MANAGEMENT — full replace of the recipe's ingredient list
        recipeToUpdate.getRecipeIngredients().clear();
        recipeToUpdate.getRecipeIngredients().addAll(toRecipeIngredients(nullSafe(updateRecipeDto.ingredients()), recipeToUpdate));

        // MATERIALIZED FEED-SORTING FIELDS
        recomputeFeedSortingFields(recipeToUpdate);

        return toAdminDto(recipeToUpdate, recipeTranslationToUpdate, toSimpleLanguagesMap(activeLangMap));
    }

    @Override
    @Transactional
    public UUID deleteRecipe(UUID publicId) {
        var recipeToDelete = recipeRepository.findByPublicId(publicId).orElseThrow(
                () -> new NoRecipeFoundException(publicId)
        );

        assertAuthorOrAdmin(recipeToDelete, publicId);

        recipeRepository.delete(recipeToDelete);
        return recipeToDelete.getPublicId();
    }

    // =========================================================
    // PRIVATE
    // =========================================================

    private List<RecipeDto> getFilteredPublished(SCFilterQuery<RecipeSpecificFilter> filterQuery, @NonNull String locale) {
        var pageable = SCPaginationUtils.toPageable(filterQuery);
        var filter = filterQuery.other();

        var recipeIds = recipeRepository.findIdsByFiltersAndLocaleOtherSort(
                locale,
                filterQuery.searchKey(),
                true,
                filter.difficulty() != null ? filter.difficulty().getValue() : null,
                filter.mealType() != null ? filter.mealType().getValue() : null,
                filter.season() != null ? filter.season().getValue() : null,
                filter.isVegetarian(),
                filter.isVegan(),
                filter.isGlutenFree(),
                filter.categoryIdCalculated(),
                pageable
        );

        if (recipeIds.hasContent()) {
            var ids = recipeIds.getContent();
            Map<Long, Recipe> byId = recipeRepository.findByIdsWithSpecificTranslation(ids, locale)
                    .stream().collect(Collectors.toMap(Recipe::getId, r -> r));
            List<Recipe> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
            return ordered.stream().map(this::toDto).toList();
        }


        return List.of(); // Placeholder
    }

    private void assertAuthorOrAdmin(Recipe recipe, UUID publicId) {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();
        if (authUser.role().isAdmin()) return;
        if (!recipe.getAuthor().getPublicId().equals(authUser.publicId()))
            throw new RecipeAuthorMismatchException(publicId);
    }

    private Category resolveCategoryOrNull(UUID categoryPublicId) {
        return categoryPublicId != null ? categoryDomainBridgeService.getCategoryEntityByPublicIdOrThrow(categoryPublicId) : null;
    }

    /**
     * Recomputes {@code totalTimeMin} and {@code economicalRatio} from the recipe's current
     * timing, servings and ingredient list. Materialized on the entity (no DB trigger, no
     * {@code @Formula}) so home-feed sorting queries never need a correlated subquery per row.
     * Must be called after ingredients/servings/prep/cook time are set, before save.
     */
    private void recomputeFeedSortingFields(Recipe recipe) {
        int prepTimeMin = recipe.getPrepTimeMin() != null ? recipe.getPrepTimeMin() : 0;
        int cookTimeMin = recipe.getCookTimeMin() != null ? recipe.getCookTimeMin() : 0;
        recipe.setTotalTimeMin(prepTimeMin + cookTimeMin);

        int effectiveServings = recipe.getServings() != null && recipe.getServings() > 0 ? recipe.getServings() : 1;
        recipe.setEconomicalRatio(BigDecimal.valueOf(recipe.getRecipeIngredients().size())
                .divide(BigDecimal.valueOf(effectiveServings), 4, RoundingMode.HALF_UP));
    }

    private <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : List.of();
    }

    private RecipeDto toDto(Recipe recipe) {
        // We have only one item in the list of translations — the first query filters by locale
        var translation = recipe.getTranslations().getFirst();
        return new RecipeDto(
                recipe.getPublicId(),
                recipe.getAuthor().getPublicId(),
                recipe.getCategory() != null ? recipe.getCategory().getPublicId() : null,
                translation.getTitle(),
                translation.getDescription(),
                false, // isFavourite — favorites domain not implemented yet
                recipe.getTotalTimeMin(),
                recipe.getEconomicalRatio()
        );
    }

    private RecipePreviewAdminDto toAdminDto(Recipe recipe, RecipeTranslation recipeTranslation, Map<String, String> activeLanguagesMap) {
        Map<String, String> translatedLanguages = recipe.getTranslations().stream()
                .map(t -> t.getLanguage().getCode())
                .filter(activeLanguagesMap::containsKey)
                .collect(Collectors.toMap(code -> code, activeLanguagesMap::get));

        String titlePreview = recipeTranslation != null ? recipeTranslation.getTitle() : null;
        return new RecipePreviewAdminDto(
                recipe.getPublicId(),
                recipe.getAuthor().getPublicId(),
                recipe.getCategory() != null ? recipe.getCategory().getPublicId() : null,
                recipe.getDifficulty(),
                recipe.getMealType(),
                recipe.getSeason(),
                recipe.getPrepTimeMin(),
                recipe.getCookTimeMin(),
                recipe.getServings(),
                recipe.isVegetarian(),
                recipe.isVegan(),
                recipe.isGlutenFree(),
                recipe.isPublished(),
                titlePreview,
                translatedLanguages
        );
    }

    private Map<String, String> toSimpleLanguagesMap(Map<String, Language> languageEntitiesMap) {
        return languageEntitiesMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));
    }

    private RecipeDetailsAdminDto toAdminDetailsDto(Recipe recipe, @Nullable RecipeTranslation specificTranslation, @NonNull String locale) {
        return new RecipeDetailsAdminDto(
                recipe.getPublicId(),
                recipe.getAuthor().getPublicId(),
                recipe.getCategory() != null ? recipe.getCategory().getPublicId() : null,
                recipe.getDifficulty(),
                recipe.getMealType(),
                recipe.getSeason(),
                recipe.getPrepTimeMin(),
                recipe.getCookTimeMin(),
                recipe.getServings(),
                recipe.isVegetarian(),
                recipe.isVegan(),
                recipe.isGlutenFree(),
                recipe.isPublished(),
                specificTranslation != null ? specificTranslation.getTitle() : null,
                specificTranslation != null ? specificTranslation.getDescription() : null,
                specificTranslation != null ? specificTranslation.getInstructions() : null,
                buildIngredientList(recipe, locale),
                buildTagList(recipe, locale)
        );
    }

    private RecipeDetailsDto toDetailsDto(Recipe recipe, @Nullable RecipeTranslation specificTranslation, @NonNull String locale) {
        return new RecipeDetailsDto(
                recipe.getPublicId(),
                recipe.getAuthor().getPublicId(),
                recipe.getCategory() != null ? recipe.getCategory().getPublicId() : null,
                recipe.getDifficulty(),
                recipe.getMealType(),
                recipe.getSeason(),
                recipe.getPrepTimeMin(),
                recipe.getCookTimeMin(),
                recipe.getServings(),
                recipe.isVegetarian(),
                recipe.isVegan(),
                recipe.isGlutenFree(),
                specificTranslation != null ? specificTranslation.getTitle() : null,
                specificTranslation != null ? specificTranslation.getDescription() : null,
                specificTranslation != null ? specificTranslation.getInstructions() : null,
                buildIngredientList(recipe, locale),
                buildTagList(recipe, locale)
        );
    }

    private List<TagDto> buildTagList(Recipe recipe, String locale) {
        return recipe.getRecipeTags().stream()
                .map(RecipeTag::getTag)
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
    }

    private List<RecipeIngredientDto> buildIngredientList(Recipe recipe, String locale) {
        return recipe.getRecipeIngredients().stream()
                .map(ri -> {
                    var ingredientTranslation = ri.getIngredient().getTranslations().stream()
                            .filter(t -> t.getLanguage().getCode().equals(locale))
                            .findFirst().orElse(null);
                    return new RecipeIngredientDto(
                            ri.getIngredient().getPublicId(),
                            ingredientTranslation != null ? ingredientTranslation.getName() : null,
                            ri.getQuantity(),
                            ri.getUnit(),
                            ri.getPreparationNote(),
                            ri.getSortOrder()
                    );
                })
                .toList();
    }

    private List<RecipeTag> toRecipeTags(List<Tag> tags, Recipe recipe) {
        return tags.stream()
                .map(tag -> {
                    RecipeTag recipeTag = new RecipeTag();
                    recipeTag.setRecipe(recipe);
                    recipeTag.setTag(tag);
                    return recipeTag;
                })
                .collect(Collectors.toList());
    }

    private List<RecipeIngredient> toRecipeIngredients(List<RecipeIngredientInputDto> inputs, Recipe recipe) {
        List<Ingredient> resolvedIngredients = ingredientDomainBridgeService.getIngredientsByPublicIds(
                inputs.stream().map(RecipeIngredientInputDto::ingredientPublicId).toList()
        );

        List<RecipeIngredient> recipeIngredients = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            var input = inputs.get(i);
            RecipeIngredient recipeIngredient = new RecipeIngredient();
            recipeIngredient.setRecipe(recipe);
            recipeIngredient.setIngredient(resolvedIngredients.get(i));
            recipeIngredient.setQuantity(input.quantity());
            recipeIngredient.setUnit(input.unit());
            recipeIngredient.setPreparationNote(input.preparationNote());
            recipeIngredient.setSortOrder(input.sortOrder());
            recipeIngredients.add(recipeIngredient);
        }
        return recipeIngredients;
    }

    private RecipeTranslation toRecipeTranslation(RecipeTranslationInputDto translationInputDto, Language lang) {
        RecipeTranslation translation = new RecipeTranslation();
        translation.setLanguage(lang);
        translation.setTitle(translationInputDto.title());
        translation.setDescription(translationInputDto.description());
        translation.setInstructions(translationInputDto.instructions());
        return translation;
    }
}
