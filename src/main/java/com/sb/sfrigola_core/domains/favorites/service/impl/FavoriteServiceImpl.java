package com.sb.sfrigola_core.domains.favorites.service.impl;

import com.sb.sfrigola_core.common.util.SCAuthenticationUtils;
import com.sb.sfrigola_core.domains.favorites.entity.Favorite;
import com.sb.sfrigola_core.domains.favorites.exception.FavoriteAlreadyExistsException;
import com.sb.sfrigola_core.domains.favorites.exception.NoFavoriteFoundException;
import com.sb.sfrigola_core.domains.favorites.repository.IFavoriteRepository;
import com.sb.sfrigola_core.domains.favorites.service.IFavoriteService;
import com.sb.sfrigola_core.domains.recipes.service.IRecipeDomainBridgeService;
import com.sb.sfrigola_core.domains.stats.service.IRecipeStatsDomainBridgeService;
import com.sb.sfrigola_core.domains.users.service.ISCUserDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FavoriteServiceImpl implements IFavoriteService {

    private final IFavoriteRepository favoriteRepository;
    private final IRecipeDomainBridgeService recipeDomainBridgeService;
    private final ISCUserDomainBridgeService userDomainBridgeService;
    private final IRecipeStatsDomainBridgeService recipeStatsDomainBridgeService;

    @Override
    @Transactional
    public void addFavorite(UUID recipePublicId) {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();

        if (favoriteRepository.existsByUser_PublicIdAndRecipe_PublicId(authUser.publicId(), recipePublicId))
            throw new FavoriteAlreadyExistsException(recipePublicId);

        var user = userDomainBridgeService.getUserEntityByPublicIdOrThrow(authUser.publicId());
        var recipe = recipeDomainBridgeService.getRecipeEntityByPublicIdOrThrow(recipePublicId);

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setRecipe(recipe);
        favoriteRepository.save(favorite);

        recipeStatsDomainBridgeService.registerFavoriteAdded(recipe);
    }

    @Override
    @Transactional
    public void removeFavorite(UUID recipePublicId) {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();

        var favorite = favoriteRepository.findByUser_PublicIdAndRecipe_PublicId(authUser.publicId(), recipePublicId)
                .orElseThrow(() -> new NoFavoriteFoundException(recipePublicId));

        favoriteRepository.delete(favorite);
        recipeStatsDomainBridgeService.registerFavoriteRemoved(favorite.getRecipe());
    }
}
