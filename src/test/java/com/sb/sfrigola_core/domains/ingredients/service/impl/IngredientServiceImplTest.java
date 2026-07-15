package com.sb.sfrigola_core.domains.ingredients.service.impl;

import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.domains.ingredients.dto.input.AddIngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.input.IngredientTranslationInputDto;
import com.sb.sfrigola_core.domains.ingredients.dto.input.UpdateIngredientDto;
import com.sb.sfrigola_core.domains.ingredients.entity.Ingredient;
import com.sb.sfrigola_core.domains.ingredients.entity.IngredientTag;
import com.sb.sfrigola_core.domains.ingredients.entity.IngredientTranslation;
import com.sb.sfrigola_core.domains.ingredients.enums.IngredientFoodGroup;
import com.sb.sfrigola_core.domains.ingredients.enums.IngredientSortField;
import com.sb.sfrigola_core.domains.ingredients.exception.IngredientSlugAlreadyExistsException;
import com.sb.sfrigola_core.domains.ingredients.exception.DuplicateIngredientLocaleException;
import com.sb.sfrigola_core.domains.ingredients.exception.MissingIngredientLocalesException;
import com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException;
import com.sb.sfrigola_core.domains.languages.exception.LocaleNotActiveException;
import com.sb.sfrigola_core.domains.ingredients.models.IngredientSpecificFilter;
import com.sb.sfrigola_core.domains.ingredients.repository.IIngredientRepository;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import com.sb.sfrigola_core.domains.tags.entity.Tag;
import com.sb.sfrigola_core.domains.tags.entity.TagTranslation;
import com.sb.sfrigola_core.domains.tags.service.ITagDomainBridgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientServiceImplTest {

    @Mock
    private IIngredientRepository ingredientRepository;
    @Mock
    private ILanguageDomainBridgeService languageDomainBridgeService;
    @Mock
    private ITagDomainBridgeService tagDomainBridgeService;

    private IngredientServiceImpl ingredientService;

    private Language english;
    private Language italian;

    @BeforeEach
    void setUp() {
        ingredientService = new IngredientServiceImpl(ingredientRepository, languageDomainBridgeService, tagDomainBridgeService);

        english = new Language();
        english.setCode("en");
        english.setName("English");

        italian = new Language();
        italian.setCode("it");
        italian.setName("Italian");
    }

    // =========================================================
    // getAll
    // =========================================================

    @Test
    void getAll_returnsMappedResultsWhenIngredientsExist() {
        var filterQuery = SCFilterQuery.<Void>pageWithSearch(null, 10, 0);
        var pageable = PageRequest.of(0, 10);
        var idsPage = new PageImpl<>(List.of(1L), pageable, 1);
        when(ingredientRepository.findIdsByFiltersAndLocaleAsc("en", null, null, null, null, null, null, null, pageable))
                .thenReturn(idsPage);

        var ingredient = new Ingredient();
        ingredient.setId(1L);
        ingredient.setSlug("tomato");
        ingredient.setFoodGroup(IngredientFoodGroup.VEGETABLE);
        ingredient.setCaloriesPer100g(new BigDecimal("18.00"));
        ingredient.setAllergens(new String[]{});
        var translation = new IngredientTranslation();
        translation.setLanguage(english);
        translation.setName("Tomato");
        ingredient.setTranslations(new ArrayList<>(List.of(translation)));
        when(ingredientRepository.findByIdsWithSpecificTranslation(List.of(1L), "en")).thenReturn(List.of(ingredient));

        var result = ingredientService.getAll(filterQuery, "en");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().slug()).isEqualTo("tomato");
        assertThat(result.content().getFirst().name()).isEqualTo("Tomato");
        verify(languageDomainBridgeService).validateLocaleIsActiveOrThrow("en");
    }

    @Test
    void getAll_returnsEmptyResultWhenNoIngredientsMatch() {
        var filterQuery = SCFilterQuery.<Void>pageWithSearch(null, 10, 0);
        var pageable = PageRequest.of(0, 10);
        when(ingredientRepository.findIdsByFiltersAndLocaleAsc("en", null, null, null, null, null, null, null, pageable))
                .thenReturn(Page.empty(pageable));

        var result = ingredientService.getAll(filterQuery, "en");

        assertThat(result.content()).isEmpty();
        verify(ingredientRepository, never()).findByIdsWithSpecificTranslation(any(), any());
    }

    // =========================================================
    // getAllAdmin
    // =========================================================

    @Test
    void getAllAdmin_ascendingSortByName_returnsMappedResults() {
        var filterQuery = SCFilterQuery.powerful(null, null, SortDirection.ASC, 10, 0, (IngredientSpecificFilter) null);
        var pageable = PageRequest.of(0, 10);
        var activeLanguagesMap = Map.of("en", "English", "it", "Italian");
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(activeLanguagesMap);

        var idsPage = new PageImpl<>(List.of(1L), pageable, 1);
        when(ingredientRepository.findIdsByFiltersAndLocaleAsc("en", null, null, null, null, null, null, null, pageable))
                .thenReturn(idsPage);

        var ingredient = new Ingredient();
        ingredient.setId(1L);
        ingredient.setSlug("tomato");
        var translation = new IngredientTranslation();
        translation.setLanguage(english);
        translation.setName("Tomato");
        ingredient.setTranslations(new ArrayList<>(List.of(translation)));
        when(ingredientRepository.findByIdsWithAllTranslations(List.of(1L))).thenReturn(List.of(ingredient));
        when(languageDomainBridgeService.buildTranslatedLanguagesMap(anyList(), eq(activeLanguagesMap))).thenReturn(activeLanguagesMap);

        var result = ingredientService.getAllAdmin(filterQuery, "en");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().namePreview()).isEqualTo("Tomato");
        verify(ingredientRepository, never()).findIdsByFiltersAndLocaleDesc(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(ingredientRepository, never()).findIdsByFiltersAndLocaleOtherSort(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getAllAdmin_descendingSortByName_callsDescendingQuery() {
        var filterQuery = SCFilterQuery.powerful(null, null, SortDirection.DESC, 10, 0, (IngredientSpecificFilter) null);
        var pageable = PageRequest.of(0, 10);
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(Map.of("en", "English"));
        when(ingredientRepository.findIdsByFiltersAndLocaleDesc("en", null, null, null, null, null, null, null, pageable))
                .thenReturn(Page.empty(pageable));

        var result = ingredientService.getAllAdmin(filterQuery, "en");

        assertThat(result.content()).isEmpty();
        verify(ingredientRepository, never()).findIdsByFiltersAndLocaleAsc(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getAllAdmin_otherSortField_callsOtherSortQueryWithNativeSort() {
        var filterQuery = SCFilterQuery.powerful(null, IngredientSortField.SLUG, SortDirection.ASC, 10, 0, (IngredientSpecificFilter) null);
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "slug"));
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(Map.of("en", "English"));
        when(ingredientRepository.findIdsByFiltersAndLocaleOtherSort("en", null, null, null, null, null, null, null, pageable))
                .thenReturn(Page.empty(pageable));

        var result = ingredientService.getAllAdmin(filterQuery, "en");

        assertThat(result.content()).isEmpty();
        verify(ingredientRepository, never()).findIdsByFiltersAndLocaleAsc(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(ingredientRepository, never()).findIdsByFiltersAndLocaleDesc(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getAllAdmin_withSpecificFilters_passesThemToRepository() {
        var specificFilter = new IngredientSpecificFilter(IngredientFoodGroup.VEGETABLE, true, true, true, 0.0, 100.0);
        var filterQuery = SCFilterQuery.powerful("tom", null, SortDirection.ASC, 10, 0, specificFilter);
        var pageable = PageRequest.of(0, 10);
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(Map.of("en", "English"));
        when(ingredientRepository.findIdsByFiltersAndLocaleAsc("en", "tom", "vegetable", true, true, true, 0.0, 100.0, pageable))
                .thenReturn(Page.empty(pageable));

        var result = ingredientService.getAllAdmin(filterQuery, "en");

        assertThat(result.content()).isEmpty();
        verify(ingredientRepository).findIdsByFiltersAndLocaleAsc("en", "tom", "vegetable", true, true, true, 0.0, 100.0, pageable);
    }

    // =========================================================
    // getByPublicIdAdmin
    // =========================================================

    @Test
    void getByPublicIdAdmin_returnsDetailsWhenFound() {
        var ingredient = new Ingredient();
        var publicId = ingredient.getPublicId();
        ingredient.setSlug("tomato");
        var translation = new IngredientTranslation();
        translation.setLanguage(english);
        translation.setName("Tomato");
        ingredient.setTranslations(new ArrayList<>(List.of(translation)));

        var tag = new Tag();
        tag.setSlug("fresh");
        var tagTranslation = new TagTranslation();
        tagTranslation.setLanguage(english);
        tagTranslation.setLabel("Fresh");
        tag.setTranslations(new ArrayList<>(List.of(tagTranslation)));
        var ingredientTag = new IngredientTag();
        ingredientTag.setIngredient(ingredient);
        ingredientTag.setTag(tag);
        ingredient.setIngredientTags(new ArrayList<>(List.of(ingredientTag)));

        when(ingredientRepository.findByPublicId(publicId)).thenReturn(Optional.of(ingredient));

        var result = ingredientService.getByPublicIdAdmin(publicId, "en");

        assertThat(result.publicId()).isEqualTo(publicId);
        assertThat(result.slug()).isEqualTo("tomato");
        assertThat(result.specificTranslationName()).isEqualTo("Tomato");
        assertThat(result.specificTranslationTagList()).hasSize(1);
        assertThat(result.specificTranslationTagList().getFirst().label()).isEqualTo("Fresh");
        verify(languageDomainBridgeService).validateLocaleIsActiveOrThrow("en");
    }

    @Test
    void getByPublicIdAdmin_throwsWhenNotFound() {
        var publicId = UUID.randomUUID();
        when(ingredientRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingredientService.getByPublicIdAdmin(publicId, "en"))
                .isInstanceOf(NoIngredientFoundException.class);
    }

    // =========================================================
    // createNewIngredient
    // =========================================================

    @Test
    void createNewIngredient_savesIngredientWhenAllActiveLocalesCovered() {
        var activeLanguages = Map.of("en", english, "it", italian);
        when(ingredientRepository.existsBySlug("tomato")).thenReturn(false);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(activeLanguages);
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguages, "en")).thenReturn(english);
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguages, "it")).thenReturn(italian);
        when(languageDomainBridgeService.toSimpleLanguagesMap(activeLanguages)).thenReturn(Map.of("en", "English", "it", "Italian"));
        when(languageDomainBridgeService.buildTranslatedLanguagesMap(anyList(), anyMap())).thenReturn(Map.of("en", "English", "it", "Italian"));
        when(tagDomainBridgeService.getTagsUsableForIngredients(List.of())).thenReturn(List.of());

        var dto = new AddIngredientDto(
                "tomato",
                IngredientFoodGroup.VEGETABLE,
                new BigDecimal("18.00"),
                new String[]{},
                true, true, true,
                List.of(),
                List.of(
                        new IngredientTranslationInputDto("en", "Tomato"),
                        new IngredientTranslationInputDto("it", "Pomodoro")
                )
        );

        var result = ingredientService.createNewIngredient(dto, "en");

        assertThat(result.slug()).isEqualTo("tomato");
        assertThat(result.namePreview()).isEqualTo("Tomato");

        var captor = ArgumentCaptor.forClass(Ingredient.class);
        verify(ingredientRepository).save(captor.capture());
        assertThat(captor.getValue().getTranslations()).hasSize(2);
    }

    @Test
    void createNewIngredient_throwsWhenSlugAlreadyExists() {
        when(ingredientRepository.existsBySlug("tomato")).thenReturn(true);

        var dto = new AddIngredientDto("tomato", IngredientFoodGroup.VEGETABLE, new BigDecimal("18.00"),
                new String[]{}, true, true, true, List.of(), List.of(new IngredientTranslationInputDto("en", "Tomato")));

        assertThatThrownBy(() -> ingredientService.createNewIngredient(dto, "en"))
                .isInstanceOf(IngredientSlugAlreadyExistsException.class);

        verifyNoInteractions(languageDomainBridgeService);
        verifyNoInteractions(tagDomainBridgeService);
        verify(ingredientRepository, never()).save(any());
    }

    @Test
    void createNewIngredient_throwsWhenLocaleAppearsTwice() {
        when(ingredientRepository.existsBySlug("tomato")).thenReturn(false);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english));

        var dto = new AddIngredientDto("tomato", IngredientFoodGroup.VEGETABLE, new BigDecimal("18.00"),
                new String[]{}, true, true, true, List.of(), List.of(
                        new IngredientTranslationInputDto("en", "Tomato"),
                        new IngredientTranslationInputDto("en", "Tomato duplicate")
                ));

        assertThatThrownBy(() -> ingredientService.createNewIngredient(dto, "en"))
                .isInstanceOf(DuplicateIngredientLocaleException.class);

        verify(ingredientRepository, never()).save(any());
    }

    @Test
    void createNewIngredient_throwsWhenActiveLocaleIsMissing() {
        when(ingredientRepository.existsBySlug("tomato")).thenReturn(false);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english, "it", italian));

        var dto = new AddIngredientDto("tomato", IngredientFoodGroup.VEGETABLE, new BigDecimal("18.00"),
                new String[]{}, true, true, true, List.of(), List.of(new IngredientTranslationInputDto("en", "Tomato")));

        assertThatThrownBy(() -> ingredientService.createNewIngredient(dto, "en"))
                .isInstanceOf(MissingIngredientLocalesException.class);

        verify(ingredientRepository, never()).save(any());
    }

    // =========================================================
    // updateIngredient
    // =========================================================

    @Test
    void updateIngredient_updatesExistingTranslationFields() {
        var ingredient = new Ingredient();
        ingredient.setSlug("tomato");
        ingredient.setFoodGroup(IngredientFoodGroup.VEGETABLE);
        ingredient.setCaloriesPer100g(new BigDecimal("18.00"));
        ingredient.setAllergens(new String[]{});
        ingredient.setVegetarian(true);
        ingredient.setVegan(true);
        ingredient.setGlutenFree(true);
        var existingTranslation = new IngredientTranslation();
        existingTranslation.setLanguage(english);
        existingTranslation.setName("Old Name");
        ingredient.setTranslations(new ArrayList<>(List.of(existingTranslation)));
        ingredient.setIngredientTags(new ArrayList<>());
        var publicId = ingredient.getPublicId();

        when(ingredientRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(ingredient));
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english));
        when(languageDomainBridgeService.toSimpleLanguagesMap(anyMap())).thenReturn(Map.of("en", "English"));
        when(languageDomainBridgeService.buildTranslatedLanguagesMap(anyList(), anyMap())).thenReturn(Map.of("en", "English"));
        when(tagDomainBridgeService.getTagsUsableForIngredients(List.of())).thenReturn(List.of());

        var dto = new UpdateIngredientDto("tomato", IngredientFoodGroup.VEGETABLE, new BigDecimal("18.00"),
                new String[]{}, true, true, true, List.of(), new IngredientTranslationInputDto("en", "New Name"));

        var result = ingredientService.updateIngredient(publicId, dto);

        assertThat(ingredient.getTranslations()).hasSize(1);
        assertThat(existingTranslation.getName()).isEqualTo("New Name");
        assertThat(result.namePreview()).isEqualTo("New Name");
    }

    @Test
    void updateIngredient_addsNewTranslationWhenLocaleMissing() {
        var ingredient = new Ingredient();
        ingredient.setSlug("tomato");
        ingredient.setAllergens(new String[]{});
        var existingTranslation = new IngredientTranslation();
        existingTranslation.setLanguage(english);
        existingTranslation.setName("Tomato");
        ingredient.setTranslations(new ArrayList<>(List.of(existingTranslation)));
        ingredient.setIngredientTags(new ArrayList<>());
        var publicId = ingredient.getPublicId();

        when(ingredientRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(ingredient));
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english, "it", italian));
        when(languageDomainBridgeService.toSimpleLanguagesMap(anyMap())).thenReturn(Map.of("en", "English", "it", "Italian"));
        when(languageDomainBridgeService.buildTranslatedLanguagesMap(anyList(), anyMap())).thenReturn(Map.of("en", "English", "it", "Italian"));
        when(tagDomainBridgeService.getTagsUsableForIngredients(List.of())).thenReturn(List.of());

        var dto = new UpdateIngredientDto("tomato", null, null,
                new String[]{}, false, false, false, List.of(), new IngredientTranslationInputDto("it", "Pomodoro"));

        var result = ingredientService.updateIngredient(publicId, dto);

        assertThat(ingredient.getTranslations()).hasSize(2);
        assertThat(result.namePreview()).isEqualTo("Pomodoro");
    }

    @Test
    void updateIngredient_replacesTagsWithNewSet() {
        var ingredient = new Ingredient();
        ingredient.setSlug("tomato");
        ingredient.setAllergens(new String[]{});
        var existingTranslation = new IngredientTranslation();
        existingTranslation.setLanguage(english);
        existingTranslation.setName("Tomato");
        ingredient.setTranslations(new ArrayList<>(List.of(existingTranslation)));

        var oldTag = new Tag();
        var oldIngredientTag = new IngredientTag();
        oldIngredientTag.setIngredient(ingredient);
        oldIngredientTag.setTag(oldTag);
        ingredient.setIngredientTags(new ArrayList<>(List.of(oldIngredientTag)));
        var publicId = ingredient.getPublicId();

        var newTagId = UUID.randomUUID();
        var newTag = new Tag();
        newTag.setPublicId(newTagId);

        when(ingredientRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(ingredient));
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english));
        when(languageDomainBridgeService.toSimpleLanguagesMap(anyMap())).thenReturn(Map.of("en", "English"));
        when(languageDomainBridgeService.buildTranslatedLanguagesMap(anyList(), anyMap())).thenReturn(Map.of("en", "English"));
        when(tagDomainBridgeService.getTagsUsableForIngredients(List.of(newTagId))).thenReturn(List.of(newTag));

        var dto = new UpdateIngredientDto("tomato", null, null,
                new String[]{}, false, false, false, List.of(newTagId), new IngredientTranslationInputDto("en", "Tomato"));

        ingredientService.updateIngredient(publicId, dto);

        assertThat(ingredient.getIngredientTags()).hasSize(1);
        assertThat(ingredient.getIngredientTags().getFirst().getTag()).isEqualTo(newTag);
    }

    @Test
    void updateIngredient_throwsWhenNewSlugAlreadyExists() {
        var ingredient = new Ingredient();
        ingredient.setSlug("tomato");
        var publicId = ingredient.getPublicId();

        when(ingredientRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(ingredient));
        when(ingredientRepository.existsBySlug("potato")).thenReturn(true);

        var dto = new UpdateIngredientDto("potato", null, null,
                new String[]{}, false, false, false, List.of(), new IngredientTranslationInputDto("en", "Tomato"));

        assertThatThrownBy(() -> ingredientService.updateIngredient(publicId, dto))
                .isInstanceOf(IngredientSlugAlreadyExistsException.class);

        verifyNoInteractions(languageDomainBridgeService);
        verifyNoInteractions(tagDomainBridgeService);
    }

    @Test
    void updateIngredient_throwsWhenIngredientNotFound() {
        var publicId = UUID.randomUUID();
        when(ingredientRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.empty());

        var dto = new UpdateIngredientDto("tomato", null, null,
                new String[]{}, false, false, false, List.of(), new IngredientTranslationInputDto("en", "Tomato"));

        assertThatThrownBy(() -> ingredientService.updateIngredient(publicId, dto))
                .isInstanceOf(NoIngredientFoundException.class);
    }

    @Test
    void updateIngredient_throwsWhenLocaleNotActive() {
        var ingredient = new Ingredient();
        ingredient.setSlug("tomato");
        ingredient.setTranslations(new ArrayList<>());
        var publicId = ingredient.getPublicId();

        var activeLanguages = Map.of("en", english);
        when(ingredientRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(ingredient));
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(activeLanguages);
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguages, "fr"))
                .thenThrow(new LocaleNotActiveException("fr"));

        var dto = new UpdateIngredientDto("tomato", null, null,
                new String[]{}, false, false, false, List.of(), new IngredientTranslationInputDto("fr", "Tomate"));

        assertThatThrownBy(() -> ingredientService.updateIngredient(publicId, dto))
                .isInstanceOf(LocaleNotActiveException.class);

        verify(ingredientRepository, never()).save(any());
        verifyNoInteractions(tagDomainBridgeService);
    }

    // =========================================================
    // deleteIngredient
    // =========================================================

    @Test
    void deleteIngredient_deletesAndReturnsPublicId() {
        var ingredient = new Ingredient();
        var publicId = ingredient.getPublicId();

        when(ingredientRepository.findByPublicId(publicId)).thenReturn(Optional.of(ingredient));

        var deletedPublicId = ingredientService.deleteIngredient(publicId);

        assertThat(deletedPublicId).isEqualTo(publicId);
        verify(ingredientRepository).delete(ingredient);
    }

    @Test
    void deleteIngredient_throwsWhenNotFound() {
        var publicId = UUID.randomUUID();
        when(ingredientRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingredientService.deleteIngredient(publicId))
                .isInstanceOf(NoIngredientFoundException.class);

        verify(ingredientRepository, never()).delete(any());
    }
}
