package com.sb.sfrigola_core.domains.ingredients.controller;

import com.sb.sfrigola_core.common.constant.SCRequestParamValidationCodeConstants;
import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.dto.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.domains.ingredients.dto.IngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.admin.IngredientInputDto;
import com.sb.sfrigola_core.domains.ingredients.dto.admin.IngredientPreviewAdminDto;
import com.sb.sfrigola_core.domains.ingredients.enums.IngredientSortField;
import com.sb.sfrigola_core.domains.ingredients.models.IngredientSpecificFilter;
import com.sb.sfrigola_core.domains.ingredients.service.IIngredientService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingredients")
@RequiredArgsConstructor
@Validated
public class IngredientController {

    private final IIngredientService ingredientService;

    // CONTRIBUTOR CONTROLLER

    @GetMapping(version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<List<IngredientDto>, SCPagedOptionDto>> getAllIngredients(
            @Min(value = 0, message = SCRequestParamValidationCodeConstants.PAGE_MUST_BE_AT_LEAST_ZERO)
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Min(value = 1, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_LEAST_ONE) @Max(value = 100, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_MOST_HUNDRED)
            @RequestParam(value = "take", required = false, defaultValue = "10") int take,
            @RequestParam(value = "searchKey", required = false) String searchKey,
            @RequestParam(required = false) @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale

    ) {
        var filterQuery = SCFilterQuery.pageWithSearch(searchKey, take, page);
        var paginatedIngredients = ingredientService.getAll(filterQuery, locale);
        return ResponseEntity.ok(SCGeneralResponseDto.success(paginatedIngredients.content(), paginatedIngredients.pagedOptionDto()));
    }

    // ADMIN CONTROLLER

    @GetMapping(value = "/admin", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<List<IngredientPreviewAdminDto>, SCPagedOptionDto>> getAllAdmin(
            @Min(value = 0, message = SCRequestParamValidationCodeConstants.PAGE_MUST_BE_AT_LEAST_ZERO)
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Min(value = 1, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_LEAST_ONE) @Max(value = 100, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_MOST_HUNDRED)
            @RequestParam(value = "take", required = false, defaultValue = "10") int take,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @Pattern(regexp = "asc|desc", message = SCRequestParamValidationCodeConstants.SORT_INVALID_VALUE)
            @RequestParam(value = "sort", required = false, defaultValue = "asc") String sort,
            @RequestParam(value = "searchKey", required = false) String searchKey,
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isVegetarian,
            @RequestParam(required = false) Boolean isVegan,
            @RequestParam(required = false) Boolean isGlutenFree,
            @RequestParam(required = false) Double minCalories,
            @RequestParam(required = false) Double maxCalories
    ){
        var specificFilterForIngredients = new IngredientSpecificFilter(
                category,
                isVegetarian,
                isVegan,
                isGlutenFree,
                minCalories,
                maxCalories
        );
        var sortField = sortBy != null ? IngredientSortField.fromString(sortBy) : null;
        var filterQuery = SCFilterQuery.powerful(searchKey, sortField, SortDirection.fromString(sort), take, page, specificFilterForIngredients);
        var paginatedIngredients = ingredientService.getAllAdmin(filterQuery, locale);
        return ResponseEntity.ok(SCGeneralResponseDto.success(paginatedIngredients.content(), paginatedIngredients.pagedOptionDto()));
    }

    @PostMapping(value = "/admin", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<IngredientPreviewAdminDto, Void>> createIngredient(
            @RequestBody @Valid IngredientInputDto newIngredient
    ) {
        var ingredientCreated = ingredientService.createIngredient(newIngredient);
        return ResponseEntity.ok(SCGeneralResponseDto.success(ingredientCreated));
    }

}
