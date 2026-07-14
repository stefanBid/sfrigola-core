package com.sb.sfrigola_core.domains.stats.service.impl;

import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.domains.recipes.entity.Recipe;
import com.sb.sfrigola_core.domains.stats.entity.RecipeStats;
import com.sb.sfrigola_core.domains.stats.enums.RecipeStatsSortField;
import com.sb.sfrigola_core.domains.stats.repository.IRecipeStatsRepository;
import com.sb.sfrigola_core.domains.stats.service.IRecipeStatsDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecipeStatsDomainBridgeServiceImpl implements IRecipeStatsDomainBridgeService {

    private final IRecipeStatsRepository recipeStatsRepository;

    @Override
    public Optional<RecipeStats> getStats(Long recipeId) {
        return recipeStatsRepository.findById(recipeId);
    }

    @Override
    public Map<Long, RecipeStats> getStatsBatch(List<Long> recipeIds) {
        if (recipeIds.isEmpty()) return Map.of();
        return recipeStatsRepository.findAllById(recipeIds).stream()
                .collect(Collectors.toMap(RecipeStats::getRecipeId, Function.identity()));
    }

    @Override
    public Map<Long, RecipeStats> getPreviewByStats(List<Long> categoryIds) {
        var filterQuery = SCFilterQuery.essential(RecipeStatsSortField.FAVORITES_COUNT, SortDirection.DESC, 10, 0);

        return recipeStatsRepository.findAllByCategoryIds(categoryIds, SCPaginationUtils.toPageable(filterQuery)).stream()
                .collect(Collectors.toMap(RecipeStats::getRecipeId, Function.identity()));
    }

    @Override
    public Page<Long> getPublishedRecipeIdsOrderByFavoritesDesc(Pageable pageable) {
        return recipeStatsRepository.findPublishedRecipeIdsOrderByFavoritesDesc(pageable);
    }

    @Override
    @Transactional
    public void registerFavoriteAdded(Recipe recipe) {
        RecipeStats stats = getOrCreateStats(recipe);
        stats.setFavoritesCount(stats.getFavoritesCount() + 1);
        stats.setLastComputed(Instant.now());
        recipeStatsRepository.save(stats);
    }

    @Override
    @Transactional
    public void registerFavoriteRemoved(Recipe recipe) {
        RecipeStats stats = getOrCreateStats(recipe);
        stats.setFavoritesCount(Math.max(0, stats.getFavoritesCount() - 1));
        stats.setLastComputed(Instant.now());
        recipeStatsRepository.save(stats);
    }

    @Override
    @Transactional
    public void registerRatingAdded(Recipe recipe, short score) {
        RecipeStats stats = getOrCreateStats(recipe);
        int newCount = stats.getRatingsCount() + 1;
        BigDecimal newAvg = stats.getAvgRating()
                .multiply(BigDecimal.valueOf(stats.getRatingsCount()))
                .add(BigDecimal.valueOf(score))
                .divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);
        stats.setRatingsCount(newCount);
        stats.setAvgRating(newAvg);
        stats.setLastComputed(Instant.now());
        recipeStatsRepository.save(stats);
    }

    @Override
    @Transactional
    public void registerRatingScoreChanged(Recipe recipe, short oldScore, short newScore) {
        RecipeStats stats = getOrCreateStats(recipe);
        if (stats.getRatingsCount() == 0) return;
        BigDecimal newAvg = stats.getAvgRating()
                .multiply(BigDecimal.valueOf(stats.getRatingsCount()))
                .subtract(BigDecimal.valueOf(oldScore))
                .add(BigDecimal.valueOf(newScore))
                .divide(BigDecimal.valueOf(stats.getRatingsCount()), 2, RoundingMode.HALF_UP);
        stats.setAvgRating(newAvg);
        stats.setLastComputed(Instant.now());
        recipeStatsRepository.save(stats);
    }

    // =========================================================
    // PRIVATE
    // =========================================================

    private RecipeStats getOrCreateStats(Recipe recipe) {
        return recipeStatsRepository.findById(recipe.getId()).orElseGet(() -> {
            RecipeStats stats = new RecipeStats();
            stats.setRecipe(recipe);
            stats.setAvgRating(BigDecimal.ZERO);
            stats.setRatingsCount(0);
            stats.setFavoritesCount(0);
            stats.setViewsCount(0);
            stats.setLastComputed(Instant.now());
            return stats;
        });
    }
}
