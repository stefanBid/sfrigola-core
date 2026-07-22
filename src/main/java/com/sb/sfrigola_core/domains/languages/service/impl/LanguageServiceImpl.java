package com.sb.sfrigola_core.domains.languages.service.impl;

import com.sb.sfrigola_core.domains.languages.dto.LanguageDto;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.exception.NoValidLangCodeToChangeException;
import com.sb.sfrigola_core.domains.languages.repository.ILanguageRepository;
import com.sb.sfrigola_core.domains.languages.service.ILanguageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LanguageServiceImpl implements ILanguageService {

    private final ILanguageRepository languageRepository;


    @Override
    public List<LanguageDto> getAllLanguages(Boolean isActive) {
        var fetchedLanguages = Boolean.TRUE.equals(isActive)
                ? languageRepository.findAllByIsActiveTrue()
                : languageRepository.findAll();

        return fetchedLanguages.stream().map(this::toDto).toList();
    }

    @Override
    public boolean existsByCodeOrThrow(String code) {
        var result = languageRepository.existsByCode(code);

        if (!result)
            throw new NoValidLangCodeToChangeException("Language code " + code + " not available to set as preferred language");
        return true;
    }

    // =========================================================
    // PRIVATE
    // =========================================================

    private LanguageDto toDto(Language language) {
        return new LanguageDto(language.getCode(), language.getName());
    }
}