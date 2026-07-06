package com.sb.sfrigola_core.domains.languages.service;

import com.sb.sfrigola_core.domains.languages.entity.Language;

import java.util.Map;

/**
 * Internal bridge contract for the languages' domain.
 * Does NOT read the security context; all required data is received explicitly.
 * Intended for cross-domain use by other services that need language data.
 */
public interface ILanguageDomainBridgeService {

    /**
     * Returns all active languages as a map keyed by BCP-47 language code, for cross-domain
     * services that only need code/name pairs (no entity reference, no domain DTO leaking across boundaries).
     *
     * @return a {@link java.util.Map} from BCP-47 code (e.g. {@code "it"}, {@code "en"})
     *         to the language's native name; never {@code null}, may be empty if no active languages exist
     */
    Map<String, String> getAllActiveLanguagesSimpleMap();


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

    /**
     * Validates that {@code locale} matches an active language, for cross-domain services that
     * only need to guard a single locale param and don't otherwise load the active languages map.
     *
     * @param locale BCP-47 code to validate (e.g. {@code "it"}, {@code "en"})
     * @throws com.sb.sfrigola_core.domains.languages.exception.LocaleNotActiveException
     *         if no active language exists with the given code
     */
    void validateLocaleIsActiveOrThrow(String locale);

}
