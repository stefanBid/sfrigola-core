package com.sb.sfrigola_core.domains.ratings.service.impl;

import com.sb.sfrigola_core.common.enums.SCUserRole;
import com.sb.sfrigola_core.common.models.context.SCAuthUser;
import com.sb.sfrigola_core.domains.ratings.dto.input.AddRatingDto;
import com.sb.sfrigola_core.domains.ratings.dto.input.UpdateRatingDto;
import com.sb.sfrigola_core.domains.ratings.entity.Rating;
import com.sb.sfrigola_core.domains.ratings.exception.NoRatingFoundException;
import com.sb.sfrigola_core.domains.ratings.exception.RatingAlreadyExistsException;
import com.sb.sfrigola_core.domains.ratings.repository.IRatingRepository;
import com.sb.sfrigola_core.domains.recipes.entity.Recipe;
import com.sb.sfrigola_core.domains.recipes.service.IRecipeDomainBridgeService;
import com.sb.sfrigola_core.domains.stats.entity.RecipeStats;
import com.sb.sfrigola_core.domains.stats.service.IRecipeStatsDomainBridgeService;
import com.sb.sfrigola_core.domains.users.entity.SCUser;
import com.sb.sfrigola_core.domains.users.service.ISCUserDomainBridgeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceImplTest {

    @Mock
    private IRatingRepository ratingRepository;
    @Mock
    private IRecipeDomainBridgeService recipeDomainBridgeService;
    @Mock
    private ISCUserDomainBridgeService userDomainBridgeService;
    @Mock
    private IRecipeStatsDomainBridgeService recipeStatsDomainBridgeService;

    private RatingServiceImpl ratingService;

    private SCAuthUser authUser;

    @BeforeEach
    void setUp() {
        ratingService = new RatingServiceImpl(ratingRepository, recipeDomainBridgeService, userDomainBridgeService, recipeStatsDomainBridgeService);

        authUser = new SCAuthUser(UUID.randomUUID(), SCUserRole.ROLE_USER, "john", "john@example.com", null, "en", true, "John", "Doe");
        var authentication = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getRatingStats_returnsStatsWhenRecipeHasBeenRated() {
        var recipePublicId = UUID.randomUUID();
        var recipe = new Recipe();
        recipe.setId(1L);
        var stats = new RecipeStats();
        stats.setAvgRating(new BigDecimal("4.25"));
        stats.setRatingsCount(8);

        when(recipeDomainBridgeService.getRecipeEntityByPublicIdOrThrow(recipePublicId)).thenReturn(recipe);
        when(recipeStatsDomainBridgeService.getStats(1L)).thenReturn(Optional.of(stats));

        var result = ratingService.getRatingStats(recipePublicId);

        assertThat(result.averageRating()).isEqualTo(new BigDecimal("4.25"));
        assertThat(result.totalRatings()).isEqualTo(8);
    }

    @Test
    void getRatingStats_returnsZeroValuesWhenRecipeHasNoStats() {
        var recipePublicId = UUID.randomUUID();
        var recipe = new Recipe();
        recipe.setId(1L);

        when(recipeDomainBridgeService.getRecipeEntityByPublicIdOrThrow(recipePublicId)).thenReturn(recipe);
        when(recipeStatsDomainBridgeService.getStats(1L)).thenReturn(Optional.empty());

        var result = ratingService.getRatingStats(recipePublicId);

        assertThat(result.averageRating()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.totalRatings()).isZero();
    }

    @Test
    void addRating_savesRatingAndRegistersStatsWhenNotAlreadyRated() {
        var recipePublicId = UUID.randomUUID();
        var user = new SCUser();
        var recipe = new Recipe();
        var dto = new AddRatingDto(recipePublicId, (short) 5, "Great recipe");

        when(ratingRepository.existsByUser_PublicIdAndRecipe_PublicId(authUser.publicId(), recipePublicId)).thenReturn(false);
        when(userDomainBridgeService.getUserEntityByPublicIdOrThrow(authUser.publicId())).thenReturn(user);
        when(recipeDomainBridgeService.getRecipeEntityByPublicIdOrThrow(recipePublicId)).thenReturn(recipe);

        var result = ratingService.addRating(dto);

        assertThat(result.score()).isEqualTo((short) 5);
        assertThat(result.comment()).isEqualTo("Great recipe");
        assertThat(result.recipePublicId()).isEqualTo(recipe.getPublicId());

        var ratingCaptor = ArgumentCaptor.forClass(Rating.class);
        verify(ratingRepository).save(ratingCaptor.capture());
        assertThat(ratingCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(ratingCaptor.getValue().getRecipe()).isEqualTo(recipe);
        assertThat(ratingCaptor.getValue().getScore()).isEqualTo((short) 5);
        verify(recipeStatsDomainBridgeService).registerRatingAdded(recipe, (short) 5);
    }

    @Test
    void addRating_throwsWhenUserAlreadyRatedRecipe() {
        var recipePublicId = UUID.randomUUID();
        var dto = new AddRatingDto(recipePublicId, (short) 5, null);
        when(ratingRepository.existsByUser_PublicIdAndRecipe_PublicId(authUser.publicId(), recipePublicId)).thenReturn(true);

        assertThatThrownBy(() -> ratingService.addRating(dto))
                .isInstanceOf(RatingAlreadyExistsException.class);

        verify(ratingRepository, never()).save(any());
        verifyNoInteractions(userDomainBridgeService, recipeStatsDomainBridgeService);
        verify(recipeDomainBridgeService, never()).getRecipeEntityByPublicIdOrThrow(any());
    }

    @Test
    void editRating_updatesScoreAndRegistersStatsWhenScoreChanges() {
        var recipePublicId = UUID.randomUUID();
        var recipe = new Recipe();
        var rating = new Rating();
        rating.setRecipe(recipe);
        rating.setScore((short) 2);
        rating.setComment("Old comment");
        var dto = new UpdateRatingDto((short) 4, "Old comment");

        when(ratingRepository.findByUser_PublicIdAndRecipe_PublicId(authUser.publicId(), recipePublicId)).thenReturn(Optional.of(rating));

        var result = ratingService.editRating(recipePublicId, dto);

        assertThat(rating.getScore()).isEqualTo((short) 4);
        assertThat(result.score()).isEqualTo((short) 4);
        verify(recipeStatsDomainBridgeService).registerRatingScoreChanged(recipe, (short) 2, (short) 4);
    }

    @Test
    void editRating_updatesCommentWhenCommentChanges() {
        var recipePublicId = UUID.randomUUID();
        var recipe = new Recipe();
        var rating = new Rating();
        rating.setRecipe(recipe);
        rating.setScore((short) 4);
        rating.setComment("Old comment");
        var dto = new UpdateRatingDto((short) 4, "New comment");

        when(ratingRepository.findByUser_PublicIdAndRecipe_PublicId(authUser.publicId(), recipePublicId)).thenReturn(Optional.of(rating));

        var result = ratingService.editRating(recipePublicId, dto);

        assertThat(rating.getComment()).isEqualTo("New comment");
        assertThat(result.comment()).isEqualTo("New comment");
        verify(recipeStatsDomainBridgeService, never()).registerRatingScoreChanged(any(), anyShort(), anyShort());
    }

    @Test
    void editRating_doesNothingWhenScoreAndCommentAreUnchanged() {
        var recipePublicId = UUID.randomUUID();
        var recipe = new Recipe();
        var rating = new Rating();
        rating.setRecipe(recipe);
        rating.setScore((short) 4);
        rating.setComment("Same comment");
        var dto = new UpdateRatingDto((short) 4, "Same comment");

        when(ratingRepository.findByUser_PublicIdAndRecipe_PublicId(authUser.publicId(), recipePublicId)).thenReturn(Optional.of(rating));

        var result = ratingService.editRating(recipePublicId, dto);

        assertThat(result.score()).isEqualTo((short) 4);
        assertThat(result.comment()).isEqualTo("Same comment");
        verifyNoInteractions(recipeStatsDomainBridgeService);
    }

    @Test
    void editRating_throwsWhenNoRatingFound() {
        var recipePublicId = UUID.randomUUID();
        var dto = new UpdateRatingDto((short) 4, null);
        when(ratingRepository.findByUser_PublicIdAndRecipe_PublicId(authUser.publicId(), recipePublicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.editRating(recipePublicId, dto))
                .isInstanceOf(NoRatingFoundException.class);

        verifyNoInteractions(recipeStatsDomainBridgeService);
    }
}
