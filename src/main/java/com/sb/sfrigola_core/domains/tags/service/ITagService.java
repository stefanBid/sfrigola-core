package com.sb.sfrigola_core.domains.tags.service;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.domains.languages.exception.LocaleNotActiveException;
import com.sb.sfrigola_core.domains.tags.dto.input.AddTagDto;
import com.sb.sfrigola_core.domains.tags.dto.input.SuggestTagDto;
import com.sb.sfrigola_core.domains.tags.dto.input.UpdateTagDto;
import com.sb.sfrigola_core.domains.tags.dto.view.TagDetailsAdminDto;
import com.sb.sfrigola_core.domains.tags.dto.view.TagDto;
import com.sb.sfrigola_core.domains.tags.dto.view.TagPreviewAdminDto;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
import com.sb.sfrigola_core.domains.tags.models.TagSpecificFilter;
import jakarta.annotation.Nullable;
import org.jspecify.annotations.NonNull;

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
     * @throws LocaleNotActiveException
     *         if {@code locale} does not match an active language (thrown by the languages domain bridge)
     */
    SCPagedResult<TagDto> getAll(SCFilterQuery<TagSpecificFilter> filterQuery, String locale);

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
     * @throws LocaleNotActiveException
     *         if {@code locale} does not match an active language (thrown by the languages domain bridge)
     */
    SCPagedResult<TagPreviewAdminDto> getAllAdmin(SCFilterQuery<TagSpecificFilter> filterQuery, @Nullable String locale);

    /**
     * Returns admin details for a single tag, previewing its translation for the requested locale.
     * <p>
     * {@code locale} is mandatory and never filters — it only selects which translation is
     * returned. If no translation exists for {@code locale}, {@code specificTranslationLabel} is {@code null}.
     *
     * @param publicId public identifier of the tag
     * @param locale   BCP-47 locale code used to select the translation for the preview label; never {@code null}
     * @return {@link TagDetailsAdminDto} with the translation preview for {@code locale}
     * @throws com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException if no tag with the given public ID exists
     * @throws LocaleNotActiveException
     *         if {@code locale} does not match an active language (thrown by the languages domain bridge)
     */
    TagDetailsAdminDto getByPublicIdAdmin(UUID publicId, @NonNull String locale);

    /**
     * Creates a new tag directly in {@code APPROVED} status (admin-authored tags are pre-approved,
     * unlike contributor suggestions which start {@code PENDING}).
     * Translations provided in {@code input} are inserted as-is; no merge occurs on create.
     *
     * @param addTagDto creation payload — slug, type, scope and at least one translation
     * @param locale    BCP-47 language code used for the label preview field; never {@code null}.
     *                  Only selects which translation is used for the preview — if no translation
     *                  exists for {@code locale}, the preview label is {@code null}
     * @return admin preview of the newly created tag
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagSlugAlreadyExistsException if a tag with the same slug already exists
     * @throws com.sb.sfrigola_core.domains.tags.exception.DuplicateTagLocaleException if the same locale appears more than once in {@code input}
     * @throws LocaleNotActiveException
     *         if a translation references a language that is not active (thrown by the languages domain bridge)
     * @throws com.sb.sfrigola_core.domains.tags.exception.MissingTagLocalesException
     *         if {@code input.translations()} does not cover all active languages
     */
    TagPreviewAdminDto createNewTag(AddTagDto addTagDto, @NonNull String locale);

    /**
     * Registers a new tag proposed by an authenticated user, in {@code PENDING} status,
     * with a single translation in the given locale. Authentication is enforced upstream
     * by the security filter chain (contributor path); this method does not read the
     * security context.
     *
     * @param suggestedTagDto slug, type, scope and the label to translate
     * @param locale          BCP-47 language code the label is written in; never {@code null}
     * @return the same {@link SuggestTagDto} received, once persisted
     * @throws LocaleNotActiveException
     *         if {@code locale} does not match an active language (thrown by the languages domain bridge)
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagSlugAlreadyExistsException if a tag with the same slug already exists
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagLabelAlreadyExistsException if a tag with the same label already exists in that language
     */
    SuggestTagDto suggestNewTag(SuggestTagDto suggestedTagDto, @NonNull String locale);

    /**
     * Updates the status of an existing tag (e.g. resolving a pending suggestion to
     * {@code APPROVED} or {@code REJECTED}). No-op if the tag already has the requested status.
     *
     * @param publicId  public identifier of the tag
     * @param newStatus target status to apply
     * @return {@code true} once the tag status is confirmed set to {@code newStatus}
     * @throws com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException if no tag with the given public ID exists
     */
    boolean updateTagStatus(UUID publicId, TagStatus newStatus);

    /**
     * Updates an existing tag's slug, type, scope and exactly one translation: {@code updateTagDto.specificTranslation()}
     * either relabels the translation for that locale if one already exists, or adds a new one. Only one
     * locale can be touched per call — to update multiple translations, call this method once per locale.
     *
     * @param publicId public identifier of the tag to update
     * @param updateTagDto new slug, type, scope, and the single translation to upsert
     * @return {@link TagPreviewAdminDto} reflecting the tag after the update, previewing the upserted translation
     * @throws com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException if no tag with the given public ID exists
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagSlugAlreadyExistsException if the new slug is already used by another tag
     * @throws LocaleNotActiveException
     *         if {@code updateTagDto.specificTranslation()} references a language that is not active (thrown by the languages domain bridge)
     */
    TagPreviewAdminDto updateTag(UUID publicId, UpdateTagDto updateTagDto);

    /**
     * Deletes a tag and all its translations. Rows in the {@code recipe_tags} / {@code ingredient_tags}
     * bridge tables referencing this tag are removed as well, via {@code CascadeType.ALL} / the
     * database's {@code ON DELETE CASCADE}.
     * No preview DTO is returned — the tag no longer exists, so only its identifier
     * is handed back (e.g. for the caller to remove it from a client-side list).
     *
     * @param publicId public identifier of the tag to delete
     * @return the {@code publicId} of the deleted tag
     * @throws com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException if no tag with the given public ID exists
     */
    UUID deleteTag(UUID publicId);

}