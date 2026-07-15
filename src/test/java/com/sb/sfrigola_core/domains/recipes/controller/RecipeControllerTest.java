package com.sb.sfrigola_core.domains.recipes.controller;

import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.config.web.WebConfig;
import com.sb.sfrigola_core.domains.recipes.dto.input.AddRecipeDto;
import com.sb.sfrigola_core.domains.recipes.dto.input.RecipeIngredientInputDto;
import com.sb.sfrigola_core.domains.recipes.dto.input.RecipeTranslationInputDto;
import com.sb.sfrigola_core.domains.recipes.dto.input.UpdateRecipeDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipeDetailsAdminDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipeDetailsDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipeDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipePreviewAdminDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipesFeedDto;
import com.sb.sfrigola_core.domains.recipes.enums.DifficultyLevel;
import com.sb.sfrigola_core.domains.recipes.enums.FeedType;
import com.sb.sfrigola_core.domains.recipes.enums.SeasonType;
import com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException;
import com.sb.sfrigola_core.domains.recipes.exception.RecipeAuthorMismatchException;
import com.sb.sfrigola_core.domains.recipes.service.IRecipeService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebConfig.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @MockitoBean
    private IRecipeService recipeService;

    private RecipeDto sampleRecipeDto() {
        return new RecipeDto(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Carbonara", "Classic Roman pasta",
                BigDecimal.valueOf(4.5), false, 30, BigDecimal.valueOf(2.0)
        );
    }

    private RecipePreviewAdminDto sampleAdminPreviewDto(UUID publicId, boolean isPublished) {
        return new RecipePreviewAdminDto(
                publicId, UUID.randomUUID(), UUID.randomUUID(),
                DifficultyLevel.EASY, null, SeasonType.ALL_YEAR,
                10, 20, (short) 2, false, false, false,
                isPublished, "Carbonara", Map.of("en", "English"),
                BigDecimal.ZERO, 0, 0
        );
    }

    private AddRecipeDto sampleAddRecipeDto() {
        return new AddRecipeDto(
                null, DifficultyLevel.EASY, null, SeasonType.ALL_YEAR,
                10, 20, (short) 2, false, false, false,
                null, null,
                List.of(new RecipeTranslationInputDto("en", "Carbonara", "desc", List.of("Boil pasta", "Mix eggs")))
        );
    }

    private UpdateRecipeDto sampleUpdateRecipeDto() {
        return new UpdateRecipeDto(
                null, DifficultyLevel.EASY, null, SeasonType.ALL_YEAR,
                10, 20, (short) 2, false, false, false,
                null, null,
                new RecipeTranslationInputDto("en", "Carbonara Updated", "desc", List.of("Boil pasta"))
        );
    }

    // ---------- getAllRecipesHomeFeed ----------

    @Test
    void getAllRecipesHomeFeed_returns200() throws Exception {
        var categoryId = UUID.randomUUID();
        var feed = Map.of(FeedType.QUICK, new RecipesFeedDto(List.of(sampleRecipeDto()), (short) 1));
        when(recipeService.getAllHomeFeed(categoryId, "en")).thenReturn(feed);

        mockMvc.perform(get("/api/recipes/home/category/{categoryId}", categoryId).param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.QUICK.sortOrder").value(1));
    }

    @Test
    void getAllRecipesHomeFeed_missingLocale_returns400() throws Exception {
        var categoryId = UUID.randomUUID();

        mockMvc.perform(get("/api/recipes/home/category/{categoryId}", categoryId))
                .andExpect(status().isBadRequest());
    }

    // ---------- getRecipesByGroup ----------

    @Test
    void getRecipesByGroup_returns200() throws Exception {
        var paged = new SCPagedResult<>(List.of(sampleRecipeDto()), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(recipeService.getAllByFeed(eq(FeedType.QUICK), any(), eq("en"))).thenReturn(paged);

        mockMvc.perform(get("/api/recipes/feed/{feedType}", "QUICK").param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].specificTranslationTitle").value("Carbonara"))
                .andExpect(jsonPath("$.option.totalElements").value(1));
    }

    @Test
    void getRecipesByGroup_takeAboveMax_returns400() throws Exception {
        mockMvc.perform(get("/api/recipes/feed/{feedType}", "QUICK").param("locale", "en").param("take", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRecipesByGroup_missingLocale_returns400() throws Exception {
        mockMvc.perform(get("/api/recipes/feed/{feedType}", "QUICK"))
                .andExpect(status().isBadRequest());
    }

    // ---------- searchRecipes ----------

    @Test
    void searchRecipes_returns200() throws Exception {
        var paged = new SCPagedResult<>(List.of(sampleRecipeDto()), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(recipeService.searchRecipes(any(), eq(null), eq("en"))).thenReturn(paged);

        mockMvc.perform(get("/api/recipes/search").param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].specificTranslationTitle").value("Carbonara"));
    }

    @Test
    void searchRecipes_withCategory_returns200() throws Exception {
        var categoryId = UUID.randomUUID();
        var paged = new SCPagedResult<>(List.of(sampleRecipeDto()), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(recipeService.searchRecipes(any(), eq(categoryId), eq("en"))).thenReturn(paged);

        mockMvc.perform(get("/api/recipes/search/category/{categoryId}", categoryId).param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].specificTranslationTitle").value("Carbonara"));
    }

    // ---------- getAllMyFavoriteRecipes ----------

    @Test
    void getAllMyFavoriteRecipes_returns200() throws Exception {
        var paged = new SCPagedResult<>(List.of(sampleRecipeDto()), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(recipeService.getAllMyFavoriteRecipes(any(), eq("en"))).thenReturn(paged);

        mockMvc.perform(get("/api/recipes/favorites").param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].specificTranslationTitle").value("Carbonara"));
    }

    // ---------- getRecipeByPublicId ----------

    @Test
    void getRecipeByPublicId_returns200() throws Exception {
        var publicId = UUID.randomUUID();
        var details = new RecipeDetailsDto(
                publicId, UUID.randomUUID(), UUID.randomUUID(),
                DifficultyLevel.EASY, null, SeasonType.ALL_YEAR,
                10, 20, (short) 2, false, false, false,
                "Carbonara", "desc", "Boil pasta",
                List.of(), List.of(), false, BigDecimal.ZERO, 0, 0
        );
        when(recipeService.getByPublicId(publicId, "en")).thenReturn(details);

        mockMvc.perform(get("/api/recipes/details/{publicId}", publicId).param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.specificTranslationTitle").value("Carbonara"));
    }

    @Test
    void getRecipeByPublicId_notFound_returns404WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();
        when(recipeService.getByPublicId(publicId, "en")).thenThrow(new NoRecipeFoundException(publicId));

        mockMvc.perform(get("/api/recipes/details/{publicId}", publicId).param("locale", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorData.errorMessageMap.RECIPE_NOT_FOUND").exists());
    }

    @Test
    void getRecipeByPublicId_missingLocale_returns400() throws Exception {
        var publicId = UUID.randomUUID();

        mockMvc.perform(get("/api/recipes/details/{publicId}", publicId))
                .andExpect(status().isBadRequest());
    }

    // ---------- getAllAdmin ----------

    @Test
    void getAllAdmin_returns200() throws Exception {
        var preview = sampleAdminPreviewDto(UUID.randomUUID(), true);
        var paged = new SCPagedResult<>(List.of(preview), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(recipeService.getAllAdmin(any(), eq(null), eq("en"))).thenReturn(paged);

        mockMvc.perform(get("/api/recipes/admin").param("page", "0").param("take", "10").param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].titlePreview").value("Carbonara"))
                .andExpect(jsonPath("$.option.totalElements").value(1));
    }

    @Test
    void getAllAdmin_withDietaryFilters_returns200() throws Exception {
        var paged = new SCPagedResult<RecipePreviewAdminDto>(List.of(), SCPagedOptionDto.noItems());
        when(recipeService.getAllAdmin(any(), eq(null), eq("en"))).thenReturn(paged);

        mockMvc.perform(get("/api/recipes/admin")
                        .param("locale", "en")
                        .param("difficulty", "easy")
                        .param("season", "spring")
                        .param("isVegetarian", "true")
                        .param("isPublished", "false"))
                .andExpect(status().isOk());
    }

    // ---------- getRecipeByPublicIdAdmin ----------

    @Test
    void getRecipeByPublicIdAdmin_returns200() throws Exception {
        var publicId = UUID.randomUUID();
        var details = new RecipeDetailsAdminDto(
                publicId, UUID.randomUUID(), UUID.randomUUID(),
                DifficultyLevel.EASY, null, SeasonType.ALL_YEAR,
                10, 20, (short) 2, false, false, false, false,
                "Carbonara", "desc", "Boil pasta",
                List.of(), List.of(), BigDecimal.ZERO, 0, 0
        );
        when(recipeService.getByPublicIdAdmin(publicId, "en")).thenReturn(details);

        mockMvc.perform(get("/api/recipes/admin/details/{publicId}", publicId).param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.specificTranslationTitle").value("Carbonara"));
    }

    // ---------- createRecipe ----------

    @Test
    void createRecipe_returns200() throws Exception {
        var dto = sampleAddRecipeDto();
        var created = sampleAdminPreviewDto(UUID.randomUUID(), false);
        when(recipeService.createRecipe(eq(dto), eq("en"))).thenReturn(created);

        mockMvc.perform(post("/api/recipes")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.titlePreview").value("Carbonara"));
    }

    @Test
    void createRecipe_missingTranslations_returns400() throws Exception {
        var invalidDto = new AddRecipeDto(
                null, DifficultyLevel.EASY, null, SeasonType.ALL_YEAR,
                10, 20, (short) 2, false, false, false,
                null, null, List.of()
        );

        mockMvc.perform(post("/api/recipes")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.translations").exists());
    }

    @Test
    void createRecipe_missingDifficulty_returns400() throws Exception {
        var invalidDto = new AddRecipeDto(
                null, null, null, SeasonType.ALL_YEAR,
                10, 20, (short) 2, false, false, false,
                null, null,
                List.of(new RecipeTranslationInputDto("en", "Carbonara", "desc", List.of("step")))
        );

        mockMvc.perform(post("/api/recipes")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.difficulty").exists());
    }

    // ---------- updateRecipe ----------

    @Test
    void updateRecipe_returns200() throws Exception {
        var publicId = UUID.randomUUID();
        var dto = sampleUpdateRecipeDto();
        var updated = sampleAdminPreviewDto(publicId, false);
        when(recipeService.updateRecipe(eq(publicId), eq(dto))).thenReturn(updated);

        mockMvc.perform(put("/api/recipes/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicId").value(publicId.toString()));
    }

    @Test
    void updateRecipe_authorMismatch_returns403WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();
        var dto = sampleUpdateRecipeDto();
        when(recipeService.updateRecipe(eq(publicId), eq(dto))).thenThrow(new RecipeAuthorMismatchException(publicId));

        mockMvc.perform(put("/api/recipes/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorData.errorMessageMap.NOT_RECIPE_OWNER").exists());
    }

    @Test
    void updateRecipe_missingTranslation_returns400() throws Exception {
        var publicId = UUID.randomUUID();
        var invalidDto = new UpdateRecipeDto(
                null, DifficultyLevel.EASY, null, SeasonType.ALL_YEAR,
                10, 20, (short) 2, false, false, false,
                null, null, null
        );

        mockMvc.perform(put("/api/recipes/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.specificTranslation").exists());
    }

    @Test
    void updateRecipe_notFound_returns404WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();
        var dto = sampleUpdateRecipeDto();
        when(recipeService.updateRecipe(eq(publicId), eq(dto))).thenThrow(new NoRecipeFoundException(publicId));

        mockMvc.perform(put("/api/recipes/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorData.errorMessageMap.RECIPE_NOT_FOUND").exists());
    }

    // ---------- publishRecipe / unpublishRecipe ----------

    @Test
    void publishRecipe_returns200() throws Exception {
        var publicId = UUID.randomUUID();
        var published = sampleAdminPreviewDto(publicId, true);
        when(recipeService.publishRecipe(publicId, "en")).thenReturn(published);

        mockMvc.perform(patch("/api/recipes/admin/{publicId}/publish", publicId).param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPublished").value(true));
    }

    @Test
    void unpublishRecipe_returns200() throws Exception {
        var publicId = UUID.randomUUID();
        var unpublished = sampleAdminPreviewDto(publicId, false);
        when(recipeService.unpublishRecipe(publicId, "en")).thenReturn(unpublished);

        mockMvc.perform(patch("/api/recipes/admin/{publicId}/unpublish", publicId).param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPublished").value(false));
    }

    @Test
    void publishRecipe_notFound_returns404WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();
        when(recipeService.publishRecipe(publicId, "en")).thenThrow(new NoRecipeFoundException(publicId));

        mockMvc.perform(patch("/api/recipes/admin/{publicId}/publish", publicId).param("locale", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorData.errorMessageMap.RECIPE_NOT_FOUND").exists());
    }

    // ---------- deleteRecipe ----------

    @Test
    void deleteRecipe_returnsDeletedPublicId() throws Exception {
        var publicId = UUID.randomUUID();
        when(recipeService.deleteRecipe(publicId)).thenReturn(publicId);

        mockMvc.perform(delete("/api/recipes/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(publicId.toString()));

        verify(recipeService).deleteRecipe(publicId);
    }

    @Test
    void deleteRecipe_authorMismatch_returns403WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();
        when(recipeService.deleteRecipe(publicId)).thenThrow(new RecipeAuthorMismatchException(publicId));

        mockMvc.perform(delete("/api/recipes/{publicId}", publicId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorData.errorMessageMap.NOT_RECIPE_OWNER").exists());
    }
}
