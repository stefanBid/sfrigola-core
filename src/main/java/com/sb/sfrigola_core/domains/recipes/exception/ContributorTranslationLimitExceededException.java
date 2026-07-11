package com.sb.sfrigola_core.domains.recipes.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.recipes.enums.RecipeErrorCode;
import org.springframework.http.HttpStatus;

public class ContributorTranslationLimitExceededException extends SCGeneralException {
    public ContributorTranslationLimitExceededException(int translationsCount) {
        super(
                HttpStatus.BAD_REQUEST,
                RecipeErrorCode.CONTRIBUTOR_TRANSLATION_LIMIT_EXCEEDED,
                "A contributor can only submit a single translation when creating a recipe, got: " + translationsCount
        );
    }
}
