package com.sb.sfrigola_core.domains.ratings.service.impl;

import com.sb.sfrigola_core.domains.ratings.dto.view.RecipeRatingStatsDto;
import com.sb.sfrigola_core.domains.ratings.service.IRatingDomainBridgeService;
import com.sb.sfrigola_core.domains.stats.service.IRecipeStatsDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RatingDomainBridgeServiceImpl implements IRatingDomainBridgeService {

    private final IRecipeStatsDomainBridgeService recipeStatsDomainBridgeService;

    @Override
    public RecipeRatingStatsDto getRatingStats(Long recipeId) {
        var stats = recipeStatsDomainBridgeService.getStats(recipeId);
        return new RecipeRatingStatsDto(stats.avgRating(), stats.ratingsCount());
    }
}
