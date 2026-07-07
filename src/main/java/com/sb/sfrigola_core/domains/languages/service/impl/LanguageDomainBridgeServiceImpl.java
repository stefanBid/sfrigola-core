package com.sb.sfrigola_core.domains.languages.service.impl;

import com.sb.sfrigola_core.domains.languages.exception.LocaleNotActiveException;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.repository.ILanguageRepository;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LanguageDomainBridgeServiceImpl implements ILanguageDomainBridgeService {

    private final ILanguageRepository languageRepository;

    @Override
    public Map<String, String> getAllActiveLanguagesSimpleMap() {
        return languageRepository.findAllByIsActiveTrue().stream().collect(Collectors.toMap(Language::getCode, Language::getName));
    }

    @Override
    public Map<String, Language> getActiveLanguageEntitiesMap() {
        return languageRepository.findAllByIsActiveTrue().stream().collect(
                Collectors.toMap(Language::getCode, lang -> lang)
        );
    }

    @Override
    public void validateLocaleIsActiveOrThrow(String locale) {
        if (!languageRepository.existsByCodeAndIsActiveTrue(locale))
            throw new LocaleNotActiveException(locale);
    }

    @Override
    public void validateLocaleIsActiveByActiveLanguagesMapKeysOrThrow(Set<String> activeLanguagesKeys, String locale) {
        if (!activeLanguagesKeys.contains(locale)) {
            throw new LocaleNotActiveException(locale);
        }
    }

    @Override
    public Language getLangFromEntitiesMapFromKeyOrThrow(Map<String, Language> activeLanguagesMap, String locale) {
        Language lang = activeLanguagesMap.get(locale);
        if (lang == null) {
            throw new LocaleNotActiveException(locale);
        }
        return lang;
    }
}
