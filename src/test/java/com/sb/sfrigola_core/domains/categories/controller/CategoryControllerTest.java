package com.sb.sfrigola_core.domains.categories.controller;

import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.config.web.WebConfig;
import com.sb.sfrigola_core.domains.categories.dto.input.AddCategoryDto;
import com.sb.sfrigola_core.domains.categories.dto.input.ReorderedCategoriesTreeDto;
import com.sb.sfrigola_core.domains.categories.dto.input.UpdateCategoryDto;
import com.sb.sfrigola_core.domains.categories.dto.input.UpsetCategoryTranslationDto;
import com.sb.sfrigola_core.domains.categories.dto.view.CategoryPreviewAdminDto;
import com.sb.sfrigola_core.domains.categories.dto.view.CategoryPublicViewDto;
import com.sb.sfrigola_core.domains.categories.dto.view.CategoryTranslationAdminDto;
import com.sb.sfrigola_core.domains.categories.exception.CategoryReorderMismatchException;
import com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException;
import com.sb.sfrigola_core.domains.categories.service.ICategoryService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @MockitoBean
    private ICategoryService categoryService;

    @Test
    void getAllCategories_returnsPagedResult() throws Exception {
        var category = new CategoryPublicViewDto(UUID.randomUUID(), "pasta", null, (short) 0, true, "Pasta", "Pasta dishes");
        var paged = new SCPagedResult<>(List.of(category), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(categoryService.getAll(any(), eq("en"))).thenReturn(paged);

        mockMvc.perform(get("/api/categories").param("page", "0").param("take", "10").param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("pasta"))
                .andExpect(jsonPath("$.option.totalElements").value(1));
    }

    @Test
    void getAllCategories_missingLocale_returns400() throws Exception {
        mockMvc.perform(get("/api/categories").param("page", "0").param("take", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllCategories_takeBelowMinimum_returns400() throws Exception {
        mockMvc.perform(get("/api/categories").param("page", "0").param("take", "0").param("locale", "en"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_withValidBody_returns200() throws Exception {
        var dto = new AddCategoryDto("pasta", true, List.of(new UpsetCategoryTranslationDto("en", "Pasta", null)));
        var created = new CategoryPreviewAdminDto(UUID.randomUUID(), "pasta", null, (short) 0, true,
                new CategoryTranslationAdminDto("Pasta", null), Map.of("en", "English"));
        when(categoryService.createNewCategory(eq(dto), isNull(), eq("en"))).thenReturn(created);

        mockMvc.perform(post("/api/categories/admin")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("pasta"));
    }

    @Test
    void createCategory_missingTranslations_returns400() throws Exception {
        var invalidDto = new AddCategoryDto("pasta", true, List.of());

        mockMvc.perform(post("/api/categories/admin")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.translations").exists());
    }

    @Test
    void getCategoryByPublicIdAdmin_notFound_returns404WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();
        when(categoryService.getByPublicIdAdmin(publicId, "en")).thenThrow(new NoCategoryFoundException(publicId));

        mockMvc.perform(get("/api/categories/admin/{publicId}", publicId).param("locale", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorData.errorMessageMap.SELECTED_CATEGORY_NOT_FOUND").exists());
    }

    @Test
    void deleteCategory_returnsDeletedPublicId() throws Exception {
        var publicId = UUID.randomUUID();
        when(categoryService.deleteCategory(publicId)).thenReturn(publicId);

        mockMvc.perform(delete("/api/categories/admin/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(publicId.toString()));

        verify(categoryService).deleteCategory(publicId);
    }

    @Test
    void getAllCategoriesAdmin_returnsPagedResult() throws Exception {
        var preview = new CategoryPreviewAdminDto(UUID.randomUUID(), "pasta", null, (short) 0, true,
                new CategoryTranslationAdminDto("Pasta", null), Map.of("en", "English"));
        var paged = new SCPagedResult<>(List.of(preview), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(categoryService.getAllAdmin(any(), eq("en"), isNull())).thenReturn(paged);

        mockMvc.perform(get("/api/categories/admin").param("page", "0").param("take", "10").param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("pasta"))
                .andExpect(jsonPath("$.option.totalElements").value(1));
    }

    @Test
    void getAllCategoriesAdmin_missingLocale_returns400() throws Exception {
        mockMvc.perform(get("/api/categories/admin").param("page", "0").param("take", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategory_withValidBody_returns200() throws Exception {
        var publicId = UUID.randomUUID();
        var dto = new UpdateCategoryDto("pasta", true, new UpsetCategoryTranslationDto("en", "Pasta", null));
        var updated = new CategoryPreviewAdminDto(publicId, "pasta", null, (short) 0, true,
                new CategoryTranslationAdminDto("Pasta", null), Map.of("en", "English"));
        when(categoryService.updateCategory(eq(dto), eq(publicId))).thenReturn(updated);

        mockMvc.perform(put("/api/categories/admin/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("pasta"));
    }

    @Test
    void updateCategory_missingTranslation_returns400() throws Exception {
        var publicId = UUID.randomUUID();
        var invalidDto = new UpdateCategoryDto("pasta", true, null);

        mockMvc.perform(put("/api/categories/admin/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.specificTranslation").exists());
    }

    @Test
    void reorderCategories_withValidBody_returns200() throws Exception {
        var idA = UUID.randomUUID();
        var idB = UUID.randomUUID();
        var dto = new ReorderedCategoriesTreeDto(null, List.of(idA, idB));
        var reordered = List.of(
                new CategoryPreviewAdminDto(idA, "pasta", null, (short) 0, true, new CategoryTranslationAdminDto("Pasta", null), Map.of("en", "English")),
                new CategoryPreviewAdminDto(idB, "risotto", null, (short) 1, true, new CategoryTranslationAdminDto("Risotto", null), Map.of("en", "English"))
        );
        when(categoryService.reorderCategories(eq(dto))).thenReturn(reordered);

        mockMvc.perform(put("/api/categories/admin/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("pasta"))
                .andExpect(jsonPath("$.data[1].slug").value("risotto"));
    }

    @Test
    void reorderCategories_emptyOrderedIds_returns400() throws Exception {
        var invalidDto = new ReorderedCategoriesTreeDto(null, List.of());

        mockMvc.perform(put("/api/categories/admin/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.orderedPublicIds").exists());
    }

    @Test
    void reorderCategories_mismatch_returns400WithErrorCode() throws Exception {
        var dto = new ReorderedCategoriesTreeDto(null, List.of(UUID.randomUUID()));
        when(categoryService.reorderCategories(eq(dto))).thenThrow(new CategoryReorderMismatchException());

        mockMvc.perform(put("/api/categories/admin/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.CATEGORY_REORDER_MISMATCH").exists());
    }
}
