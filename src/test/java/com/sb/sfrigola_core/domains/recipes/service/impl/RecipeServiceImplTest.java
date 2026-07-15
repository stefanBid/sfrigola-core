package com.sb.sfrigola_core.domains.recipes.service.impl;

import com.sb.sfrigola_core.common.enums.SCUserRole;
import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.common.models.context.SCAuthUser;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.domains.categories.entity.Category;
import com.sb.sfrigola_core.domains.categories.service.ICategoryDomainBridgeService;
import com.sb.sfrigola_core.domains.favorites.service.IFavoriteDomainBridgeService;
import com.sb.sfrigola_core.domains.ingredients.service.IIngredientDomainBridgeService;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import com.sb.sfrigola_core.domains.recipes.dto.input.AddRecipeDto;
import com.sb.sfrigola_core.domains.recipes.dto.input.RecipeTranslationInputDto;
import com.sb.sfrigola_core.domains.recipes.dto.input.UpdateRecipeDto;
import com.sb.sfrigola_core.domains.recipes.entity.Recipe;
import com.sb.sfrigola_core.domains.recipes.entity.RecipeTranslation;
import com.sb.sfrigola_core.domains.recipes.enums.DifficultyLevel;
import com.sb.sfrigola_core.domains.recipes.enums.FeedType;
import com.sb.sfrigola_core.domains.recipes.enums.RecipeSortField;
import com.sb.sfrigola_core.domains.recipes.enums.SeasonType;
import com.sb.sfrigola_core.domains.recipes.exception.ContributorTranslationLimitExceededException;
import com.sb.sfrigola_core.domains.recipes.exception.DuplicateRecipeLocaleException;
import com.sb.sfrigola_core.domains.recipes.exception.MissingRecipeLocalesException;
import com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException;
import com.sb.sfrigola_core.domains.recipes.exception.RecipeAuthorMismatchException;
import com.sb.sfrigola_core.domains.recipes.models.RecipeSpecificFilter;
import com.sb.sfrigola_core.domains.recipes.repository.IRecipeRepository;
import com.sb.sfrigola_core.domains.stats.entity.RecipeStats;
import com.sb.sfrigola_core.domains.stats.service.IRecipeStatsDomainBridgeService;
import com.sb.sfrigola_core.domains.tags.service.ITagDomainBridgeService;
import com.sb.sfrigola_core.domains.users.entity.SCUser;
import com.sb.sfrigola_core.domains.users.service.ISCUserDomainBridgeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceImplTest {

    @Mock
    private IRecipeRepository recipeRepository;
    @Mock
    private ILanguageDomainBridgeService languageDomainBridgeService;
    @Mock
    private ITagDomainBridgeService tagDomainBridgeService;
    @Mock
    private IIngredientDomainBridgeService ingredientDomainBridgeService;
    @Mock
    private ICategoryDomainBridgeService categoryDomainBridgeService;
    @Mock
    private ISCUserDomainBridgeService userDomainBridgeService;
    @Mock
    private IFavoriteDomainBridgeService favoriteDomainBridgeService;
    @Mock
    private IRecipeStatsDomainBridgeService recipeStatsDomainBridgeService;

    private RecipeServiceImpl recipeService;

    private Language english;
    private Language italian;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeServiceImpl(
                recipeRepository,
                languageDomainBridgeService,
                tagDomainBridgeService,
                ingredientDomainBridgeService,
                categoryDomainBridgeService,
                userDomainBridgeService,
                favoriteDomainBridgeService,
                recipeStatsDomainBridgeService
        );

        english = new Language();
        english.setCode("en");
        english.setName("English");

        italian = new Language();
        italian.setCode("it");
        italian.setName("Italian");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SCAuthUser setAuth(SCUserRole role, UUID publicId) {
        var authUser = new SCAuthUser(publicId, role, "john", "john@example.com", null, "en", true, "John", "Doe");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(authUser, null, List.of()));
        return authUser;
    }

    private Recipe buildRecipe(UUID authorPublicId, boolean isPublished) {
        var author = new SCUser();
        author.setPublicId(authorPublicId);

        var recipe = new Recipe();
        recipe.setId(1L);
        recipe.setAuthor(author);
        recipe.setPublished(isPublished);
        recipe.setDifficulty(DifficultyLevel.EASY);
        recipe.setSeason(SeasonType.ALL_YEAR);
        recipe.setTranslations(new ArrayList<>());
        return recipe;
    }

    private RecipeTranslation buildTranslation(Recipe recipe, Language lang, String title) {
        var translation = new RecipeTranslation();
        translation.setRecipe(recipe);
        translation.setLanguage(lang);
        translation.setTitle(title);
        translation.setDescription("desc");
        translation.setInstructions("step1. step2");
        return translation;
    }

    private AddRecipeDto addRecipeDtoWithTranslations(List<RecipeTranslationInputDto> translations) {
        return new AddRecipeDto(
                null, DifficultyLevel.EASY, null, SeasonType.ALL_YEAR,
                10, 20, (short) 2, false, false, false,
                null, null, translations
        );
    }

    private UpdateRecipeDto updateRecipeDtoWithTranslation(RecipeTranslationInputDto translation, DifficultyLevel difficulty) {
        return new UpdateRecipeDto(
                null, difficulty, null, SeasonType.ALL_YEAR,
                10, 20, (short) 2, false, false, false,
                null, null, translation
        );
    }

    // =========================================================
    // createRecipe
    // =========================================================

    @Test
    void createRecipe_contributor_singleTranslation_createsDraft() {
        var authorPublicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_CONTRIBUTOR, authorPublicId);

        var author = new SCUser();
        author.setPublicId(authorPublicId);
        when(userDomainBridgeService.getUserEntityByPublicIdOrThrow(authorPublicId)).thenReturn(author);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english, "it", italian));
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(anyMap(), eq("en"))).thenReturn(english);

        var dto = addRecipeDtoWithTranslations(List.of(new RecipeTranslationInputDto("en", "Carbonara", "desc", List.of("step1"))));

        var result = recipeService.createRecipe(dto, "en");

        assertThat(result.isPublished()).isFalse();
        assertThat(result.titlePreview()).isEqualTo("Carbonara");

        var captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).save(captor.capture());
        assertThat(captor.getValue().isPublished()).isFalse();
        assertThat(captor.getValue().getTranslations()).hasSize(1);
        assertThat(captor.getValue().getAuthor()).isSameAs(author);
    }

    @Test
    void createRecipe_contributor_multipleTranslations_throwsContributorTranslationLimitExceeded() {
        var authorPublicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_CONTRIBUTOR, authorPublicId);

        when(userDomainBridgeService.getUserEntityByPublicIdOrThrow(authorPublicId)).thenReturn(new SCUser());
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english, "it", italian));

        var dto = addRecipeDtoWithTranslations(List.of(
                new RecipeTranslationInputDto("en", "Carbonara", "desc", List.of("step1")),
                new RecipeTranslationInputDto("it", "Carbonara IT", "desc", List.of("step1"))
        ));

        assertThatThrownBy(() -> recipeService.createRecipe(dto, "en"))
                .isInstanceOf(ContributorTranslationLimitExceededException.class);

        verify(recipeRepository, never()).save(any());
        verifyNoInteractions(tagDomainBridgeService, ingredientDomainBridgeService, categoryDomainBridgeService);
    }

    @Test
    void createRecipe_admin_allActiveLocalesCovered_createsPublished() {
        var authorPublicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_ADMIN, authorPublicId);

        var author = new SCUser();
        author.setPublicId(authorPublicId);
        when(userDomainBridgeService.getUserEntityByPublicIdOrThrow(authorPublicId)).thenReturn(author);
        var activeMap = Map.of("en", english, "it", italian);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(activeMap);
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeMap, "en")).thenReturn(english);
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeMap, "it")).thenReturn(italian);

        var dto = addRecipeDtoWithTranslations(List.of(
                new RecipeTranslationInputDto("en", "Carbonara", "desc", List.of("step1")),
                new RecipeTranslationInputDto("it", "Carbonara IT", "desc", List.of("step1"))
        ));

        var result = recipeService.createRecipe(dto, "en");

        assertThat(result.isPublished()).isTrue();

        var captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).save(captor.capture());
        assertThat(captor.getValue().isPublished()).isTrue();
        assertThat(captor.getValue().getTranslations()).hasSize(2);
    }

    @Test
    void createRecipe_admin_duplicateLocale_throwsDuplicateRecipeLocaleException() {
        var authorPublicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_ADMIN, authorPublicId);

        when(userDomainBridgeService.getUserEntityByPublicIdOrThrow(authorPublicId)).thenReturn(new SCUser());
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english, "it", italian));

        var dto = addRecipeDtoWithTranslations(List.of(
                new RecipeTranslationInputDto("en", "Carbonara", "desc", List.of("step1")),
                new RecipeTranslationInputDto("en", "Carbonara duplicate", "desc", List.of("step1"))
        ));

        assertThatThrownBy(() -> recipeService.createRecipe(dto, "en"))
                .isInstanceOf(DuplicateRecipeLocaleException.class);

        verify(recipeRepository, never()).save(any());
    }

    @Test
    void createRecipe_admin_missingActiveLocale_throwsMissingRecipeLocalesException() {
        var authorPublicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_ADMIN, authorPublicId);

        when(userDomainBridgeService.getUserEntityByPublicIdOrThrow(authorPublicId)).thenReturn(new SCUser());
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english, "it", italian));

        var dto = addRecipeDtoWithTranslations(List.of(
                new RecipeTranslationInputDto("en", "Carbonara", "desc", List.of("step1"))
        ));

        assertThatThrownBy(() -> recipeService.createRecipe(dto, "en"))
                .isInstanceOf(MissingRecipeLocalesException.class);

        verify(recipeRepository, never()).save(any());
    }

    @Test
    void createRecipe_resolvesCategoryFromBridgeWhenProvided() {
        var authorPublicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_CONTRIBUTOR, authorPublicId);

        var author = new SCUser();
        author.setPublicId(authorPublicId);
        when(userDomainBridgeService.getUserEntityByPublicIdOrThrow(authorPublicId)).thenReturn(author);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english));
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(anyMap(), eq("en"))).thenReturn(english);

        var categoryPublicId = UUID.randomUUID();
        var category = new Category();
        when(categoryDomainBridgeService.getCategoryEntityByPublicIdOrThrow(categoryPublicId)).thenReturn(category);

        var dto = new AddRecipeDto(
                categoryPublicId, DifficultyLevel.EASY, null, SeasonType.ALL_YEAR,
                10, 20, (short) 2, false, false, false,
                null, null, List.of(new RecipeTranslationInputDto("en", "Carbonara", "desc", List.of("step1")))
        );

        recipeService.createRecipe(dto, "en");

        var captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isSameAs(category);
    }

    // =========================================================
    // updateRecipe
    // =========================================================

    @Test
    void updateRecipe_contributorAuthor_revertsToUnpublished() {
        var authorPublicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_CONTRIBUTOR, authorPublicId);

        var recipe = buildRecipe(authorPublicId, true); // was published
        var publicId = recipe.getPublicId();
        var existingTranslation = buildTranslation(recipe, english, "Old title");
        recipe.getTranslations().add(existingTranslation);

        when(recipeRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(recipe));
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english));
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(anyMap(), eq("en"))).thenReturn(english);
        when(recipeStatsDomainBridgeService.getStats(recipe.getId())).thenReturn(Optional.empty());

        var dto = updateRecipeDtoWithTranslation(new RecipeTranslationInputDto("en", "New title", "desc", List.of("step1")), DifficultyLevel.EASY);

        var result = recipeService.updateRecipe(publicId, dto);

        assertThat(recipe.isPublished()).isFalse();
        assertThat(result.isPublished()).isFalse();
    }

    @Test
    void updateRecipe_admin_doesNotTouchIsPublished() {
        var authorPublicId = UUID.randomUUID();
        var adminPublicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_ADMIN, adminPublicId);

        var recipe = buildRecipe(authorPublicId, true); // was published, admin editing someone else's recipe
        var publicId = recipe.getPublicId();
        var existingTranslation = buildTranslation(recipe, english, "Old title");
        recipe.getTranslations().add(existingTranslation);

        when(recipeRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(recipe));
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english));
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(anyMap(), eq("en"))).thenReturn(english);
        when(recipeStatsDomainBridgeService.getStats(recipe.getId())).thenReturn(Optional.empty());

        var dto = updateRecipeDtoWithTranslation(new RecipeTranslationInputDto("en", "New title", "desc", List.of("step1")), DifficultyLevel.EASY);

        var result = recipeService.updateRecipe(publicId, dto);

        assertThat(recipe.isPublished()).isTrue();
        assertThat(result.isPublished()).isTrue();
    }

    @Test
    void updateRecipe_admin_draftRecipeStaysUnpublished() {
        var authorPublicId = UUID.randomUUID();
        var adminPublicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_ADMIN, adminPublicId);

        var recipe = buildRecipe(authorPublicId, false); // still draft
        var publicId = recipe.getPublicId();
        var existingTranslation = buildTranslation(recipe, english, "Old title");
        recipe.getTranslations().add(existingTranslation);

        when(recipeRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(recipe));
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english, "it", italian));
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(anyMap(), eq("it"))).thenReturn(italian);
        when(recipeStatsDomainBridgeService.getStats(recipe.getId())).thenReturn(Optional.empty());

        // admin adds the missing "it" translation via update; recipe stays draft (only publish endpoint can flip it)
        var dto = updateRecipeDtoWithTranslation(new RecipeTranslationInputDto("it", "Titolo IT", "desc", List.of("passo1")), DifficultyLevel.EASY);

        var result = recipeService.updateRecipe(publicId, dto);

        assertThat(recipe.isPublished()).isFalse();
        assertThat(result.isPublished()).isFalse();
        assertThat(recipe.getTranslations()).hasSize(2);
    }

    @Test
    void updateRecipe_nonAuthorNonAdmin_throwsRecipeAuthorMismatchException() {
        var authorPublicId = UUID.randomUUID();
        var otherUserPublicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_CONTRIBUTOR, otherUserPublicId);

        var recipe = buildRecipe(authorPublicId, true);
        var publicId = recipe.getPublicId();

        when(recipeRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(recipe));

        var dto = updateRecipeDtoWithTranslation(new RecipeTranslationInputDto("en", "New title", "desc", List.of("step1")), DifficultyLevel.EASY);

        assertThatThrownBy(() -> recipeService.updateRecipe(publicId, dto))
                .isInstanceOf(RecipeAuthorMismatchException.class);

        verifyNoInteractions(languageDomainBridgeService, tagDomainBridgeService, ingredientDomainBridgeService);
    }

    @Test
    void updateRecipe_notFound_throwsNoRecipeFoundException() {
        var publicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_ADMIN, UUID.randomUUID());
        when(recipeRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.empty());

        var dto = updateRecipeDtoWithTranslation(new RecipeTranslationInputDto("en", "Title", "desc", List.of("step1")), DifficultyLevel.EASY);

        assertThatThrownBy(() -> recipeService.updateRecipe(publicId, dto))
                .isInstanceOf(NoRecipeFoundException.class);
    }

    @Test
    void updateRecipe_existingTranslation_updatesFieldsInPlace() {
        var authorPublicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_ADMIN, authorPublicId);

        var recipe = buildRecipe(authorPublicId, true);
        var publicId = recipe.getPublicId();
        var existingTranslation = buildTranslation(recipe, english, "Old title");
        recipe.getTranslations().add(existingTranslation);

        when(recipeRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(recipe));
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english));
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(anyMap(), eq("en"))).thenReturn(english);
        when(recipeStatsDomainBridgeService.getStats(recipe.getId())).thenReturn(Optional.empty());

        var dto = updateRecipeDtoWithTranslation(new RecipeTranslationInputDto("en", "Brand new title", "new desc", List.of("new step")), DifficultyLevel.HARD);

        var result = recipeService.updateRecipe(publicId, dto);

        assertThat(recipe.getTranslations()).hasSize(1);
        assertThat(existingTranslation.getTitle()).isEqualTo("Brand new title");
        assertThat(existingTranslation.getDescription()).isEqualTo("new desc");
        assertThat(recipe.getDifficulty()).isEqualTo(DifficultyLevel.HARD);
        assertThat(result.titlePreview()).isEqualTo("Brand new title");
    }

    // =========================================================
    // publishRecipe / unpublishRecipe
    // =========================================================

    @Test
    void publishRecipe_setsPublishedTrue() {
        var recipe = buildRecipe(UUID.randomUUID(), false);
        var publicId = recipe.getPublicId();
        recipe.getTranslations().add(buildTranslation(recipe, english, "Carbonara"));

        when(recipeRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(recipe));
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(Map.of("en", "English"));
        when(recipeStatsDomainBridgeService.getStats(recipe.getId())).thenReturn(Optional.empty());

        var result = recipeService.publishRecipe(publicId, "en");

        assertThat(recipe.isPublished()).isTrue();
        assertThat(result.isPublished()).isTrue();
        verify(languageDomainBridgeService).validateLocaleIsActiveOrThrow("en");
    }

    @Test
    void unpublishRecipe_setsPublishedFalse() {
        var recipe = buildRecipe(UUID.randomUUID(), true);
        var publicId = recipe.getPublicId();
        recipe.getTranslations().add(buildTranslation(recipe, english, "Carbonara"));

        when(recipeRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(recipe));
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(Map.of("en", "English"));
        when(recipeStatsDomainBridgeService.getStats(recipe.getId())).thenReturn(Optional.empty());

        var result = recipeService.unpublishRecipe(publicId, "en");

        assertThat(recipe.isPublished()).isFalse();
        assertThat(result.isPublished()).isFalse();
    }

    @Test
    void publishRecipe_notFound_throwsNoRecipeFoundException() {
        var publicId = UUID.randomUUID();
        when(recipeRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.publishRecipe(publicId, "en"))
                .isInstanceOf(NoRecipeFoundException.class);
    }

    // =========================================================
    // deleteRecipe
    // =========================================================

    @Test
    void deleteRecipe_asAuthor_deletesAndReturnsPublicId() {
        var authorPublicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_CONTRIBUTOR, authorPublicId);

        var recipe = buildRecipe(authorPublicId, true);
        var publicId = recipe.getPublicId();
        when(recipeRepository.findByPublicId(publicId)).thenReturn(Optional.of(recipe));

        var result = recipeService.deleteRecipe(publicId);

        assertThat(result).isEqualTo(publicId);
        verify(recipeRepository).delete(recipe);
    }

    @Test
    void deleteRecipe_asAdmin_deletesEvenIfNotAuthor() {
        var authorPublicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_ADMIN, UUID.randomUUID());

        var recipe = buildRecipe(authorPublicId, true);
        var publicId = recipe.getPublicId();
        when(recipeRepository.findByPublicId(publicId)).thenReturn(Optional.of(recipe));

        var result = recipeService.deleteRecipe(publicId);

        assertThat(result).isEqualTo(publicId);
        verify(recipeRepository).delete(recipe);
    }

    @Test
    void deleteRecipe_nonAuthorNonAdmin_throwsRecipeAuthorMismatchException() {
        var authorPublicId = UUID.randomUUID();
        setAuth(SCUserRole.ROLE_CONTRIBUTOR, UUID.randomUUID());

        var recipe = buildRecipe(authorPublicId, true);
        var publicId = recipe.getPublicId();
        when(recipeRepository.findByPublicId(publicId)).thenReturn(Optional.of(recipe));

        assertThatThrownBy(() -> recipeService.deleteRecipe(publicId))
                .isInstanceOf(RecipeAuthorMismatchException.class);

        verify(recipeRepository, never()).delete(any());
    }

    @Test
    void deleteRecipe_notFound_throwsNoRecipeFoundException() {
        var publicId = UUID.randomUUID();
        when(recipeRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.deleteRecipe(publicId))
                .isInstanceOf(NoRecipeFoundException.class);

        verify(recipeRepository, never()).delete(any());
    }

    // =========================================================
    // getByPublicId (public)
    // =========================================================

    @Test
    void getByPublicId_publishedRecipe_returnsDetails() {
        setAuth(SCUserRole.ROLE_USER, UUID.randomUUID());
        var authUser = com.sb.sfrigola_core.common.util.SCAuthenticationUtils.getAuthUserByContextHolder();

        var recipe = buildRecipe(UUID.randomUUID(), true);
        var publicId = recipe.getPublicId();
        recipe.getTranslations().add(buildTranslation(recipe, english, "Carbonara"));

        when(recipeRepository.findByPublicId(publicId)).thenReturn(Optional.of(recipe));
        var loggedUser = new SCUser();
        loggedUser.setId(99L);
        when(userDomainBridgeService.getUserEntityByPublicIdOrThrow(authUser.publicId())).thenReturn(loggedUser);
        when(favoriteDomainBridgeService.isFavoritedByUser(99L, recipe.getId())).thenReturn(true);
        var stats = new RecipeStats();
        stats.setAvgRating(BigDecimal.valueOf(4.2));
        stats.setRatingsCount(3);
        stats.setFavoritesCount(5);
        when(recipeStatsDomainBridgeService.getStats(recipe.getId())).thenReturn(Optional.of(stats));

        var result = recipeService.getByPublicId(publicId, "en");

        assertThat(result.specificTranslationTitle()).isEqualTo("Carbonara");
        assertThat(result.isFavourite()).isTrue();
        assertThat(result.avgRating()).isEqualByComparingTo(BigDecimal.valueOf(4.2));
        assertThat(result.ratingsCount()).isEqualTo(3);
    }

    @Test
    void getByPublicId_draftRecipe_throwsNoRecipeFoundExceptionLikeNonExistent() {
        var recipe = buildRecipe(UUID.randomUUID(), false); // draft
        var publicId = recipe.getPublicId();
        when(recipeRepository.findByPublicId(publicId)).thenReturn(Optional.of(recipe));

        assertThatThrownBy(() -> recipeService.getByPublicId(publicId, "en"))
                .isInstanceOf(NoRecipeFoundException.class);
    }

    @Test
    void getByPublicId_notFound_throwsNoRecipeFoundException() {
        var publicId = UUID.randomUUID();
        when(recipeRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.getByPublicId(publicId, "en"))
                .isInstanceOf(NoRecipeFoundException.class);
    }

    // =========================================================
    // getByPublicIdAdmin
    // =========================================================

    @Test
    void getByPublicIdAdmin_returnsDraftRecipeDetails() {
        var recipe = buildRecipe(UUID.randomUUID(), false); // draft included for admin
        var publicId = recipe.getPublicId();
        recipe.getTranslations().add(buildTranslation(recipe, english, "Carbonara"));

        when(recipeRepository.findByPublicId(publicId)).thenReturn(Optional.of(recipe));
        when(recipeStatsDomainBridgeService.getStats(recipe.getId())).thenReturn(Optional.empty());

        var result = recipeService.getByPublicIdAdmin(publicId, "en");

        assertThat(result.specificTranslationTitle()).isEqualTo("Carbonara");
        assertThat(result.isPublished()).isFalse();
    }

    @Test
    void getByPublicIdAdmin_notFound_throwsNoRecipeFoundException() {
        var publicId = UUID.randomUUID();
        when(recipeRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.getByPublicIdAdmin(publicId, "en"))
                .isInstanceOf(NoRecipeFoundException.class);
    }

    // =========================================================
    // getAllAdmin
    // =========================================================

    @Test
    void getAllAdmin_defaultTitleSortAscending_callsAscQuery() {
        var pageable = PageRequest.of(0, 10);
        var idsPage = new PageImpl<>(List.of(1L), pageable, 1);
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(Map.of("en", "English"));
        when(recipeRepository.findIdsByFiltersAndLocaleAsc(eq("en"), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(idsPage);

        var recipe = buildRecipe(UUID.randomUUID(), true);
        recipe.setId(1L);
        recipe.getTranslations().add(buildTranslation(recipe, english, "Carbonara"));
        when(recipeRepository.findByIdsWithAllTranslations(List.of(1L))).thenReturn(List.of(recipe));
        when(recipeStatsDomainBridgeService.getStatsBatch(List.of(1L))).thenReturn(Map.of());

        var filterQuery = SCFilterQuery.<RecipeSpecificFilter>powerful(null, null, SortDirection.ASC, 10, 0, null);
        var result = recipeService.getAllAdmin(filterQuery, null, "en");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().titlePreview()).isEqualTo("Carbonara");
        verify(recipeRepository, never()).findIdsByFiltersAndLocaleDesc(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(recipeRepository, never()).findIdsByFiltersAndLocaleOtherSort(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getAllAdmin_titleSortDescending_callsDescQuery() {
        var pageable = PageRequest.of(0, 10);
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(Map.of("en", "English"));
        when(recipeRepository.findIdsByFiltersAndLocaleDesc(eq("en"), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty(pageable));

        var filterQuery = SCFilterQuery.<RecipeSpecificFilter>powerful(null, null, SortDirection.DESC, 10, 0, null);
        var result = recipeService.getAllAdmin(filterQuery, null, "en");

        assertThat(result.content()).isEmpty();
        verify(recipeRepository, never()).findIdsByFiltersAndLocaleAsc(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getAllAdmin_nonTitleSortField_callsOtherSortQueryRegardlessOfDirection() {
        var pageable = PageRequest.of(0, 10);
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(Map.of("en", "English"));
        when(recipeRepository.findIdsByFiltersAndLocaleOtherSort(eq("en"), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty(pageable));

        var filterQuery = SCFilterQuery.<RecipeSpecificFilter>powerful(null, RecipeSortField.DIFFICULTY, SortDirection.ASC, 10, 0, null);
        var result = recipeService.getAllAdmin(filterQuery, null, "en");

        assertThat(result.content()).isEmpty();
        verify(recipeRepository, never()).findIdsByFiltersAndLocaleAsc(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(recipeRepository, never()).findIdsByFiltersAndLocaleDesc(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // =========================================================
    // searchRecipes
    // =========================================================

    @Test
    void searchRecipes_returnsMappedResults() {
        setAuth(SCUserRole.ROLE_USER, UUID.randomUUID());
        var authUser = com.sb.sfrigola_core.common.util.SCAuthenticationUtils.getAuthUserByContextHolder();

        var pageable = PageRequest.of(0, 10);
        var idsPage = new PageImpl<>(List.of(1L), pageable, 1);
        when(recipeRepository.findIdsBySearchKeyAndLocale(eq("en"), any(), any(), any())).thenReturn(idsPage);

        var recipe = buildRecipe(UUID.randomUUID(), true);
        recipe.setId(1L);
        recipe.getTranslations().add(buildTranslation(recipe, english, "Carbonara"));
        when(recipeRepository.findByIdsWithSpecificTranslation(List.of(1L), "en")).thenReturn(List.of(recipe));

        var loggedUser = new SCUser();
        loggedUser.setId(42L);
        when(userDomainBridgeService.getUserEntityByPublicIdOrThrow(authUser.publicId())).thenReturn(loggedUser);
        when(favoriteDomainBridgeService.getFavoritedRecipeIds(42L, List.of(1L))).thenReturn(java.util.Set.of(1L));
        when(recipeStatsDomainBridgeService.getStatsBatch(List.of(1L))).thenReturn(Map.of());

        var filterQuery = SCFilterQuery.<Void>pageWithSearch("carbo", 10, 0);
        var result = recipeService.searchRecipes(filterQuery, null, "en");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().isFavourite()).isTrue();
        verify(languageDomainBridgeService).validateLocaleIsActiveOrThrow("en");
    }

    @Test
    void searchRecipes_noMatches_returnsEmptyResult() {
        var pageable = PageRequest.of(0, 10);
        when(recipeRepository.findIdsBySearchKeyAndLocale(eq("en"), any(), any(), any())).thenReturn(Page.empty(pageable));

        var filterQuery = SCFilterQuery.<Void>pageWithSearch("nomatch", 10, 0);
        var result = recipeService.searchRecipes(filterQuery, null, "en");

        assertThat(result.content()).isEmpty();
        verify(recipeRepository, never()).findByIdsWithSpecificTranslation(any(), any());
    }

    // =========================================================
    // getAllMyFavoriteRecipes
    // =========================================================

    @Test
    void getAllMyFavoriteRecipes_forcesIsFavouriteTrueForEveryRecipe() {
        var authUser = setAuth(SCUserRole.ROLE_USER, UUID.randomUUID());

        var pageable = PageRequest.of(0, 10);
        var idsPage = new PageImpl<>(List.of(1L), pageable, 1);
        when(favoriteDomainBridgeService.getFavoritedRecipeIdsPage(eq(authUser.publicId()), any())).thenReturn(idsPage);

        var recipe = buildRecipe(UUID.randomUUID(), true);
        recipe.setId(1L);
        recipe.getTranslations().add(buildTranslation(recipe, english, "Carbonara"));
        when(recipeRepository.findByIdsWithSpecificTranslation(List.of(1L), "en")).thenReturn(List.of(recipe));
        when(recipeStatsDomainBridgeService.getStatsBatch(List.of(1L))).thenReturn(Map.of());

        var filterQuery = SCFilterQuery.<Void>pagedOnly(10, 0);
        var result = recipeService.getAllMyFavoriteRecipes(filterQuery, "en");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().isFavourite()).isTrue();
        // decorateRecipesWithoutFavourites never re-checks favorites — every recipe already came from the favorites bridge
        verify(favoriteDomainBridgeService, never()).getFavoritedRecipeIds(any(), any());
        verify(favoriteDomainBridgeService, never()).isFavoritedByUser(any(), any());
    }

    // =========================================================
    // getAllByFeed
    // =========================================================

    @Test
    void getAllByFeed_quick_delegatesToOtherSortQueryWithPublishedOnly() {
        var pageable = PageRequest.of(0, 10);
        when(recipeRepository.findIdsByFiltersAndLocaleOtherSort(eq("en"), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty(pageable));

        var filterQuery = SCFilterQuery.<Void>pagedOnly(10, 0);
        var result = recipeService.getAllByFeed(FeedType.QUICK, filterQuery, "en");

        assertThat(result.content()).isEmpty();
        verify(languageDomainBridgeService).validateLocaleIsActiveOrThrow("en");
    }

    @Test
    void getAllByFeed_viral_usesStatsBridgeOrderedByFavorites() {
        var pageable = PageRequest.of(0, 10);
        var idsPage = new PageImpl<>(List.of(1L), pageable, 1);
        when(recipeStatsDomainBridgeService.getPublishedRecipeIdsOrderByFavoritesDesc(any())).thenReturn(idsPage);

        var recipe = buildRecipe(UUID.randomUUID(), true);
        recipe.setId(1L);
        recipe.getTranslations().add(buildTranslation(recipe, english, "Carbonara"));
        when(recipeRepository.findByIdsWithSpecificTranslation(List.of(1L), "en")).thenReturn(List.of(recipe));
        when(recipeStatsDomainBridgeService.getStatsBatch(List.of(1L))).thenReturn(Map.of());

        var filterQuery = SCFilterQuery.<Void>pagedOnly(10, 0);
        var result = recipeService.getAllByFeed(FeedType.VIRAL, filterQuery, "en");

        assertThat(result.content()).hasSize(1);
        verify(recipeRepository, never()).findIdsByFiltersAndLocaleOtherSort(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // =========================================================
    // getAllHomeFeed
    // =========================================================

    @Test
    void getAllHomeFeed_returnsAllFourFeedTypeRows() {
        var categoryId = UUID.randomUUID();
        var category = new Category();
        category.setId(5L);
        when(categoryDomainBridgeService.getCategoryEntityByPublicIdOrThrow(categoryId)).thenReturn(category);
        when(categoryDomainBridgeService.getChildrenCategories(category)).thenReturn(List.of());

        when(recipeStatsDomainBridgeService.getPreviewByStats(any())).thenReturn(Map.of());
        when(recipeRepository.findIdsByFiltersAndLocaleOtherSort(eq("en"), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));

        var homeFeed = recipeService.getAllHomeFeed(categoryId, "en");

        assertThat(homeFeed).containsOnlyKeys(FeedType.QUICK, FeedType.LIKE_A_CHEF, FeedType.ECONOMICAL, FeedType.VIRAL);
        assertThat(homeFeed.get(FeedType.QUICK).recipes()).isEmpty();
        assertThat(homeFeed.get(FeedType.VIRAL).recipes()).isEmpty();
        verify(languageDomainBridgeService).validateLocaleIsActiveOrThrow("en");
    }
}
