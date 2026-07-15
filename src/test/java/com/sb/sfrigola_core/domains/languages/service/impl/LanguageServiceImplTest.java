package com.sb.sfrigola_core.domains.languages.service.impl;

import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.enums.LanguageSortField;
import com.sb.sfrigola_core.domains.languages.exception.NoValidLangCodeToChangeException;
import com.sb.sfrigola_core.domains.languages.repository.ILanguageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        var filterQuery = SCFilterQuery.<Void>pagedOnly(10, 0);
        var pageable = PageRequest.of(0, 10);
        var english = buildLanguage("en", "English");
        var page = new PageImpl<>(List.of(english), pageable, 1);

        when(languageRepository.findAllByIsActiveTrue(pageable)).thenReturn(page);

        var result = languageService.getAllLanguages(filterQuery, true);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().code()).isEqualTo("en");
        assertThat(result.content().getFirst().name()).isEqualTo("English");
        assertThat(result.pagedOptionDto().totalElements()).isEqualTo(1L);
        verify(languageRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllLanguages_isActiveFalse_returnsAllLanguagesRegardlessOfStatus() {
        var filterQuery = SCFilterQuery.<Void>pagedOnly(10, 0);
        var pageable = PageRequest.of(0, 10);
        var english = buildLanguage("en", "English");
        var german = buildLanguage("de", "Deutsch");
        var page = new PageImpl<>(List.of(english, german), pageable, 2);

        when(languageRepository.findAll(pageable)).thenReturn(page);

        var result = languageService.getAllLanguages(filterQuery, false);

        assertThat(result.content()).hasSize(2);
        verify(languageRepository, never()).findAllByIsActiveTrue(any());
    }

    @Test
    void getAllLanguages_isActiveNull_returnsAllLanguages() {
        var filterQuery = SCFilterQuery.<Void>pagedOnly(10, 0);
        var pageable = PageRequest.of(0, 10);
        when(languageRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        var result = languageService.getAllLanguages(filterQuery, null);

        assertThat(result.content()).isEmpty();
        verify(languageRepository, never()).findAllByIsActiveTrue(any());
    }

    @Test
    void getAllLanguages_withSortByCodeDescending_buildsSortedPageable() {
        var filterQuery = SCFilterQuery.essential(LanguageSortField.CODE, SortDirection.DESC, 5, 1);
        var expectedPageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "code"));
        var italian = buildLanguage("it", "Italiano");
        var page = new PageImpl<>(List.of(italian), expectedPageable, 1);

        when(languageRepository.findAll(expectedPageable)).thenReturn(page);

        var result = languageService.getAllLanguages(filterQuery, null);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().code()).isEqualTo("it");

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(languageRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("code")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("code").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getAllLanguages_noResults_returnsEmptyContent() {
        var filterQuery = SCFilterQuery.<Void>pagedOnly(10, 0);
        var pageable = PageRequest.of(0, 10);
        when(languageRepository.findAllByIsActiveTrue(pageable)).thenReturn(Page.empty(pageable));

        var result = languageService.getAllLanguages(filterQuery, true);

        assertThat(result.content()).isEmpty();
        assertThat(result.pagedOptionDto().totalElements()).isEqualTo(0L);
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
