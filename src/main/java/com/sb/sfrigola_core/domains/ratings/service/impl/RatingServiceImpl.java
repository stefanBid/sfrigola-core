package com.sb.sfrigola_core.domains.ratings.service.impl;

import com.sb.sfrigola_core.common.util.SCAuthenticationUtils;
import com.sb.sfrigola_core.domains.ratings.dto.input.AddRatingDto;
import com.sb.sfrigola_core.domains.ratings.dto.input.UpdateRatingDto;
import com.sb.sfrigola_core.domains.ratings.dto.view.RatingDto;
import com.sb.sfrigola_core.domains.ratings.dto.view.RecipeRatingStatsDto;
import com.sb.sfrigola_core.domains.ratings.entity.Rating;
import com.sb.sfrigola_core.domains.ratings.exception.NoRatingFoundException;
import com.sb.sfrigola_core.domains.ratings.exception.RatingAlreadyExistsException;
import com.sb.sfrigola_core.domains.ratings.repository.IRatingRepository;
import com.sb.sfrigola_core.domains.ratings.service.IRatingService;
import com.sb.sfrigola_core.domains.recipes.entity.Recipe;
import com.sb.sfrigola_core.domains.recipes.service.IRecipeDomainBridgeService;
import com.sb.sfrigola_core.domains.stats.service.IRecipeStatsDomainBridgeService;
import com.sb.sfrigola_core.domains.users.service.ISCUserDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RatingServiceImpl implements IRatingService {

    private final IRatingRepository ratingRepository;
    private final IRecipeDomainBridgeService recipeDomainBridgeService;
    private final ISCUserDomainBridgeService userDomainBridgeService;
    private final IRecipeStatsDomainBridgeService recipeStatsDomainBridgeService;

    @Override
    public RecipeRatingStatsDto getRatingStats(UUID recipePublicId) {
        Recipe recipe = recipeDomainBridgeService.getRecipeEntityByPublicIdOrThrow(recipePublicId);
        return recipeStatsDomainBridgeService.getStats(recipe.getId())
                .map(stats -> new RecipeRatingStatsDto(stats.getAvgRating(), stats.getRatingsCount()))
                .orElse(new RecipeRatingStatsDto(BigDecimal.ZERO, 0));
    }

    @Override
    @Transactional
    public RatingDto addRating(AddRatingDto addRatingDto) {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();

        if (ratingRepository.existsByUser_PublicIdAndRecipe_PublicId(authUser.publicId(), addRatingDto.recipePublicId()))
            throw new RatingAlreadyExistsException(addRatingDto.recipePublicId());

        var user = userDomainBridgeService.getUserEntityByPublicIdOrThrow(authUser.publicId());
        var recipe = recipeDomainBridgeService.getRecipeEntityByPublicIdOrThrow(addRatingDto.recipePublicId());

        Rating rating = new Rating();
        rating.setUser(user);
        rating.setRecipe(recipe);
        rating.setScore(addRatingDto.score());
        rating.setComment(addRatingDto.comment());
        ratingRepository.save(rating);

        recipeStatsDomainBridgeService.registerRatingAdded(recipe, addRatingDto.score());

        return toDto(rating);
    }

    @Override
    @Transactional
    public RatingDto editRating(UUID recipePublicId, UpdateRatingDto updateRatingDto) {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();

        var rating = ratingRepository.findByUser_PublicIdAndRecipe_PublicId(authUser.publicId(), recipePublicId)
                .orElseThrow(() -> new NoRatingFoundException(recipePublicId));

        if (!rating.getScore().equals(updateRatingDto.score())) {
            short oldScore = rating.getScore();
            rating.setScore(updateRatingDto.score());
            recipeStatsDomainBridgeService.registerRatingScoreChanged(rating.getRecipe(), oldScore, updateRatingDto.score());
        }

        if (!Objects.equals(rating.getComment(), updateRatingDto.comment()))
            rating.setComment(updateRatingDto.comment());

        return toDto(rating);
    }

    // =========================================================
    // PRIVATE
    // =========================================================

    private RatingDto toDto(Rating rating) {
        return new RatingDto(
                rating.getPublicId(),
                rating.getRecipe().getPublicId(),
                rating.getScore(),
                rating.getComment()
        );
    }
}
