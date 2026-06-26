package com.sb.sfrigola_core.domains.languages.service;

import com.sb.sfrigola_core.domains.languages.dto.LanguageDto;
import com.sb.sfrigola_core.domains.languages.entity.Language;

import java.util.List;
import java.util.Map;

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


    /**
     * Returns all active languages as a map keyed by BCP-47 language code, providing
     * direct access to {@link com.sb.sfrigola_core.domains.languages.entity.Language} entities
     * for cross-domain services that need to resolve a locale code to an entity reference
     * (e.g. when building {@code @ManyToOne} relationships on translation entities).
     * <p>
     * Loads all active languages in a single query; callers should perform locale lookups
     * in memory rather than issuing per-locale queries.
     *
     * @return a {@link java.util.Map} from BCP-47 code (e.g. {@code "it"}, {@code "en"})
     *         to the corresponding {@link com.sb.sfrigola_core.domains.languages.entity.Language} entity;
     *         never {@code null}, may be empty if no active languages exist
     */
    Map<String, Language> getActiveLanguageEntitiesMap();

}
