package com.sb.sfrigola_core.domains.tags.service;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.domains.tags.dto.TagDto;
import com.sb.sfrigola_core.domains.tags.dto.admin.TagDetailsAdminDto;
import com.sb.sfrigola_core.domains.tags.dto.admin.TagPreviewAdminDto;
import com.sb.sfrigola_core.domains.tags.dto.consumer.TagSuggestDto;
import com.sb.sfrigola_core.domains.tags.models.TagSpecificFilter;
import jakarta.annotation.Nullable;

import java.util.UUID;

/**
 * Controller-facing contract for tag read operations.
 * Reads the security context internally where needed.
 * All methods follow the "succeed or throw" contract.
 */
public interface ITagService {

    /**
     * Returns a paginated list of tags with their translated label for the requested locale,
     * ordered alphabetically by label. Optionally filters by a search keyword matched against the label.
     *
     * @param filterQuery pagination parameters and optional {@code searchKey} (matched case-insensitively against the label)
     * @param locale      BCP-47 locale code used to select the translation (e.g. {@code "en"}, {@code "it"})
     * @return paginated result wrapping a list of {@link TagDto}
     */
    SCPagedResult<TagDto> getAll(SCFilterQuery<Void> filterQuery, String locale);

    /**
     * Returns a paginated admin preview of tags, including localization coverage counts
     * (present and missing) to support CMS overviews.
     * <p>
     * When {@code locale} is {@code null}, all tags are returned and the translation preview
     * is taken from the first available translation in the collection.
     * When {@code locale} is provided, only tags that have a translation for that locale
     * are returned and the preview uses that specific translation.
     *
     * @param filterQuery pagination parameters and optional {@code searchKey} (matched case-insensitively against the label)
     * @param locale      BCP-47 locale code for filtering and preview selection; {@code null} returns all tags
     * @return paginated result wrapping a list of {@link TagPreviewAdminDto}
     */
    SCPagedResult<TagPreviewAdminDto> getAllAdmin(SCFilterQuery<TagSpecificFilter> filterQuery, @Nullable String locale);

    /**
     * Returns full admin details for a single tag, including every existing translation
     * and the list of active languages still missing a translation.
     *
     * @param publicId public identifier of the tag
     * @return {@link TagDetailsAdminDto} with translation coverage details
     * @throws com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException if no tag with the given public ID exists
     */
    TagDetailsAdminDto getByPublicIdAdmin(UUID publicId);

    /**
     * Registers a new tag proposed by an authenticated user, in {@code PENDING} status,
     * with a single translation in the user's preferred language. Reads the authenticated
     * user from the security context to resolve the target language.
     *
     * @param newTagSuggested slug, type, scope and the label to translate in the user's preferred language
     * @return the same {@link TagSuggestDto} received, once persisted
     * @throws com.sb.sfrigola_core.config.security.exception.ex.SCAuthenticatedUserNotFoundException if no authenticated user is present in the security context
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagLanguageNotActiveException if the user's preferred language is not active in the system
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagSlugAlreadyExistsException if a tag with the same slug already exists
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagLabelAlreadyExistsException if a tag with the same label already exists in that language
     */
    TagSuggestDto suggestNewTag(TagSuggestDto newTagSuggested);
}