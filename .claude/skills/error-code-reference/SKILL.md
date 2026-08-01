---
name: error-code-reference
description: Full mapping of every custom exception in sfrigola-core to its HTTP status code and error code string, grouped by status. Load when adding a new exception, wiring SCExceptionHandler, or debugging what error code a response should carry.
---

# HTTP Status Code Legend — every exception in the codebase, grouped by code

For the decision rule on picking a bucket for a *new* exception, see "HTTP Status Code Policy" in the root `CLAUDE.md` — that rule stays there since it's needed every time, not just when consulting this table.

**500 `INTERNAL_SERVER_ERROR`** — server bug, not caused by client input, global bucket:
| Exception | Error code | Cause |
|---|---|---|
| `Exception.class` (generic fallback) | `SERVER_ERROR` | anything uncaught |
| `SCNoRowsAffectedException` | `NO_ROWS_AFFECTED` | expected update/delete affected 0 rows |
| `SCDataCorruptionException` | `DATA_CORRUPTED` | unmappable value read from DB |
| `SCAuthSecuritySystemException` | `SECURITY_SYSTEM_ERROR` | JWT generation/validation infra failure |
| `NoValidRoleFromExternalException` | `INVALID_ROLE_FROM_STRING` | corrupted role value in DB |

**401 `UNAUTHORIZED`** — session/token invalid, global bucket:
| Exception / handler | Error code | Cause |
|---|---|---|
| `JwtValidationFilter` | `ENV_NOT_AVAILABLE`, `JWT_EXPIRED`, `JWT_VALIDATION_FAILED` | missing/expired/invalid JWT on a protected route |
| `CustomAuthenticationEntryPoint` | `NOT_AUTHORIZED` | protected endpoint hit with no auth at all |
| `SCAuthenticatedUserNotFoundException` | `NO_USER_AUTH` | JWT valid but principal no longer exists in DB |

**403 `FORBIDDEN`** — authenticated but not allowed, case-by-case:
| Exception / handler | Error code | Cause |
|---|---|---|
| `CustomAccessDeniedHandler` | `NOT_AUTHORIZED` | Spring Security role/path authorization failure |
| `SCUserInactiveException` | `USER_NOT_ACTIVE` | account deactivated |
| `RecipeAuthorMismatchException` | `NOT_RECIPE_OWNER` | contributor editing/deleting a recipe they don't own |
| `NoChangeRoleToAdminException` | `CANNOT_CHANGE_ROLE_TO_ADMIN` | admin tried to promote a user straight to `ROLE_ADMIN` via the user-management endpoint |
| `SCCanNotActiveOrDeactivateYourselfException` | `CANNOT_CHANGE_OWN_ACTIVE_STATUS` | admin tried to activate/deactivate their own account |

**404 `NOT_FOUND`** — resource doesn't exist, case-by-case:
| Exception | Error code |
|---|---|
| `EntityNotFoundException` (jakarta, generic fallback) | `ENTITY_NOT_FOUND` |
| `NoCategoryFoundException` | `SELECTED_CATEGORY_NOT_FOUND` |
| `NoTagFoundException` | `TAG_NOT_FOUND` |
| `NoRecipeFoundException` | `RECIPE_NOT_FOUND` |
| `NoIngredientFoundException` | `INGREDIENT_NOT_FOUND` |
| `NoUserFoundException` | `USER_NOT_FOUND` |
| `NoFavoriteFoundException` | `FAVORITE_NOT_FOUND` |
| `NoRatingFoundException` | `RATING_NOT_FOUND` |

**409 `CONFLICT`** — resource already exists, case-by-case:
| Exception | Error code |
|---|---|
| `SCUserAlreadyExistsException` | `USER_ALREADY_EXISTS` |
| `CategorySlugAlreadyExistsException` | `CATEGORY_SLUG_ALREADY_EXISTS` |
| `TagSlugAlreadyExistsException` | `TAG_SLUG_ALREADY_EXISTS` |
| `TagLabelAlreadyExistsException` | `TAG_LABEL_ALREADY_EXISTS` |
| `IngredientSlugAlreadyExistsException` | `INGREDIENT_SLUG_ALREADY_EXISTS` |
| `FavoriteAlreadyExistsException` | `FAVORITE_ALREADY_EXISTS` |
| `RatingAlreadyExistsException` | `RATING_ALREADY_EXISTS` |

**400 `BAD_REQUEST`** — validation + business-rule violations, case-by-case:
| Exception | Error code |
|---|---|
| `MethodArgumentNotValidException`, `HandlerMethodValidationException`, `ConstraintViolationException`, `MissingServletRequestParameterException` | per-field messages (Jakarta Validation) |
| `HttpMessageNotReadableException` | `MALFORMED_JSON` |
| `IllegalArgumentException` | `ILLEGAL_ARGUMENT` |
| `SCEnumValidationException` | `INVALID_ENUM_CODE` |
| `SCBadCredentialException` | `BAD_CREDENTIALS` — wrong email/password on `/auth/login`; deliberately **not** `401` (see policy in root CLAUDE.md) |
| `SCNewPasswordSameAsOldPasswordException` | `NEW_PASSWORD_SAME_AS_OLD_PASSWORD` |
| `SCPasswordAndConfirmationPasswordDoesntMatchException` | `PASSWORD_DOES_NOT_MATCH_CONFIRMATION_PASSWORD` |
| `SCCompromisedPasswordException` | `COMPROMISED_PASSWORD` |
| `SCOldPasswordNotMatchException` | `OLD_PASSWORD_NOT_MATCH` |
| `LocaleNotActiveException` | `LOCALE_NOT_ACTIVE` |
| `NoValidLangCodeToChangeException` | `INVALID_LANG_CODE` |
| `Missing{Category,Tag,Recipe,Ingredient}LocalesException` | `MISSING_*_LOCALES` — translation list doesn't cover every active language on create |
| `Duplicate{Category,Tag,Recipe,Ingredient}LocaleException` | `DUPLICATE_*_LOCALE` — same locale listed twice on create |
| `CategoryHasChildrenException` | `CATEGORY_HAS_CHILDREN` |
| `CategoryReorderMismatchException` | `CATEGORY_REORDER_MISMATCH` |
| `TagScopeNotAllowedException` | `TAG_SCOPE_NOT_ALLOWED` |
| `ContributorTranslationLimitExceededException` | `CONTRIBUTOR_TRANSLATION_LIMIT_EXCEEDED` |
