package com.sb.sfrigola_core.domains.tags.service;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.domains.tags.dto.TagDto;
import com.sb.sfrigola_core.domains.tags.dto.admin.TagDetailsAdminDto;
import com.sb.sfrigola_core.domains.tags.dto.admin.TagInputDto;
import com.sb.sfrigola_core.domains.tags.dto.admin.TagPreviewAdminDto;
import com.sb.sfrigola_core.domains.tags.dto.contributor.TagSuggestDto;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
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
     * Creates a new tag directly in {@code APPROVED} status (admin-authored tags are pre-approved,
     * unlike contributor suggestions which start {@code PENDING}).
     * Translations provided in {@code input} are inserted as-is; no merge occurs on create.
     *
     * @param input creation payload — slug, type, scope and at least one translation
     * @return admin preview of the newly created tag
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagSlugAlreadyExistsException if a tag with the same slug already exists
     * @throws com.sb.sfrigola_core.domains.tags.exception.DuplicateTagLocaleException if the same locale appears more than once in {@code input}
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagLanguageNotActiveException if a translation references a language that is not active
     */
    TagPreviewAdminDto createNewTag(TagInputDto input);

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

    /**
     * Updates the status of an existing tag (e.g. resolving a pending suggestion to
     * {@code APPROVED} or {@code REJECTED}). No-op if the tag already has the requested status.
     *
     * @param newStatus target status to apply
     * @param publicId  public identifier of the tag
     * @return {@code true} once the tag status is confirmed set to {@code newStatus}
     * @throws com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException if no tag with the given public ID exists
     */
    boolean updateTagStatus(TagStatus newStatus, UUID publicId);

    /**
     * Updates an existing tag's slug, type, scope and translations, merging the translation
     * set per locale rather than replacing it wholesale: a locale already present is relabeled,
     * a new locale is added, and a locale sent with a blank/{@code null} label is removed.
     *
     * @param publicId public identifier of the tag to update
     * @param input    new slug, type, scope and the per-locale translation changes to apply
     * @return {@link TagPreviewAdminDto} reflecting the tag after the update
     * @throws com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException if no tag with the given public ID exists
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagSlugAlreadyExistsException if the new slug is already used by another tag
     * @throws com.sb.sfrigola_core.domains.tags.exception.DuplicateTagLocaleException if the same locale appears more than once in {@code input}
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagLanguageNotActiveException if a translation references a language that is not active
     */
    TagPreviewAdminDto updateTag(UUID publicId, TagInputDto input);

    /**
     * Permanently deletes a tag and all of its translations (cascade). Rows in the
     * {@code recipe_tags} / {@code ingredient_tags} bridge tables referencing this tag
     * are removed as well, via the database's {@code ON DELETE CASCADE}.
     *
     * @param publicId public identifier of the tag to delete
     * @return {@link TagPreviewAdminDto} snapshot of the tag as it was right before deletion
     * @throws com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException if no tag with the given public ID exists
     */
    TagPreviewAdminDto deleteTag(UUID publicId);

}