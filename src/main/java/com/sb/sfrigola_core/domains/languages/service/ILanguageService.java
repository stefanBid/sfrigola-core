package com.sb.sfrigola_core.domains.languages.service;

import com.sb.sfrigola_core.domains.languages.dto.LanguageDto;

import java.util.List;

/**
 * Contract for language management operations.
 * Write methods follow the "succeed or throw" contract: return {@code true} on success,
 * throw a specific exception if any step fails.
 */
public interface ILanguageService {

    /**
     * Returns the list of languages, optionally filtered by active status.
     *
     * @param isActive {@code true} to return only active languages, {@code false} or {@code null} to return all languages regardless of status
     * @return list of {@link LanguageDto}
     */
    List<LanguageDto> getAllLanguages(Boolean isActive);


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
