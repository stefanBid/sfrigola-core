package com.sb.sfrigola_core.domains.languages.controller;

import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.config.web.WebConfig;
import com.sb.sfrigola_core.domains.languages.dto.LanguageDto;
import com.sb.sfrigola_core.domains.languages.service.ILanguageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LanguagesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebConfig.class)
class LanguagesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ILanguageService languageService;

    @Test
    void getLanguages_defaultParams_returnsPagedResult() throws Exception {
        var language = new LanguageDto("en", "English");
        var paged = new SCPagedResult<>(List.of(language), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(languageService.getAllLanguages(any(), isNull())).thenReturn(paged);

        mockMvc.perform(get("/api/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("en"))
                .andExpect(jsonPath("$.data[0].name").value("English"))
                .andExpect(jsonPath("$.option.totalElements").value(1));
    }

    @Test
    void getLanguages_withPaginationAndSort_returnsPagedResult() throws Exception {
        var language = new LanguageDto("it", "Italiano");
        var paged = new SCPagedResult<>(List.of(language), SCPagedOptionDto.of(1, 5, 6L, 2, false));
        when(languageService.getAllLanguages(any(), isNull())).thenReturn(paged);

        mockMvc.perform(get("/api/languages")
                        .param("page", "1")
                        .param("take", "5")
                        .param("sortBy", "code")
                        .param("sort", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("it"))
                .andExpect(jsonPath("$.option.currentPage").value(1))
                .andExpect(jsonPath("$.option.totalPages").value(2));
    }

    @Test
    void getLanguages_isActiveFilter_passesFlagThrough() throws Exception {
        var language = new LanguageDto("en", "English");
        var paged = new SCPagedResult<>(List.of(language), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(languageService.getAllLanguages(any(), eq(true))).thenReturn(paged);

        mockMvc.perform(get("/api/languages").param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("en"));
    }

    @Test
    void getLanguages_noResults_returnsEmptyList() throws Exception {
        var paged = new SCPagedResult<LanguageDto>(List.of(), SCPagedOptionDto.noItems());
        when(languageService.getAllLanguages(any(), isNull())).thenReturn(paged);

        mockMvc.perform(get("/api/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void getLanguages_pageBelowMinimum_returns400() throws Exception {
        mockMvc.perform(get("/api/languages").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getLanguages_takeBelowMinimum_returns400() throws Exception {
        mockMvc.perform(get("/api/languages").param("take", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getLanguages_takeAboveMaximum_returns400() throws Exception {
        mockMvc.perform(get("/api/languages").param("take", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getLanguages_invalidSortBy_returns400WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/languages").param("sortBy", "unknownField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.INVALID_ENUM_CODE").exists());
    }

    @Test
    void getLanguages_invalidSortDirection_returns400WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/languages").param("sort", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.INVALID_ENUM_CODE").exists());
    }
}
