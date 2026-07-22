package com.sb.sfrigola_core.domains.languages.controller;

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
    void getLanguages_defaultParams_returnsAllLanguages() throws Exception {
        var language = new LanguageDto("en", "English");
        when(languageService.getAllLanguages(isNull())).thenReturn(List.of(language));

        mockMvc.perform(get("/api/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("en"))
                .andExpect(jsonPath("$.data[0].name").value("English"));
    }

    @Test
    void getLanguages_isActiveFilter_passesFlagThrough() throws Exception {
        var language = new LanguageDto("en", "English");
        when(languageService.getAllLanguages(eq(true))).thenReturn(List.of(language));

        mockMvc.perform(get("/api/languages").param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("en"));
    }

    @Test
    void getLanguages_noResults_returnsEmptyList() throws Exception {
        when(languageService.getAllLanguages(isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
