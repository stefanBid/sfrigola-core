package com.sb.sfrigola_core.domains.languages.service.impl;

import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.exception.NoValidLangCodeToChangeException;
import com.sb.sfrigola_core.domains.languages.repository.ILanguageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LanguageServiceImplTest {

    @Mock
    private ILanguageRepository languageRepository;

    private LanguageServiceImpl languageService;

    @BeforeEach
    void setUp() {
        languageService = new LanguageServiceImpl(languageRepository);
    }

    private Language buildLanguage(String code, String name) {
        var language = new Language();
        language.setCode(code);
        language.setName(name);
        return language;
    }

    @Test
    void getAllLanguages_isActiveTrue_returnsOnlyActiveLanguages() {
        var english = buildLanguage("en", "English");
        when(languageRepository.findAllByIsActiveTrue()).thenReturn(List.of(english));

        var result = languageService.getAllLanguages(true);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().code()).isEqualTo("en");
        assertThat(result.getFirst().name()).isEqualTo("English");
        verify(languageRepository, never()).findAll();
    }

    @Test
    void getAllLanguages_isActiveFalse_returnsAllLanguagesRegardlessOfStatus() {
        var english = buildLanguage("en", "English");
        var german = buildLanguage("de", "Deutsch");
        when(languageRepository.findAll()).thenReturn(List.of(english, german));

        var result = languageService.getAllLanguages(false);

        assertThat(result).hasSize(2);
        verify(languageRepository, never()).findAllByIsActiveTrue();
    }

    @Test
    void getAllLanguages_isActiveNull_returnsAllLanguages() {
        when(languageRepository.findAll()).thenReturn(List.of());

        var result = languageService.getAllLanguages(null);

        assertThat(result).isEmpty();
        verify(languageRepository, never()).findAllByIsActiveTrue();
    }

    @Test
    void getAllLanguages_noResults_returnsEmptyContent() {
        when(languageRepository.findAllByIsActiveTrue()).thenReturn(List.of());

        var result = languageService.getAllLanguages(true);

        assertThat(result).isEmpty();
    }

    @Test
    void existsByCodeOrThrow_returnsTrueWhenCodeExists() {
        when(languageRepository.existsByCode("en")).thenReturn(true);

        var result = languageService.existsByCodeOrThrow("en");

        assertThat(result).isTrue();
    }

    @Test
    void existsByCodeOrThrow_throwsWhenCodeDoesNotExist() {
        when(languageRepository.existsByCode("xx")).thenReturn(false);

        assertThatThrownBy(() -> languageService.existsByCodeOrThrow("xx"))
                .isInstanceOf(NoValidLangCodeToChangeException.class);
    }
}
