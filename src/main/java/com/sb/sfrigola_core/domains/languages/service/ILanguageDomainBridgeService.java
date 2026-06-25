package com.sb.sfrigola_core.domains.languages.service;

import com.sb.sfrigola_core.domains.languages.dto.LanguageDto;

import java.util.List;

/**
 * Internal bridge contract for the languages' domain.
 * Does NOT read the security context; all required data is received explicitly.
 * Intended for cross-domain use by other services that need language data.
 */
public interface ILanguageDomainBridgeService {

    /**
     * Returns all languages currently marked as active in the system.
     *
     * @return a {@link java.util.List} of {@link com.sb.sfrigola_core.domains.languages.dto.LanguageDto}
     *         representing every active language; never {@code null}, may be empty
     */
    List<LanguageDto> getAllActiveLanguages();
}
