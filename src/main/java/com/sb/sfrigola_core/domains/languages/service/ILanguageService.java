package com.sb.sfrigola_core.domains.languages.service;

import com.sb.sfrigola_core.common.dto.internal.SCFilterParamsServiceArgs;
import com.sb.sfrigola_core.common.dto.internal.SCPageableServiceResultDto;
import com.sb.sfrigola_core.domains.languages.dto.LanguageDto;

/**
 * Contract for language management operations.
 * Write methods follow the "succeed or throw" contract: return {@code true} on success,
 * throw a specific exception if any step fails.
 */
public interface ILanguageService {

    /**
     * Returns a paginated list of languages, optionally filtered by active status.
     *
     * @param filterArgs pagination and sorting parameters
     * @param isActive   {@code true} to return only active languages, {@code false} for inactive ones
     * @return paginated result wrapping a list of {@link LanguageDto}
     */
    SCPageableServiceResultDto<LanguageDto> getAllLanguages(SCFilterParamsServiceArgs filterArgs, boolean isActive);

    /**
     * Verifies that a language with the given ISO code exists and is valid, or throws.
     * Always returns {@code true} on success — use the return value to chain calls if needed.
     *
     * @param code ISO 639-1 language code (e.g. {@code "it"}, {@code "en"})
     * @return {@code true} if a language with the given code exists
     * @throws com.sb.sfrigola_core.domains.languages.exception.NoValidLangCodeToChangeException if no language with the given code exists
     */
    boolean existsByCodeOrThrow(String code);

}
