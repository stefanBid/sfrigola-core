package com.sb.sfrigola_core.domains.languages.service;

import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.exception.LocaleNotActiveException;

import java.util.Map;
import java.util.Set;

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
     * @throws LocaleNotActiveException
     *         if no active language exists with the given code
     */
    void validateLocaleIsActiveOrThrow(String locale);

    /**
     * Validates that {@code locale} is present among {@code activeLanguagesKeys}, for cross-domain
     * services that already loaded the active languages map (e.g. via {@link #getAllActiveLanguagesSimpleMap()}
     * or {@link #getActiveLanguageEntitiesMap()}) and want to guard a locale without an extra query.
     *
     * @param activeLanguagesKeys BCP-47 codes of the currently active languages, previously loaded by the caller
     * @param locale              BCP-47 code to validate (e.g. {@code "it"}, {@code "en"})
     * @throws LocaleNotActiveException
     *         if {@code locale} is not contained in {@code activeLanguagesKeys}
     */
    void validateLocaleIsActiveByActiveLanguagesMapKeysOrThrow(Set<String> activeLanguagesKeys, String locale);

    /**
     * Returns the {@link com.sb.sfrigola_core.domains.languages.entity.Language} entity for {@code locale}
     * from {@code activeLanguagesMap}, for cross-domain services that already loaded the active languages map
     * (e.g. via {@link #getActiveLanguageEntitiesMap()}) and want to resolve a locale to an entity without an extra query.
     *
     * @param activeLanguagesMap a map of active languages keyed by BCP-47 code
     * @param locale             BCP-47 code to resolve (e.g. {@code "it"}, {@code "en"})
     * @return the corresponding {@link com.sb.sfrigola_core.domains.languages.entity.Language} entity
     * @throws LocaleNotActiveException
     *        if {@code locale} is not contained in {@code activeLanguagesMap}
     */
    Language getLangFromEntitiesMapFromKeyOrThrow(Map<String, Language> activeLanguagesMap, String locale);

}
