package com.sb.sfrigola_core.domains.tags.controller;

import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.config.web.WebConfig;
import com.sb.sfrigola_core.domains.tags.dto.input.AddTagDto;
import com.sb.sfrigola_core.domains.tags.dto.input.SuggestTagDto;
import com.sb.sfrigola_core.domains.tags.dto.input.TagTranslationInputDto;
import com.sb.sfrigola_core.domains.tags.dto.input.UpdateTagDto;
import com.sb.sfrigola_core.domains.tags.dto.view.TagDetailsAdminDto;
import com.sb.sfrigola_core.domains.tags.dto.view.TagDto;
import com.sb.sfrigola_core.domains.tags.dto.view.TagPreviewAdminDto;
import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
import com.sb.sfrigola_core.domains.tags.enums.TagType;
import com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException;
import com.sb.sfrigola_core.domains.tags.exception.TagSlugAlreadyExistsException;
import com.sb.sfrigola_core.domains.tags.service.ITagService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebConfig.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @MockitoBean
    private ITagService tagService;

    @Test
    void getAllTags_returnsPagedResult() throws Exception {
        var tag = new TagDto(UUID.randomUUID(), "vegan", "Vegan");
        var paged = new SCPagedResult<>(List.of(tag), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(tagService.getAll(any(), eq("en"))).thenReturn(paged);

        mockMvc.perform(get("/api/tags").param("page", "0").param("take", "10").param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("vegan"))
                .andExpect(jsonPath("$.option.totalElements").value(1));
    }

    @Test
    void getAllTags_missingLocale_returns400() throws Exception {
        mockMvc.perform(get("/api/tags").param("page", "0").param("take", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllTags_takeBelowMinimum_returns400() throws Exception {
        mockMvc.perform(get("/api/tags").param("page", "0").param("take", "0").param("locale", "en"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllTags_invalidScope_returns400WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/tags").param("locale", "en").param("scope", "not-a-scope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.INVALID_ENUM_CODE").exists());
    }

    @Test
    void suggestNewTag_withValidBody_returns200() throws Exception {
        var dto = new SuggestTagDto("spicy", TagType.FLAVOR, TagScope.BOTH, "Spicy");
        when(tagService.suggestNewTag(eq(dto), eq("en"))).thenReturn(dto);

        mockMvc.perform(post("/api/tags/suggest")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("spicy"));
    }

    @Test
    void suggestNewTag_blankLabel_returns400() throws Exception {
        var invalidDto = new SuggestTagDto("spicy", TagType.FLAVOR, TagScope.BOTH, " ");

        mockMvc.perform(post("/api/tags/suggest")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.translationByConsumerLang").exists());
    }

    @Test
    void suggestNewTag_missingLocale_returns400() throws Exception {
        var dto = new SuggestTagDto("spicy", TagType.FLAVOR, TagScope.BOTH, "Spicy");

        mockMvc.perform(post("/api/tags/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void suggestNewTag_slugAlreadyExists_returns409WithErrorCode() throws Exception {
        var dto = new SuggestTagDto("spicy", TagType.FLAVOR, TagScope.BOTH, "Spicy");
        when(tagService.suggestNewTag(eq(dto), eq("en"))).thenThrow(new TagSlugAlreadyExistsException("spicy"));

        mockMvc.perform(post("/api/tags/suggest")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorData.errorMessageMap.TAG_SLUG_ALREADY_EXISTS").exists());
    }

    @Test
    void getAllTagsAdmin_returnsPagedResult() throws Exception {
        var preview = new TagPreviewAdminDto(UUID.randomUUID(), "vegan", TagType.DIETARY, TagScope.BOTH, TagStatus.APPROVED,
                "Vegan", Map.of("en", "English"));
        var paged = new SCPagedResult<>(List.of(preview), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(tagService.getAllAdmin(any(), eq("en"))).thenReturn(paged);

        mockMvc.perform(get("/api/tags/admin").param("page", "0").param("take", "10").param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("vegan"))
                .andExpect(jsonPath("$.option.totalElements").value(1));
    }

    @Test
    void getAllTagsAdmin_missingLocale_returns400() throws Exception {
        mockMvc.perform(get("/api/tags/admin").param("page", "0").param("take", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllTagsAdmin_invalidStatus_returns400WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/tags/admin").param("locale", "en").param("status", "not-a-status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.INVALID_ENUM_CODE").exists());
    }

    @Test
    void getTagByPublicIdAdmin_returnsDetails() throws Exception {
        var publicId = UUID.randomUUID();
        var details = new TagDetailsAdminDto(publicId, "vegan", TagType.DIETARY, TagScope.BOTH, TagStatus.APPROVED, "Vegan");
        when(tagService.getByPublicIdAdmin(publicId, "en")).thenReturn(details);

        mockMvc.perform(get("/api/tags/admin/{publicId}", publicId).param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("vegan"));
    }

    @Test
    void getTagByPublicIdAdmin_notFound_returns404WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();
        when(tagService.getByPublicIdAdmin(publicId, "en")).thenThrow(new NoTagFoundException(publicId));

        mockMvc.perform(get("/api/tags/admin/{publicId}", publicId).param("locale", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorData.errorMessageMap.TAG_NOT_FOUND").exists());
    }

    @Test
    void createTag_withValidBody_returns200() throws Exception {
        var dto = new AddTagDto("vegan", TagType.DIETARY, TagScope.BOTH, List.of(new TagTranslationInputDto("en", "Vegan")));
        var created = new TagPreviewAdminDto(UUID.randomUUID(), "vegan", TagType.DIETARY, TagScope.BOTH, TagStatus.APPROVED,
                "Vegan", Map.of("en", "English"));
        when(tagService.createNewTag(eq(dto), eq("en"))).thenReturn(created);

        mockMvc.perform(post("/api/tags/admin")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("vegan"));
    }

    @Test
    void createTag_missingTranslations_returns400() throws Exception {
        var invalidDto = new AddTagDto("vegan", TagType.DIETARY, TagScope.BOTH, List.of());

        mockMvc.perform(post("/api/tags/admin")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.translations").exists());
    }

    @Test
    void createTag_missingSlug_returns400() throws Exception {
        var invalidDto = new AddTagDto(null, TagType.DIETARY, TagScope.BOTH, List.of(new TagTranslationInputDto("en", "Vegan")));

        mockMvc.perform(post("/api/tags/admin")
                        .param("locale", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.slug").exists());
    }

    @Test
    void updateTagStatus_returns200() throws Exception {
        var publicId = UUID.randomUUID();
        when(tagService.updateTagStatus(publicId, TagStatus.APPROVED)).thenReturn(true);

        mockMvc.perform(patch("/api/tags/admin/{publicId}/status/{status}", publicId, "approved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Tag status updated successfully"));

        verify(tagService).updateTagStatus(publicId, TagStatus.APPROVED);
    }

    @Test
    void updateTagStatus_invalidStatus_returns400WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();

        mockMvc.perform(patch("/api/tags/admin/{publicId}/status/{status}", publicId, "not-a-status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.INVALID_ENUM_CODE").exists());
    }

    @Test
    void updateTagStatus_tagNotFound_returns404WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();
        when(tagService.updateTagStatus(publicId, TagStatus.REJECTED)).thenThrow(new NoTagFoundException(publicId));

        mockMvc.perform(patch("/api/tags/admin/{publicId}/status/{status}", publicId, "rejected"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorData.errorMessageMap.TAG_NOT_FOUND").exists());
    }

    @Test
    void updateTag_withValidBody_returns200() throws Exception {
        var publicId = UUID.randomUUID();
        var dto = new UpdateTagDto("vegan", TagType.DIETARY, TagScope.BOTH, new TagTranslationInputDto("en", "Vegan"));
        var updated = new TagPreviewAdminDto(publicId, "vegan", TagType.DIETARY, TagScope.BOTH, TagStatus.APPROVED,
                "Vegan", Map.of("en", "English"));
        when(tagService.updateTag(eq(publicId), eq(dto))).thenReturn(updated);

        mockMvc.perform(put("/api/tags/admin/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("vegan"));
    }

    @Test
    void updateTag_missingTranslation_returns400() throws Exception {
        var publicId = UUID.randomUUID();
        var invalidDto = new UpdateTagDto("vegan", TagType.DIETARY, TagScope.BOTH, null);

        mockMvc.perform(put("/api/tags/admin/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.specificTranslation").exists());
    }

    @Test
    void updateTag_slugAlreadyExists_returns409WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();
        var dto = new UpdateTagDto("spicy", TagType.FLAVOR, TagScope.BOTH, new TagTranslationInputDto("en", "Spicy"));
        when(tagService.updateTag(eq(publicId), eq(dto))).thenThrow(new TagSlugAlreadyExistsException("spicy"));

        mockMvc.perform(put("/api/tags/admin/{publicId}", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorData.errorMessageMap.TAG_SLUG_ALREADY_EXISTS").exists());
    }

    @Test
    void deleteTag_returnsDeletedPublicId() throws Exception {
        var publicId = UUID.randomUUID();
        when(tagService.deleteTag(publicId)).thenReturn(publicId);

        mockMvc.perform(delete("/api/tags/admin/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(publicId.toString()));

        verify(tagService).deleteTag(publicId);
    }

    @Test
    void deleteTag_notFound_returns404WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();
        when(tagService.deleteTag(publicId)).thenThrow(new NoTagFoundException(publicId));

        mockMvc.perform(delete("/api/tags/admin/{publicId}", publicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorData.errorMessageMap.TAG_NOT_FOUND").exists());
    }
}
