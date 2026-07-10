package com.sb.sfrigola_core.domains.favorites.controller;

import com.sb.sfrigola_core.common.constant.SCRequestParamValidationCodeConstants;
import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.dto.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.domains.favorites.service.IFavoriteService;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipeDto;
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
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Validated
public class FavoriteController {

    private final IFavoriteService favoriteService;

    // [Authenticated]
    @GetMapping(version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<List<RecipeDto>, SCPagedOptionDto>> getAllMyFavorites(
            @Min(value = 0, message = SCRequestParamValidationCodeConstants.PAGE_MUST_BE_AT_LEAST_ZERO)
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Min(value = 1, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_LEAST_ONE) @Max(value = 100, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_MOST_HUNDRED)
            @RequestParam(value = "take", required = false, defaultValue = "10") int take,
            @RequestParam(required = false) @NotBlank(message = SCRequestParamValidationCodeConstants.LOCALE_MUST_NOT_BE_BLANK) String locale
    ) {
        var filterQuery = SCFilterQuery.pagedOnly(take, page);
        var paginatedFavorites = favoriteService.getAllMyFavorites(filterQuery, locale);
        return ResponseEntity.ok(SCGeneralResponseDto.success(paginatedFavorites.content(), paginatedFavorites.pagedOptionDto()));
    }

    // [Authenticated]
    @PostMapping(value = "/{recipePublicId}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<String, Void>> addFavorite(@PathVariable("recipePublicId") UUID recipePublicId) {
        favoriteService.addFavorite(recipePublicId);
        return ResponseEntity.ok(SCGeneralResponseDto.successMutation("Recipe added to favorites"));
    }

    // [Authenticated]
    @DeleteMapping(value = "/{recipePublicId}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<String, Void>> removeFavorite(@PathVariable("recipePublicId") UUID recipePublicId) {
        favoriteService.removeFavorite(recipePublicId);
        return ResponseEntity.ok(SCGeneralResponseDto.successMutation("Recipe removed from favorites"));
    }
}
