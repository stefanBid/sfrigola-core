package com.sb.sfrigola_core.domains.favorites.controller;

import com.sb.sfrigola_core.common.dto.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.domains.favorites.service.IFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Validated
public class FavoriteController {

    private final IFavoriteService favoriteService;

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
