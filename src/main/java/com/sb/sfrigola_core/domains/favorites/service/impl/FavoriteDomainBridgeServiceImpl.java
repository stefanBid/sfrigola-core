package com.sb.sfrigola_core.domains.favorites.service.impl;

import com.sb.sfrigola_core.domains.favorites.repository.IFavoriteRepository;
import com.sb.sfrigola_core.domains.favorites.service.IFavoriteDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FavoriteDomainBridgeServiceImpl implements IFavoriteDomainBridgeService {

    private final IFavoriteRepository favoriteRepository;

    @Override
    public boolean isFavoritedByUser(Long userId, Long recipeId) {
        return favoriteRepository.existsByUser_IdAndRecipe_Id(userId, recipeId);
    }

    @Override
    public Set<Long> getFavoritedRecipeIds(Long userId, List<Long> recipeIds) {
        if (recipeIds.isEmpty()) return Set.of();
        return favoriteRepository.findFavoritedRecipeIds(userId, recipeIds);
    }
}
