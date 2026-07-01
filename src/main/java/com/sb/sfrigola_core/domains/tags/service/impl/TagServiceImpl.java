package com.sb.sfrigola_core.domains.tags.service.impl;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.util.SCAuthenticationUtils;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.domains.tags.dto.TagDto;
import com.sb.sfrigola_core.domains.tags.dto.admin.TagDetailsAdminDto;
import com.sb.sfrigola_core.domains.tags.dto.admin.TagTranslationDetailsAdminDto;
import com.sb.sfrigola_core.domains.tags.dto.admin.TagPreviewAdminDto;
import com.sb.sfrigola_core.domains.tags.entity.Tag;
import com.sb.sfrigola_core.domains.tags.entity.TagTranslation;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
import com.sb.sfrigola_core.domains.tags.models.TagSpecificFilter;
import com.sb.sfrigola_core.domains.tags.repository.ITagRepository;
import com.sb.sfrigola_core.domains.tags.service.ITagService;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import lombok.RequiredArgsConstructor;
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
                () -> new RuntimeException("NoTagFoundException")
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
    public boolean suggestNewTag(String label) {
        // Obtain lang code from authUser
        var authUserPreferredLang = SCAuthenticationUtils.getAuthUserByContextHolder().preferredLang();
        if(authUserPreferredLang == null){
            throw new RuntimeException("UnauthorizedException: Only authenticated users can suggest new tags");
        }

        return false;
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


    private TagTranslationDetailsAdminDto toTagTranslationDetails(TagTranslation translation) {
        return new TagTranslationDetailsAdminDto(
                translation.getLanguage().getCode(),
                translation.getLanguage().getName(),
                translation.getLabel()
        );
    }
}