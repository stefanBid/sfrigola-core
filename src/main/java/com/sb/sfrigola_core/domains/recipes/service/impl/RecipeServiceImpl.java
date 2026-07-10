package com.sb.sfrigola_core.domains.recipes.service.impl;

import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.util.SCAuthenticationUtils;
import com.sb.sfrigola_core.common.util.SCDataStructureUtils;
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

        List<Long> categoryCalculatedIds = resolveCategoryFilterIds(categoryId);

        // VIRAL FEED (Not Implemented yet)
        // FAVOURITE FEED (Not Implemented yet)

        Map<FeedType, RecipesFeedDto> homeFeed = new EnumMap<>(FeedType.class);

        // QUICK FEED — shortest prep + cook time first
        var quickFilter = new RecipeSpecificFilter(null, null, null, null, null, null, true);
        var quickFilterQuery = SCFilterQuery.powerful(null, RecipeSortField.TOTAL_TIME_MIN, SortDirection.ASC, 10, 0, quickFilter);
        homeFeed.put(FeedType.QUICK, new RecipesFeedDto(getFilteredPublished(quickFilterQuery, categoryCalculatedIds, locale), (short) 0));

        // LIKE_A_CHEF FEED — hard difficulty, longest prep + cook time first
        var gourmetFilter = new RecipeSpecificFilter(DifficultyLevel.HARD, null, null, null, null, null, true);
        var gourmetFilterQuery = SCFilterQuery.powerful(null, RecipeSortField.TOTAL_TIME_MIN, SortDirection.DESC, 10, 0, gourmetFilter);
        homeFeed.put(FeedType.LIKE_A_CHEF, new RecipesFeedDto(getFilteredPublished(gourmetFilterQuery, categoryCalculatedIds, locale), (short) 1));

        // ECONOMICAL FEED — lowest ingredient-count-to-servings ratio first
        var economicalFilter = new RecipeSpecificFilter(null, null, null, null, null, null, true);
        var economicalFilterQuery = SCFilterQuery.powerful(null, RecipeSortField.ECONOMICAL_RATIO, SortDirection.ASC, 10, 0, economicalFilter);
        homeFeed.put(FeedType.ECONOMICAL, new RecipesFeedDto(getFilteredPublished(economicalFilterQuery,categoryCalculatedIds, locale), (short) 2));

        return homeFeed;
    }

    @Override
    public SCPagedResult<RecipePreviewAdminDto> getAllAdmin(SCFilterQuery<RecipeSpecificFilter> filterQuery, UUID categoryId, @NonNull String locale) {
        var activeLanguagesSimpleMap = languageDomainBridgeService.getAllActiveLanguagesSimpleMap();

        languageDomainBridgeService.validateLocaleIsActiveByActiveLanguagesMapKeysOrThrow(activeLanguagesSimpleMap.keySet(), locale);

        List<Long> categoryDbIds = resolveCategoryFilterIds(categoryId);

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
                    ? recipeRepository.findIdsByFiltersAndLocaleDesc(locale, filterQuery.searchKey(), isPublished, difficulty, mealType, season, isVegetarian, isVegan, isGlutenFree, categoryDbIds, pageable)
                    : recipeRepository.findIdsByFiltersAndLocaleAsc(locale, filterQuery.searchKey(), isPublished, difficulty, mealType, season, isVegetarian, isVegan, isGlutenFree, categoryDbIds, pageable);
        } else {
            idsPage = recipeRepository.findIdsByFiltersAndLocaleOtherSort(locale, filterQuery.searchKey(), isPublished, difficulty, mealType, season, isVegetarian, isVegan, isGlutenFree, categoryDbIds, pageable);
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

        List<Long> categoryDbIds = resolveCategoryFilterIds(categoryId);

        var pageable = SCPaginationUtils.toPageable(filterQuery);

        var recipeIds = recipeRepository.findIdsBySearchKeyAndLocale(locale, filterQuery.searchKey(), categoryDbIds, pageable);

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
        List<Tag> tagsForRecipe = tagDomainBridgeService.getTagsUsableForRecipes(SCDataStructureUtils.nullSafeList(addRecipeDto.recipeTagsIds()));
        newRecipe.setRecipeTags(toRecipeTags(tagsForRecipe, newRecipe));

        // INGREDIENTS MANAGEMENT
        newRecipe.setRecipeIngredients(toRecipeIngredients(SCDataStructureUtils.nullSafeList(addRecipeDto.ingredients()), newRecipe));

        // MATERIALIZED FEED-SORTING FIELDS
        recomputeFeedSortingFields(newRecipe);

        recipeRepository.save(newRecipe);

        return toAdminDto(
                newRecipe,
                translations.stream().filter(t -> t.getLanguage().getCode().equals(locale)).findFirst().orElse(null),
                languageDomainBridgeService.toSimpleLanguagesMap(activeLanguageMap));
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
            String newInstructions = SCDataStructureUtils.joinListIntoString(updateRecipeDto.specificTranslation().instructions(), ". ");

            if (!recipeTranslationToUpdate.getInstructions().equals(newInstructions))
                recipeTranslationToUpdate.setInstructions(newInstructions);
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

        // TAGS MANAGEMENT — reconcile to the requested tag set (not clear+re-add: RecipeTag's
        // id is @MapsId(recipe+tag), so a fresh instance for a tag that's kept unchanged would
        // collide with the still-managed, pending-orphan-removal old row for that same id)
        List<Tag> tagsForRecipe = tagDomainBridgeService.getTagsUsableForRecipes(SCDataStructureUtils.nullSafeList(updateRecipeDto.recipeTagsIds()));
        reconcileRecipeTags(recipeToUpdate, tagsForRecipe);

        // INGREDIENTS MANAGEMENT — reconcile to the requested ingredient lines (not clear+re-add:
        // see reconcileRecipeIngredients for why)
        reconcileRecipeIngredients(recipeToUpdate, SCDataStructureUtils.nullSafeList(updateRecipeDto.ingredients()));

        // MATERIALIZED FEED-SORTING FIELDS
        recomputeFeedSortingFields(recipeToUpdate);

        return toAdminDto(recipeToUpdate, recipeTranslationToUpdate, languageDomainBridgeService.toSimpleLanguagesMap(activeLangMap));
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

    /**
     * Runs one home-feed row: fetches published recipe IDs matching {@code filterQuery} (sort
     * field/direction/limit come from it) restricted to {@code categoryCalculatedIds}, then loads
     * and maps them to {@link RecipeDto} in the same order. Shared by all {@link FeedType} rows
     * built in {@link #getAllHomeFeed}, each with its own filter/sort combination.
     *
     * @param filterQuery          pagination/sort/dietary filter for this specific feed row
     * @param categoryCalculatedIds category IDs to restrict to (as resolved by {@link #resolveCategoryFilterIds}); {@code null} means no restriction
     * @param locale               locale used both to filter (recipe must have a translation for it) and to localize the result
     * @return recipes for this row, empty if none match
     */
    private List<RecipeDto> getFilteredPublished(SCFilterQuery<RecipeSpecificFilter> filterQuery, List<Long> categoryCalculatedIds, @NonNull String locale) {
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
                categoryCalculatedIds,
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

    /**
     * Guards {@link #updateRecipe} and {@link #deleteRecipe}: reads the authenticated user from
     * the security context and allows the call to proceed only if they are an admin or the
     * recipe's author.
     *
     * @param recipe   the recipe being mutated
     * @param publicId the recipe's public ID, used only to build the exception message
     * @throws RecipeAuthorMismatchException if the authenticated user is neither the author nor an admin
     */
    private void assertAuthorOrAdmin(Recipe recipe, UUID publicId) {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();
        if (authUser.role().isAdmin()) return;
        if (!recipe.getAuthor().getPublicId().equals(authUser.publicId()))
            throw new RecipeAuthorMismatchException(publicId);
    }

    /**
     * Resolves the {@link Category} entity to assign as {@code recipe.category} on create/update.
     * Unlike {@link #resolveCategoryFilterIds}, this does no parent/children expansion — it just
     * resolves the single entity the recipe belongs to.
     *
     * @param categoryPublicId public ID from the request payload; {@code null} means "no category"
     * @return the resolved {@link Category}, or {@code null} if {@code categoryPublicId} is {@code null}
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException if no category matches
     */
    private Category resolveCategoryOrNull(UUID categoryPublicId) {
        return categoryPublicId != null ? categoryDomainBridgeService.getCategoryEntityByPublicIdOrThrow(categoryPublicId) : null;
    }

    /**
     * Resolves a category public ID into the set of internal category IDs to filter recipes by:
     * the category itself plus, if it is a parent category, all of its direct children.
     * Returns {@code null} (no filter) when {@code categoryPublicId} is {@code null}.
     */
    private List<Long> resolveCategoryFilterIds(UUID categoryPublicId) {
        if (categoryPublicId == null) return null;

        Category category = categoryDomainBridgeService.getCategoryEntityByPublicIdOrThrow(categoryPublicId);

        List<Long> categoryIds = new ArrayList<>();
        var isParent = category.getParent() == null;
        if (isParent) {
            var children = categoryDomainBridgeService.getChildrenCategories(category);
            categoryIds.addAll(children.stream().map(Category::getId).toList());
        }
        categoryIds.add(category.getId());

        return categoryIds;
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

    /**
     * Maps a {@link Recipe} to the public {@link RecipeDto} used by feed/search results.
     * Assumes {@code recipe.getTranslations()} contains exactly one entry — the caller must have
     * already filtered to a single locale (see {@link IRecipeRepository#findByIdsWithSpecificTranslation}).
     *
     * @param recipe the recipe entity, with its translations list pre-filtered to one locale
     * @return the mapped {@link RecipeDto}
     */
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

    /**
     * Maps a {@link Recipe} to {@link RecipePreviewAdminDto}, including which active languages
     * the recipe has a translation for (localization coverage, used by the admin CMS).
     *
     * @param recipe              the recipe entity, with its full translations collection loaded
     * @param recipeTranslation   the translation to preview (title), or {@code null} if none exists for the requested locale
     * @param activeLanguagesMap  active language code → display name, used to build the coverage map
     * @return the mapped {@link RecipePreviewAdminDto}
     */
    private RecipePreviewAdminDto toAdminDto(Recipe recipe, RecipeTranslation recipeTranslation, Map<String, String> activeLanguagesMap) {
        Map<String, String> translatedLanguages = languageDomainBridgeService.buildTranslatedLanguagesMap(recipe.getTranslations(), activeLanguagesMap);

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

    /**
     * Maps a {@link Recipe} to full admin details, including ingredient and tag lists localized
     * to {@code locale}. Used by {@link #getByPublicIdAdmin}; unlike {@link #toDto}, includes
     * draft-recipe fields and never filters by {@code isPublished}.
     *
     * @param recipe             the recipe entity, with ingredients/tags collections loaded
     * @param specificTranslation the translation to preview, or {@code null} if none exists for {@code locale}
     * @param locale             locale used to localize the ingredient and tag names
     * @return the mapped {@link RecipeDetailsAdminDto}
     */
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

    /**
     * Maps a {@link Recipe} to public details, including ingredient and tag lists localized to
     * {@code locale}. Used by {@link #getByPublicId}, which has already checked {@code isPublished}
     * before calling this — no dietary/publish flags beyond what's already on the entity are re-checked here.
     *
     * @param recipe             the recipe entity, with ingredients/tags collections loaded
     * @param specificTranslation the translation to preview, or {@code null} if none exists for {@code locale}
     * @param locale             locale used to localize the ingredient and tag names
     * @return the mapped {@link RecipeDetailsDto}
     */
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

    /**
     * Builds the recipe's tag list localized to {@code locale}, for {@link #toAdminDetailsDto}
     * and {@link #toDetailsDto}. If a tag has no translation for {@code locale}, its label is
     * {@code null} — the tag itself is still included.
     *
     * @param recipe the recipe entity, with {@code recipeTags} loaded
     * @param locale locale used to pick each tag's label
     * @return one {@link TagDto} per tag on the recipe
     */
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

    /**
     * Builds the recipe's ingredient list localized to {@code locale}, for {@link #toAdminDetailsDto}
     * and {@link #toDetailsDto}. If an ingredient has no translation for {@code locale}, its name
     * is {@code null} — the ingredient line (quantity/unit/note) is still included.
     *
     * @param recipe the recipe entity, with {@code recipeIngredients} loaded
     * @param locale locale used to pick each ingredient's name
     * @return one {@link RecipeIngredientDto} per ingredient line on the recipe, in {@code sortOrder}
     */
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

    /**
     * Wraps already-resolved and scope-validated {@link Tag} entities into new {@link RecipeTag}
     * bridge-entity instances linked to {@code recipe}. Used by both {@link #createRecipe} and
     * {@link #updateRecipe} (full tag-set replace) — resolution/validation happens beforehand via
     * {@code tagDomainBridgeService.getTagsUsableForRecipes}.
     *
     * @param tags   resolved tags to attach, already checked for {@code recipe}/{@code both} scope
     * @param recipe the owning recipe, set as the back-reference on each {@link RecipeTag}
     * @return one new {@link RecipeTag} per tag, not yet persisted
     */
    /**
     * Reconciles {@code recipe.recipeTags} to exactly {@code desiredTags}: removes tags no longer
     * wanted (triggers {@code orphanRemoval} deletion) and adds only the tags not already present.
     * Used instead of clear+re-add by {@link #updateRecipe} — {@link RecipeTag}'s id is
     * {@code @MapsId(recipe+tag)}, so re-adding a fresh instance for a tag that's unchanged would
     * collide with the old row for that same id, still managed in the persistence context pending
     * its (now unnecessary) orphan-removal delete: {@code NonUniqueObjectException}.
     *
     * @param recipe      the recipe whose {@code recipeTags} collection is mutated in place
     * @param desiredTags the full target tag set, already resolved and scope-validated
     */
    private void reconcileRecipeTags(Recipe recipe, List<Tag> desiredTags) {
        Set<Long> desiredTagIds = desiredTags.stream().map(Tag::getId).collect(Collectors.toSet());
        recipe.getRecipeTags().removeIf(rt -> !desiredTagIds.contains(rt.getTag().getId()));

        Set<Long> existingTagIds = recipe.getRecipeTags().stream().map(rt -> rt.getTag().getId()).collect(Collectors.toSet());
        List<Tag> tagsToAdd = desiredTags.stream().filter(t -> !existingTagIds.contains(t.getId())).toList();
        recipe.getRecipeTags().addAll(toRecipeTags(tagsToAdd, recipe));
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

    /**
     * Resolves each input line's {@code ingredientPublicId} to an {@link Ingredient} entity (one
     * bulk lookup, order-preserving) and builds the corresponding {@link RecipeIngredient} rows
     * linked to {@code recipe}. Used by both {@link #createRecipe} and {@link #updateRecipe}
     * (full ingredient-list replace).
     *
     * @param inputs one line per ingredient (public ID, quantity, unit, note, sort order)
     * @param recipe the owning recipe, set as the back-reference on each {@link RecipeIngredient}
     * @return one new {@link RecipeIngredient} per input line, in the same order, not yet persisted
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException
     *         if any {@code ingredientPublicId} does not match an existing ingredient
     */
    private List<RecipeIngredient> toRecipeIngredients(List<RecipeIngredientInputDto> inputs, Recipe recipe) {
        List<Ingredient> resolvedIngredients = ingredientDomainBridgeService.getIngredientsByPublicIds(
                inputs.stream().map(RecipeIngredientInputDto::ingredientPublicId).toList()
        );

        List<RecipeIngredient> recipeIngredients = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            RecipeIngredient recipeIngredient = new RecipeIngredient();
            recipeIngredient.setRecipe(recipe);
            recipeIngredient.setIngredient(resolvedIngredients.get(i));
            applyRecipeIngredientFields(recipeIngredient, inputs.get(i));
            recipeIngredients.add(recipeIngredient);
        }
        return recipeIngredients;
    }

    /**
     * Reconciles {@code recipe.recipeIngredients} to exactly {@code inputs}: removes lines for
     * ingredients no longer present (triggers {@code orphanRemoval} deletion), updates the
     * quantity/unit/note/sortOrder of lines for ingredients that are kept, and adds only genuinely
     * new ingredient lines. Used instead of clear+re-add by {@link #updateRecipe} — the table has
     * a DB unique constraint on {@code (recipe_id, ingredient_id)}, and Hibernate flushes inserts
     * before deletes, so a fresh row for an ingredient kept unchanged would violate it before the
     * old row's deferred delete runs.
     *
     * @param recipe the recipe whose {@code recipeIngredients} collection is mutated in place
     * @param inputs the full target ingredient-line list
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException
     *         if any {@code ingredientPublicId} does not match an existing ingredient
     */
    private void reconcileRecipeIngredients(Recipe recipe, List<RecipeIngredientInputDto> inputs) {
        List<Ingredient> resolvedIngredients = ingredientDomainBridgeService.getIngredientsByPublicIds(
                inputs.stream().map(RecipeIngredientInputDto::ingredientPublicId).toList()
        );

        Map<Long, RecipeIngredient> existingByIngredientId = recipe.getRecipeIngredients().stream()
                .collect(Collectors.toMap(ri -> ri.getIngredient().getId(), ri -> ri));

        Set<Long> desiredIngredientIds = resolvedIngredients.stream().map(Ingredient::getId).collect(Collectors.toSet());
        recipe.getRecipeIngredients().removeIf(ri -> !desiredIngredientIds.contains(ri.getIngredient().getId()));

        for (int i = 0; i < inputs.size(); i++) {
            Ingredient ingredient = resolvedIngredients.get(i);
            RecipeIngredient existing = existingByIngredientId.get(ingredient.getId());
            if (existing != null) {
                applyRecipeIngredientFields(existing, inputs.get(i));
            } else {
                RecipeIngredient newLine = new RecipeIngredient();
                newLine.setRecipe(recipe);
                newLine.setIngredient(ingredient);
                applyRecipeIngredientFields(newLine, inputs.get(i));
                recipe.getRecipeIngredients().add(newLine);
            }
        }
    }

    private void applyRecipeIngredientFields(RecipeIngredient target, RecipeIngredientInputDto input) {
        target.setQuantity(input.quantity());
        target.setUnit(input.unit());
        target.setPreparationNote(input.preparationNote());
        target.setSortOrder(input.sortOrder());
    }

    /**
     * Builds a new (unsaved, un-linked-to-recipe) {@link RecipeTranslation} from an input DTO,
     * condensing its {@code instructions} steps via {@link SCDataStructureUtils#joinListIntoString(List, String)}. The caller is
     * responsible for setting {@code translation.setRecipe(...)} afterward.
     *
     * @param translationInputDto title/description/instruction-steps for one locale
     * @param lang                the already-resolved, already-active {@link Language} for this translation
     * @return the built {@link RecipeTranslation}, not yet linked to a recipe
     */
    private RecipeTranslation toRecipeTranslation(RecipeTranslationInputDto translationInputDto, Language lang) {
        RecipeTranslation translation = new RecipeTranslation();
        translation.setLanguage(lang);
        translation.setTitle(translationInputDto.title());
        translation.setDescription(translationInputDto.description());
        translation.setInstructions(SCDataStructureUtils.joinListIntoString(translationInputDto.instructions(), ". "));
        return translation;
    }
}
