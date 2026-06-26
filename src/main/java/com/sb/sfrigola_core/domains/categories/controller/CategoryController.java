package com.sb.sfrigola_core.domains.categories.controller;

import com.sb.sfrigola_core.common.constant.SCRequestParamValidationCodeConstants;
import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.dto.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.domains.categories.dto.CategoryDetailsAdminDto;
import com.sb.sfrigola_core.domains.categories.dto.CategoryDto;
import com.sb.sfrigola_core.domains.categories.dto.CategoryPreviewAdminDto;
import com.sb.sfrigola_core.domains.categories.dto.CategoryUpsertDto;
import com.sb.sfrigola_core.domains.categories.service.ICategoryService;
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
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final ICategoryService categoryService;

    @GetMapping(version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<List<CategoryDto>, SCPagedOptionDto>> getAllCategories(
            @Min(value = 0, message = SCRequestParamValidationCodeConstants.PAGE_MUST_BE_AT_LEAST_ZERO)
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Min(value = 1, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_LEAST_ONE) @Max(value = 100, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_MOST_HUNDRED)
            @RequestParam(value = "take", required = false, defaultValue = "10") int take,
            @RequestParam(required = false) @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale
    ) {
        var filterQuery = SCFilterQuery.pagedOnly( take, page);
        var paginatedCategories = categoryService.getAll(filterQuery, locale);
        return ResponseEntity.ok(SCGeneralResponseDto.success(paginatedCategories.content(), paginatedCategories.pagedOptionDto()));
    }

    // ADMIN CONTROLLER

    @GetMapping(value = "/admin", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<List<CategoryPreviewAdminDto>, SCPagedOptionDto>> getAllCategoriesAdmin(
            @Min(value = 0, message = SCRequestParamValidationCodeConstants.PAGE_MUST_BE_AT_LEAST_ZERO)
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Min(value = 1, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_LEAST_ONE) @Max(value = 100, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_MOST_HUNDRED)
            @RequestParam(value = "take", required = false, defaultValue = "10") int take,
            @RequestParam(value = "searchKey", required = false) String searchKey,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(required = false) @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale
    ){

        var filterQuery = SCFilterQuery.pageWithSearch(searchKey, take, page);
        var paginatedCategories = categoryService.getAllAdmin(filterQuery, locale, isActive);
        return ResponseEntity.ok(SCGeneralResponseDto.success(paginatedCategories.content(), paginatedCategories.pagedOptionDto()));
    }

    @GetMapping(value = "/admin/{publicId}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<CategoryDetailsAdminDto, Void>> getCategoryByPublicIdAdmin(
            @PathVariable("publicId") UUID publicId,
            @RequestParam(required = false) @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale
    ) {
        var selectedCategory = categoryService.getByPublicIdAdmin(publicId, locale);

        return ResponseEntity.ok(SCGeneralResponseDto.success(selectedCategory));
    }

    @PostMapping(value = {"/admin/create", "/admin/create/{parentPublicId}"}, version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<CategoryPreviewAdminDto, Void>> createCategory(
            @PathVariable(value = "parentPublicId", required = false) UUID parentPublicId,
            @RequestBody @Valid CategoryUpsertDto categoryUpsertDto
    ) {
        CategoryPreviewAdminDto created = categoryService.createNewCategory(categoryUpsertDto, parentPublicId);
        return ResponseEntity.ok(SCGeneralResponseDto.success(created));
    }
}
