package com.sb.sfrigola_core.domains.languages.service.impl;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.domains.languages.dto.LanguageDto;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.exception.NoValidLangCodeToChangeException;
import com.sb.sfrigola_core.domains.languages.repository.ILanguageRepository;
import com.sb.sfrigola_core.domains.languages.service.ILanguageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LanguageServiceImpl implements ILanguageService {

    private final ILanguageRepository languageRepository;


    @Override
    public SCPagedResult<LanguageDto> getAllLanguages(SCFilterQuery<Void> filterQuery, Boolean isActive) {
        var pageable = SCPaginationUtils.toPageable(filterQuery);
        var fetchedLanguages = Boolean.TRUE.equals(isActive)
                ? languageRepository.findAllByIsActiveTrue(pageable)
                : languageRepository.findAll(pageable);

        return new SCPagedResult<>(
                fetchedLanguages.getContent().stream().map(this::toDto).toList(),
                SCPaginationUtils.toPagedOption(fetchedLanguages)
        );
    }

    @Override
    public boolean existsByCodeOrThrow(String code) {
        var result = languageRepository.existsByCode(code);

        if (!result)
            throw new NoValidLangCodeToChangeException("Language code " + code + " not available to set as preferred language");
        return true;
    }

    private LanguageDto toDto(Language language) {
        return new LanguageDto(language.getCode(), language.getName());
    }
}