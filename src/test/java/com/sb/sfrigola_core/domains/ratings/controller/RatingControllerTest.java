package com.sb.sfrigola_core.domains.ratings.controller;

import com.sb.sfrigola_core.config.web.WebConfig;
import com.sb.sfrigola_core.domains.ratings.dto.input.AddRatingDto;
import com.sb.sfrigola_core.domains.ratings.dto.input.UpdateRatingDto;
import com.sb.sfrigola_core.domains.ratings.dto.view.RatingDto;
import com.sb.sfrigola_core.domains.ratings.dto.view.RecipeRatingStatsDto;
import com.sb.sfrigola_core.domains.ratings.exception.NoRatingFoundException;
import com.sb.sfrigola_core.domains.ratings.exception.RatingAlreadyExistsException;
import com.sb.sfrigola_core.domains.ratings.service.IRatingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RatingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebConfig.class)
class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @MockitoBean
    private IRatingService ratingService;

    @Test
    void getRatingStats_returnsStats() throws Exception {
        var recipePublicId = UUID.randomUUID();
        var stats = new RecipeRatingStatsDto(new BigDecimal("4.50"), 12);
        when(ratingService.getRatingStats(recipePublicId)).thenReturn(stats);

        mockMvc.perform(get("/api/ratings/recipe/{recipePublicId}/stats", recipePublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating").value(4.50))
                .andExpect(jsonPath("$.data.totalRatings").value(12));
    }

    @Test
    void addRating_withValidBody_returns200() throws Exception {
        var recipePublicId = UUID.randomUUID();
        var dto = new AddRatingDto(recipePublicId, (short) 5, "Great recipe");
        var created = new RatingDto(UUID.randomUUID(), recipePublicId, (short) 5, "Great recipe");
        when(ratingService.addRating(eq(dto))).thenReturn(created);

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(5))
                .andExpect(jsonPath("$.data.comment").value("Great recipe"));
    }

    @Test
    void addRating_missingRecipeId_returns400() throws Exception {
        var invalidDto = new AddRatingDto(null, (short) 5, null);

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.recipePublicId").exists());
    }

    @Test
    void addRating_scoreBelowMinimum_returns400() throws Exception {
        var invalidDto = new AddRatingDto(UUID.randomUUID(), (short) 0, null);

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.score").exists());
    }

    @Test
    void addRating_scoreAboveMaximum_returns400() throws Exception {
        var invalidDto = new AddRatingDto(UUID.randomUUID(), (short) 6, null);

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.score").exists());
    }

    @Test
    void addRating_scoreMissing_returns400() throws Exception {
        var invalidDto = new AddRatingDto(UUID.randomUUID(), null, null);

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.score").exists());
    }

    @Test
    void addRating_commentTooLong_returns400() throws Exception {
        var invalidDto = new AddRatingDto(UUID.randomUUID(), (short) 4, "a".repeat(2001));

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.comment").exists());
    }

    @Test
    void addRating_alreadyExists_returns409WithErrorCode() throws Exception {
        var recipePublicId = UUID.randomUUID();
        var dto = new AddRatingDto(recipePublicId, (short) 5, null);
        when(ratingService.addRating(eq(dto))).thenThrow(new RatingAlreadyExistsException(recipePublicId));

        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorData.errorMessageMap.RATING_ALREADY_EXISTS").exists());
    }

    @Test
    void editRating_withValidBody_returns200() throws Exception {
        var recipePublicId = UUID.randomUUID();
        var dto = new UpdateRatingDto((short) 3, "Updated comment");
        var updated = new RatingDto(UUID.randomUUID(), recipePublicId, (short) 3, "Updated comment");
        when(ratingService.editRating(eq(recipePublicId), eq(dto))).thenReturn(updated);

        mockMvc.perform(put("/api/ratings/recipe/{recipePublicId}", recipePublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(3))
                .andExpect(jsonPath("$.data.comment").value("Updated comment"));
    }

    @Test
    void editRating_scoreOutOfRange_returns400() throws Exception {
        var recipePublicId = UUID.randomUUID();
        var invalidDto = new UpdateRatingDto((short) 6, null);

        mockMvc.perform(put("/api/ratings/recipe/{recipePublicId}", recipePublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.score").exists());
    }

    @Test
    void editRating_notFound_returns404WithErrorCode() throws Exception {
        var recipePublicId = UUID.randomUUID();
        var dto = new UpdateRatingDto((short) 3, null);
        when(ratingService.editRating(eq(recipePublicId), eq(dto))).thenThrow(new NoRatingFoundException(recipePublicId));

        mockMvc.perform(put("/api/ratings/recipe/{recipePublicId}", recipePublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorData.errorMessageMap.RATING_NOT_FOUND").exists());
    }
}
