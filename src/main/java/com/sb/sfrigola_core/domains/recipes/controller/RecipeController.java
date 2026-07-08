package com.sb.sfrigola_core.domains.recipes.controller;

import com.sb.sfrigola_core.common.constant.SCRequestParamValidationCodeConstants;
import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.dto.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.domains.recipes.dto.input.AddRecipeDto;
import com.sb.sfrigola_core.domains.recipes.dto.input.UpdateRecipeDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipeDetailsAdminDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipeDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipePreviewAdminDto;
import com.sb.sfrigola_core.domains.recipes.enums.DifficultyLevel;
import com.sb.sfrigola_core.domains.recipes.enums.MealType;
import com.sb.sfrigola_core.domains.recipes.enums.RecipeSortField;
import com.sb.sfrigola_core.domains.recipes.enums.SeasonType;
import com.sb.sfrigola_core.domains.recipes.models.RecipeSpecificFilter;
import com.sb.sfrigola_core.domains.recipes.service.IRecipeService;
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
@RequestMapping("/recipes")
@RequiredArgsConstructor
@Validated
public class RecipeController {

    private final IRecipeService recipeService;

    // [Any authenticated role — USER, Contributor, Admin]
    @GetMapping(version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<List<RecipeDto>, SCPagedOptionDto>> getAllRecipes(
            @Min(value = 0, message = SCRequestParamValidationCodeConstants.PAGE_MUST_BE_AT_LEAST_ZERO)
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Min(value = 1, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_LEAST_ONE) @Max(value = 100, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_MOST_HUNDRED)
            @RequestParam(value = "take", required = false, defaultValue = "10") int take,
            @RequestParam(value = "searchKey", required = false) String searchKey,
            @RequestParam(required = false) @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale
    ) {
        var filterQuery = SCFilterQuery.pageWithSearch(searchKey, take, page);
        var paginatedRecipes = recipeService.getAll(filterQuery, locale);
        return ResponseEntity.ok(SCGeneralResponseDto.success(paginatedRecipes.content(), paginatedRecipes.pagedOptionDto()));
    }

    // [Admin]
    @GetMapping(value = "/admin", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<List<RecipePreviewAdminDto>, SCPagedOptionDto>> getAllAdmin(
            @Min(value = 0, message = SCRequestParamValidationCodeConstants.PAGE_MUST_BE_AT_LEAST_ZERO)
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Min(value = 1, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_LEAST_ONE) @Max(value = 100, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_MOST_HUNDRED)
            @RequestParam(value = "take", required = false, defaultValue = "10") int take,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @Pattern(regexp = "asc|desc", message = SCRequestParamValidationCodeConstants.SORT_INVALID_VALUE)
            @RequestParam(value = "sort", required = false, defaultValue = "asc") String sort,
            @RequestParam(value = "searchKey", required = false) String searchKey,
            @RequestParam(required = false) @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String mealType,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) Boolean isVegetarian,
            @RequestParam(required = false) Boolean isVegan,
            @RequestParam(required = false) Boolean isGlutenFree,
            @RequestParam(required = false) Boolean isPublished
    ) {
        var recipeSpecificFilter = new RecipeSpecificFilter(
                difficulty != null ? DifficultyLevel.fromValue(difficulty) : null,
                mealType != null ? MealType.fromValue(mealType) : null,
                season != null ? SeasonType.fromValue(season) : null,
                isVegetarian,
                isVegan,
                isGlutenFree,
                isPublished
        );
        var filterQuery = SCFilterQuery.powerful(searchKey, sortBy != null ? RecipeSortField.fromString(sortBy) : null, SortDirection.fromString(sort), take, page, recipeSpecificFilter);
        var paginatedRecipes = recipeService.getAllAdmin(filterQuery, locale);
        return ResponseEntity.ok(SCGeneralResponseDto.success(paginatedRecipes.content(), paginatedRecipes.pagedOptionDto()));
    }

    // [Admin]
    @GetMapping(value = "/admin/{publicId}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<RecipeDetailsAdminDto, Void>> getRecipeByPublicIdAdmin(
            @PathVariable("publicId") UUID publicId,
            @RequestParam @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale
    ) {
        var recipeDetails = recipeService.getByPublicIdAdmin(publicId, locale);
        return ResponseEntity.ok(SCGeneralResponseDto.success(recipeDetails));
    }

    // [Contributor, Admin]
    @PostMapping(version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<RecipePreviewAdminDto, Void>> createRecipe(
            @RequestBody @Valid AddRecipeDto newRecipe,
            @RequestParam @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale
    ) {
        var recipeCreated = recipeService.createRecipe(newRecipe, locale);
        return ResponseEntity.ok(SCGeneralResponseDto.success(recipeCreated));
    }

    // [Contributor(own), Admin]
    @PutMapping(value = "/{publicId}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<RecipePreviewAdminDto, Void>> updateRecipe(
            @PathVariable("publicId") UUID publicId,
            @RequestBody @Valid UpdateRecipeDto updateRecipeDto
    ) {
        var updated = recipeService.updateRecipe(publicId, updateRecipeDto);
        return ResponseEntity.ok(SCGeneralResponseDto.success(updated));
    }

    // [Contributor(own), Admin]
    @DeleteMapping(value = "/{publicId}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<UUID, Void>> deleteRecipe(
            @PathVariable("publicId") UUID publicId
    ) {
        var deleted = recipeService.deleteRecipe(publicId);
        return ResponseEntity.ok(SCGeneralResponseDto.success(deleted));
    }
}
