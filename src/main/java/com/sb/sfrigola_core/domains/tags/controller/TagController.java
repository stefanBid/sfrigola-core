package com.sb.sfrigola_core.domains.tags.controller;

import com.sb.sfrigola_core.common.constant.SCRequestParamValidationCodeConstants;
import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.dto.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.domains.tags.dto.TagDto;
import com.sb.sfrigola_core.domains.tags.dto.admin.TagDetailsAdminDto;
import com.sb.sfrigola_core.domains.tags.dto.admin.TagPreviewAdminDto;
import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
import com.sb.sfrigola_core.domains.tags.enums.TagType;
import com.sb.sfrigola_core.domains.tags.models.TagSpecificFilter;
import com.sb.sfrigola_core.domains.tags.service.ITagService;
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

    @GetMapping(version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<List<TagDto>, SCPagedOptionDto>> getAllTags(
            @Min(value = 0, message = SCRequestParamValidationCodeConstants.PAGE_MUST_BE_AT_LEAST_ZERO)
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Min(value = 1, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_LEAST_ONE) @Max(value = 100, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_MOST_HUNDRED)
            @RequestParam(value = "take", required = false, defaultValue = "10") int take,
            @RequestParam(required = false) @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale,
            @RequestParam(value = "searchKey", required = false) String searchKey
    ) {

        var filterQuery = SCFilterQuery.pageWithSearch(searchKey, take, page);
        var paginatedTags = tagService.getAll(filterQuery, locale);

        return ResponseEntity.ok(SCGeneralResponseDto.success(paginatedTags.content(), paginatedTags.pagedOptionDto()));
    }

    // ADMIN-CONTRIBUTOR CONTROLLER

    // ADMIN CONTROLLER

    @GetMapping(value = "/admin", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<List<TagPreviewAdminDto>, SCPagedOptionDto>> getAllTagsAdmin(
            @Min(value = 0, message = SCRequestParamValidationCodeConstants.PAGE_MUST_BE_AT_LEAST_ZERO)
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Min(value = 1, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_LEAST_ONE) @Max(value = 100, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_MOST_HUNDRED)
            @RequestParam(value = "take", required = false, defaultValue = "10") int take,
            @RequestParam(required = false) String locale,
            @RequestParam(value = "searchKey", required = false) String searchKey,
            @RequestParam(required = false) TagStatus status,
            @RequestParam(required = false) TagType type,
            @RequestParam(required = false) TagScope scope
    ) {
        var filterQuery = SCFilterQuery.powerful(searchKey, null, null, take, page, new TagSpecificFilter(status, type, scope));
        var paginatedTags = tagService.getAllAdmin(filterQuery, locale);

        return ResponseEntity.ok(SCGeneralResponseDto.success(paginatedTags.content(), paginatedTags.pagedOptionDto()));
    }

    @GetMapping(value = "/admin/{publicId}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<TagDetailsAdminDto, Void>> getTagByPublicIdAdmin(
            @PathVariable("publicId") UUID publicId
    ){
      var tagDetails = tagService.getByPublicIdAdmin(publicId);
      return ResponseEntity.ok(SCGeneralResponseDto.success(tagDetails));
    }
}
