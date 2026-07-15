package com.sb.sfrigola_core.domains.favorites.service.impl;

import com.sb.sfrigola_core.common.enums.SCUserRole;
import com.sb.sfrigola_core.common.models.context.SCAuthUser;
import com.sb.sfrigola_core.domains.favorites.entity.Favorite;
import com.sb.sfrigola_core.domains.favorites.exception.FavoriteAlreadyExistsException;
import com.sb.sfrigola_core.domains.favorites.exception.NoFavoriteFoundException;
import com.sb.sfrigola_core.domains.favorites.repository.IFavoriteRepository;
import com.sb.sfrigola_core.domains.recipes.entity.Recipe;
import com.sb.sfrigola_core.domains.recipes.service.IRecipeDomainBridgeService;
import com.sb.sfrigola_core.domains.stats.service.IRecipeStatsDomainBridgeService;
import com.sb.sfrigola_core.domains.users.entity.SCUser;
import com.sb.sfrigola_core.domains.users.service.ISCUserDomainBridgeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceImplTest {

    @Mock
    private IFavoriteRepository favoriteRepository;
    @Mock
    private IRecipeDomainBridgeService recipeDomainBridgeService;
    @Mock
    private ISCUserDomainBridgeService userDomainBridgeService;
    @Mock
    private IRecipeStatsDomainBridgeService recipeStatsDomainBridgeService;

    private FavoriteServiceImpl favoriteService;

    private SCAuthUser authUser;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteServiceImpl(favoriteRepository, recipeDomainBridgeService, userDomainBridgeService, recipeStatsDomainBridgeService);

        authUser = new SCAuthUser(UUID.randomUUID(), SCUserRole.ROLE_USER, "john", "john@example.com", null, "en", true, "John", "Doe");
        var authentication = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addFavorite_savesFavoriteAndRegistersStatsWhenNotAlreadyFavorited() {
        var recipePublicId = UUID.randomUUID();
        var user = new SCUser();
        var recipe = new Recipe();

        when(favoriteRepository.existsByUser_PublicIdAndRecipe_PublicId(authUser.publicId(), recipePublicId)).thenReturn(false);
        when(userDomainBridgeService.getUserEntityByPublicIdOrThrow(authUser.publicId())).thenReturn(user);
        when(recipeDomainBridgeService.getRecipeEntityByPublicIdOrThrow(recipePublicId)).thenReturn(recipe);

        favoriteService.addFavorite(recipePublicId);

        verify(favoriteRepository).save(any(Favorite.class));
        verify(recipeStatsDomainBridgeService).registerFavoriteAdded(recipe);
    }

    @Test
    void addFavorite_throwsWhenAlreadyFavorited() {
        var recipePublicId = UUID.randomUUID();
        when(favoriteRepository.existsByUser_PublicIdAndRecipe_PublicId(authUser.publicId(), recipePublicId)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addFavorite(recipePublicId))
                .isInstanceOf(FavoriteAlreadyExistsException.class);

        verify(favoriteRepository, never()).save(any());
        verify(recipeStatsDomainBridgeService, never()).registerFavoriteAdded(any());
    }

    @Test
    void removeFavorite_deletesFavoriteAndRegistersStatsWhenFound() {
        var recipePublicId = UUID.randomUUID();
        var recipe = new Recipe();
        var favorite = new Favorite();
        favorite.setRecipe(recipe);

        when(favoriteRepository.findByUser_PublicIdAndRecipe_PublicId(authUser.publicId(), recipePublicId)).thenReturn(Optional.of(favorite));

        favoriteService.removeFavorite(recipePublicId);

        verify(favoriteRepository).delete(favorite);
        verify(recipeStatsDomainBridgeService).registerFavoriteRemoved(recipe);
    }

    @Test
    void removeFavorite_throwsWhenNotFound() {
        var recipePublicId = UUID.randomUUID();
        when(favoriteRepository.findByUser_PublicIdAndRecipe_PublicId(authUser.publicId(), recipePublicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.removeFavorite(recipePublicId))
                .isInstanceOf(NoFavoriteFoundException.class);

        verify(favoriteRepository, never()).delete(any());
        verify(recipeStatsDomainBridgeService, never()).registerFavoriteRemoved(any());
    }
}
