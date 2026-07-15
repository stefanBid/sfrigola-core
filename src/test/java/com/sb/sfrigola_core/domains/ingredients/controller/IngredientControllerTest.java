package com.sb.sfrigola_core.domains.ingredients.controller;

import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.config.web.WebConfig;
import com.sb.sfrigola_core.domains.ingredients.dto.input.AddIngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.input.IngredientTranslationInputDto;
import com.sb.sfrigola_core.domains.ingredients.dto.input.UpdateIngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.view.IngredientDetailsAdminDto;
import com.sb.sfrigola_core.domains.ingredients.dto.view.IngredientDto;
import com.sb.sfrigola_core.domains.ingredients.dto.view.IngredientPreviewAdminDto;
import com.sb.sfrigola_core.domains.ingredients.enums.IngredientFoodGroup;
import com.sb.sfrigola_core.domains.ingredients.exception.IngredientSlugAlreadyExistsException;
import com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException;
import com.sb.sfrigola_core.domains.ingredients.service.IIngredientService;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IngredientController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebConfig.class)
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @MockitoBean
    private IIngredientService ingredientService;

    // =========================================================
    // GET /ingredients
    // =========================================================

    @Test
    void getAllIngredients_returnsPagedResult() throws Exception {
        var ingredient = new IngredientDto(UUID.randomUUID(), "tomato", "Tomato", IngredientFoodGroup.VEGETABLE,
                new BigDecimal("18.00"), new String[]{}, true, true, true);
        var paged = new SCPagedResult<>(List.of(ingredient), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(ingredientService.getAll(any(), eq("en"))).thenReturn(paged);

        mockMvc.perform(get("/api/ingredients").param("page", "0").param("take", "10").param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("tomato"))
                .andExpect(jsonPath("$.data[0].name").value("Tomato"))
                .andExpect(jsonPath("$.option.totalElements").value(1));
    }

    @Test
    void getAllIngredients_missingLocale_returns400() throws Exception {
        mockMvc.perform(get("/api/ingredients").param("page", "0").param("take", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllIngredients_takeBelowMinimum_returns400() throws Exception {
        mockMvc.perform(get("/api/ingredients").param("page", "0").param("take", "0").param("locale", "en"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllIngredients_takeAboveMaximum_returns400() throws Exception {
        mockMvc.perform(get("/api/ingredients").param("page", "0").param("take", "101").param("locale", "en"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    // GET /ingredients/admin
    // =========================================================

    @Test
    void getAllAdmin_returnsPagedResult() throws Exception {
        var preview = new IngredientPreviewAdminDto(UUID.randomUUID(), "tomato", IngredientFoodGroup.VEGETABLE,
                new BigDecimal("18.00"), new String[]{}, true, true, true, "Tomato", Map.of("en", "English"));
        var paged = new SCPagedResult<>(List.of(preview), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(ingredientService.getAllAdmin(any(), eq("en"))).thenReturn(paged);

        mockMvc.perform(get("/api/ingredients/admin")
                        .param("page", "0").param("take", "10").param("locale", "en")
                        .param("foodGroup", "vegetable")
                        .param("isVegetarian", "true")
                        .param("isVegan", "true")
                        .param("isGlutenFree", "true")
                        .param("minCalories", "0")
                        .param("maxCalories", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("tomato"))
                .andExpect(jsonPath("$.data[0].namePreview").value("Tomato"))
                .andExpect(jsonPath("$.option.totalElements").value(1));
    }

    @Test
    void getAllAdmin_missingLocale_returns400() throws Exception {
        mockMvc.perform(get("/api/ingredients/admin").param("page", "0").param("take", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllAdmin_invalidSortPattern_returns400() throws Exception {
        mockMvc.perform(get("/api/ingredients/admin")
                        .param("page", "0").param("take", "10").param("locale", "en")
                        .param("sort", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllAdmin_invalidFoodGroup_returns400WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/ingredients/admin")
                        .param("page", "0").param("take", "10").param("locale", "en")
                        .param("foodGroup", "not-a-food-group"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.INVALID_ENUM_CODE").exists());
    }

    @Test
    void getAllAdmin_invalidSortBy_returns400WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/ingredients/admin")
                        .param("page", "0").param("take", "10").param("locale", "en")
                        .param("sortBy", "not-a-field"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.INVALID_ENUM_CODE").exists());
    }

    // =========================================================
    // GET /ingredients/admin/{publicId}
    // =========================================================

    @Test
    void getIngredientByPublicIdAdmin_returns200() throws Exception {
        var publicId = UUID.randomUUID();
        var details = new IngredientDetailsAdminDto(publicId, "tomato", IngredientFoodGroup.VEGETABLE,
                new BigDecimal("18.00"), new String[]{}, true, true, true, "Tomato", List.of());
        when(ingredientService.getByPublicIdAdmin(publicId, "en")).thenReturn(details);

        mockMvc.perform(get("/api/ingredients/admin/{publicId}", publicId).param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("tomato"))
                .andExpect(jsonPath("$.data.specificTranslationName").value("Tomato"));
    }

    @Test
    void getIngredientByPublicIdAdmin_notFound_returns404WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();
        when(ingredientService.getByPublicIdAdmin(publicId, "en")).thenThrow(new NoIngredientFoundException(publicId));

        mockMvc.perform(get("/api/ingredients/admin/{publicId}", publicId).param("locale", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorData.errorMessageMap.INGREDIENT_NOT_FOUND").exists());
    }

    @Test
    void getIngredientByPublicIdAdmin_missingLocale_returns400() throws Exception {
        var publicId = UUID.randomUUID();

        mockMvc.perform(get("/api/ingredients/admin/{publicId}", publicId))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    // POST /ingredients/admin
    // =========================================================

    @Test
    void createIngredient_withValidBody_returns200() throws Exception {
        var dto = new AddIngredientDto("tomato", IngredientFoodGroup.VEGETABLE, new BigDecimal("18.00"),
                new String[]{}, true, true, true, List.of(), List.of(new IngredientTranslationInputDto("en", "Tomato")));
        var created = new IngredientPreviewAdminDto(UUID.randomUUID(), "tomato", IngredientFoodGroup.VEGETABLE,
                new BigDecimal("18.00"), new String[]{}, true, true, true, "Tomato", Map.of("en", "English"));
        when(ingredientService.createNewIngredient(argThat(actual -> matchesAddDto(actual, dto)), eq("en"))).thenReturn(created);

        mockMvc.perform(post("/api/ingredients/admin")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("tomato"))
                .andExpect(jsonPath("$.data.namePreview").value("Tomato"));
    }

    @Test
    void createIngredient_missingTranslations_returns400() throws Exception {
        var invalidDto = new AddIngredientDto("tomato", IngredientFoodGroup.VEGETABLE, new BigDecimal("18.00"),
                new String[]{}, true, true, true, List.of(), List.of());

        mockMvc.perform(post("/api/ingredients/admin")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.translations").exists());
    }

    @Test
    void createIngredient_invalidSlugFormat_returns400() throws Exception {
        var invalidDto = new AddIngredientDto("Invalid Slug!", IngredientFoodGroup.VEGETABLE, new BigDecimal("18.00"),
                new String[]{}, true, true, true, List.of(), List.of(new IngredientTranslationInputDto("en", "Tomato")));

        mockMvc.perform(post("/api/ingredients/admin")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.slug").exists());
    }

    @Test
    void createIngredient_negativeCalories_returns400() throws Exception {
        var invalidDto = new AddIngredientDto("tomato", IngredientFoodGroup.VEGETABLE, new BigDecimal("-1.00"),
                new String[]{}, true, true, true, List.of(), List.of(new IngredientTranslationInputDto("en", "Tomato")));

        mockMvc.perform(post("/api/ingredients/admin")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.caloriesPer100g").exists());
    }

    @Test
    void createIngredient_slugAlreadyExists_returns409WithErrorCode() throws Exception {
        var dto = new AddIngredientDto("tomato", IngredientFoodGroup.VEGETABLE, new BigDecimal("18.00"),
                new String[]{}, true, true, true, List.of(), List.of(new IngredientTranslationInputDto("en", "Tomato")));
        when(ingredientService.createNewIngredient(argThat(actual -> matchesAddDto(actual, dto)), eq("en"))).thenThrow(new IngredientSlugAlreadyExistsException("tomato"));

        mockMvc.perform(post("/api/ingredients/admin")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorData.errorMessageMap.INGREDIENT_SLUG_ALREADY_EXISTS").exists());
    }

    @Test
    void createIngredient_missingLocale_returns400() throws Exception {
        var dto = new AddIngredientDto("tomato", IngredientFoodGroup.VEGETABLE, new BigDecimal("18.00"),
                new String[]{}, true, true, true, List.of(), List.of(new IngredientTranslationInputDto("en", "Tomato")));

        mockMvc.perform(post("/api/ingredients/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    // PUT /ingredients/admin/{publicId}
    // =========================================================

    @Test
    void updateIngredient_withValidBody_returns200() throws Exception {
        var publicId = UUID.randomUUID();
        var dto = new UpdateIngredientDto("tomato", IngredientFoodGroup.VEGETABLE, new BigDecimal("18.00"),
                new String[]{}, true, true, true, List.of(), new IngredientTranslationInputDto("en", "Tomato"));
        var updated = new IngredientPreviewAdminDto(publicId, "tomato", IngredientFoodGroup.VEGETABLE,
                new BigDecimal("18.00"), new String[]{}, true, true, true, "Tomato", Map.of("en", "English"));
        when(ingredientService.updateIngredient(eq(publicId), argThat(actual -> matchesUpdateDto(actual, dto)))).thenReturn(updated);

        mockMvc.perform(put("/api/ingredients/admin/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("tomato"));
    }

    @Test
    void updateIngredient_missingSpecificTranslation_returns400() throws Exception {
        var publicId = UUID.randomUUID();
        var invalidDto = new UpdateIngredientDto("tomato", IngredientFoodGroup.VEGETABLE, new BigDecimal("18.00"),
                new String[]{}, true, true, true, List.of(), null);

        mockMvc.perform(put("/api/ingredients/admin/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.specificTranslation").exists());
    }

    @Test
    void updateIngredient_notFound_returns404WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();
        var dto = new UpdateIngredientDto("tomato", IngredientFoodGroup.VEGETABLE, new BigDecimal("18.00"),
                new String[]{}, true, true, true, List.of(), new IngredientTranslationInputDto("en", "Tomato"));
        when(ingredientService.updateIngredient(eq(publicId), argThat(actual -> matchesUpdateDto(actual, dto)))).thenThrow(new NoIngredientFoundException(publicId));

        mockMvc.perform(put("/api/ingredients/admin/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorData.errorMessageMap.INGREDIENT_NOT_FOUND").exists());
    }

    // =========================================================
    // DELETE /ingredients/admin/{publicId}
    // =========================================================

    @Test
    void deleteIngredient_returnsDeletedPublicId() throws Exception {
        var publicId = UUID.randomUUID();
        when(ingredientService.deleteIngredient(publicId)).thenReturn(publicId);

        mockMvc.perform(delete("/api/ingredients/admin/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(publicId.toString()));

        verify(ingredientService).deleteIngredient(publicId);
    }

    @Test
    void deleteIngredient_notFound_returns404WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();
        when(ingredientService.deleteIngredient(publicId)).thenThrow(new NoIngredientFoundException(publicId));

        mockMvc.perform(delete("/api/ingredients/admin/{publicId}", publicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorData.errorMessageMap.INGREDIENT_NOT_FOUND").exists());
    }

    // =========================================================
    // PRIVATE — DTO matchers (records with array fields don't get value equality for free)
    // =========================================================

    private static boolean matchesAddDto(AddIngredientDto actual, AddIngredientDto expected) {
        return actual != null
                && actual.slug().equals(expected.slug())
                && actual.foodGroup() == expected.foodGroup()
                && actual.caloriesPer100g().equals(expected.caloriesPer100g())
                && Arrays.equals(actual.allergens(), expected.allergens())
                && actual.isVegetarian() == expected.isVegetarian()
                && actual.isVegan() == expected.isVegan()
                && actual.isGlutenFree() == expected.isGlutenFree()
                && actual.ingredientTagsIds().equals(expected.ingredientTagsIds())
                && actual.translations().equals(expected.translations());
    }

    private static boolean matchesUpdateDto(UpdateIngredientDto actual, UpdateIngredientDto expected) {
        return actual != null
                && actual.slug().equals(expected.slug())
                && actual.foodGroup() == expected.foodGroup()
                && actual.caloriesPer100g().equals(expected.caloriesPer100g())
                && Arrays.equals(actual.allergens(), expected.allergens())
                && actual.isVegetarian() == expected.isVegetarian()
                && actual.isVegan() == expected.isVegan()
                && actual.isGlutenFree() == expected.isGlutenFree()
                && actual.ingredientTagsIds().equals(expected.ingredientTagsIds())
                && actual.specificTranslation().equals(expected.specificTranslation());
    }
}
