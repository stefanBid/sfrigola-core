package com.sb.sfrigola_core.common.interfaces;

import com.sb.sfrigola_core.domains.languages.entity.Language;

/**
 * Interface for JPA translation entities (e.g. {@code CategoryTranslation}, {@code TagTranslation},
 * {@code RecipeTranslation}, {@code IngredientTranslation}) that link a parent entity to a
 * {@link Language} for one locale. Lets cross-domain code (e.g.
 * {@link com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService}) build
 * localization-coverage maps without depending on any specific domain's translation entity.
 */
public interface ISCTranslationEntity {

    Language getLanguage();
}
