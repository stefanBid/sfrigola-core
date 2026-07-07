package com.sb.sfrigola_core.domains.tags.controller;

import com.sb.sfrigola_core.common.constant.SCRequestParamValidationCodeConstants;
import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.dto.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.domains.tags.dto.input.AddTagDto;
import com.sb.sfrigola_core.domains.tags.dto.input.SuggestTagDto;
import com.sb.sfrigola_core.domains.tags.dto.input.UpdateTagDto;
import com.sb.sfrigola_core.domains.tags.dto.view.TagDetailsAdminDto;
import com.sb.sfrigola_core.domains.tags.dto.view.TagDto;
import com.sb.sfrigola_core.domains.tags.dto.view.TagPreviewAdminDto;
import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagSortField;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
import com.sb.sfrigola_core.domains.tags.enums.TagType;
import com.sb.sfrigola_core.domains.tags.models.TagSpecificFilter;
import com.sb.sfrigola_core.domains.tags.service.ITagService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
@Validated
public class TagController {

    private final ITagService tagService;

    // CONTRIBUTOR CONTROLLER

    @GetMapping(version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<List<TagDto>, SCPagedOptionDto>> getAllTags(
            @Min(value = 0, message = SCRequestParamValidationCodeConstants.PAGE_MUST_BE_AT_LEAST_ZERO)
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Min(value = 1, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_LEAST_ONE) @Max(value = 100, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_MOST_HUNDRED)
            @RequestParam(value = "take", required = false, defaultValue = "10") int take,
            @RequestParam(required = false) @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale,
            @RequestParam(value = "searchKey", required = false) String searchKey,
            @RequestParam(required = false) String scope
    ) {
        var tagSpecificFilter = new TagSpecificFilter(
                null,
                null,
                scope != null ? TagScope.fromValue(scope) : null
        );
        var filterQuery = SCFilterQuery.powerful(searchKey, null, null, take, page, tagSpecificFilter);
        var paginatedTags = tagService.getAll(filterQuery, locale);

        return ResponseEntity.ok(SCGeneralResponseDto.success(paginatedTags.content(), paginatedTags.pagedOptionDto()));
    }

    @PostMapping(value = "/suggest", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<SuggestTagDto, Void>> suggestNewTag(
            @RequestBody @Valid SuggestTagDto newTagSuggested,
            @RequestParam @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale
    ) {
        var suggested = tagService.suggestNewTag(newTagSuggested, locale);
        return ResponseEntity.ok(SCGeneralResponseDto.success(suggested));
    }

    // ADMIN CONTROLLER

    @GetMapping(value = "/admin", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<List<TagPreviewAdminDto>, SCPagedOptionDto>> getAllTagsAdmin(
            @Min(value = 0, message = SCRequestParamValidationCodeConstants.PAGE_MUST_BE_AT_LEAST_ZERO)
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Min(value = 1, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_LEAST_ONE) @Max(value = 100, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_MOST_HUNDRED)
            @RequestParam(value = "take", required = false, defaultValue = "10") int take,
            @RequestParam(value = "sort", required = false, defaultValue = "asc") String sort,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(required = false) String locale,
            @RequestParam(value = "searchKey", required = false) String searchKey,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String scope
    ) {
        var tagSpecificFilter = new TagSpecificFilter(
                status != null ? TagStatus.fromValue(status) : null,
                type != null ? TagType.fromValue(type) : null,
                scope != null ? TagScope.fromValue(scope) : null
        );
        var filterQuery = SCFilterQuery.powerful(searchKey, sortBy != null ? TagSortField.fromString(sortBy) : null, SortDirection.fromString(sort), take, page, tagSpecificFilter);
        var paginatedTags = tagService.getAllAdmin(filterQuery, locale);

        return ResponseEntity.ok(SCGeneralResponseDto.success(paginatedTags.content(), paginatedTags.pagedOptionDto()));
    }

    @GetMapping(value = "/admin/{publicId}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<TagDetailsAdminDto, Void>> getTagByPublicIdAdmin(
            @PathVariable("publicId") UUID publicId,
            @RequestParam @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale
    ){
      var tagDetails = tagService.getByPublicIdAdmin(publicId, locale);
      return ResponseEntity.ok(SCGeneralResponseDto.success(tagDetails));
    }

    @PostMapping(value = "/admin", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<TagPreviewAdminDto, Void>> createTag(
            @RequestBody @Valid AddTagDto addTagDto,
            @RequestParam @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale
    ) {
        var createdTag = tagService.createNewTag(addTagDto, locale);
        return ResponseEntity.ok(SCGeneralResponseDto.success(createdTag));
    }

    @PatchMapping(value = "/admin/{publicId}/status/{status}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<String, Void>> updateTagStatus(
            @PathVariable("publicId") UUID publicId,
            @PathVariable("status") String status
    ) {
        tagService.updateTagStatus(publicId, TagStatus.fromValue(status));
        return ResponseEntity.ok(SCGeneralResponseDto.successMutation("Tag status updated successfully"));
    }

    @PutMapping(value = "/admin/{publicId}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<TagPreviewAdminDto, Void>> updateTag(
            @PathVariable("publicId") UUID publicId,
            @RequestBody @Valid UpdateTagDto updateTagDto
    ) {
        var updated = tagService.updateTag(publicId, updateTagDto);
        return ResponseEntity.ok(SCGeneralResponseDto.success(updated));
    }

    @DeleteMapping(value = "/admin/{publicId}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<UUID, Void>> deleteTag(
            @PathVariable("publicId") UUID publicId
    ) {
        var deleted = tagService.deleteTag(publicId);
        return ResponseEntity.ok(SCGeneralResponseDto.success(deleted));
    }
}
