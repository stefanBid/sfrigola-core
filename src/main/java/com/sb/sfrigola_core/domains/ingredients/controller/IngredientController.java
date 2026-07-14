package com.sb.sfrigola_core.domains.ingredients.controller;

import com.sb.sfrigola_core.common.constant.SCRequestParamValidationCodeConstants;
import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.dto.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.domains.ingredients.dto.view.IngredientDetailsAdminDto;
import com.sb.sfrigola_core.domains.ingredients.dto.view.IngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.input.AddIngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.input.UpdateIngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.view.IngredientPreviewAdminDto;
import com.sb.sfrigola_core.domains.ingredients.enums.IngredientFoodGroup;
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
import java.util.UUID;

@RestController
@RequestMapping("/ingredients")
@RequiredArgsConstructor
@Validated
public class IngredientController {

    private final IIngredientService ingredientService;

    // [Contributor, Admin]
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

    // [Admin]
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
            @RequestParam(required = false) @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale,
            @RequestParam(required = false) String foodGroup,
            @RequestParam(required = false) Boolean isVegetarian,
            @RequestParam(required = false) Boolean isVegan,
            @RequestParam(required = false) Boolean isGlutenFree,
            @RequestParam(required = false) Double minCalories,
            @RequestParam(required = false) Double maxCalories
    ){
        var specificFilterForIngredients = new IngredientSpecificFilter(
                foodGroup != null ? IngredientFoodGroup.fromString(foodGroup) : null,
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

    // [Admin]
    @GetMapping(value = "/admin/{publicId}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<IngredientDetailsAdminDto, Void>> getIngredientByPublicIdAdmin(
            @PathVariable("publicId") UUID publicId,
            @RequestParam @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale
    ) {
        var ingredientDetails = ingredientService.getByPublicIdAdmin(publicId, locale);
        return ResponseEntity.ok(SCGeneralResponseDto.success(ingredientDetails));
    }

    // [Admin]
    @PostMapping(value = "/admin", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<IngredientPreviewAdminDto, Void>> createIngredient(
            @RequestBody @Valid AddIngredientDto newIngredient,
            @RequestParam @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale
    ) {
        var ingredientCreated = ingredientService.createNewIngredient(newIngredient, locale);
        return ResponseEntity.ok(SCGeneralResponseDto.success(ingredientCreated));
    }

    // [Admin]
    @PutMapping(value = "/admin/{publicId}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<IngredientPreviewAdminDto, Void>> updateIngredient(
            @PathVariable("publicId") UUID publicId,
            @RequestBody @Valid UpdateIngredientDto updateIngredientDto
    ) {
        var updated = ingredientService.updateIngredient(publicId, updateIngredientDto);
        return ResponseEntity.ok(SCGeneralResponseDto.success(updated));
    }

    // [Admin]
    @DeleteMapping(value = "/admin/{publicId}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<UUID, Void>> deleteIngredient(
            @PathVariable("publicId") UUID publicId
    ) {
        var deleted = ingredientService.deleteIngredient(publicId);
        return ResponseEntity.ok(SCGeneralResponseDto.success(deleted));
    }

}
