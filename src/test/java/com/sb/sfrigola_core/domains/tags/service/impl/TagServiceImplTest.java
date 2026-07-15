package com.sb.sfrigola_core.domains.tags.service.impl;

import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import com.sb.sfrigola_core.domains.tags.dto.input.AddTagDto;
import com.sb.sfrigola_core.domains.tags.dto.input.SuggestTagDto;
import com.sb.sfrigola_core.domains.tags.dto.input.TagTranslationInputDto;
import com.sb.sfrigola_core.domains.tags.dto.input.UpdateTagDto;
import com.sb.sfrigola_core.domains.tags.entity.Tag;
import com.sb.sfrigola_core.domains.tags.entity.TagTranslation;
import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagSortField;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
import com.sb.sfrigola_core.domains.tags.enums.TagType;
import com.sb.sfrigola_core.domains.tags.exception.DuplicateTagLocaleException;
import com.sb.sfrigola_core.domains.tags.exception.MissingTagLocalesException;
import com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException;
import com.sb.sfrigola_core.domains.tags.exception.TagLabelAlreadyExistsException;
import com.sb.sfrigola_core.domains.tags.exception.TagSlugAlreadyExistsException;
import com.sb.sfrigola_core.domains.tags.models.TagSpecificFilter;
import com.sb.sfrigola_core.domains.tags.repository.ITagRepository;
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
class TagServiceImplTest {

    @Mock
    private ITagRepository tagRepository;
    @Mock
    private ILanguageDomainBridgeService languageDomainBridgeService;

    private TagServiceImpl tagService;

    private Language english;
    private Language italian;

    @BeforeEach
    void setUp() {
        tagService = new TagServiceImpl(tagRepository, languageDomainBridgeService);

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
    void getAll_returnsMappedResultsWhenTagsExist() {
        SCFilterQuery<TagSpecificFilter> filterQuery = SCFilterQuery.powerful(null, null, null, 10, 0, null);
        var pageable = PageRequest.of(0, 10);
        var idsPage = new PageImpl<>(List.of(1L), pageable, 1);
        when(tagRepository.findIdsByFiltersAndLocaleAsc("en", null, TagStatus.APPROVED.getValue(), null, null, pageable))
                .thenReturn(idsPage);

        var tag = new Tag();
        tag.setId(1L);
        tag.setSlug("vegan");
        var translation = new TagTranslation();
        translation.setLanguage(english);
        translation.setLabel("Vegan");
        tag.setTranslations(new ArrayList<>(List.of(translation)));
        when(tagRepository.findByIdsWithSpecificTranslation(List.of(1L), "en")).thenReturn(List.of(tag));

        var result = tagService.getAll(filterQuery, "en");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().slug()).isEqualTo("vegan");
        assertThat(result.content().getFirst().label()).isEqualTo("Vegan");
        verify(languageDomainBridgeService).validateLocaleIsActiveOrThrow("en");
    }

    @Test
    void getAll_returnsEmptyResultWhenNoTagsMatch() {
        SCFilterQuery<TagSpecificFilter> filterQuery = SCFilterQuery.powerful(null, null, null, 10, 0, null);
        var pageable = PageRequest.of(0, 10);
        when(tagRepository.findIdsByFiltersAndLocaleAsc("en", null, TagStatus.APPROVED.getValue(), null, null, pageable))
                .thenReturn(Page.empty(pageable));

        var result = tagService.getAll(filterQuery, "en");

        assertThat(result.content()).isEmpty();
        verify(tagRepository, never()).findByIdsWithSpecificTranslation(any(), any());
    }

    @Test
    void getAll_withScopeFilter_passesScopeValueToRepository() {
        var filter = new TagSpecificFilter(null, null, TagScope.INGREDIENT);
        SCFilterQuery<TagSpecificFilter> filterQuery = SCFilterQuery.powerful(null, null, null, 10, 0, filter);
        var pageable = PageRequest.of(0, 10);
        when(tagRepository.findIdsByFiltersAndLocaleAsc("en", null, TagStatus.APPROVED.getValue(), "ingredient", null, pageable))
                .thenReturn(Page.empty(pageable));

        var result = tagService.getAll(filterQuery, "en");

        assertThat(result.content()).isEmpty();
    }

    // =========================================================
    // getAllAdmin
    // =========================================================

    @Test
    void getAllAdmin_ascendingSortByLabel_returnsMappedResults() {
        SCFilterQuery<TagSpecificFilter> filterQuery = SCFilterQuery.powerful(null, null, SortDirection.ASC, 10, 0, null);
        var pageable = PageRequest.of(0, 10);
        var activeLanguagesMap = Map.of("en", "English", "it", "Italian");
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(activeLanguagesMap);

        var idsPage = new PageImpl<>(List.of(1L), pageable, 1);
        when(tagRepository.findIdsByFiltersAndLocaleAsc("en", null, null, null, null, pageable)).thenReturn(idsPage);

        var tag = new Tag();
        tag.setId(1L);
        tag.setSlug("vegan");
        var translation = new TagTranslation();
        translation.setLanguage(english);
        translation.setLabel("Vegan");
        tag.setTranslations(new ArrayList<>(List.of(translation)));
        when(tagRepository.findByIdsWithAllTranslations(List.of(1L))).thenReturn(List.of(tag));
        when(languageDomainBridgeService.buildTranslatedLanguagesMap(anyList(), eq(activeLanguagesMap))).thenReturn(activeLanguagesMap);

        var result = tagService.getAllAdmin(filterQuery, "en");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().labelPreview()).isEqualTo("Vegan");
        verify(tagRepository, never()).findIdsByFiltersAndLocaleDesc(any(), any(), any(), any(), any(), any());
        verify(tagRepository, never()).findIdsByFiltersAndLocaleOtherSort(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getAllAdmin_descendingSortByLabel_callsDescendingQuery() {
        SCFilterQuery<TagSpecificFilter> filterQuery = SCFilterQuery.powerful(null, null, SortDirection.DESC, 10, 0, null);
        var pageable = PageRequest.of(0, 10);
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(Map.of("en", "English"));
        when(tagRepository.findIdsByFiltersAndLocaleDesc("en", null, null, null, null, pageable)).thenReturn(Page.empty(pageable));

        var result = tagService.getAllAdmin(filterQuery, "en");

        assertThat(result.content()).isEmpty();
        verify(tagRepository, never()).findIdsByFiltersAndLocaleAsc(any(), any(), any(), any(), any(), any());
        verify(tagRepository, never()).findIdsByFiltersAndLocaleOtherSort(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getAllAdmin_sortByNonLabelField_callsOtherSortQuery() {
        SCFilterQuery<TagSpecificFilter> filterQuery = SCFilterQuery.powerful(null, TagSortField.TYPE, SortDirection.DESC, 10, 0, null);
        var sort = Sort.by(Sort.Direction.DESC, "type");
        var pageable = PageRequest.of(0, 10, sort);
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(Map.of("en", "English"));
        when(tagRepository.findIdsByFiltersAndLocaleOtherSort("en", null, null, null, null, pageable)).thenReturn(Page.empty(pageable));

        var result = tagService.getAllAdmin(filterQuery, "en");

        assertThat(result.content()).isEmpty();
        verify(tagRepository, never()).findIdsByFiltersAndLocaleAsc(any(), any(), any(), any(), any(), any());
        verify(tagRepository, never()).findIdsByFiltersAndLocaleDesc(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getAllAdmin_withFilters_passesFilterValuesToRepository() {
        var filter = new TagSpecificFilter(TagStatus.PENDING, TagType.DIETARY, TagScope.RECIPE);
        SCFilterQuery<TagSpecificFilter> filterQuery = SCFilterQuery.powerful("veg", null, SortDirection.ASC, 10, 0, filter);
        var pageable = PageRequest.of(0, 10);
        when(languageDomainBridgeService.getAllActiveLanguagesSimpleMap()).thenReturn(Map.of("en", "English"));
        when(tagRepository.findIdsByFiltersAndLocaleAsc("en", "veg", "pending", "recipe", "dietary", pageable))
                .thenReturn(Page.empty(pageable));

        var result = tagService.getAllAdmin(filterQuery, "en");

        assertThat(result.content()).isEmpty();
    }

    // =========================================================
    // getByPublicIdAdmin
    // =========================================================

    @Test
    void getByPublicIdAdmin_returnsDetailsWhenFound() {
        var tag = new Tag();
        var publicId = tag.getPublicId();
        var translation = new TagTranslation();
        translation.setLanguage(english);
        translation.setLabel("Vegan");
        tag.setTranslations(new ArrayList<>(List.of(translation)));

        when(tagRepository.findByPublicId(publicId)).thenReturn(Optional.of(tag));

        var result = tagService.getByPublicIdAdmin(publicId, "en");

        assertThat(result.publicId()).isEqualTo(publicId);
        assertThat(result.specificTranslationLabel()).isEqualTo("Vegan");
        verify(languageDomainBridgeService).validateLocaleIsActiveOrThrow("en");
    }

    @Test
    void getByPublicIdAdmin_throwsWhenNotFound() {
        var publicId = UUID.randomUUID();
        when(tagRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.getByPublicIdAdmin(publicId, "en"))
                .isInstanceOf(NoTagFoundException.class);
    }

    // =========================================================
    // createNewTag
    // =========================================================

    @Test
    void createNewTag_savesTagWhenAllActiveLocalesCovered() {
        var activeLanguages = Map.of("en", english, "it", italian);
        when(tagRepository.existsBySlug("vegan")).thenReturn(false);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(activeLanguages);
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguages, "en")).thenReturn(english);
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguages, "it")).thenReturn(italian);
        when(languageDomainBridgeService.toSimpleLanguagesMap(activeLanguages)).thenReturn(Map.of("en", "English", "it", "Italian"));
        when(languageDomainBridgeService.buildTranslatedLanguagesMap(anyList(), anyMap())).thenReturn(Map.of("en", "English", "it", "Italian"));

        var dto = new AddTagDto("vegan", TagType.DIETARY, TagScope.BOTH, List.of(
                new TagTranslationInputDto("en", "Vegan"),
                new TagTranslationInputDto("it", "Vegano")
        ));

        var result = tagService.createNewTag(dto, "en");

        assertThat(result.slug()).isEqualTo("vegan");
        assertThat(result.labelPreview()).isEqualTo("Vegan");
        assertThat(result.status()).isEqualTo(TagStatus.APPROVED);

        var tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(tagCaptor.capture());
        assertThat(tagCaptor.getValue().getTranslations()).hasSize(2);
    }

    @Test
    void createNewTag_throwsWhenSlugAlreadyExists() {
        when(tagRepository.existsBySlug("vegan")).thenReturn(true);

        var dto = new AddTagDto("vegan", TagType.DIETARY, TagScope.BOTH, List.of(new TagTranslationInputDto("en", "Vegan")));

        assertThatThrownBy(() -> tagService.createNewTag(dto, "en"))
                .isInstanceOf(TagSlugAlreadyExistsException.class);

        verifyNoInteractions(languageDomainBridgeService);
        verify(tagRepository, never()).save(any());
    }

    @Test
    void createNewTag_throwsWhenLocaleAppearsTwice() {
        when(tagRepository.existsBySlug("vegan")).thenReturn(false);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english));

        var dto = new AddTagDto("vegan", TagType.DIETARY, TagScope.BOTH, List.of(
                new TagTranslationInputDto("en", "Vegan"),
                new TagTranslationInputDto("en", "Vegan duplicate")
        ));

        assertThatThrownBy(() -> tagService.createNewTag(dto, "en"))
                .isInstanceOf(DuplicateTagLocaleException.class);

        verify(tagRepository, never()).save(any());
    }

    @Test
    void createNewTag_throwsWhenActiveLocaleIsMissing() {
        when(tagRepository.existsBySlug("vegan")).thenReturn(false);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english, "it", italian));

        var dto = new AddTagDto("vegan", TagType.DIETARY, TagScope.BOTH, List.of(new TagTranslationInputDto("en", "Vegan")));

        assertThatThrownBy(() -> tagService.createNewTag(dto, "en"))
                .isInstanceOf(MissingTagLocalesException.class);

        verify(tagRepository, never()).save(any());
    }

    // =========================================================
    // suggestNewTag
    // =========================================================

    @Test
    void suggestNewTag_savesPendingTagAndReturnsInputDto() {
        var activeLanguages = Map.of("en", english);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(activeLanguages);
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguages, "en")).thenReturn(english);
        when(tagRepository.existsBySlug("spicy")).thenReturn(false);
        when(tagRepository.existsByLabelAndLanguage("Spicy", "en")).thenReturn(false);

        var dto = new SuggestTagDto("spicy", TagType.FLAVOR, TagScope.BOTH, "Spicy");

        var result = tagService.suggestNewTag(dto, "en");

        assertThat(result).isEqualTo(dto);

        var tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(tagCaptor.capture());
        var savedTag = tagCaptor.getValue();
        assertThat(savedTag.getSlug()).isEqualTo("spicy");
        assertThat(savedTag.getStatus()).isEqualTo(TagStatus.PENDING);
        assertThat(savedTag.getTranslations()).hasSize(1);
        assertThat(savedTag.getTranslations().getFirst().getLabel()).isEqualTo("Spicy");
    }

    @Test
    void suggestNewTag_throwsWhenSlugAlreadyExists() {
        var activeLanguages = Map.of("en", english);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(activeLanguages);
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguages, "en")).thenReturn(english);
        when(tagRepository.existsBySlug("spicy")).thenReturn(true);

        var dto = new SuggestTagDto("spicy", TagType.FLAVOR, TagScope.BOTH, "Spicy");

        assertThatThrownBy(() -> tagService.suggestNewTag(dto, "en"))
                .isInstanceOf(TagSlugAlreadyExistsException.class);

        verify(tagRepository, never()).save(any());
    }

    @Test
    void suggestNewTag_throwsWhenLabelAlreadyExists() {
        var activeLanguages = Map.of("en", english);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(activeLanguages);
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguages, "en")).thenReturn(english);
        when(tagRepository.existsBySlug("spicy")).thenReturn(false);
        when(tagRepository.existsByLabelAndLanguage("Spicy", "en")).thenReturn(true);

        var dto = new SuggestTagDto("spicy", TagType.FLAVOR, TagScope.BOTH, "Spicy");

        assertThatThrownBy(() -> tagService.suggestNewTag(dto, "en"))
                .isInstanceOf(TagLabelAlreadyExistsException.class);

        verify(tagRepository, never()).save(any());
    }

    // =========================================================
    // updateTagStatus
    // =========================================================

    @Test
    void updateTagStatus_updatesStatusWhenDifferent() {
        var tag = new Tag();
        tag.setStatus(TagStatus.PENDING);
        var publicId = tag.getPublicId();
        when(tagRepository.findByPublicId(publicId)).thenReturn(Optional.of(tag));

        var result = tagService.updateTagStatus(publicId, TagStatus.APPROVED);

        assertThat(result).isTrue();
        assertThat(tag.getStatus()).isEqualTo(TagStatus.APPROVED);
    }

    @Test
    void updateTagStatus_noopWhenStatusAlreadySame() {
        var tag = new Tag();
        tag.setStatus(TagStatus.APPROVED);
        var publicId = tag.getPublicId();
        when(tagRepository.findByPublicId(publicId)).thenReturn(Optional.of(tag));

        var result = tagService.updateTagStatus(publicId, TagStatus.APPROVED);

        assertThat(result).isTrue();
        assertThat(tag.getStatus()).isEqualTo(TagStatus.APPROVED);
    }

    @Test
    void updateTagStatus_throwsWhenTagNotFound() {
        var publicId = UUID.randomUUID();
        when(tagRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.updateTagStatus(publicId, TagStatus.APPROVED))
                .isInstanceOf(NoTagFoundException.class);
    }

    // =========================================================
    // updateTag
    // =========================================================

    @Test
    void updateTag_updatesExistingTranslationFields() {
        var tag = new Tag();
        tag.setSlug("vegan");
        tag.setType(TagType.DIETARY);
        tag.setScope(TagScope.BOTH);
        var existingTranslation = new TagTranslation();
        existingTranslation.setLanguage(english);
        existingTranslation.setLabel("Old Label");
        tag.setTranslations(new ArrayList<>(List.of(existingTranslation)));
        var publicId = tag.getPublicId();

        when(tagRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(tag));
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english));
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(anyMap(), eq("en"))).thenReturn(english);
        when(languageDomainBridgeService.toSimpleLanguagesMap(anyMap())).thenReturn(Map.of("en", "English"));
        when(languageDomainBridgeService.buildTranslatedLanguagesMap(anyList(), anyMap())).thenReturn(Map.of("en", "English"));

        var dto = new UpdateTagDto("vegan", TagType.DIETARY, TagScope.BOTH, new TagTranslationInputDto("en", "New Label"));

        var result = tagService.updateTag(publicId, dto);

        assertThat(tag.getTranslations()).hasSize(1);
        assertThat(existingTranslation.getLabel()).isEqualTo("New Label");
        assertThat(result.labelPreview()).isEqualTo("New Label");
    }

    @Test
    void updateTag_addsNewTranslationWhenLocaleMissing() {
        var tag = new Tag();
        tag.setSlug("vegan");
        tag.setType(TagType.DIETARY);
        tag.setScope(TagScope.BOTH);
        var existingTranslation = new TagTranslation();
        existingTranslation.setLanguage(english);
        existingTranslation.setLabel("Vegan");
        tag.setTranslations(new ArrayList<>(List.of(existingTranslation)));
        var publicId = tag.getPublicId();

        when(tagRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(tag));
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english, "it", italian));
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(anyMap(), eq("it"))).thenReturn(italian);
        when(languageDomainBridgeService.toSimpleLanguagesMap(anyMap())).thenReturn(Map.of("en", "English", "it", "Italian"));
        when(languageDomainBridgeService.buildTranslatedLanguagesMap(anyList(), anyMap())).thenReturn(Map.of("en", "English", "it", "Italian"));

        var dto = new UpdateTagDto("vegan", TagType.DIETARY, TagScope.BOTH, new TagTranslationInputDto("it", "Vegano"));

        var result = tagService.updateTag(publicId, dto);

        assertThat(tag.getTranslations()).hasSize(2);
        assertThat(result.labelPreview()).isEqualTo("Vegano");
    }

    @Test
    void updateTag_updatesSlugTypeScopeWhenChanged() {
        var tag = new Tag();
        tag.setSlug("vegan");
        tag.setType(TagType.DIETARY);
        tag.setScope(TagScope.BOTH);
        var existingTranslation = new TagTranslation();
        existingTranslation.setLanguage(english);
        existingTranslation.setLabel("Vegan");
        tag.setTranslations(new ArrayList<>(List.of(existingTranslation)));
        var publicId = tag.getPublicId();

        when(tagRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(tag));
        when(tagRepository.existsBySlug("vegan-new")).thenReturn(false);
        when(languageDomainBridgeService.getActiveLanguageEntitiesMap()).thenReturn(Map.of("en", english));
        when(languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(anyMap(), eq("en"))).thenReturn(english);
        when(languageDomainBridgeService.toSimpleLanguagesMap(anyMap())).thenReturn(Map.of("en", "English"));
        when(languageDomainBridgeService.buildTranslatedLanguagesMap(anyList(), anyMap())).thenReturn(Map.of("en", "English"));

        var dto = new UpdateTagDto("vegan-new", TagType.FLAVOR, TagScope.RECIPE, new TagTranslationInputDto("en", "Vegan"));

        tagService.updateTag(publicId, dto);

        assertThat(tag.getSlug()).isEqualTo("vegan-new");
        assertThat(tag.getType()).isEqualTo(TagType.FLAVOR);
        assertThat(tag.getScope()).isEqualTo(TagScope.RECIPE);
    }

    @Test
    void updateTag_throwsWhenNewSlugAlreadyExists() {
        var tag = new Tag();
        tag.setSlug("vegan");
        var publicId = tag.getPublicId();

        when(tagRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.of(tag));
        when(tagRepository.existsBySlug("spicy")).thenReturn(true);

        var dto = new UpdateTagDto("spicy", TagType.FLAVOR, TagScope.BOTH, new TagTranslationInputDto("en", "Spicy"));

        assertThatThrownBy(() -> tagService.updateTag(publicId, dto))
                .isInstanceOf(TagSlugAlreadyExistsException.class);

        verifyNoInteractions(languageDomainBridgeService);
    }

    @Test
    void updateTag_throwsWhenTagNotFound() {
        var publicId = UUID.randomUUID();
        when(tagRepository.findByPublicIdWithAllTranslation(publicId)).thenReturn(Optional.empty());

        var dto = new UpdateTagDto("vegan", TagType.DIETARY, TagScope.BOTH, new TagTranslationInputDto("en", "Vegan"));

        assertThatThrownBy(() -> tagService.updateTag(publicId, dto))
                .isInstanceOf(NoTagFoundException.class);
    }

    // =========================================================
    // deleteTag
    // =========================================================

    @Test
    void deleteTag_deletesAndReturnsPublicId() {
        var tag = new Tag();
        var publicId = tag.getPublicId();
        when(tagRepository.findByPublicId(publicId)).thenReturn(Optional.of(tag));

        var deletedPublicId = tagService.deleteTag(publicId);

        assertThat(deletedPublicId).isEqualTo(publicId);
        verify(tagRepository).delete(tag);
    }

    @Test
    void deleteTag_throwsWhenTagNotFound() {
        var publicId = UUID.randomUUID();
        when(tagRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.deleteTag(publicId))
                .isInstanceOf(NoTagFoundException.class);

        verify(tagRepository, never()).delete(any());
    }
}
