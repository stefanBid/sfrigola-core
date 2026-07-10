package com.sb.sfrigola_core.domains.ratings.controller;

import com.sb.sfrigola_core.common.dto.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.domains.ratings.dto.input.AddRatingDto;
import com.sb.sfrigola_core.domains.ratings.dto.input.UpdateRatingDto;
import com.sb.sfrigola_core.domains.ratings.dto.view.RatingDto;
import com.sb.sfrigola_core.domains.ratings.dto.view.RecipeRatingStatsDto;
import com.sb.sfrigola_core.domains.ratings.service.IRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/ratings")
@RequiredArgsConstructor
@Validated
public class RatingController {

    private final IRatingService ratingService;

    // [Authenticated]
    @GetMapping(value = "/recipe/{recipePublicId}/stats", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<RecipeRatingStatsDto, Void>> getRatingStats(
            @PathVariable("recipePublicId") UUID recipePublicId
    ) {
        var stats = ratingService.getRatingStats(recipePublicId);
        return ResponseEntity.ok(SCGeneralResponseDto.success(stats));
    }

    // [Authenticated]
    @PostMapping(version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<RatingDto, Void>> addRating(@RequestBody @Valid AddRatingDto addRatingDto) {
        var created = ratingService.addRating(addRatingDto);
        return ResponseEntity.ok(SCGeneralResponseDto.success(created));
    }

    // [Authenticated]
    @PutMapping(value = "/recipe/{recipePublicId}", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<RatingDto, Void>> editRating(
            @PathVariable("recipePublicId") UUID recipePublicId,
            @RequestBody @Valid UpdateRatingDto updateRatingDto
    ) {
        var updated = ratingService.editRating(recipePublicId, updateRatingDto);
        return ResponseEntity.ok(SCGeneralResponseDto.success(updated));
    }
}
