package com.sb.sfrigola_core.domains.languages.service.impl;

import com.sb.sfrigola_core.domains.languages.dto.LanguageDto;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.repository.ILanguageRepository;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LanguageDomainBridgeServiceImpl implements ILanguageDomainBridgeService {

    private final ILanguageRepository languageRepository;

    @Override
    public List<LanguageDto> getAllActiveLanguages() {
        return languageRepository.findAllByIsActiveTrue().stream().map(this::toDto).toList();
    }

    @Override
    public Map<String, Language> getActiveLanguageEntitiesMap() {
        return languageRepository.findAllByIsActiveTrue().stream().collect(
                Collectors.toMap(Language::getCode, lang -> lang)
        );
    }

    // =========================================================
    // PRIVATE
    // =========================================================
    private LanguageDto toDto(Language language) {
        return new LanguageDto(language.getCode(), language.getName());
    }
}
