package com.sb.sfrigola_core.domains.languages.service;

import com.sb.sfrigola_core.common.dto.internal.SCFilterParamsServiceArgs;
import com.sb.sfrigola_core.common.dto.internal.SCPageableServiceResultDto;
import com.sb.sfrigola_core.domains.languages.dto.LanguageDto;
import jakarta.annotation.Nullable;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Contract for language management operations.
 */
public interface ILanguageService {

    /**
     * Returns a paginated list of languages, optionally filtered by active status.
     *
     * @param filterArgs pagination and sorting parameters
     * @param isActive   {@code true} to return only active languages, {@code false} for all
     * @return paginated result wrapping a list of {@link LanguageDto}
     */
    SCPageableServiceResultDto<LanguageDto> getAllLanguages(SCFilterParamsServiceArgs filterArgs, boolean isActive);

    /**
     * Checks whether a language with the given ISO code exists.
     *
     * @param code ISO 639-1 language code (e.g. {@code "it"}, {@code "en"})
     * @return {@code true} if a language with the given code exists
     * @throws com.sb.sfrigola_core.domains.languages.exception.NoValidLangCodeToChangeException if no language with the given code exists
     */
    boolean existsByCodeOrThrow(String code);
}