package com.sb.sfrigola_core.domains.languages.service;

import com.sb.sfrigola_core.common.dto.internal.SCFilterParamsServiceArgs;
import com.sb.sfrigola_core.common.dto.internal.SCPageableServiceResultDto;
import com.sb.sfrigola_core.domains.languages.dto.LanguageDto;
import jakarta.annotation.Nullable;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

public interface ILanguageService {

    SCPageableServiceResultDto<LanguageDto> getAllLanguages(SCFilterParamsServiceArgs filterArgs, boolean isActive);
    boolean existsByCode(String code);
}