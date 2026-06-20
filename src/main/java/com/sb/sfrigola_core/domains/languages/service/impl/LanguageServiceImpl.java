package com.sb.sfrigola_core.domains.languages.service.impl;

import com.sb.sfrigola_core.common.dto.external.option.SCPageableOptionDto;
import com.sb.sfrigola_core.common.dto.internal.SCFilterParamsServiceArgs;
import com.sb.sfrigola_core.common.dto.internal.SCPageableServiceResultDto;
import com.sb.sfrigola_core.common.util.SCServiceUtils;
import com.sb.sfrigola_core.domains.languages.dto.LanguageDto;
import com.sb.sfrigola_core.domains.languages.entity.Language;
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
    public SCPageableServiceResultDto<LanguageDto> getAllLanguages(SCFilterParamsServiceArgs filterArgs, boolean isActive) {
        var fetchedLanguages = isActive ? languageRepository.findAllByIsActiveTrue(SCServiceUtils.getPageableByFilterParamsServiceArgs(filterArgs)) : languageRepository.findAll(SCServiceUtils.getPageableByFilterParamsServiceArgs(filterArgs));

        SCPageableOptionDto pageableOption = new SCPageableOptionDto(
                fetchedLanguages.getNumber(),
                fetchedLanguages.getSize(),
                fetchedLanguages.getTotalElements(),
                fetchedLanguages.getTotalPages(),
                fetchedLanguages.hasNext()
        );

        return new SCPageableServiceResultDto<>(
                fetchedLanguages.getContent().stream().map(this::toDto).toList(),
                pageableOption
        );
    }

    @Override
    public boolean existsByCode(String code) {
        return languageRepository.existsByCode(code);
    }

    private LanguageDto toDto(Language language) {
        return new LanguageDto(language.getCode(), language.getName());
    }
}