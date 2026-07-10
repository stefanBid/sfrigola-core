package com.sb.sfrigola_core.domains.favorites.service.impl;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.util.SCAuthenticationUtils;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.domains.favorites.entity.Favorite;
import com.sb.sfrigola_core.domains.favorites.exception.FavoriteAlreadyExistsException;
import com.sb.sfrigola_core.domains.favorites.exception.NoFavoriteFoundException;
import com.sb.sfrigola_core.domains.favorites.repository.IFavoriteRepository;
import com.sb.sfrigola_core.domains.favorites.service.IFavoriteService;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipeDto;
import com.sb.sfrigola_core.domains.recipes.entity.Recipe;
import com.sb.sfrigola_core.domains.recipes.entity.RecipeTranslation;
import com.sb.sfrigola_core.domains.recipes.service.IRecipeDomainBridgeService;
import com.sb.sfrigola_core.domains.stats.service.IRecipeStatsDomainBridgeService;
import com.sb.sfrigola_core.domains.users.service.ISCUserDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FavoriteServiceImpl implements IFavoriteService {

    private final IFavoriteRepository favoriteRepository;
    private final ILanguageDomainBridgeService languageDomainBridgeService;
    private final IRecipeDomainBridgeService recipeDomainBridgeService;
    private final ISCUserDomainBridgeService userDomainBridgeService;
    private final IRecipeStatsDomainBridgeService recipeStatsDomainBridgeService;

    @Override
    public SCPagedResult<RecipeDto> getAllMyFavorites(SCFilterQuery<Void> filterQuery, @NonNull String locale) {
        languageDomainBridgeService.validateLocaleIsActiveOrThrow(locale);

        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();
        var pageable = SCPaginationUtils.toPageable(filterQuery);

        var recipeIdsPage = favoriteRepository.findRecipeIdsByUserPublicId(authUser.publicId(), pageable);

        if (recipeIdsPage.hasContent()) {
            var ids = recipeIdsPage.getContent();
            Map<Long, Recipe> byId = recipeDomainBridgeService.getRecipesByIdsWithLocale(ids, locale)
                    .stream().collect(Collectors.toMap(Recipe::getId, r -> r));
            List<Recipe> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();

            return new SCPagedResult<>(
                    ordered.stream().map(this::toDto).toList(),
                    SCPaginationUtils.toPagedOption(recipeIdsPage)
            );
        }
        return SCPagedResult.empty();
    }

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

    // =========================================================
    // PRIVATE
    // =========================================================

    /**
     * Maps a favorited {@link Recipe} to {@link RecipeDto}. Assumes {@code recipe.getTranslations()}
     * contains at most one entry — the caller must have already filtered to a single locale
     * (see {@link IRecipeDomainBridgeService#getRecipesByIdsWithLocale}). {@code isFavourite} is
     * always {@code true} here, since every recipe in this list came from the favorites repository.
     *
     * @param recipe the recipe entity, with its translations list pre-filtered to one locale
     * @return the mapped {@link RecipeDto}
     */
    private RecipeDto toDto(Recipe recipe) {
        RecipeTranslation translation = recipe.getTranslations().isEmpty() ? null : recipe.getTranslations().getFirst();
        return new RecipeDto(
                recipe.getPublicId(),
                recipe.getAuthor().getPublicId(),
                recipe.getCategory() != null ? recipe.getCategory().getPublicId() : null,
                translation != null ? translation.getTitle() : null,
                translation != null ? translation.getDescription() : null,
                true,
                recipe.getTotalTimeMin(),
                recipe.getEconomicalRatio()
        );
    }
}
