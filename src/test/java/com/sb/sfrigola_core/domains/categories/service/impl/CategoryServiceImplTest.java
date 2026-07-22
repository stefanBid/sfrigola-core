package com.sb.sfrigola_core.domains.categories.service.impl;

import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.domains.categories.dto.input.AddCategoryDto;
import com.sb.sfrigola_core.domains.categories.dto.input.ReorderedCategoriesTreeDto;
import com.sb.sfrigola_core.domains.categories.dto.input.UpdateCategoryDto;
import com.sb.sfrigola_core.domains.categories.dto.input.UpsetCategoryTranslationDto;
import com.sb.sfrigola_core.domains.categories.entity.Category;
import com.sb.sfrigola_core.domains.categories.entity.CategoryTranslation;
import com.sb.sfrigola_core.domains.categories.exception.CategoryHasChildrenException;
import com.sb.sfrigola_core.domains.categories.exception.CategoryReorderMismatchException;
import com.sb.sfrigola_core.domains.categories.exception.CategorySlugAlreadyExistsException;
import com.sb.sfrigola_core.domains.categories.exception.DuplicateCategoryLocaleException;
import com.sb.sfrigola_core.domains.categories.exception.MissingCategoryLocalesException;
import com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException;
import com.sb.sfrigola_core.domains.categories.repository.ICategoryRepository;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
class CategoryServiceImplTest {

    @Mock
    private ICategoryRepository categoryRepository;
    @Mock
    private ILanguageDomainBridgeService languageDomainBridgeService;

    private CategoryServiceImpl categoryService;

    private Language english;
    private Language italian;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository, languageDomainBridgeService);

        english = new Language();
        english.setCode("en");
        english.setName("English");

        italian = new Language();
        italian.setCode("it");
        italian.setName("Italian");
    }

    @Test
    void createNewCategory_savesCategoryWhenAllActiveLocalesCovered() {
        var activeLanguages = Map.of("en", english, "it", italian);
        when(categoryRepository.existsBySlug("pasta")).thenReturn(false);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(activeLanguages);
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguages, "en")).thenReturn(english);
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguages, "it")).thenReturn(italian);
        when(languageDomainBridgeService.toSimpleLanguagesMap(activeLanguages)).thenReturn(Map.of("en", "English", "it", "Italian"));
        when(languageDomainBridgeService.buildTranslatedLanguagesMap(anyList(), anyMap())).thenReturn(Map.of("en", "English", "it", "Italian"));
        when(categoryRepository.findMaxSortOrderInGroup(null)).thenReturn(-1);

        var dto = new AddCategoryDto(
                "pasta",
                true,
                List.of(
                        new UpsetCategoryTranslationDto("en", "Pasta", "Pasta dishes"),
                        new UpsetCategoryTranslationDto("it", "Pasta", "Piatti di pasta")
                )
        );

        var result = categoryService.createNewCategory(dto, null, "en");

        assertThat(result.slug()).isEqualTo("pasta");
        assertThat(result.sortOrder()).isEqualTo((short) 0);
        assertThat(result.translationPreview().namePreview()).isEqualTo("Pasta");

        var categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getTranslations()).hasSize(2);
    }

    @Test
    void createNewCategory_throwsWhenSlugAlreadyExists() {
        when(categoryRepository.existsBySlug("pasta")).thenReturn(true);

        var dto = new AddCategoryDto("pasta", true, List.of(new UpsetCategoryTranslationDto("en", "Pasta", null)));

        assertThatThrownBy(() -> categoryService.createNewCategory(dto, null, "en"))
                .isInstanceOf(CategorySlugAlreadyExistsException.class);

        verifyNoInteractions(languageDomainBridgeService);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createNewCategory_throwsWhenLocaleAppearsTwice() {
        when(categoryRepository.existsBySlug("pasta")).thenReturn(false);

        var dto = new AddCategoryDto("pasta", true, List.of(
                new UpsetCategoryTranslationDto("en", "Pasta", null),
                new UpsetCategoryTranslationDto("en", "Pasta duplicate", null)
        ));

        assertThatThrownBy(() -> categoryService.createNewCategory(dto, null, "en"))
                .isInstanceOf(DuplicateCategoryLocaleException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createNewCategory_throwsWhenActiveLocaleIsMissing() {
        when(categoryRepository.existsBySlug("pasta")).thenReturn(false);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english, "it", italian));

        var dto = new AddCategoryDto("pasta", true, List.of(new UpsetCategoryTranslationDto("en", "Pasta", null)));

        assertThatThrownBy(() -> categoryService.createNewCategory(dto, null, "en"))
                .isInstanceOf(MissingCategoryLocalesException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createNewCategory_throwsWhenParentDoesNotExist() {
        var parentPublicId = UUID.randomUUID();
        when(categoryRepository.existsBySlug("pasta")).thenReturn(false);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english));
        when(categoryRepository.findByPublicId(parentPublicId)).thenReturn(Optional.empty());

        var dto = new AddCategoryDto("pasta", true, List.of(new UpsetCategoryTranslationDto("en", "Pasta", null)));

        assertThatThrownBy(() -> categoryService.createNewCategory(dto, parentPublicId, "en"))
                .isInstanceOf(NoCategoryFoundException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategory_deletesAndReturnsPublicIdWhenNoChildren() {
        var category = new Category();
        var publicId = category.getPublicId();

        when(categoryRepository.findByPublicId(publicId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByParentId(category.getId())).thenReturn(false);

        var deletedPublicId = categoryService.deleteCategory(publicId);

        assertThat(deletedPublicId).isEqualTo(publicId);
        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_throwsWhenCategoryNotFound() {
        var publicId = UUID.randomUUID();
        when(categoryRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(publicId))
                .isInstanceOf(NoCategoryFoundException.class);

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_throwsWhenCategoryHasChildren() {
        var category = new Category();
        var publicId = category.getPublicId();

        when(categoryRepository.findByPublicId(publicId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByParentId(category.getId())).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(publicId))
                .isInstanceOf(CategoryHasChildrenException.class);

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void getAll_returnsMappedResultsWhenCategoriesExist() {
        var filterQuery = SCFilterQuery.pagedOnly(10, 0);
        var pageable = PageRequest.of(0, 10);
        var idsPage = new PageImpl<>(List.of(1L), pageable, 1);
        when(categoryRepository.findIdsForViewUse("en", pageable)).thenReturn(idsPage);

        var category = new Category();
        category.setId(1L);
        category.setSlug("pasta");
        var translation = new CategoryTranslation();
        translation.setLanguage(english);
        translation.setName("Pasta");
        translation.setDescription("Pasta dishes");
        category.setTranslations(new ArrayList<>(List.of(translation)));
        when(categoryRepository.findByIdsWithSpecificTranslation(List.of(1L), "en")).thenReturn(List.of(category));

        var result = categoryService.getAll(filterQuery, "en");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().slug()).isEqualTo("pasta");
        assertThat(result.content().getFirst().name()).isEqualTo("Pasta");
        verify(languageDomainBridgeService).validateLocaleIsActiveOrThrow("en");
    }

    @Test
    void getAll_returnsEmptyResultWhenNoCategoriesMatch() {
        var filterQuery = SCFilterQuery.pagedOnly(10, 0);
        var pageable = PageRequest.of(0, 10);
        when(categoryRepository.findIdsForViewUse("en", pageable)).thenReturn(Page.empty(pageable));

        var result = categoryService.getAll(filterQuery, "en");

        assertThat(result.content()).isEmpty();
        verify(categoryRepository, never()).findByIdsWithSpecificTranslation(any(), any());
    }

    @Test
    void getAllAdmin_ascendingSort_returnsMappedResults() {
        var filterQuery = SCFilterQuery.essentialWithSearch(null, null, SortDirection.ASC, 10, 0);
        var pageable = PageRequest.of(0, 10);
        var activeLanguagesMap = Map.of("en", "English", "it", "Italian");
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(activeLanguagesMap);

        var idsPage = new PageImpl<>(List.of(1L), pageable, 1);
        when(categoryRepository.findIdsByLocaleAndIsActiveAndSearchKeyAsc("en", null, null, pageable)).thenReturn(idsPage);

        var category = new Category();
        category.setId(1L);
        category.setSlug("pasta");
        var translation = new CategoryTranslation();
        translation.setLanguage(english);
        translation.setName("Pasta");
        category.setTranslations(new ArrayList<>(List.of(translation)));
        when(categoryRepository.findByIdsWithAllTranslations(List.of(1L))).thenReturn(List.of(category));
        when(languageDomainBridgeService.buildTranslatedLanguagesMap(anyList(), eq(activeLanguagesMap))).thenReturn(activeLanguagesMap);

        var result = categoryService.getAllAdmin(filterQuery, "en", null);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().translationPreview().namePreview()).isEqualTo("Pasta");
        verify(categoryRepository, never()).findIdsByLocaleAndIsActiveAndSearchKeyDesc(any(), any(), any(), any());
    }

    @Test
    void getAllAdmin_descendingSort_callsDescendingQuery() {
        var filterQuery = SCFilterQuery.essentialWithSearch(null, null, SortDirection.DESC, 10, 0);
        var pageable = PageRequest.of(0, 10);
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(Map.of("en", "English"));
        when(categoryRepository.findIdsByLocaleAndIsActiveAndSearchKeyDesc("en", null, null, pageable)).thenReturn(Page.empty(pageable));

        var result = categoryService.getAllAdmin(filterQuery, "en", null);

        assertThat(result.content()).isEmpty();
        verify(categoryRepository, never()).findIdsByLocaleAndIsActiveAndSearchKeyAsc(any(), any(), any(), any());
    }

    @Test
    void getByPublicIdAdmin_returnsDetailsWhenFound() {
        var category = new Category();
        var publicId = category.getPublicId();
        var translation = new CategoryTranslation();
        translation.setLanguage(english);
        translation.setName("Pasta");
        translation.setDescription("Pasta dishes");
        category.setTranslations(new ArrayList<>(List.of(translation)));

        when(categoryRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(category));

        var result = categoryService.getByPublicIdAdmin(publicId, "en");

        assertThat(result.publicId()).isEqualTo(publicId);
        assertThat(result.specificTranslation().namePreview()).isEqualTo("Pasta");
        verify(languageDomainBridgeService).validateLocaleIsActiveOrThrow("en");
    }

    @Test
    void getByPublicIdAdmin_throwsWhenNotFound() {
        var publicId = UUID.randomUUID();
        when(categoryRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getByPublicIdAdmin(publicId, "en"))
                .isInstanceOf(NoCategoryFoundException.class);
    }

    @Test
    void updateCategory_updatesExistingTranslationFields() {
        var category = new Category();
        category.setSlug("pasta");
        category.setActive(true);
        var existingTranslation = new CategoryTranslation();
        existingTranslation.setLanguage(english);
        existingTranslation.setName("Old Name");
        existingTranslation.setDescription("Old Description");
        category.setTranslations(new ArrayList<>(List.of(existingTranslation)));
        var publicId = category.getPublicId();

        when(categoryRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(category));
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english));
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(anyMap(), eq("en"))).thenReturn(english);
        when(languageDomainBridgeService.toSimpleLanguagesMap(anyMap())).thenReturn(Map.of("en", "English"));
        when(languageDomainBridgeService.buildTranslatedLanguagesMap(anyList(), anyMap())).thenReturn(Map.of("en", "English"));

        var dto = new UpdateCategoryDto("pasta", true, new UpsetCategoryTranslationDto("en", "New Name", "New Description"));

        var result = categoryService.updateCategory(dto, publicId);

        assertThat(category.getTranslations()).hasSize(1);
        assertThat(existingTranslation.getName()).isEqualTo("New Name");
        assertThat(existingTranslation.getDescription()).isEqualTo("New Description");
        assertThat(result.translationPreview().namePreview()).isEqualTo("New Name");
    }

    @Test
    void updateCategory_addsNewTranslationWhenLocaleMissing() {
        var category = new Category();
        category.setSlug("pasta");
        category.setActive(true);
        var existingTranslation = new CategoryTranslation();
        existingTranslation.setLanguage(english);
        existingTranslation.setName("Pasta");
        category.setTranslations(new ArrayList<>(List.of(existingTranslation)));
        var publicId = category.getPublicId();

        when(categoryRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(category));
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english, "it", italian));
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(anyMap(), eq("it"))).thenReturn(italian);
        when(languageDomainBridgeService.toSimpleLanguagesMap(anyMap())).thenReturn(Map.of("en", "English", "it", "Italian"));
        when(languageDomainBridgeService.buildTranslatedLanguagesMap(anyList(), anyMap())).thenReturn(Map.of("en", "English", "it", "Italian"));

        var dto = new UpdateCategoryDto("pasta", true, new UpsetCategoryTranslationDto("it", "Pasta IT", null));

        var result = categoryService.updateCategory(dto, publicId);

        assertThat(category.getTranslations()).hasSize(2);
        assertThat(result.translationPreview().namePreview()).isEqualTo("Pasta IT");
    }

    @Test
    void updateCategory_throwsWhenNewSlugAlreadyExists() {
        var category = new Category();
        category.setSlug("pasta");
        var publicId = category.getPublicId();

        when(categoryRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsBySlug("risotto")).thenReturn(true);

        var dto = new UpdateCategoryDto("risotto", true, new UpsetCategoryTranslationDto("en", "Pasta", null));

        assertThatThrownBy(() -> categoryService.updateCategory(dto, publicId))
                .isInstanceOf(CategorySlugAlreadyExistsException.class);

        verifyNoInteractions(languageDomainBridgeService);
    }

    @Test
    void updateCategory_throwsWhenCategoryNotFound() {
        var publicId = UUID.randomUUID();
        when(categoryRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.empty());

        var dto = new UpdateCategoryDto("pasta", true, new UpsetCategoryTranslationDto("en", "Pasta", null));

        assertThatThrownBy(() -> categoryService.updateCategory(dto, publicId))
                .isInstanceOf(NoCategoryFoundException.class);
    }

    @Test
    void reorderCategories_reordersRootCategoriesInGivenOrder() {
        var categoryA = new Category();
        categoryA.setSlug("pasta");
        var categoryB = new Category();
        categoryB.setSlug("risotto");

        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(categoryA, categoryB));
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(Map.of("en", "English"));

        var reorderDto = new ReorderedCategoriesTreeDto(null, List.of(categoryB.getPublicId(), categoryA.getPublicId()));

        var result = categoryService.reorderCategories(reorderDto);

        assertThat(categoryB.getSortOrder()).isEqualTo((short) 0);
        assertThat(categoryA.getSortOrder()).isEqualTo((short) 1);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).slug()).isEqualTo("risotto");
        assertThat(result.get(1).slug()).isEqualTo("pasta");
    }

    @Test
    void reorderCategories_throwsWhenIdsMismatch() {
        var categoryA = new Category();
        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(categoryA));

        var reorderDto = new ReorderedCategoriesTreeDto(null, List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> categoryService.reorderCategories(reorderDto))
                .isInstanceOf(CategoryReorderMismatchException.class);

        verifyNoInteractions(languageDomainBridgeService);
    }

    @Test
    void reorderCategories_throwsWhenParentNotFound() {
        var parentPublicId = UUID.randomUUID();
        when(categoryRepository.findByPublicId(parentPublicId)).thenReturn(Optional.empty());

        var reorderDto = new ReorderedCategoriesTreeDto(parentPublicId, List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> categoryService.reorderCategories(reorderDto))
                .isInstanceOf(NoCategoryFoundException.class);

        verify(categoryRepository, never()).findByParentIsNull();
    }
}
