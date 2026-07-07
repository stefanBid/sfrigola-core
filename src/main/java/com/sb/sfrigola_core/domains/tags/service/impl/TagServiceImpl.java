package com.sb.sfrigola_core.domains.tags.service.impl;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.tags.dto.input.AddTagDto;
import com.sb.sfrigola_core.domains.tags.dto.input.SuggestTagDto;
import com.sb.sfrigola_core.domains.tags.dto.input.TagTranslationInputDto;
import com.sb.sfrigola_core.domains.tags.dto.input.UpdateTagDto;
import com.sb.sfrigola_core.domains.tags.dto.view.TagDetailsAdminDto;
import com.sb.sfrigola_core.domains.tags.dto.view.TagDto;
import com.sb.sfrigola_core.domains.tags.dto.view.TagPreviewAdminDto;
import com.sb.sfrigola_core.domains.tags.entity.Tag;
import com.sb.sfrigola_core.domains.tags.entity.TagTranslation;
import com.sb.sfrigola_core.domains.tags.enums.TagSortField;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
import com.sb.sfrigola_core.domains.tags.exception.DuplicateTagLocaleException;
import com.sb.sfrigola_core.domains.tags.exception.MissingTagLocalesException;
import com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException;
import com.sb.sfrigola_core.domains.tags.exception.TagLabelAlreadyExistsException;
import com.sb.sfrigola_core.domains.tags.exception.TagSlugAlreadyExistsException;
import com.sb.sfrigola_core.domains.tags.models.TagSpecificFilter;
import com.sb.sfrigola_core.domains.tags.repository.ITagRepository;
import com.sb.sfrigola_core.domains.tags.service.ITagService;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
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
public class TagServiceImpl implements ITagService {

    private final ITagRepository tagRepository;
    private final ILanguageDomainBridgeService languageDomainBridgeService;

    @Override
    public SCPagedResult<TagDto> getAll(SCFilterQuery<TagSpecificFilter> filterQuery, String locale) {
        languageDomainBridgeService.validateLocaleIsActiveOrThrow(locale);

        var pageable = SCPaginationUtils.toPageable(filterQuery);
        var filterOtherExtracted = filterQuery.other();
        var scope  = filterOtherExtracted != null && filterOtherExtracted.scope()  != null ? filterOtherExtracted.scope().getValue()  : null;

        // Step 1: Fetch IDs of Tags for the given locale — public endpoint returns only approved tags
        var tagIds = tagRepository.findIdsByFiltersAndLocaleAsc(locale, filterQuery.searchKey(), TagStatus.APPROVED.getValue(), scope, null, pageable);

        // Step 2: Fetch and restore the ordered ID sequence from step 1
        if (tagIds.hasContent()) {
            var ids = tagIds.getContent();
            Map<Long, Tag> byId = tagRepository.findByIdsWithSpecificTranslation(ids, locale)
                    .stream().collect(Collectors.toMap(Tag::getId, t -> t));
            List<Tag> orderedTags = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
            return new SCPagedResult<>(
                    orderedTags.stream().map(this::toDto).toList(),
                    SCPaginationUtils.toPagedOption(tagIds)
            );
        }
        return SCPagedResult.empty();
    }

    @Override
    public SCPagedResult<TagPreviewAdminDto> getAllAdmin(SCFilterQuery<TagSpecificFilter> filterQuery, String locale) {
        Map<String, String> activeLanguagesSimpleMap = languageDomainBridgeService.getAllActiveLanguagesSimpleMap();

        // LOCALE CHECK
        languageDomainBridgeService.validateLocaleIsActiveByActiveLanguagesMapKeysOrThrow(activeLanguagesSimpleMap.keySet(), locale);

        // SORT BY SWITCHER
        // CASE: SortBy is null or is LABEL  use forced group-by on label (in another table)
        // CASE: SortBy is not null and not LABEL use Pageable Sort  because these fields are present in the entity
        boolean isSortByLabel = filterQuery.sortBy() == null ||  filterQuery.sortBy().getEntityFieldName().equals(TagSortField.LABEL.getEntityFieldName());

        // SORT SWITCHER
        // CASE: Sort is ASC call query with GOUP-BY ASC
        // CASE: Sort is DESC call query with GOUP-BY DESC
        boolean descending = filterQuery.sort() != null && !filterQuery.sort().isAsc();

        // STEP 1: Obtain ids
        var filterOtherExtracted = filterQuery.other();

        var status = filterOtherExtracted != null && filterOtherExtracted.status() != null ? filterOtherExtracted.status().getValue() : null;
        var scope  = filterOtherExtracted != null && filterOtherExtracted.scope()  != null ? filterOtherExtracted.scope().getValue()  : null;
        var type   = filterOtherExtracted != null && filterOtherExtracted.type()   != null ? filterOtherExtracted.type().getValue()   : null;

        var pageable = SCPaginationUtils.toPageable(filterQuery, isSortByLabel);
        Page<Long> tagIds;

        if (isSortByLabel){
            if (descending)
                tagIds = tagRepository.findIdsByFiltersAndLocaleDesc(locale, filterQuery.searchKey(), status, scope, type, pageable);
            else
                tagIds = tagRepository.findIdsByFiltersAndLocaleAsc(locale, filterQuery.searchKey(), status, scope, type, pageable);
        } else {
            tagIds = tagRepository.findIdsByFiltersAndLocaleOtherSort(locale, filterQuery.searchKey(), status, scope, type, pageable);
        }

        if (tagIds.hasContent()) {
            var ids = tagIds.getContent();
            Map<Long, Tag> byId = tagRepository.findByIdsWithAllTranslations(ids)
                    .stream().collect(Collectors.toMap(Tag::getId, t -> t));
            List<Tag> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();

            return new SCPagedResult<>(
                    ordered.stream().map(tag -> {
                        TagTranslation tagTranslation = tag.getTranslations().stream()
                                .filter(t -> t.getLanguage().getCode().equals(locale))
                                .findFirst().orElse(null);

                        return toAdminDto(tag, tagTranslation, activeLanguagesSimpleMap);
                    }).toList(),
                    SCPaginationUtils.toPagedOption(tagIds)
            );
        }
        return SCPagedResult.empty();
    }

    @Override
        public TagDetailsAdminDto getByPublicIdAdmin(UUID publicId, @NonNull String locale) {
        languageDomainBridgeService.validateLocaleIsActiveOrThrow(locale);

        // ID CHECK:
        var tag = tagRepository.findByPublicId(publicId).orElseThrow(
                () -> new NoTagFoundException(publicId)
        );

        var tagTranslation = tag.getTranslations().stream()
                .filter(t -> t.getLanguage().getCode().equals(locale))
                .findFirst().orElse(null);

        return toAdminDetailsDto(tag, tagTranslation);
    }

    @Override
    @Transactional
    public TagPreviewAdminDto createNewTag(AddTagDto addTagDto, @NonNull String locale) {
        // SLUG CHECK:
        if(tagRepository.existsBySlug(addTagDto.slug()))
            throw new TagSlugAlreadyExistsException(addTagDto.slug());

        // TRANSLATION CHECKS:
        // 1) No duplicated translation
        // 2) A new tag must have all active languages covered in translation, otherwise it is not valid
        var activeLanguageMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();
        var activeCodeSet = activeLanguageMap.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());

        Set<String> seenLocales = new HashSet<>();
        addTagDto.translations().forEach(t -> {
            if(!seenLocales.add(t.langCode()))
                throw new DuplicateTagLocaleException(t.langCode());
        });

        if(!activeCodeSet.containsAll(seenLocales) || activeCodeSet.size() != seenLocales.size())
            throw new MissingTagLocalesException();

        Tag newTag = new Tag();

        List<TagTranslation> translations = addTagDto.translations().stream()
                .map(t -> {
                    Language lang = languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguageMap, t.langCode());
                    return toTagTranslation(t, lang);
                })
                .collect(Collectors.toCollection(ArrayList::new));
        translations.forEach(t -> t.setTag(newTag));

        newTag.setSlug(addTagDto.slug());
        newTag.setType(addTagDto.type());
        newTag.setScope(addTagDto.scope());
        newTag.setTranslations(translations);

        tagRepository.save(newTag);

        return toAdminDto(
                newTag,
                translations.stream().filter(t -> t.getLanguage().getCode().equals(locale)).findFirst().orElse(null),
                toSimpleLanguagesMap(activeLanguageMap)
        );
    }

    @Override
    @Transactional
    public SuggestTagDto suggestNewTag(SuggestTagDto newTagSuggested, @NonNull String locale) {
        // LANG RESOLVE:
        var activeLanguagesMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();
        Language lang = languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguagesMap, locale);

        // SLUG CHECK:
        var existingTag = tagRepository.existsBySlug(newTagSuggested.slug());
        if(existingTag) throw new TagSlugAlreadyExistsException(newTagSuggested.slug());

        // LABEL CHECK:
        var existingLabel = tagRepository.existsByLabelAndLanguage(newTagSuggested.translationByConsumerLang(), lang.getCode());
        if(existingLabel) throw new TagLabelAlreadyExistsException(newTagSuggested.translationByConsumerLang(), lang.getCode());

        Tag newSuggestedTag = new Tag();
        TagTranslation newSuggestedTagTranslation = new TagTranslation();

        newSuggestedTagTranslation.setTag(newSuggestedTag);
        newSuggestedTagTranslation.setLanguage(lang);
        newSuggestedTagTranslation.setLabel(newTagSuggested.translationByConsumerLang());

        newSuggestedTag.setSlug(newTagSuggested.slug());
        newSuggestedTag.setType(newTagSuggested.type());
        newSuggestedTag.setScope(newTagSuggested.scope());
        newSuggestedTag.setStatus(TagStatus.PENDING);
        newSuggestedTag.setTranslations(new ArrayList<>(List.of(newSuggestedTagTranslation)));
        
        tagRepository.save(newSuggestedTag);

        return newTagSuggested;
    }

    @Override
    @Transactional
    public boolean updateTagStatus(UUID publicId, TagStatus newStatus) {
        // ID CHECK:
        var existingTag = tagRepository.findByPublicId(publicId).orElseThrow(
                () -> new NoTagFoundException(publicId)
        );

        if(!newStatus.getValue().equals(existingTag.getStatus().getValue()))
            existingTag.setStatus(newStatus);
        return true;
    }

    @Override
    @Transactional
    public TagPreviewAdminDto updateTag(UUID publicId, UpdateTagDto updateTagDto) {
        // ID CHECK:
        var tagToUpdate = tagRepository.findByPublicIdWithAllTranslation(publicId).orElseThrow(
                () -> new NoTagFoundException(publicId)
        );

        // SLUG CHECK:
        if(!updateTagDto.slug().equals(tagToUpdate.getSlug()) && tagRepository.existsBySlug(updateTagDto.slug()))
            throw new TagSlugAlreadyExistsException(updateTagDto.slug());

        // TRANSLATION CHECK: Update translation is of an active locale
        var activeLangMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();
        Language lang = languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLangMap, updateTagDto.specificTranslation().langCode());

        var tagTranslationToUpdate = tagToUpdate.getTranslations().stream()
                .filter(t -> t.getLanguage().getCode().equals(updateTagDto.specificTranslation().langCode()))
                .findFirst().orElse(null);

        if(tagTranslationToUpdate == null) {
            // New Translation
            tagTranslationToUpdate = new TagTranslation();
            tagTranslationToUpdate.setTag(tagToUpdate);
            tagTranslationToUpdate.setLanguage(lang);
            tagTranslationToUpdate.setLabel(updateTagDto.specificTranslation().label());
            tagToUpdate.getTranslations().add(tagTranslationToUpdate);
        } else if (!tagTranslationToUpdate.getLabel().equals(updateTagDto.specificTranslation().label())) {
            // Update existing translation — only touch fields that actually changed
            tagTranslationToUpdate.setLabel(updateTagDto.specificTranslation().label());
        }

        if(!tagToUpdate.getSlug().equals(updateTagDto.slug()))
            tagToUpdate.setSlug(updateTagDto.slug());
        if(!tagToUpdate.getType().equals(updateTagDto.type()))
            tagToUpdate.setType(updateTagDto.type());
        if(!tagToUpdate.getScope().equals(updateTagDto.scope()))
            tagToUpdate.setScope(updateTagDto.scope());

        return toAdminDto(tagToUpdate, tagTranslationToUpdate, toSimpleLanguagesMap(activeLangMap));
    }

    @Override
    @Transactional
    public UUID deleteTag(UUID publicId) {
        // ID CHECK:
        var tagToDelete = tagRepository.findByPublicId(publicId).orElseThrow(
                () -> new NoTagFoundException(publicId)
        );

        tagRepository.delete(tagToDelete);
        return tagToDelete.getPublicId();
    }

    // =========================================================
    // PRIVATE
    // =========================================================

    private TagDto toDto(Tag tag) {
        // We have only one item in the list of translations — the first query filters by locale
        var translation = tag.getTranslations().getFirst();
        return new TagDto(
                tag.getPublicId(),
                tag.getSlug(),
                translation.getLabel()
        );
    }

    private TagPreviewAdminDto toAdminDto(Tag tag, TagTranslation tagTranslation, Map<String, String> activeLanguagesMap) {
        Map<String, String> translatedLanguages = tag.getTranslations().stream()
                .map(t -> t.getLanguage().getCode())
                .filter(activeLanguagesMap::containsKey)
                .collect(Collectors.toMap(code -> code, activeLanguagesMap::get));

        String translationLabelPreview = tagTranslation != null ? tagTranslation.getLabel() : null;
        return new TagPreviewAdminDto(
                tag.getPublicId(),
                tag.getSlug(),
                tag.getType(),
                tag.getScope(),
                tag.getStatus(),
                translationLabelPreview,
                translatedLanguages
        );
    }

    private Map<String, String> toSimpleLanguagesMap(Map<String, Language> languageEntitiesMap) {
        return languageEntitiesMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));
    }

    private TagDetailsAdminDto toAdminDetailsDto(Tag tag, @Nullable TagTranslation tagTranslation) {
        var extractLabelPreview = tagTranslation != null ? tagTranslation.getLabel() : null;
        return new TagDetailsAdminDto(
                tag.getPublicId(),
                tag.getSlug(),
                tag.getType(),
                tag.getScope(),
                tag.getStatus(),
                extractLabelPreview
        );
    }


    private TagTranslation toTagTranslation(TagTranslationInputDto translationInputDto, Language lang) {
        TagTranslation translation = new TagTranslation();
        translation.setLanguage(lang);
        translation.setLabel(translationInputDto.label());
        return translation;
    }
}