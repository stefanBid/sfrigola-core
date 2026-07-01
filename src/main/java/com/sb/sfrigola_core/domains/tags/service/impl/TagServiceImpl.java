package com.sb.sfrigola_core.domains.tags.service.impl;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.util.SCAuthenticationUtils;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.tags.dto.TagDto;
import com.sb.sfrigola_core.domains.tags.dto.admin.*;
import com.sb.sfrigola_core.domains.tags.dto.contributor.TagSuggestDto;
import com.sb.sfrigola_core.domains.tags.entity.Tag;
import com.sb.sfrigola_core.domains.tags.entity.TagTranslation;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
import com.sb.sfrigola_core.domains.tags.exception.DuplicateTagLocaleException;
import com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException;
import com.sb.sfrigola_core.domains.tags.exception.TagLabelAlreadyExistsException;
import com.sb.sfrigola_core.domains.tags.exception.TagLanguageNotActiveException;
import com.sb.sfrigola_core.domains.tags.exception.TagSlugAlreadyExistsException;
import com.sb.sfrigola_core.domains.tags.models.TagSpecificFilter;
import com.sb.sfrigola_core.domains.tags.repository.ITagRepository;
import com.sb.sfrigola_core.domains.tags.service.ITagService;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import com.sb.sfrigola_core.config.security.exception.ex.SCAuthenticatedUserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
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
    public SCPagedResult<TagDto> getAll(SCFilterQuery<Void> filterQuery, String locale) {
        var pageable = SCPaginationUtils.toPageable(filterQuery);

        // Step 1: Fetch IDs of Tags for the given locale — public endpoint returns only approved tags
        var tagIds = tagRepository.findIdsByFiltersAndLocale(locale, filterQuery.searchKey(), TagStatus.APPROVED.getValue(), null, null, pageable);

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
        var pageable = SCPaginationUtils.toPageable(filterQuery);
        var totalActiveLanguages = languageDomainBridgeService.getAllActiveLanguages().size();

        // LOCALE SWITCHER
        // CASE: locale is null → all tags returned; preview = first translation in collection
        // CASE: locale has value → only tags with a translation for that locale; preview = that specific translation
        var filter = filterQuery.other();
        var status = filter != null && filter.status() != null ? filter.status().getValue() : null;
        var scope  = filter != null && filter.scope()  != null ? filter.scope().getValue()  : null;
        var type   = filter != null && filter.type()   != null ? filter.type().getValue()   : null;
        var tagIds = locale != null
                ? tagRepository.findIdsByFiltersAndLocale(locale, filterQuery.searchKey(), status, scope, type, pageable)
                : tagRepository.findIdsByFilters(filterQuery.searchKey(), status, scope, type, pageable);

        if (tagIds.hasContent()) {
            var ids = tagIds.getContent();
            Map<Long, Tag> byId = tagRepository.findByIdsWithAllTranslations(ids)
                    .stream().collect(Collectors.toMap(Tag::getId, t -> t));
            List<Tag> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();

            return new SCPagedResult<>(
                    ordered.stream().map(tag -> {
                        TagTranslation tagTranslation;
                        if (locale != null)
                            tagTranslation = tag.getTranslations().stream()
                                    .filter(t -> t.getLanguage().getCode().equals(locale))
                                    .findFirst().orElse(null);
                        else
                            tagTranslation = tag.getTranslations().stream().findFirst().orElse(null);

                        String translationLabelPreview = tagTranslation != null ? tagTranslation.getLabel() : null;
                        return toAdminDto(tag, translationLabelPreview, totalActiveLanguages);
                    }).toList(),
                    SCPaginationUtils.toPagedOption(tagIds)
            );
        }
        return SCPagedResult.empty();
    }

    @Override
    public TagDetailsAdminDto getByPublicIdAdmin(UUID publicId) {
        var activeLanguages = new ArrayList<>(languageDomainBridgeService.getAllActiveLanguages());
        var tag = tagRepository.findByPublicId(publicId).orElseThrow(
                () -> new NoTagFoundException(publicId)
        );
        // Preparing Details for this Tag
        int totalLocalization = tag.getTranslations().size();
        int totalMissingLocalization = activeLanguages.size() - totalLocalization;
        ArrayList<TagTranslationDetailsAdminDto> missingTranslation = new ArrayList<>();

        // Populate missing Translation array only if there are missing languages
        if(totalMissingLocalization > 0){
            // Remove from the activeLanguages list all languages that already have a translation for this tag
            tag.getTranslations().forEach(t -> activeLanguages.removeIf(l -> l.code().equals(t.getLanguage().getCode())));
            activeLanguages.forEach(al -> missingTranslation.add(new TagTranslationDetailsAdminDto(al.code(), al.name(), null)));
        }
        return toAdminDetailsDto(tag,missingTranslation);
    }

    @Override
    @Transactional
    public TagPreviewAdminDto createNewTag(TagInputDto inputDto) {
        // Guard for existing slug
        if(tagRepository.existsBySlug(inputDto.slug()))
            throw new TagSlugAlreadyExistsException(inputDto.slug());

        // Guard for duplicate locale in input — fail fast before building entities
        Set<String> seenLocales = new HashSet<>();
        inputDto.translations().forEach(t -> {
            if(!seenLocales.add(t.langCode()))
                throw new DuplicateTagLocaleException(t.langCode());
        });

        Tag newTag = new Tag();

        var activeLanguageMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();
        List<TagTranslation> translations = inputDto.translations().stream()
                .map(t -> {
                    Language lang = activeLanguageMap.get(t.langCode());
                    if(lang == null) throw new TagLanguageNotActiveException(t.langCode());
                    return toTagTranslation(t, lang);
                })
                .collect(Collectors.toCollection(ArrayList::new));
        translations.forEach(t -> t.setTag(newTag));

        newTag.setSlug(inputDto.slug());
        newTag.setType(inputDto.type());
        newTag.setScope(inputDto.scope());
        newTag.setTranslations(translations);

        tagRepository.save(newTag);

        var dataForTranslationPreview = inputDto.translations().stream().findFirst().orElse(null);
        var labelPreview = dataForTranslationPreview != null ? dataForTranslationPreview.label() : null;

        return toAdminDto(newTag, labelPreview, activeLanguageMap.size());
    }

    @Override
    @Transactional
    public TagSuggestDto suggestNewTag(TagSuggestDto newTagSuggested) {
        // Obtain lang code from authUser
        Language lang = getLanguageOrThrow();

        // Check: suggested slug already exists in the system (case-insensitive)
        var existingTag = tagRepository.existsBySlug(newTagSuggested.slug());
        if(existingTag) throw new TagSlugAlreadyExistsException(newTagSuggested.slug());

        //Check: suggested label already exists in the system (case-insensitive)
        var existingLabel = tagRepository.existsByLabelAndLanguage(newTagSuggested.translationByConsumerLang(), lang.getCode());
        if(existingLabel) throw new TagLabelAlreadyExistsException(newTagSuggested.translationByConsumerLang(), lang.getCode());

        // Prepare Entities
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
    public boolean updateTagStatus(TagStatus newStatus, UUID publicId) {
        var existingTag = tagRepository.findByPublicId(publicId).orElseThrow(
                () -> new NoTagFoundException(publicId)
        );

        if(!newStatus.getValue().equals(existingTag.getStatus().getValue()))
            existingTag.setStatus(newStatus);
        return true;
    }

    @Override
    @Transactional
    public TagPreviewAdminDto updateTag(UUID publicId, TagInputDto inputDto) {
        var tagToUpdate = tagRepository.findByPublicIdWithAllTranslation(publicId).orElseThrow(
                () -> new NoTagFoundException(publicId)
        );

        // Check: new slug not already used by a different tag
        if(!inputDto.slug().equals(tagToUpdate.getSlug()) && tagRepository.existsBySlug(inputDto.slug()))
            throw new TagSlugAlreadyExistsException(inputDto.slug());

        // Check: duplicate locale in input — fail fast before touching any translation
        Set<String> seenLocales = new HashSet<>();
        inputDto.translations().forEach(t -> {
            if(!seenLocales.add(t.langCode())) throw new DuplicateTagLocaleException(t.langCode());
        });

        // Prepare Translation update
        var activeLanguagesMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();
        Map<String, TagTranslation> tagToUpdateTranslationMapped = tagToUpdate.getTranslations().stream()
                .collect(Collectors.toMap(t -> t.getLanguage().getCode(), t-> t));

        List<TagTranslation> toRemove = new ArrayList<>();

        for(TagTranslationInputDto input : inputDto.translations()) {
            Language lang = activeLanguagesMap.get(input.langCode());
            if(lang == null) throw new TagLanguageNotActiveException(input.langCode());

            boolean deleteSignal = input.label() == null || input.label().isBlank();
            TagTranslation extractedTagTranslation = tagToUpdateTranslationMapped.get(input.langCode());
            if(deleteSignal){
                if(extractedTagTranslation != null) toRemove.add(extractedTagTranslation);
            } else if (extractedTagTranslation != null) {
                // Update existing translation
                extractedTagTranslation.setLabel(input.label());
            } else {
                // Create new translation
                TagTranslation newTranslation = new TagTranslation();
                newTranslation.setTag(tagToUpdate);
                newTranslation.setLanguage(lang);
                newTranslation.setLabel(input.label());
                tagToUpdate.getTranslations().add(newTranslation);

            }
        }

        tagToUpdate.getTranslations().removeAll(toRemove);
        tagToUpdate.setSlug(inputDto.slug());
        tagToUpdate.setType(inputDto.type());
        tagToUpdate.setScope(inputDto.scope());

        var labelPreview = tagToUpdate.getTranslations().stream().findFirst().map(TagTranslation::getLabel).orElse(null);

        return toAdminDto(tagToUpdate, labelPreview, activeLanguagesMap.size());
    }

    @Override
    @Transactional
    public TagPreviewAdminDto deleteTag(UUID publicId) {
        var tagToDelete = tagRepository.findByPublicIdWithAllTranslation(publicId).orElseThrow(
                () -> new NoTagFoundException(publicId)
        );

        var totalActiveLanguages = languageDomainBridgeService.getAllActiveLanguages().size();
        var labelPreview = tagToDelete.getTranslations().stream().findFirst().map(TagTranslation::getLabel).orElse(null);

        tagRepository.delete(tagToDelete);
        return toAdminDto(tagToDelete, labelPreview, totalActiveLanguages);
    }

    // =========================================================
    // PRIVATE
    // =========================================================

    private @NonNull Language getLanguageOrThrow() {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();
        if(authUser.preferredLang() == null){
            throw new SCAuthenticatedUserNotFoundException("Only authenticated users can suggest new tags");
        }
        // Check: preferredLang is active in the system
        var activeLanguagesMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();
        Language lang = activeLanguagesMap.get(authUser.preferredLang());
        if(lang == null)
            throw new TagLanguageNotActiveException(authUser.preferredLang());
        return lang;
    }

    private TagDto toDto(Tag tag) {
        // We have only one item in the list of translations — the first query filters by locale
        var translation = tag.getTranslations().getFirst();
        return new TagDto(
                tag.getPublicId(),
                tag.getSlug(),
                translation.getLabel()
        );
    }

    private TagPreviewAdminDto toAdminDto(Tag tag, String translationLabelPreview, int totalActiveLanguages) {
        var translationCount = tag.getTranslations().size();
        return new TagPreviewAdminDto(
                tag.getPublicId(),
                tag.getSlug(),
                tag.getType(),
                tag.getScope(),
                tag.getStatus(),
                translationLabelPreview,
                translationCount,
                totalActiveLanguages - translationCount
        );
    }

    private TagDetailsAdminDto toAdminDetailsDto(Tag tag, ArrayList<TagTranslationDetailsAdminDto> missingTranslation) {
        var extractLabelPreview = tag.getTranslations().stream().findFirst().map(TagTranslation::getLabel).orElse(null);
        return new TagDetailsAdminDto(
                tag.getPublicId(),
                tag.getSlug(),
                tag.getType(),
                tag.getScope(),
                tag.getStatus(),
                extractLabelPreview,
                tag.getTranslations().stream().map(this::toTagTranslationDetails).toList(),
                missingTranslation
        );
    }


    private TagTranslation toTagTranslation(TagTranslationInputDto translationInputDto, Language lang) {
        TagTranslation translation = new TagTranslation();
        translation.setLanguage(lang);
        translation.setLabel(translationInputDto.label());
        return translation;
    }

    private TagTranslationDetailsAdminDto toTagTranslationDetails(TagTranslation translation) {
        return new TagTranslationDetailsAdminDto(
                translation.getLanguage().getCode(),
                translation.getLanguage().getName(),
                translation.getLabel()
        );
    }
}