# CLAUDE.md — Project Context

> This file is automatically loaded by Claude Code at startup.
> Update it as the project evolves.

---

## Project Identity

- **GroupId:** `com.sb`
- **ArtifactId:** `sfrigola-core`
- **Version:** `0.0.1-SNAPSHOT`
- **Base package:** `com.sb.sfrigola_core`
- **Context path:** `/sfrigola-core`
- **Port:** configured via env `SERVER_PORT`

---

## Tech Stack

- **Framework:** Spring Boot 4.1.0
- **Language:** Java 25
- **Build tool:** Maven
- **Database:** PostgreSQL (driver `org.postgresql`)
- **ORM:** Spring Data JPA (Hibernate) — `ddl-auto=none` (schema managed manually via SQL)
- **Security:** Spring Security + JWT stateless (jjwt 0.13.0)
- **Validation:** Spring Validation (Jakarta)
- **Web:** Spring MVC (`spring-boot-starter-webmvc`)
- **Utilities:** Lombok
- **Docker:** Spring Boot Docker Compose (auto-starts containers in dev)
- **Dev tools:** Spring Boot DevTools (hot reload)
- **Auditing:** Spring Data JPA Auditing (`@EnableJpaAuditing`)

### Test Dependencies

- `spring-boot-starter-data-jpa-test`
- `spring-boot-starter-security-test`
- `spring-boot-starter-validation-test`
- `spring-boot-starter-webmvc-test`

---

## Actual Project Structure

```
com.sb.sfrigola_core/
├── SfrigolaCoreApplication.java
│
├── common/                          # Cross-cutting reusable code
│   ├── constant/
│   │   ├── SCGeneralConstants.java
│   │   └── SCRequestParamValidationCodeConstants.java   # messages for @Min/@Max/@Pattern
│   ├── dto/
│   │   ├── option/
│   │   │   ├── SCPagedOptionDto.java        # currentPage, pageSize, totalElements, totalPages, hasMore
│   │   │   └── SCCounterOptionDto.java
│   │   └── response/
│   │       ├── SCGeneralResponseDto.java    # universal API envelope: data + option + errorData
│   │       └── SCErrorDataDto.java
│   ├── entity/
│   │   └── BaseEntity.java                 # @MappedSuperclass: createdAt, updatedAt, createdBy, updatedBy
│   ├── enums/
│   │   ├── GeneralErrorCode.java            # ENTITY_NOT_FOUND, ILLEGAL_ARGUMENT, MALFORMED_JSON, SERVER_ERROR
│   │   ├── SCUserRole.java                  # ROLE_ADMIN, ROLE_USER, ROLE_CONTRIBUTOR
│   │   └── SortDirection.java
│   ├── exception/
│   │   ├── ISCErrorCode.java                # interface: String code()
│   │   ├── SCExceptionHandler.java          # global @RestControllerAdvice
│   │   └── ex/
│   │       ├── SCGeneralException.java      # abstract base: status + errorCode + message → toErrorMap()
│   │       ├── SCDataCorruptionException.java
│   │       └── SCNoRowsAffectedException.java
│   ├── models/
│   │   ├── context/
│   │   │   └── SCAuthUser.java              # principal in the security context
│   │   └── contracts/
│   │       ├── SCFilterQuery.java           # searchKey, sortBy, sort, take, page, other (generic)
│   │       └── SCPagedResult.java           # content (List<T>) + pagedOptionDto
│   └── util/
│       ├── SCAuthenticationUtils.java
│       ├── SCErrorDataBuilderUtils.java      # builds ResponseEntity<SCGeneralResponseDto<Void,Void>>
│       └── SCPaginationUtils.java
│
├── config/                                  # Spring configuration (not a domain)
│   ├── auditor/
│   │   ├── AuditorAwareConfig.java
│   │   └── AuditorAwareImpl.java
│   ├── security/
│   │   ├── SecurityConfig.java              # SecurityFilterChain, CORS, path-based authorization
│   │   ├── SecurityBeansConfig.java         # beans: publicPath, authPath, adminPath, userPath, contributorPath, allowedOriginsPaths, scHierarchy
│   │   ├── authprovider/
│   │   │   └── UsernamePwAuthenticationProvider.java
│   │   ├── exception/
│   │   │   ├── CustomAccessDeniedHandler.java
│   │   │   ├── CustomAuthenticationEntryPoint.java
│   │   │   ├── SecurityErrorCode.java
│   │   │   └── ex/
│   │   │       ├── SCAuthenticatedUserNotFoundException.java
│   │   │       ├── SCBadCredentialException.java
│   │   │       └── SCUserInactiveException.java
│   │   └── jwt/
│   │       ├── JwtValidationFilter.java     # OncePerRequestFilter before BasicAuthenticationFilter
│   │       └── jwtservice/
│   │           ├── IJWTService.java
│   │           └── JwtService.java
│   └── web/
│       └── WebConfig.java
│
└── domains/                                 # Features organized by domain
    ├── auth/
    ├── languages/
    ├── users/
    ├── categories/                          # entity + CategoryTranslation; no repo/service/controller yet
    └── tags/
        ├── entity/
        │   ├── Tag.java
        │   └── TagTranslation.java
        └── enums/                           # domain enums + JPA converters co-located
            ├── TagType.java + TagTypeConverter.java
            ├── TagScope.java + TagScopeConverter.java
            └── TagStatus.java + TagStatusConverter.java
```

---

## Internal Structure of Each Domain

```
domains/<feature>/
├── annotations/        # (only if the feature has custom annotations/validators)
├── constants/          # domain-specific validation constants
├── controller/         # thin controller, HTTP wiring only
├── dto/                # Java records for API input/output
├── entity/             # @Entity JPA
├── enums/              # ErrorCode enum (implements ISCErrorCode)
├── exception/          # custom exceptions extending SCGeneralException
│   └── ex/
├── repository/         # JPA Repository interfaces
└── service/
    ├── IXService.java                   # controller-facing contract (reads security context)
    ├── IXDomainBridgeService.java       # internal bridge contract (does NOT read security context)
    └── impl/
        ├── XServiceImpl.java
        └── XDomainBridgeServiceImpl.java
```

---

## Database Schema

PostgreSQL schema managed manually via `src/main/resources/sql/createSfrigolaDB.sql`.
Hibernate does not generate or alter tables (`ddl-auto=none`).

**Source of truth:** always read the SQL file directly. This section is a faithful transcription — if they diverge, the SQL file wins.

### Column conventions (applies to all standard tables)

Every standard table (non-bridge) has:
- `id BIGSERIAL PRIMARY KEY` — internal, never exposed in APIs
- `public_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid()` — exposed in APIs
- audit: `created_at TIMESTAMP NOT NULL DEFAULT NOW()`, `updated_at TIMESTAMP NOT NULL DEFAULT NOW()`, `created_by VARCHAR(50) NOT NULL DEFAULT 'system'`, `updated_by VARCHAR(50) NOT NULL DEFAULT 'system'`

**Exceptions:**
- Bridge tables (`ingredient_tags`, `recipe_tags`): no `id`, no `public_id`, no `updated_*` — only `created_at` + `created_by`
- `recipe_stats`: no `public_id`, no audit columns — only stats columns

### PostgreSQL ENUMs

```
difficulty_level  → easy | medium | hard
meal_type         → breakfast | lunch | dinner | snack | dessert | appetizer
season_type       → spring | summer | autumn | winter | all_year
tag_type          → recipe | flavor | texture | season | dietary
tag_scope         → recipe | ingredient | both
tag_status        → approved | pending | rejected
```

### Tables

#### `languages`
- `id` BIGSERIAL PK
- `public_id` UUID NOT NULL UNIQUE
- `code` VARCHAR(10) NOT NULL UNIQUE — BCP-47: 'en', 'it'
- `name` VARCHAR(50) NOT NULL — native name
- `is_default` BOOLEAN NOT NULL DEFAULT FALSE — max 1 TRUE (unique partial index)
- `is_active` BOOLEAN NOT NULL DEFAULT TRUE
- audit columns

#### `roles`
- `id` BIGSERIAL PK
- `public_id` UUID NOT NULL UNIQUE
- `name` VARCHAR(50) NOT NULL UNIQUE — ROLE_ADMIN / ROLE_USER / ROLE_CONTRIBUTOR
- `description` VARCHAR(200) nullable
- audit columns

#### `users`
- `id` BIGSERIAL PK
- `public_id` UUID NOT NULL UNIQUE
- `role_id` BIGINT NOT NULL FK → roles(id)
- `username` VARCHAR(50) NOT NULL UNIQUE
- `email` VARCHAR(150) NOT NULL UNIQUE
- `password_hash` VARCHAR(255) NOT NULL — BCrypt
- `preferred_lang` VARCHAR(10) NOT NULL DEFAULT 'en' FK → languages(code)
- `is_active` BOOLEAN NOT NULL DEFAULT TRUE
- `first_name` VARCHAR(100) nullable
- `last_name` VARCHAR(100) nullable
- `avatar_url` VARCHAR(500) nullable
- `bio` TEXT nullable
- audit columns

#### `tags`
- `id` BIGSERIAL PK
- `public_id` UUID NOT NULL UNIQUE
- `slug` VARCHAR(100) NOT NULL UNIQUE — universal English key
- `type` tag_type NOT NULL
- `scope` tag_scope NOT NULL DEFAULT 'both'
- `status` tag_status NOT NULL DEFAULT 'approved'
- audit columns

#### `tag_translations`
- `id` BIGSERIAL PK
- `public_id` UUID NOT NULL UNIQUE
- `tag_id` BIGINT NOT NULL FK → tags(id) ON DELETE CASCADE
- `locale` VARCHAR(10) NOT NULL FK → languages(code) ON DELETE CASCADE
- `label` VARCHAR(100) NOT NULL
- UNIQUE (tag_id, locale)
- audit columns

#### `categories`
- `id` BIGSERIAL PK
- `public_id` UUID NOT NULL UNIQUE
- `slug` VARCHAR(100) NOT NULL UNIQUE — URL-safe English key
- `parent_id` BIGINT nullable FK → categories(id) ON DELETE SET NULL — self-referential
- `sort_order` SMALLINT NOT NULL DEFAULT 0
- `is_active` BOOLEAN NOT NULL DEFAULT TRUE
- audit columns

Seed hierarchy (11 categories):
- Root: appetizers, first-courses, main-courses, side-dishes, desserts, beverages
- Children of first-courses: pasta, risotto, soups
- Children of main-courses: fish, meat

#### `category_translations`
- `id` BIGSERIAL PK
- `public_id` UUID NOT NULL UNIQUE
- `category_id` BIGINT NOT NULL FK → categories(id) ON DELETE CASCADE
- `locale` VARCHAR(10) NOT NULL FK → languages(code) ON DELETE CASCADE
- `name` VARCHAR(100) NOT NULL
- `description` TEXT nullable
- UNIQUE (category_id, locale)
- audit columns

#### `ingredients`
- `id` BIGSERIAL PK
- `public_id` UUID NOT NULL UNIQUE
- `slug` VARCHAR(150) NOT NULL UNIQUE
- `category` VARCHAR(100) nullable — free text: 'vegetable'/'dairy'/'protein'/'grain'
- `calories_per_100g` NUMERIC(7,2) nullable
- `allergens` TEXT[] nullable — GIN index
- `is_vegetarian` BOOLEAN NOT NULL DEFAULT FALSE
- `is_vegan` BOOLEAN NOT NULL DEFAULT FALSE
- `is_gluten_free` BOOLEAN NOT NULL DEFAULT FALSE
- audit columns

#### `ingredient_translations`
- `id` BIGSERIAL PK
- `public_id` UUID NOT NULL UNIQUE
- `ingredient_id` BIGINT NOT NULL FK → ingredients(id) ON DELETE CASCADE
- `locale` VARCHAR(10) NOT NULL FK → languages(code) ON DELETE CASCADE
- `name` VARCHAR(150) NOT NULL
- UNIQUE (ingredient_id, locale)
- audit columns

#### `ingredient_tags`
Bridge ingredient ↔ tag (scope='ingredient' or 'both' only).
- `ingredient_id` BIGINT NOT NULL FK → ingredients(id) ON DELETE CASCADE
- `tag_id` BIGINT NOT NULL FK → tags(id) ON DELETE CASCADE
- PRIMARY KEY (ingredient_id, tag_id)
- `created_at` TIMESTAMP NOT NULL DEFAULT NOW()
- `created_by` VARCHAR(50) NOT NULL DEFAULT 'system'
- **No** `id`, `public_id`, `updated_at`, `updated_by`

#### `recipes`
- `id` BIGSERIAL PK
- `public_id` UUID NOT NULL UNIQUE
- `author_id` BIGINT NOT NULL FK → users(id)
- `category_id` BIGINT nullable FK → categories(id) ON DELETE SET NULL
- `difficulty` difficulty_level NOT NULL DEFAULT 'medium'
- `meal_type` meal_type nullable
- `season` season_type NOT NULL DEFAULT 'all_year'
- `prep_time_min` INT nullable CHECK (>= 0)
- `cook_time_min` INT nullable CHECK (>= 0)
- `servings` SMALLINT nullable CHECK (> 0)
- `is_vegetarian` BOOLEAN NOT NULL DEFAULT FALSE
- `is_vegan` BOOLEAN NOT NULL DEFAULT FALSE
- `is_gluten_free` BOOLEAN NOT NULL DEFAULT FALSE
- `is_published` BOOLEAN NOT NULL DEFAULT FALSE
- audit columns

#### `recipe_translations`
- `id` BIGSERIAL PK
- `public_id` UUID NOT NULL UNIQUE
- `recipe_id` BIGINT NOT NULL FK → recipes(id) ON DELETE CASCADE
- `locale` VARCHAR(10) NOT NULL FK → languages(code) ON DELETE CASCADE
- `title` VARCHAR(200) NOT NULL
- `description` TEXT nullable
- `instructions` TEXT NOT NULL
- UNIQUE (recipe_id, locale)
- audit columns

#### `recipe_tags`
Bridge recipe ↔ tag (scope='recipe' or 'both' only).
- `recipe_id` BIGINT NOT NULL FK → recipes(id) ON DELETE CASCADE
- `tag_id` BIGINT NOT NULL FK → tags(id) ON DELETE CASCADE
- PRIMARY KEY (recipe_id, tag_id)
- `created_at` TIMESTAMP NOT NULL DEFAULT NOW()
- `created_by` VARCHAR(50) NOT NULL DEFAULT 'system'
- **No** `id`, `public_id`, `updated_at`, `updated_by`

#### `recipe_ingredients`
- `id` BIGSERIAL PK
- `public_id` UUID NOT NULL UNIQUE
- `recipe_id` BIGINT NOT NULL FK → recipes(id) ON DELETE CASCADE
- `ingredient_id` BIGINT NOT NULL FK → ingredients(id)
- `quantity` NUMERIC(8,2) nullable
- `unit` VARCHAR(50) nullable — 'g', 'ml', 'tbsp', 'cup', 'to taste'
- `preparation_note` VARCHAR(200) nullable — 'finely chopped'
- `sort_order` SMALLINT NOT NULL DEFAULT 0
- UNIQUE (recipe_id, ingredient_id)
- audit columns

#### `favorites`
- `id` BIGSERIAL PK
- `public_id` UUID NOT NULL UNIQUE
- `user_id` BIGINT NOT NULL FK → users(id) ON DELETE CASCADE
- `recipe_id` BIGINT NOT NULL FK → recipes(id) ON DELETE CASCADE
- UNIQUE (user_id, recipe_id)
- audit columns
- Updates `recipe_stats` via service layer (no DB trigger)

#### `ratings`
- `id` BIGSERIAL PK
- `public_id` UUID NOT NULL UNIQUE
- `user_id` BIGINT NOT NULL FK → users(id) ON DELETE CASCADE
- `recipe_id` BIGINT NOT NULL FK → recipes(id) ON DELETE CASCADE
- `score` SMALLINT NOT NULL CHECK (score BETWEEN 1 AND 5)
- `comment` TEXT nullable
- UNIQUE (user_id, recipe_id)
- audit columns
- Updates `recipe_stats` via service layer (no DB trigger)

#### `recipe_stats`
1:1 with recipes. `recipe_id` is both PK and FK.
- `recipe_id` BIGINT PK FK → recipes(id) ON DELETE CASCADE
- `avg_rating` NUMERIC(3,2) NOT NULL DEFAULT 0.00
- `ratings_count` INT NOT NULL DEFAULT 0
- `favorites_count` INT NOT NULL DEFAULT 0
- `views_count` INT NOT NULL DEFAULT 0
- `last_computed` TIMESTAMP NOT NULL DEFAULT NOW()
- **No** `public_id`, **no** audit columns
- No REST controller — internal service called by `ratings` and `favorites`

### Indexes

```sql
-- Users
idx_users_role               ON users (role_id)

-- Categories
idx_categories_parent        ON categories (parent_id)

-- Ingredients
idx_ingredients_allergens    ON ingredients USING GIN (allergens)

-- Tags
idx_tags_type_scope_status   ON tags (type, scope, status)

-- Bridge reverse lookups
idx_recipe_tags_tag          ON recipe_tags (tag_id)
idx_ingredient_tags_tag      ON ingredient_tags (tag_id)

-- Recipes
idx_recipes_author           ON recipes (author_id)
idx_recipes_category         ON recipes (category_id)
idx_recipes_published        ON recipes (id) WHERE is_published = TRUE
idx_recipes_season           ON recipes (season)
idx_recipes_meal_type        ON recipes (meal_type)
idx_recipes_dietary          ON recipes (is_vegetarian, is_vegan, is_gluten_free)

-- Stats
idx_recipe_stats_rating      ON recipe_stats (avg_rating DESC)
idx_recipe_stats_favs        ON recipe_stats (favorites_count DESC)

-- Ratings / Favorites
idx_ratings_recipe           ON ratings (recipe_id)
idx_favorites_user           ON favorites (user_id)
```

---

## Implementation Status (Sprint 3 in progress — Sprints 1 & 2 complete)

| Domain      | Entity | Repository | Service | Controller | Notes |
|-------------|--------|------------|---------|------------|-------|
| `auth`      | SCUser/SCRole (in users) | — | done | done | login, register, change-email, change-password |
| `languages` | done | done | done | done | GET paginated |
| `users`     | done | done | done | done | update-profile, change-lang, become-contributor, admin CRUD |
| `categories`| done | missing | missing | missing | Category + CategoryTranslation entities; self-referential parent |
| `tags`      | done | missing | missing | missing | Tag + TagTranslation + enums + converters |
| `ingredients` | missing | missing | missing | missing | not started |
| `recipes`   | missing | missing | missing | missing | not started |
| `favorites` | missing | missing | missing | missing | not started |
| `ratings`   | missing | missing | missing | missing | not started |
| `stats`     | missing | missing | missing | no controller by design | not started |

---

## Implemented API Endpoints

Base path: `/sfrigola-core`

### Auth — `/auth`

| Method | Path | Access | Notes |
|--------|------|--------|-------|
| POST | `/auth/login` | public | returns JWT |
| POST | `/auth/register` | public | |
| PATCH | `/auth/change-email` | authenticated | |
| PATCH | `/auth/change-password` | authenticated | |

### Languages — `/languages`

| Method | Path | Access | Notes |
|--------|------|--------|-------|
| GET | `/languages` | authenticated | paginated, isActive filter |

### Users — `/users`

| Method | Path | Access | Notes |
|--------|------|--------|-------|
| PATCH | `/users/settings/change-preferred-lang/{code}` | authenticated | |
| PATCH | `/users/profile/update` | authenticated | |
| PATCH | `/users/profile/became-contributor` | authenticated | promotes to ROLE_CONTRIBUTOR |
| GET | `/users/admin` | ROLE_ADMIN | paginated, sort/search/isActive filters |
| PATCH | `/users/admin/{publicId}/status` | ROLE_ADMIN | activate/deactivate user |

---

## Architectural Patterns

### Universal Response Envelope

Every endpoint returns `SCGeneralResponseDto<T, K>`:
```java
SCGeneralResponseDto.success(data)                    // single item
SCGeneralResponseDto.success(list, pagedOptionDto)    // paginated list
SCGeneralResponseDto.successMutation("Done")          // mutation (string message)
SCGeneralResponseDto.error(errorData)                 // error (handled by SCExceptionHandler)
```

### Pagination

Always use `SCFilterQuery` + `SCPagedResult` + `SCPagedOptionDto`:
```java
// Controller
SCFilterQuery.essential(sortBy, SortDirection.fromString(sort), take, page);
SCFilterQuery.essentialWithSearch(searchKey, sortBy, sort, take, page);
SCFilterQuery.powerful(searchKey, sortBy, sort, take, page, customFilter);

// Service returns
SCPagedResult<XDto>   // { content: List<XDto>, pagedOptionDto: SCPagedOptionDto }
```

### Custom Exceptions

All extend `SCGeneralException`:
```java
protected SCGeneralException(HttpStatus status, ISCErrorCode errorCode, String errorMessage)
// → toErrorMap() = Map.of(errorCode.code(), errorMessage)
```

Each domain has its own `XErrorCode enum implements ISCErrorCode`.

### Endpoint Versioning

Spring Boot 4.x feature — `version = "1.0"` on mapping annotations:
```java
@GetMapping(version = "1.0")
@PostMapping(value = "/login", version = "1.0")
```

### Security: path-based via @Qualifier beans

Paths configured as `List<String>` beans in `SecurityBeansConfig`:
- `publicPath` — no auth required
- `authPath` — any authenticated role
- `adminPath` — minimum role ROLE_ADMIN
- `userPath` — minimum role ROLE_USER
- `contributorPath` — minimum role ROLE_CONTRIBUTOR
- `allowedOriginsPaths` — CORS allowed origins

A `RoleHierarchy` bean (`scHierarchy`) declares `ROLE_ADMIN > ROLE_CONTRIBUTOR > ROLE_USER`, applied via `AuthorityAuthorizationManager` in `SecurityConfig`. Higher roles automatically satisfy lower-role path checks (e.g. ROLE_ADMIN passes `contributorPath` and `userPath` too) — path beans express the *minimum* role required, not an exclusive match.

Every new API endpoint must be registered in the correct bean in `SecurityBeansConfig`.

### Dual Service Contract

1. **`IXService`** — controller-facing: reads security context internally
2. **`IXDomainBridgeService`** — internal bridge: receives data explicitly, does NOT read security context

Example: `ISCUserService` (controller-facing) vs `ISCUserDomainBridgeService` (bridge used by auth).

### Translatable Entities Pattern

Applies to every entity with a `*_translations` child table (`categories`, `tags`, `ingredients`, `recipes`). Modeled first on `categories`/`tags` in sprint 4/5 — follow this exactly for new translatable domains instead of re-deriving it.

**Locale validation/resolution always goes through `ILanguageDomainBridgeService`** — never inline a `Language`/locale null-check that duplicates it:
- `validateLocaleIsActiveOrThrow(locale)` — single locale, no map loaded yet.
- `validateLocaleIsActiveByActiveLanguagesMapKeysOrThrow(activeLanguagesKeys, locale)` — guard a locale when the active-languages map is already in memory (avoids a second query).
- `getLangFromEntitiesMapFromKeyOrThrow(activeLanguagesMap, locale)` — resolve **and** validate a `Language` entity in one call, when the map of entities is already loaded.
- All three throw `common.exception.ex.SCLocaleNotActiveException` — domains must not keep their own `LocaleNotActiveException`/`InvalidXLocaleException`.

**The `translatedLanguages: Map<String, String>` field on admin preview DTOs (`XPreviewAdminDto`) is also built via `ILanguageDomainBridgeService`** — never inline the stream that computes it:
- Every JPA translation entity (`CategoryTranslation`, `TagTranslation`, `IngredientTranslation`, `RecipeTranslation`) implements `common.interfaces.ISCTranslationEntity` (exposes `getLanguage()`). New translatable domains must do the same — it's a one-line `implements` addition, Lombok's `@Getter` already provides the method.
- `buildTranslatedLanguagesMap(translations, activeLanguagesMap)` — generic over any `List<? extends ISCTranslationEntity>`; filters the entity's translations down to currently-active locales and maps each to its display name. This is the localization-coverage map shown in admin CMS previews.
- `toSimpleLanguagesMap(languageEntitiesMap)` — flattens a `Map<String, Language>` (e.g. from `getActiveLanguageEntitiesMap()`) into `Map<String, String>`, for the common case where a `create`/`update` method already loaded the entities map for translation resolution and also needs the simple map to feed `buildTranslatedLanguagesMap` or the response DTO.
- Domains must not keep their own private `toSimpleLanguagesMap`/translated-languages-stream copies — this was previously duplicated across `categories`/`tags`/`ingredients`/`recipes` and consolidated into the bridge for exactly this reason.

**Create (`createNewX`)** — payload carries a `List<XTranslationInputDto>` that must cover **every** active language, no more no less:
1. Reject duplicate `langCode` in the list → `DuplicateXLocaleException`.
2. Reject if the set of `langCode`s doesn't exactly match the active-languages set → `MissingXLocalesException`.
3. Resolve each `Language` via `getLangFromEntitiesMapFromKeyOrThrow`, build translation entities, set the owning-entity back-reference on each before save.
4. The endpoint/service also takes a `locale` param used **only** to pick which translation to show in the response preview (filter the just-built list by `locale`) — it has no bearing on validation.

**Update (`updateX`)** — payload carries a **single** `specificTranslation` (one locale per call), never a list:
1. Resolve+validate that locale via `getLangFromEntitiesMapFromKeyOrThrow`.
2. Look up an existing translation for that locale on the entity: if absent, create it (set the back-reference, add to the collection); if present, patch only the fields that actually changed.
3. No "cover all locales" check on update (unlike create) — partial edits are expected.
4. To touch N locales, the caller makes N calls. No `locale` query param needed — the response preview is always the translation just upserted.
5. No delete-of-a-single-translation capability via update. If a domain ever needs that, it must be a separate explicit endpoint/method — don't overload update with a "blank label ⇒ delete" convention.

**Delete (`deleteX`)** — no `locale` param, no translation preview, no admin-preview DTO returned:
- Delete the parent row; translations cascade via `CascadeType.ALL` on the JPA relation (and DB `ON DELETE CASCADE`) — never touch/query translations explicitly in the delete method.
- Return only the deleted entity's `publicId` (`UUID`), not a DTO.

### BaseEntity

`@MappedSuperclass` with automatic auditing via Spring Data JPA Auditing:
- `createdAt` / `updatedAt` — `Instant`
- `createdBy` / `updatedBy` — `String` (actor's username)

All entities must extend `BaseEntity`.

### Entity Convention

```java
@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;                           // internal PK, never exposed in APIs

@Column(name = "public_id", nullable = false, unique = true, updatable = false)
private UUID publicId = UUID.randomUUID(); // exposed in APIs
```

### PostgreSQL ENUM → Java Enum (AttributeConverter)

PostgreSQL native enums (e.g. `tag_type`, `tag_scope`, `tag_status`) are mapped via `@Converter(autoApply = true)`:

```java
// enums/TagTypeConverter.java
@Converter(autoApply = true)
public class TagTypeConverter implements AttributeConverter<TagType, String> {
    @Override
    public String convertToDatabaseColumn(TagType attr) {
        return attr == null ? null : attr.getValue();
    }
    @Override
    public TagType convertToEntityAttribute(String dbData) {
        return Arrays.stream(TagType.values())
            .filter(e -> e.getValue().equals(dbData))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown: " + dbData));
    }
}

// Entity field — must declare columnDefinition to reference the PG ENUM type
// AND @ColumnTransformer to force an explicit cast on write (see rules below)
@Column(name = "type", nullable = false, columnDefinition = "tag_type")
@ColumnTransformer(write = "?::tag_type")
private TagType type;
```

Rules:
- One converter class per enum, co-located in the same `enums/` package as the enum.
- `autoApply = true` — no `@Convert` annotation needed on entity fields.
- `columnDefinition = "pg_enum_name"` is mandatory on the `@Column` — without it Hibernate maps to VARCHAR and PostgreSQL rejects the cast.
- `@ColumnTransformer(write = "?::pg_enum_name")` (`org.hibernate.annotations.ColumnTransformer`) is **also mandatory** on every converted-enum field. `columnDefinition` only affects DDL generation (irrelevant here since `ddl-auto=none`) — it does NOT change how Hibernate binds the JDBC parameter at runtime. Without `@ColumnTransformer`, Hibernate sends the converted value as a plain `VARCHAR` on every INSERT/UPDATE, and PostgreSQL rejects the implicit `varchar → pg_enum` assignment cast with: `column "x" is of type x_enum but expression is of type character varying`. `@ColumnTransformer(write = "?::pg_enum_name")` makes Hibernate emit the explicit cast in the generated SQL (`?::tag_type`), which PostgreSQL accepts.
- Enum must expose `getValue()` returning the lowercase string stored in the DB.

### Boolean fields in entities

Always use `boolean` primitive (NOT `Boolean` wrapper) for NOT NULL boolean columns:

```java
// CORRECT — column is NOT NULL
@Column(name = "is_active", nullable = false)
private boolean isActive = true;

// WRONG — wrapper implies nullable, Lombok generates getIsActive() not isActive()
@Column(name = "is_active", nullable = false)
private Boolean isActive = true;
```

Use `Boolean` wrapper only when the DB column is genuinely nullable.

---

## Best Practices

### Project Structure

Package-by-feature under `domains/`. No global horizontal layers (`/controller`, `/service`, etc.).
Cross-cutting config goes in `config/`. Reusable cross-domain code goes in `common/`.

### Dependency Injection

Always constructor injection with `@RequiredArgsConstructor` + `final`.

### REST Controller

- Always return DTOs, never JPA entities.
- `ResponseEntity<SCGeneralResponseDto<T, K>>` using the record factory methods.
- `@Validated` on the class to validate `@RequestParam`/`@PathVariable`.
- `@Valid` on `@RequestBody` to validate DTOs.
- Thin controllers: zero business logic.

### Service Layer

- `@Transactional(readOnly = true)` for reads.
- `@Transactional` for writes.
- All business logic in the service, never in the controller.

### Repository

- `JpaRepository<Entity, Long>` as base.
- Derived queries for simple cases.
- `@Query` JPQL for complex queries.
- Avoid N+1: use `JOIN FETCH` or `@EntityGraph`.

### Service Interfaces (Javadoc)

Mandatory Javadoc on every interface:
- Class: general contract, whether it reads security context or receives explicit data, "succeed or throw" contract if applicable.
- Method: `@param`, `@return`, `@throws` with fully-qualified names (no extra imports just for docs).

### Exception Handling

Global handler: `SCExceptionHandler` (`@RestControllerAdvice` in `common/exception/`).
Custom exceptions extend `SCGeneralException`. Do not add handlers at controller level.

### Input Validation

Jakarta Validation on DTOs. Message constants in `SCRequestParamValidationCodeConstants`.

### Configuration

- Secrets always via env vars (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`, `${SERVER_PORT}`).
- `application.properties` contains no sensitive values.
- DB schema managed via `src/main/resources/sql/createSfrigolaDB.sql`, not by Hibernate.

### Logging

`@Slf4j` + SLF4J. Never log passwords, tokens, or personal data.

---

## Code Conventions

- Classes: `PascalCase`
- Methods/variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase`
- Cross-domain class prefix: `SC` (e.g. `SCUser`, `SCRole`, `SCGeneralException`)
- Commits: conventional commits in English (`feat:`, `fix:`, `refactor:`, etc.)
- Every PR must have at least one associated test

---

## Useful Commands

```bash
# Run locally
./mvnw spring-boot:run

# Build
./mvnw clean package -DskipTests

# Compile only (check for errors)
./mvnw compile

# Run tests
./mvnw test
```

---

## Notes for Claude

- Actual domain package: `com.sb.sfrigola_core.domains.<feature>` — not `shared/` as in older versions.
- Translation entities (e.g. `CategoryTranslation`, `TagTranslation`) live in the same domain as their parent entity (`domains/categories/entity/`), never in a separate `translations` domain — but they must `implements ISCTranslationEntity` (`common/interfaces`) so `ILanguageDomainBridgeService.buildTranslatedLanguagesMap`/`toSimpleLanguagesMap` work for them (see Translatable Entities Pattern).
- Security, auditing, web config: in `config/`, never inside a domain.
- Cross-cutting reusable code: in `common/`, not in a domain.
- `stats` has no controller: internal service called by `rating` and `favorite`.
- Every new API path must be registered in the correct bean in `SecurityBeansConfig`.
- `public_id` (UUID) is what gets exposed in APIs; `id` (Long) is internal DB only.
- `preferred_lang` in `SCUser` is a `String` (FK to `languages.code`), not a referenced entity.
- `ddl-auto=none` — any schema change requires updating the SQL file.
- Tag approval flow (`pending → approved/rejected`) is ROLE_ADMIN only.
- `tags/enums/` holds both the enum and its converter — keep them co-located, never split to `common/`.
- For any new PostgreSQL native ENUM: create the enum + `AttributeConverter` in the domain's `enums/` package; use `columnDefinition` on the entity `@Column`.
- Flag any request that violates these conventions before proceeding.